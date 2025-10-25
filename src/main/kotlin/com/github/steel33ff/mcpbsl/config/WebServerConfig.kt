package com.github.steel33ff.mcpbsl.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer

/**
 * Web server configuration that conditionally disables web server for stdio mode
 */
@Configuration
class WebServerConfig {

    /**
     * Disable web server for stdio transport mode
     */
    @Bean
    @ConditionalOnProperty(name = ["mcp.transport"], havingValue = "stdio")
    fun webServerFactoryCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            factory.setPort(-1) // Disable web server
        }
    }
}
