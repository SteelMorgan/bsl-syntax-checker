package com.github.steel33ff.mcpbsl.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * JSON utilities for MCP
 */
object JsonUtils {
    val mapper = jacksonObjectMapper()

    fun toJson(obj: Any): String {
        return mapper.writeValueAsString(obj)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return mapper.readValue(json, clazz)
    }

    inline fun <reified T> fromJson(json: String): T {
        return mapper.readValue(json, T::class.java)
    }

    /**
     * Convert object to NDJSON line (single line with newline)
     */
    fun toNDJsonLine(obj: Any): String {
        return toJson(obj) + "\n"
    }

    /**
     * Create SSE event data
     */
    fun sseEvent(event: String, data: Any): Map<String, Any> {
        return mapOf(
            "event" to event,
            "data" to data
        )
    }
}

