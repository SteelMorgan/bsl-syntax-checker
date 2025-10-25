package com.github.steel33ff.mcpbsl.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.steel33ff.mcpbsl.dto.AnalyzeRequest
import com.github.steel33ff.mcpbsl.dto.FormatRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.nio.file.Files
import java.nio.file.Path
import com.github.steel33ff.mcpbsl.bsl.BslCliService
import com.github.steel33ff.mcpbsl.service.PathMappingService
import com.github.steel33ff.mcpbsl.service.PathTypeService
import com.github.steel33ff.mcpbsl.service.PathType
import com.github.steel33ff.mcpbsl.bsl.AnalyzeResult
import com.github.steel33ff.mcpbsl.bsl.FormatResult
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any

@WebMvcTest(BslApiController::class)
@TestPropertySource(properties = [
    "path.mount.host-root=/tmp",
    "path.mount.container-root=/workspaces"
])
class BslApiControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var bslCliService: BslCliService

    @MockBean
    private lateinit var pathMappingService: PathMappingService

    @MockBean
    private lateinit var pathTypeService: PathTypeService

    @Test
    fun `should analyze BSL file`(@TempDir tempDir: Path) {
        // Given
        val bslFile = tempDir.resolve("test.bsl")
        Files.writeString(bslFile, "Процедура Тест()\nКонецПроцедуры")
        
        val request = AnalyzeRequest(
            srcDir = bslFile.toString(),
            reporters = listOf("json"),
            language = "ru"
        )

        // Mock services
        doReturn(bslFile).`when`(pathMappingService).translateToContainerPath(bslFile.toString())
        doReturn(true).`when`(pathMappingService).validatePath(bslFile)
        doReturn(PathType.BSL_FILE).`when`(pathTypeService).getPathType(bslFile)
        doReturn(
            AnalyzeResult.success(mapOf(
                "summary" to mapOf(
                    "errors" to 0,
                    "warnings" to 0,
                    "info" to 0,
                    "total" to 0
                ),
                "diagnostics" to emptyList<Map<String, Any>>()
            ))
        ).`when`(bslCliService).analyze(bslFile, listOf("json"), "ru")

        // When & Then
        mockMvc.perform(
            post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summary").exists())
            .andExpect(jsonPath("$.diagnostics").exists())
    }

    @Test
    fun `should analyze BSL directory`(@TempDir tempDir: Path) {
        // Given
        val bslFile1 = tempDir.resolve("test1.bsl")
        val bslFile2 = tempDir.resolve("test2.bsl")
        Files.writeString(bslFile1, "Процедура Тест1()\nКонецПроцедуры")
        Files.writeString(bslFile2, "Процедура Тест2()\nКонецПроцедуры")
        
        val request = AnalyzeRequest(
            srcDir = tempDir.toString(),
            reporters = listOf("json"),
            language = "ru"
        )

        // Mock services
        doReturn(tempDir).`when`(pathMappingService).translateToContainerPath(tempDir.toString())
        doReturn(true).`when`(pathMappingService).validatePath(tempDir)
        doReturn(PathType.BSL_DIRECTORY).`when`(pathTypeService).getPathType(tempDir)
        doReturn(
            AnalyzeResult.success(mapOf(
                "summary" to mapOf(
                    "errors" to 0,
                    "warnings" to 0,
                    "info" to 0,
                    "total" to 0
                ),
                "diagnostics" to emptyList<Map<String, Any>>()
            ))
        ).`when`(bslCliService).analyze(tempDir, listOf("json"), "ru")

        // When & Then
        mockMvc.perform(
            post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summary").exists())
            .andExpect(jsonPath("$.diagnostics").exists())
    }

    @Test
    fun `should format BSL file`(@TempDir tempDir: Path) {
        // Given
        val bslFile = tempDir.resolve("test.bsl")
        Files.writeString(bslFile, "Процедура Тест()\nКонецПроцедуры")
        
        val request = FormatRequest(
            src = bslFile.toString(),
            inPlace = false
        )

        // Mock services
        doReturn(bslFile).`when`(pathMappingService).translateToContainerPath(bslFile.toString())
        doReturn(true).`when`(pathMappingService).validatePath(bslFile)
        doReturn(PathType.BSL_FILE).`when`(pathTypeService).getPathType(bslFile)
        doReturn(
            FormatResult.success(mapOf(
                "formatted" to true,
                "filesChanged" to 1,
                "files" to listOf(
                    mapOf(
                        "file" to "test.bsl",
                        "formatted" to true,
                        "hasContent" to false
                    )
                )
            ))
        ).`when`(bslCliService).format(bslFile, false)

        // When & Then
        mockMvc.perform(
            post("/api/format")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.formatted").exists())
            .andExpect(jsonPath("$.filesChanged").exists())
    }

    @Test
    fun `should get path info for BSL file`(@TempDir tempDir: Path) {
        // Given
        val bslFile = tempDir.resolve("test.bsl")
        Files.writeString(bslFile, "Процедура Тест()\nКонецПроцедуры")

        // Mock services
        doReturn(bslFile).`when`(pathMappingService).translateToContainerPath(bslFile.toString())
        doReturn(PathType.BSL_FILE).`when`(pathTypeService).getPathType(bslFile)
        doReturn(100L).`when`(pathTypeService).getFileSize(bslFile)

        // When & Then
        mockMvc.perform(
            get("/api/path-info")
                .param("path", bslFile.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("BSL_FILE"))
            .andExpect(jsonPath("$.exists").value(true))
            .andExpect(jsonPath("$.isBsl").value(true))
            .andExpect(jsonPath("$.sizeBytes").exists())
            .andExpect(jsonPath("$.containerPath").exists())
    }

    @Test
    fun `should get path info for BSL directory`(@TempDir tempDir: Path) {
        // Given
        val bslFile1 = tempDir.resolve("test1.bsl")
        val bslFile2 = tempDir.resolve("test2.bsl")
        Files.writeString(bslFile1, "Процедура Тест1()\nКонецПроцедуры")
        Files.writeString(bslFile2, "Процедура Тест2()\nКонецПроцедуры")

        // Mock services
        doReturn(tempDir).`when`(pathMappingService).translateToContainerPath(tempDir.toString())
        doReturn(PathType.BSL_DIRECTORY).`when`(pathTypeService).getPathType(tempDir)
        doReturn(200L).`when`(pathTypeService).getDirectorySize(tempDir)
        doReturn(listOf(bslFile1, bslFile2)).`when`(pathTypeService).findBslFiles(tempDir)

        // When & Then
        mockMvc.perform(
            get("/api/path-info")
                .param("path", tempDir.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("BSL_DIRECTORY"))
            .andExpect(jsonPath("$.exists").value(true))
            .andExpect(jsonPath("$.isBsl").value(true))
            .andExpect(jsonPath("$.bslFileCount").value(2))
            .andExpect(jsonPath("$.containerPath").exists())
    }

    @Test
    fun `should return NOT_FOUND for non-existent path`() {
        // Given
        val nonExistentPath = "/nonexistent/path.bsl"
        val nonExistentPathObj = kotlin.io.path.Path(nonExistentPath)

        // Mock services
        doReturn(nonExistentPathObj).`when`(pathMappingService).translateToContainerPath(nonExistentPath)
        doReturn(PathType.NOT_FOUND).`when`(pathTypeService).getPathType(nonExistentPathObj)

        // When & Then
        mockMvc.perform(
            get("/api/path-info")
                .param("path", nonExistentPath)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("NOT_FOUND"))
            .andExpect(jsonPath("$.exists").value(false))
            .andExpect(jsonPath("$.isBsl").value(false))
    }

    @Test
    fun `should return error for analyze with non-existent path`() {
        // Given
        val request = AnalyzeRequest(
            srcDir = "/nonexistent/path.bsl",
            reporters = listOf("json"),
            language = "ru"
        )

        // When & Then
        mockMvc.perform(
            post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.code").value("PATH_NOT_FOUND"))
    }

    @Test
    fun `should return error for format with non-existent path`() {
        // Given
        val request = FormatRequest(
            src = "/nonexistent/path.bsl",
            inPlace = true
        )

        // When & Then
        mockMvc.perform(
            post("/api/format")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.code").value("PATH_NOT_FOUND"))
    }
}
