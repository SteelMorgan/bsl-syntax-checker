package com.github.steel33ff.mcpbsl.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

/**
 * Service for determining path types and providing path-related utilities
 */
@Service
@OptIn(ExperimentalPathApi::class)
class PathTypeService {

    /**
     * Determine if the path is a BSL file
     */
    fun isBslFile(path: Path): Boolean {
        return try {
            path.isRegularFile() && path.extension.lowercase() in listOf("bsl", "os")
        } catch (e: Exception) {
            logger.warn(e) { "Failed to check if path is BSL file: $path" }
            false
        }
    }

    /**
     * Determine if the path is a directory containing BSL files
     */
    fun isBslDirectory(path: Path): Boolean {
        return try {
            if (!path.isDirectory()) return false
            
            // Check if directory contains BSL files
            path.listDirectoryEntries()
                .any { it.isRegularFile() && it.extension.lowercase() in listOf("bsl", "os") }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to check if path is BSL directory: $path" }
            false
        }
    }

    /**
     * Get path type information
     */
    fun getPathType(path: Path): PathType {
        return try {
            when {
                !path.exists() -> PathType.NOT_FOUND
                path.isRegularFile() -> {
                    if (isBslFile(path)) PathType.BSL_FILE else PathType.FILE
                }
                path.isDirectory() -> {
                    if (isBslDirectory(path)) PathType.BSL_DIRECTORY else PathType.DIRECTORY
                }
                else -> PathType.UNKNOWN
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to determine path type: $path" }
            PathType.UNKNOWN
        }
    }

    /**
     * Find all BSL files in a directory recursively
     */
    fun findBslFiles(directory: Path): List<Path> {
        return try {
            if (!directory.isDirectory()) return emptyList()
            
            directory.walk()
                .filter { it.isRegularFile() && it.extension.lowercase() in listOf("bsl", "os") }
                .toList()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to find BSL files in directory: $directory" }
            emptyList()
        }
    }

    /**
     * Get file size in bytes
     */
    fun getFileSize(path: Path): Long {
        return try {
            if (path.isRegularFile()) path.fileSize() else 0L
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get file size: $path" }
            0L
        }
    }

    /**
     * Get directory size in bytes (sum of all files)
     */
    fun getDirectorySize(directory: Path): Long {
        return try {
            if (!directory.isDirectory()) return 0L
            
            directory.walk()
                .filter { it.isRegularFile() }
                .sumOf { it.fileSize() }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get directory size: $directory" }
            0L
        }
    }
}

/**
 * Path type enumeration
 */
enum class PathType {
    BSL_FILE,           // BSL source file (.bsl, .os)
    BSL_DIRECTORY,      // Directory containing BSL files
    FILE,               // Regular file (not BSL)
    DIRECTORY,          // Regular directory (no BSL files)
    NOT_FOUND,          // Path does not exist
    UNKNOWN             // Unknown or inaccessible path
}
