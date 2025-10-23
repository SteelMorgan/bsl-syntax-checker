package com.github.steel33ff.mcpbsl.service

import com.github.steel33ff.mcpbsl.config.PathMappingProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

/**
 * Service for host-to-container path mapping and validation
 */
@Service
class PathMappingService(
    private val properties: PathMappingProperties
) {

    /**
     * Translate host absolute path to container path
     * @throws PathMappingException if path is invalid or outside mounted root
     */
    fun translateToContainerPath(hostPath: String): Path {
        if (!properties.isEnabled()) {
            throw PathMappingException(
                "Path mapping is not configured. Set MOUNT_HOST_ROOT environment variable."
            )
        }

        val normalizedHostPath = Path(hostPath).normalize()
        val normalizedHostRoot = Path(properties.hostRoot).normalize()

        // Security check: path must be under host root
        if (!normalizedHostPath.startsWith(normalizedHostRoot)) {
            throw PathMappingException(
                "Path '$hostPath' is outside allowed root '${properties.hostRoot}'"
            )
        }

        // Calculate relative path and append to container root
        val relativePath = normalizedHostRoot.relativize(normalizedHostPath)
        val containerPath = Path(properties.containerRoot).resolve(relativePath).normalize()

        logger.debug { "Path translated: $hostPath -> $containerPath" }
        return containerPath
    }

    /**
     * Translate container path back to host path
     */
    fun translateToHostPath(containerPath: Path): Path {
        if (!properties.isEnabled()) {
            throw PathMappingException("Path mapping is not configured")
        }

        val normalizedContainerPath = containerPath.normalize()
        val containerRoot = Path(properties.containerRoot).normalize()

        if (!normalizedContainerPath.startsWith(containerRoot)) {
            throw PathMappingException(
                "Container path '$containerPath' is outside container root '${properties.containerRoot}'"
            )
        }

        val relativePath = containerRoot.relativize(normalizedContainerPath)
        val hostPath = Path(properties.hostRoot).resolve(relativePath).normalize()

        logger.debug { "Path translated: $containerPath -> $hostPath" }
        return hostPath
    }

    /**
     * Validate that path is accessible
     */
    fun validatePath(path: Path): Boolean {
        return try {
            path.isDirectory()
        } catch (e: Exception) {
            logger.warn(e) { "Path validation failed: $path" }
            false
        }
    }

    /**
     * Get normalized container path without host validation
     * (for use when already inside container)
     */
    fun getContainerPath(relativePath: String): Path {
        return Path(properties.containerRoot).resolve(relativePath).normalize()
    }
}

class PathMappingException(message: String) : RuntimeException(message)

