package com.github.steel33ff.mcpbsl.controller

import com.github.steel33ff.mcpbsl.bsl.BslSession
import com.github.steel33ff.mcpbsl.bsl.BslSessionPool
import com.github.steel33ff.mcpbsl.service.PathMappingService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.nio.file.Path
import kotlin.io.path.Path

@WebMvcTest(SessionController::class)
@ActiveProfiles("test")
class SessionControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun sessionPool(): BslSessionPool = mockk(relaxed = true)

        @Bean
        fun pathMappingService(): PathMappingService = mockk(relaxed = true)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var sessionPool: BslSessionPool

    @Autowired
    private lateinit var pathMappingService: PathMappingService

    @Test
    fun `startSession should create new session`() {
        // Arrange
        val containerPath = Path("/workspaces/test-project")
        val mockSession = mockk<BslSession> {
            every { id } returns "test-session-id"
            every { projectPath } returns containerPath
        }

        every { pathMappingService.translateToContainerPath(any()) } returns containerPath
        every { sessionPool.createSession(any()) } returns mockSession

        val requestBody = """
            {
                "projectPath": "D:\\Projects\\TestProject"
            }
        """.trimIndent()

        // Act & Assert
        mockMvc.perform(
            post("/api/session/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionId").value("test-session-id"))
            .andExpect(jsonPath("$.project").exists())

        verify { sessionPool.createSession(containerPath) }
    }

    @Test
    fun `getSessionStatus should return status for existing session`() {
        // Arrange
        val mockSession = mockk<BslSession> {
            every { isAlive } returns true
            every { getUptimeSeconds() } returns 120L
        }

        every { sessionPool.getSession("test-session-id") } returns mockSession

        // Act & Assert
        mockMvc.perform(
            get("/api/session/status")
                .param("sessionId", "test-session-id")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ready"))
            .andExpect(jsonPath("$.uptimeSeconds").value(120))
    }

    @Test
    fun `getSessionStatus should return 404 for non-existing session`() {
        // Arrange
        every { sessionPool.getSession(any()) } returns null

        // Act & Assert
        mockMvc.perform(
            get("/api/session/status")
                .param("sessionId", "non-existing-id")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Session not found"))
    }

    @Test
    fun `stopSession should stop existing session`() {
        // Arrange
        every { sessionPool.stopSession("test-session-id") } returns true

        // Act & Assert
        mockMvc.perform(
            post("/api/session/stop")
                .param("sessionId", "test-session-id")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.stopped").value(true))

        verify { sessionPool.stopSession("test-session-id") }
    }

    @Test
    fun `stopSession should return 404 for non-existing session`() {
        // Arrange
        every { sessionPool.stopSession(any()) } returns false

        // Act & Assert
        mockMvc.perform(
            post("/api/session/stop")
                .param("sessionId", "non-existing-id")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Session not found"))
    }
}

