package com.github.steel33ff.mcpbsl.bsl

import com.github.steel33ff.mcpbsl.config.BslServerProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

/**
 * Pool for managing BSL Language Server sessions
 */
@Component
class BslSessionPool(
    private val properties: BslServerProperties
) : AutoCloseable {

    private val sessions = ConcurrentHashMap<String, BslSession>()
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor()

    init {
        // Schedule cleanup task
        cleanupExecutor.scheduleAtFixedRate(
            { cleanupExpiredSessions() },
            1,
            1,
            TimeUnit.MINUTES
        )
        logger.info { "BSL Session Pool initialized: maxSize=${properties.pool.maxSize}, ttl=${properties.pool.ttlMinutes}m" }
    }

    fun createSession(projectPath: Path): BslSession {
        val normalizedPath = projectPath.normalize().toString()

        if (sessions.size >= properties.pool.maxSize) {
            evictOldestSession()
        }

        val sessionId = UUID.randomUUID().toString()
        val session = BslSession(
            id = sessionId,
            projectPath = projectPath,
            jarPath = Path(properties.jarPath),
            maxHeap = properties.jvm.maxHeap,
            ttlMinutes = properties.pool.ttlMinutes
        )

        session.start()
        sessions[sessionId] = session

        logger.info { "Created BSL session: id=$sessionId, project=$normalizedPath" }
        return session
    }

    fun getSession(sessionId: String): BslSession? {
        return sessions[sessionId]?.also {
            it.updateLastAccess()
        }
    }

    fun stopSession(sessionId: String): Boolean {
        val session = sessions.remove(sessionId)
        return if (session != null) {
            session.close()
            logger.info { "Stopped BSL session: id=$sessionId" }
            true
        } else {
            logger.warn { "Session not found: id=$sessionId" }
            false
        }
    }

    fun getActiveSessionsCount(): Int = sessions.size

    fun listSessions(): List<SessionInfo> {
        return sessions.values.map { session ->
            SessionInfo(
                id = session.id,
                projectPath = session.projectPath.toString(),
                status = if (session.isAlive) "running" else "stopped",
                uptimeSeconds = session.getUptimeSeconds(),
                lastAccessTime = session.lastAccessTime
            )
        }
    }

    private fun cleanupExpiredSessions() {
        val now = Instant.now()
        val expiredSessions = sessions.values.filter { it.isExpired(now) }

        expiredSessions.forEach { session ->
            logger.info { "Cleaning up expired session: id=${session.id}" }
            sessions.remove(session.id)
            session.close()
        }
    }

    private fun evictOldestSession() {
        val oldest = sessions.values.minByOrNull { it.lastAccessTime }
        oldest?.let {
            logger.info { "Evicting oldest session to free up pool space: id=${it.id}" }
            sessions.remove(it.id)
            it.close()
        }
    }

    override fun close() {
        logger.info { "Shutting down BSL Session Pool" }

        cleanupExecutor.shutdown()
        cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)

        sessions.values.forEach { it.close() }
        sessions.clear()

        logger.info { "BSL Session Pool shut down" }
    }
}

data class BslSession(
    val id: String,
    val projectPath: Path,
    private val jarPath: Path,
    private val maxHeap: String,
    private val ttlMinutes: Long
) : AutoCloseable {

    private var process: BslProcess? = null
    private val startTime: Instant = Instant.now()
    var lastAccessTime: Instant = Instant.now()
        private set

    val isAlive: Boolean
        get() = process?.isAlive ?: false

    fun start() {
        if (isAlive) {
            logger.warn { "Session already started: id=$id" }
            return
        }

        process = BslProcess(
            jarPath = jarPath,
            maxHeap = maxHeap,
            workspaceDir = projectPath,
            mode = BslMode.LSP
        )
        process?.start()
    }

    fun updateLastAccess() {
        lastAccessTime = Instant.now()
    }

    fun getUptimeSeconds(): Long {
        return java.time.Duration.between(startTime, Instant.now()).seconds
    }

    fun isExpired(now: Instant): Boolean {
        val idleMinutes = java.time.Duration.between(lastAccessTime, now).toMinutes()
        return idleMinutes >= ttlMinutes
    }

    fun sendRequest(request: String): String? {
        updateLastAccess()
        return process?.sendRequest(request)
    }

    override fun close() {
        process?.close()
        process = null
    }
}

data class SessionInfo(
    val id: String,
    val projectPath: String,
    val status: String,
    val uptimeSeconds: Long,
    val lastAccessTime: Instant
)

