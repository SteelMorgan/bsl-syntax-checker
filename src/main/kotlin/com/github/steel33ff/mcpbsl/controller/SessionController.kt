package com.github.steel33ff.mcpbsl.controller

import com.github.steel33ff.mcpbsl.bsl.BslSessionPool
import com.github.steel33ff.mcpbsl.dto.*
import com.github.steel33ff.mcpbsl.service.PathMappingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Session management controller
 */
@RestController
@RequestMapping("/api/session")
@Tag(name = "Session", description = "BSL session management endpoints")
class SessionController(
    private val sessionPool: BslSessionPool,
    private val pathMappingService: PathMappingService
) {

    @PostMapping("/start")
    @Operation(
        summary = "Start new BSL session",
        description = "Create and start a new BSL Language Server session for a project"
    )
    fun startSession(@RequestBody request: SessionStartRequest): ResponseEntity<Any> {
        return try {
            val containerPath = pathMappingService.translateToContainerPath(request.projectPath)

            val session = sessionPool.createSession(containerPath)

            ResponseEntity.ok(
                SessionStartResponse(
                    sessionId = session.id,
                    project = containerPath.toString()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(error = e.message ?: "Failed to start session", code = "SESSION_START_FAILED"))
        }
    }

    @GetMapping("/status")
    @Operation(
        summary = "Get session status",
        description = "Query the status of a BSL session"
    )
    fun getSessionStatus(
        @Parameter(description = "Session ID", required = true)
        @RequestParam sessionId: String
    ): ResponseEntity<Any> {
        val session = sessionPool.getSession(sessionId)

        return if (session != null) {
            ResponseEntity.ok(
                SessionStatusResponse(
                    status = if (session.isAlive) "ready" else "stopped",
                    uptimeSeconds = session.getUptimeSeconds()
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(error = "Session not found", code = "SESSION_NOT_FOUND"))
        }
    }

    @PostMapping("/stop")
    @Operation(
        summary = "Stop BSL session",
        description = "Stop and remove a BSL session from the pool"
    )
    fun stopSession(
        @Parameter(description = "Session ID", required = true)
        @RequestParam sessionId: String
    ): ResponseEntity<Any> {
        val stopped = sessionPool.stopSession(sessionId)

        return if (stopped) {
            ResponseEntity.ok(SessionStopResponse(stopped = true))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(error = "Session not found", code = "SESSION_NOT_FOUND"))
        }
    }

    @GetMapping("/list")
    @Operation(
        summary = "List all active sessions",
        description = "Get list of all active BSL sessions"
    )
    fun listSessions(): ResponseEntity<Any> {
        val sessions = sessionPool.listSessions()
        return ResponseEntity.ok(sessions)
    }
}

