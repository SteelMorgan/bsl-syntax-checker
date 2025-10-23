package com.github.steel33ff.mcpbsl.service

import com.github.steel33ff.mcpbsl.config.PathMappingProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path

class PathMappingServiceTest {

    @Test
    fun `translateToContainerPath should translate valid path`() {
        // Arrange
        val properties = PathMappingProperties(
            hostRoot = "D:\\Projects\\1C",
            containerRoot = "/workspaces"
        )
        val service = PathMappingService(properties)

        // Act
        val result = service.translateToContainerPath("D:\\Projects\\1C\\MyProject")

        // Assert
        assertEquals("/workspaces/MyProject", result.toString().replace("\\", "/"))
    }

    @Test
    fun `translateToContainerPath should reject path outside root`() {
        // Arrange
        val properties = PathMappingProperties(
            hostRoot = "D:\\Projects\\1C",
            containerRoot = "/workspaces"
        )
        val service = PathMappingService(properties)

        // Act & Assert
        assertThrows<PathMappingException> {
            service.translateToContainerPath("C:\\Windows\\System32")
        }
    }

    @Test
    fun `translateToContainerPath should throw when mapping not configured`() {
        // Arrange
        val properties = PathMappingProperties(
            hostRoot = "",
            containerRoot = "/workspaces"
        )
        val service = PathMappingService(properties)

        // Act & Assert
        val exception = assertThrows<PathMappingException> {
            service.translateToContainerPath("D:\\Projects\\MyProject")
        }
        assertTrue(exception.message!!.contains("not configured"))
    }

    @Test
    fun `translateToHostPath should translate container path back`() {
        // Arrange
        val properties = PathMappingProperties(
            hostRoot = "D:\\Projects\\1C",
            containerRoot = "/workspaces"
        )
        val service = PathMappingService(properties)

        // Act
        val result = service.translateToHostPath(Path("/workspaces/MyProject"))

        // Assert
        assertTrue(result.toString().contains("Projects"))
        assertTrue(result.toString().contains("MyProject"))
    }

    @Test
    fun `translateToHostPath should reject path outside container root`() {
        // Arrange
        val properties = PathMappingProperties(
            hostRoot = "D:\\Projects\\1C",
            containerRoot = "/workspaces"
        )
        val service = PathMappingService(properties)

        // Act & Assert
        assertThrows<PathMappingException> {
            service.translateToHostPath(Path("/etc/passwd"))
        }
    }

    @Test
    fun `getContainerPath should return normalized path`() {
        // Arrange
        val properties = PathMappingProperties(
            hostRoot = "",
            containerRoot = "/workspaces"
        )
        val service = PathMappingService(properties)

        // Act
        val result = service.getContainerPath("project1/src")

        // Assert
        assertEquals("/workspaces/project1/src", result.toString().replace("\\", "/"))
    }

    @Test
    fun `isEnabled should return true when hostRoot is set`() {
        // Arrange
        val properties = PathMappingProperties(
            hostRoot = "D:\\Projects",
            containerRoot = "/workspaces"
        )

        // Act & Assert
        assertTrue(properties.isEnabled())
    }

    @Test
    fun `isEnabled should return false when hostRoot is empty`() {
        // Arrange
        val properties = PathMappingProperties(
            hostRoot = "",
            containerRoot = "/workspaces"
        )

        // Act & Assert
        assertFalse(properties.isEnabled())
    }
}

