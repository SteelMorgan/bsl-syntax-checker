package com.github.steel33ff.mcpbsl.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for host-to-container path mapping
 */
@ConfigurationProperties(prefix = "path.mount")
data class PathMappingProperties(
    val hostRoot: String = "",
    val containerRoot: String = "/workspaces"
) {
    fun isEnabled(): Boolean = hostRoot.isNotBlank()
}

