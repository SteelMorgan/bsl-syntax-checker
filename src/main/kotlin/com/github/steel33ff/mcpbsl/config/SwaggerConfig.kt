package com.github.steel33ff.mcpbsl.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger/OpenAPI configuration
 */
@Configuration
class SwaggerConfig {

    @Value("\${server.port:9090}")
    private lateinit var serverPort: String

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("MCP BSL Server API")
                    .description("Model Context Protocol interface for BSL Language Server. Provides analyze, format, and session management endpoints.")
                    .version("0.1.0")
                    .license(License().name("MIT").url("https://opensource.org/licenses/MIT"))
            )
            .servers(
                listOf(
                    Server().url("http://localhost:$serverPort").description("Local server")
                )
            )
    }
}

