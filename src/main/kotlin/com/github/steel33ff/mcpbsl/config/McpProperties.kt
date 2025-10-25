package com.github.steel33ff.mcpbsl.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * MCP transport configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "mcp")
data class McpProperties(
    /**
     * MCP transport mode: stdio, http, sse, ndjson
     */
    var transport: String = "stdio",
    
    /**
     * Port for HTTP-based transports (http, sse, ndjson)
     * Not used for stdio mode
     */
    var port: Int = 9090
) {
    enum class Transport {
        STDIO,
        HTTP,
        SSE,
        NDJSON;
        
        companion object {
            fun fromString(value: String): Transport {
                return valueOf(value.uppercase())
            }
        }
    }
    
    fun getTransportMode(): Transport {
        return Transport.fromString(transport)
    }
    
    fun isStdio(): Boolean = getTransportMode() == Transport.STDIO
    fun isHttp(): Boolean = getTransportMode() == Transport.HTTP
    fun isSse(): Boolean = getTransportMode() == Transport.SSE
    fun isNdjson(): Boolean = getTransportMode() == Transport.NDJSON
    
    fun requiresHttpServer(): Boolean = isHttp() || isSse() || isNdjson()
}

