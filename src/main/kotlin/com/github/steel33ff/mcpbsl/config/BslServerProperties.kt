package com.github.steel33ff.mcpbsl.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for BSL Language Server
 */
@ConfigurationProperties(prefix = "bsl")
data class BslServerProperties(
    val jarPath: String = "/opt/bsl/bsl-language-server.jar",
    val jvm: JvmProperties = JvmProperties(),
    val pool: PoolProperties = PoolProperties()
) {
    data class JvmProperties(
        val maxHeap: String = "4g"
    )

    data class PoolProperties(
        val maxSize: Int = 5,
        val ttlMinutes: Long = 60
    )
}

