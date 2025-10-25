package com.github.steel33ff.mcpbsl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * MCP BSL Server - Spring Boot application
 * 
 * Provides MCP (Model Context Protocol) interface for BSL Language Server
 * with support for stdio, HTTP, SSE, and NDJSON transports.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class McpBslServerApplication

fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    logger.info { "Starting MCP BSL Server - testing Loki logging" }
    logger.warn { "This is a WARNING log to test Loki" }
    logger.error { "This is an ERROR log to test Loki" }
    runApplication<McpBslServerApplication>(*args)
}

