package com.github.steel33ff.mcpbsl

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
    runApplication<McpBslServerApplication>(*args)
}

