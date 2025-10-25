package com.github.steel33ff.mcpbsl.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web configuration for registering interceptors
 */
@Configuration
class WebConfig(
    private val loggingInterceptor: LoggingInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/actuator/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico",
                "/error"
            )
    }
}
