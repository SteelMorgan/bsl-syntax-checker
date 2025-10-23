package com.github.steel33ff.mcpbsl

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class McpBslServerApplicationTests {

    @Test
    fun contextLoads() {
        // Verify Spring context loads successfully
    }
}

