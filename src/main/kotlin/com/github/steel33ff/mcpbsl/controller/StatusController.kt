package com.github.steel33ff.mcpbsl.controller

import com.github.steel33ff.mcpbsl.config.McpProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Status and health check endpoints
 */
@RestController
@RequestMapping("/status")
@Tag(name = "Status", description = "Server status and health check endpoints")
class StatusController(
    private val buildProperties: BuildProperties?,
    private val mcpProperties: McpProperties
) {

    @GetMapping
    @Operation(summary = "Get server status", description = "Returns basic server status information including MCP transport mode")
    fun getStatus(): Map<String, Any> {
        return mapOf(
            "status" to "running",
            "timestamp" to Instant.now().toString(),
            "version" to (buildProperties?.version ?: "unknown"),
            "name" to (buildProperties?.name ?: "mcp-bsl-server"),
            "deployment" to "docker-only",
            "mcp" to mapOf(
                "transport" to mcpProperties.transport.lowercase(),
                "port" to if (mcpProperties.requiresHttpServer()) mcpProperties.port else null,
                "requiresHttpServer" to mcpProperties.requiresHttpServer()
            ),
                "webUI" to mapOf(
                    "swagger" to "/swagger-ui/index.html",
                    "actuator" to "/actuator",
                    "prometheus" to "/actuator/prometheus",
                    "grafana" to "http://localhost:3000"
                )
        )
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    fun health(): Map<String, String> {
        return mapOf("status" to "UP")
    }
}

