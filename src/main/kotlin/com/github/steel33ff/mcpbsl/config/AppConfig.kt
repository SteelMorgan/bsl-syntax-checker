package com.github.steel33ff.mcpbsl.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

/**
 * General application configuration
 */
@Configuration
class AppConfig {

    @Bean
    @ConditionalOnMissingBean(BuildProperties::class)
    fun buildProperties(): BuildProperties {
        val properties = Properties()
        properties["name"] = "mcp-bsl-server"
        properties["version"] = "0.1.0-SNAPSHOT"
        properties["group"] = "com.github.steel33ff"
        properties["artifact"] = "mcp-bsl-server"
        return BuildProperties(properties)
    }
}

