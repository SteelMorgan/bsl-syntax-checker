package com.github.steel33ff.mcpbsl.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.steel33ff.mcpbsl.dto.AnalyzeRequest
import com.github.steel33ff.mcpbsl.dto.FormatRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = [
    "path.mount.host-root=/tmp",
    "path.mount.container-root=/workspaces"
])
class BslApiControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
            inPlace = true
        )

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
        // When & Then
        mockMvc.perform(
            get("/api/path-info")
                .param("path", "/nonexistent/path.bsl")
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
