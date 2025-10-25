package com.github.steel33ff.mcpbsl.controller

import com.github.steel33ff.mcpbsl.bsl.AnalyzeResult
import com.github.steel33ff.mcpbsl.bsl.BslCliService
import com.github.steel33ff.mcpbsl.bsl.FormatResult
import com.github.steel33ff.mcpbsl.service.PathMappingService
import com.github.steel33ff.mcpbsl.service.PathTypeService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.nio.file.Path
import kotlin.io.path.Path

@WebMvcTest(BslApiController::class)
@ActiveProfiles("test")
class BslApiControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun bslCliService(): BslCliService = mockk(relaxed = true)

        @Bean
        fun pathMappingService(): PathMappingService = mockk(relaxed = true)

        @Bean
        fun pathTypeService(): PathTypeService = mockk(relaxed = true)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var bslCliService: BslCliService

    @Autowired
    private lateinit var pathMappingService: PathMappingService

    @Test
    fun `analyze should return success response`() {
        // Arrange
        val containerPath = Path("/workspaces/test-project")
        val analyzeResult = AnalyzeResult.success(
            mapOf(
                "summary" to mapOf("errors" to 5, "warnings" to 12),
                "diagnostics" to emptyList<Map<String, Any>>()
            )
        )

        every { pathMappingService.translateToContainerPath(any()) } returns containerPath
        every { bslCliService.analyze(any(), any(), any()) } returns analyzeResult

        val requestBody = """
            {
                "srcDir": "D:\\Projects\\TestProject",
                "reporters": ["json"],
                "language": "ru"
            }
        """.trimIndent()

        // Act & Assert
        mockMvc.perform(
            post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summary.errors").value(5))
            .andExpect(jsonPath("$.summary.warnings").value(12))
            .andExpect(jsonPath("$.diagnostics").isArray)

        verify { pathMappingService.translateToContainerPath("D:\\Projects\\TestProject") }
        verify { bslCliService.analyze(containerPath, listOf("json"), "ru") }
    }

    @Test
    fun `analyze should return error on invalid path`() {
        // Arrange
        every { pathMappingService.translateToContainerPath(any()) } throws 
            IllegalArgumentException("Path outside mounted root")

        val requestBody = """
            {
                "srcDir": "C:\\Invalid\\Path",
                "reporters": ["json"],
                "language": "ru"
            }
        """.trimIndent()

        // Act & Assert
        mockMvc.perform(
            post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `format should return success response`() {
        // Arrange
        val containerPath = Path("/workspaces/test-project/Module.bsl")
        val formatResult = FormatResult.success(
            mapOf(
                "formatted" to true,
                "filesChanged" to 3
            )
        )

        every { pathMappingService.translateToContainerPath(any()) } returns containerPath
        every { pathMappingService.validatePath(any()) } returns true
        every { bslCliService.format(any(), any()) } returns formatResult

        val requestBody = """
            {
                "src": "D:\\Projects\\TestProject\\Module.bsl",
                "inPlace": false
            }
        """.trimIndent()

        // Act & Assert
        mockMvc.perform(
            post("/api/format")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.formatted").value(true))
            .andExpect(jsonPath("$.filesChanged").value(3))

        verify { pathMappingService.translateToContainerPath("D:\\Projects\\TestProject\\Module.bsl") }
        verify { bslCliService.format(containerPath, false) }
    }

    @Test
    fun `format should return error when path is not writable for inPlace formatting`() {
        // Arrange
        val containerPath = Path("/workspaces/test-project/Module.bsl")
        
        every { pathMappingService.translateToContainerPath(any()) } returns containerPath
        every { pathMappingService.validatePath(any()) } returns true

        val requestBody = """
            {
                "src": "D:\\Projects\\TestProject\\Module.bsl",
                "inPlace": true
            }
        """.trimIndent()

        // Act & Assert
        mockMvc.perform(
            post("/api/format")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Path 'D:\\Projects\\TestProject\\Module.bsl' is not writable. Check Docker volume permissions."))
            .andExpect(jsonPath("$.code").value("PATH_NOT_WRITABLE"))

        verify { pathMappingService.translateToContainerPath("D:\\Projects\\TestProject\\Module.bsl") }
    }

    @Test
    fun `analyze should handle analysis failure`() {
        // Arrange
        val containerPath = Path("/workspaces/test-project")
        val analyzeResult = AnalyzeResult.failure("BSL LS execution failed")

        every { pathMappingService.translateToContainerPath(any()) } returns containerPath
        every { bslCliService.analyze(any(), any(), any()) } returns analyzeResult

        val requestBody = """
            {
                "srcDir": "D:\\Projects\\TestProject",
                "reporters": ["json"],
                "language": "ru"
            }
        """.trimIndent()

        // Act & Assert
        mockMvc.perform(
            post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("BSL LS execution failed"))
    }
}

