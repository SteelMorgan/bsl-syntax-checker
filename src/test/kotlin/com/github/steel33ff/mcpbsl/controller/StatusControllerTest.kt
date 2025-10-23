package com.github.steel33ff.mcpbsl.controller

import com.github.steel33ff.mcpbsl.config.McpProperties
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(StatusController::class)
@Import(McpProperties::class)
@ActiveProfiles("test")
class StatusControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `getStatus should return server status`() {
        mockMvc.perform(get("/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("running"))
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.version").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.deployment").value("docker-only"))
            .andExpect(jsonPath("$.mcp.transport").value("stdio"))
    }

    @Test
    fun `health should return UP status`() {
        mockMvc.perform(get("/status/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
}

