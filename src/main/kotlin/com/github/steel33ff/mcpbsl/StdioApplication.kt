package com.github.steel33ff.mcpbsl

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile

/**
 * Spring Boot application for stdio mode (no web server)
 */
@SpringBootApplication(
    exclude = [
        WebMvcAutoConfiguration::class,
        ErrorMvcAutoConfiguration::class
    ]
)
@Profile("stdio")
class StdioApplication

fun main(args: Array<String>) {
    runApplication<StdioApplication>(*args) {
        setAdditionalProfiles("stdio")
    }
}
