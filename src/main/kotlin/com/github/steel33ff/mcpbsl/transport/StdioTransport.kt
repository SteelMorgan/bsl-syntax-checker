package com.github.steel33ff.mcpbsl.transport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.steel33ff.mcpbsl.bsl.AnalyzeResult
import com.github.steel33ff.mcpbsl.bsl.BslCliService
import com.github.steel33ff.mcpbsl.bsl.BslSessionPool
import com.github.steel33ff.mcpbsl.bsl.FormatResult
import com.github.steel33ff.mcpbsl.service.PathMappingService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}

/**
 * MCP stdio transport implementation
 * 
 * Reads JSON requests from stdin, processes them, and writes JSON responses to stdout.
 * Activated when running with --stdio or when TRANSPORT_MODE=stdio
 */
@Component
@ConditionalOnProperty(name = ["transport.mode"], havingValue = "stdio", matchIfMissing = false)
class StdioTransport(
    private val bslCliService: BslCliService,
    private val sessionPool: BslSessionPool,
    private val pathMappingService: PathMappingService
) : CommandLineRunner {

    private val objectMapper = jacksonObjectMapper()

    override fun run(vararg args: String) {
        logger.info { "Starting MCP stdio transport mode" }

        val reader = BufferedReader(InputStreamReader(System.`in`))

        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        processRequest(line)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Stdio transport error" }
            writeError("Transport error: ${e.message}")
        }

        logger.info { "Stdio transport shutting down" }
    }

    private fun processRequest(jsonLine: String) {
        try {
            val request = objectMapper.readValue<Map<String, Any>>(jsonLine)
            val tool = request["tool"] as? String ?: run {
                writeError("Missing 'tool' field in request")
                return
            }

            val params = request["params"] as? Map<String, Any> ?: emptyMap()

            logger.debug { "Processing tool: $tool" }

            val response = when (tool) {
                "bslcheck_analyze" -> handleAnalyze(params)
                "bslcheck_format" -> handleFormat(params)
                "bslcheck_session_start" -> handleSessionStart(params)
                "bslcheck_session_status" -> handleSessionStatus(params)
                "bslcheck_session_stop" -> handleSessionStop(params)
                else -> mapOf("error" to "Unknown tool: $tool")
            }

            writeResponse(response)
        } catch (e: Exception) {
            logger.error(e) { "Error processing request: $jsonLine" }
            writeError("Request processing error: ${e.message}")
        }
    }

    private fun handleAnalyze(params: Map<String, Any>): Map<String, Any> {
        val srcDir = params["srcDir"] as? String ?: return mapOf("error" to "Missing srcDir parameter")
        val reporters = (params["reporters"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("json")
        val language = params["language"] as? String ?: "ru"

        return try {
            val containerPath = pathMappingService.translateToContainerPath(srcDir)
            val result = bslCliService.analyze(containerPath, reporters, language)

            when (result) {
                is AnalyzeResult.Success -> mapOf(
                    "status" to "success",
                    "data" to result.data
                )
                is AnalyzeResult.Failure -> mapOf(
                    "status" to "error",
                    "error" to result.error
                )
            }
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Analysis failed"))
        }
    }

    private fun handleFormat(params: Map<String, Any>): Map<String, Any> {
        val src = params["src"] as? String ?: return mapOf("error" to "Missing src parameter")
        val inPlace = params["inPlace"] as? Boolean ?: true

        return try {
            val containerPath = pathMappingService.translateToContainerPath(src)
            val result = bslCliService.format(containerPath, inPlace)

            when (result) {
                is FormatResult.Success -> mapOf(
                    "status" to "success",
                    "data" to result.data
                )
                is FormatResult.Failure -> mapOf(
                    "status" to "error",
                    "error" to result.error
                )
            }
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Format failed"))
        }
    }

    private fun handleSessionStart(params: Map<String, Any>): Map<String, Any> {
        val projectPath = params["projectPath"] as? String ?: return mapOf("error" to "Missing projectPath parameter")

        return try {
            val containerPath = pathMappingService.translateToContainerPath(projectPath)
            val session = sessionPool.createSession(containerPath)

            mapOf(
                "status" to "success",
                "sessionId" to session.id,
                "project" to containerPath.toString()
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Session start failed"))
        }
    }

    private fun handleSessionStatus(params: Map<String, Any>): Map<String, Any> {
        val sessionId = params["sessionId"] as? String ?: return mapOf("error" to "Missing sessionId parameter")

        val session = sessionPool.getSession(sessionId)
        return if (session != null) {
            mapOf(
                "status" to if (session.isAlive) "ready" else "stopped",
                "uptimeSeconds" to session.getUptimeSeconds()
            )
        } else {
            mapOf("error" to "Session not found")
        }
    }

    private fun handleSessionStop(params: Map<String, Any>): Map<String, Any> {
        val sessionId = params["sessionId"] as? String ?: return mapOf("error" to "Missing sessionId parameter")

        val stopped = sessionPool.stopSession(sessionId)
        return mapOf(
            "status" to if (stopped) "success" else "error",
            "stopped" to stopped
        )
    }

    private fun writeResponse(response: Map<String, Any>) {
        val json = objectMapper.writeValueAsString(response)
        println(json)
        System.out.flush()
    }

    private fun writeError(message: String) {
        writeResponse(mapOf("error" to message))
    }
}

