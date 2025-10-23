package com.github.steel33ff.mcpbsl.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * CORS configuration for MCP BSL Server
 * 
 * Enables Cross-Origin Resource Sharing for:
 * - Swagger UI to access API endpoints
 * - Web clients to interact with the server
 * - Development and testing scenarios
 */
@Configuration
class CorsConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
        
        registry.addMapping("/actuator/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
        
        registry.addMapping("/status/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        
        // Allow all origins for development (can be restricted in production)
        configuration.allowedOriginPatterns = listOf("*")
        
        // Allow common HTTP methods
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        
        // Allow all headers
        configuration.allowedHeaders = listOf("*")
        
        // Allow credentials (cookies, authorization headers)
        configuration.allowCredentials = true
        
        // Cache preflight response for 1 hour
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        
        // Apply CORS to all API endpoints
        source.registerCorsConfiguration("/api/**", configuration)
        source.registerCorsConfiguration("/actuator/**", configuration)
        source.registerCorsConfiguration("/status/**", configuration)
        
        return source
    }
}
