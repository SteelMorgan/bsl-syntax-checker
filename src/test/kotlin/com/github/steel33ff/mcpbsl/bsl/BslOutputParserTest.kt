package com.github.steel33ff.mcpbsl.bsl

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BslOutputParserTest {

    private val parser = BslOutputParser()

    @Test
    fun `should parse valid JSON analysis output`() {
        val jsonOutput = """
        {
            "summary": {
                "errors": 2,
                "warnings": 5,
                "info": 1,
                "total": 8
            },
            "diagnostics": [
                {
                    "file": "Module.bsl",
                    "line": 10,
                    "column": 5,
                    "code": "LineLength",
                    "severity": "warning",
                    "message": "Line is too long"
                },
                {
                    "file": "Module.bsl",
                    "line": 15,
                    "column": 0,
                    "code": "SyntaxError",
                    "severity": "error",
                    "message": "Syntax error"
                }
            ]
        }
        """.trimIndent()

        val result = parser.parseAnalysisOutput(jsonOutput)

        assertNotNull(result)
        assertEquals(2, (result["summary"] as Map<*, *>)["errors"])
        assertEquals(5, (result["summary"] as Map<*, *>)["warnings"])
        assertEquals(1, (result["summary"] as Map<*, *>)["info"])
        assertEquals(8, (result["summary"] as Map<*, *>)["total"])

        val diagnostics = result["diagnostics"] as List<*>
        assertEquals(2, diagnostics.size)

        val firstDiagnostic = diagnostics[0] as Map<*, *>
        assertEquals("Module.bsl", firstDiagnostic["file"])
        assertEquals(10, firstDiagnostic["line"])
        assertEquals("LineLength", firstDiagnostic["code"])
        assertEquals("warning", firstDiagnostic["severity"])
        assertEquals("Line is too long", firstDiagnostic["message"])
    }

    @Test
    fun `should parse valid JSON format output`() {
        val jsonOutput = """
        {
            "formatted": true,
            "filesChanged": 3,
            "files": [
                {
                    "file": "Module.bsl",
                    "formatted": true,
                    "content": "formatted content"
                },
                {
                    "file": "AnotherModule.bsl",
                    "formatted": false
                }
            ]
        }
        """.trimIndent()

        val result = parser.parseFormatOutput(jsonOutput)

        assertNotNull(result)
        assertEquals(true, result["formatted"])
        assertEquals(3, result["filesChanged"])

        val files = result["files"] as List<*>
        assertEquals(2, files.size)

        val firstFile = files[0] as Map<*, *>
        assertEquals("Module.bsl", firstFile["file"])
        assertEquals(true, firstFile["formatted"])
        assertEquals(true, firstFile["hasContent"])
    }

    @Test
    fun `should handle text output when JSON parsing fails`() {
        val textOutput = """
        BSL Language Server Analysis
        Found 3 errors and 7 warnings
        Analysis completed successfully
        """.trimIndent()

        val result = parser.parseAnalysisOutput(textOutput)

        assertNotNull(result)
        assertEquals(3, (result["summary"] as Map<*, *>)["errors"])
        assertEquals(7, (result["summary"] as Map<*, *>)["warnings"])
        assertEquals(10, (result["summary"] as Map<*, *>)["total"])
    }

    @Test
    fun `should handle mixed output with JSON`() {
        val mixedOutput = """
        Starting BSL analysis...
        {
            "summary": {
                "errors": 1,
                "warnings": 2,
                "info": 0,
                "total": 3
            },
            "diagnostics": []
        }
        Analysis completed.
        """.trimIndent()

        val result = parser.parseAnalysisOutput(mixedOutput)

        assertNotNull(result)
        assertEquals(1, (result["summary"] as Map<*, *>)["errors"])
        assertEquals(2, (result["summary"] as Map<*, *>)["warnings"])
        assertEquals(3, (result["summary"] as Map<*, *>)["total"])
    }

    @Test
    fun `should handle empty output gracefully`() {
        val result = parser.parseAnalysisOutput("")
        
        assertNotNull(result)
        assertEquals(0, (result["summary"] as Map<*, *>)["errors"])
        assertEquals(0, (result["summary"] as Map<*, *>)["warnings"])
        assertTrue((result["diagnostics"] as List<*>).isEmpty())
    }

    @Test
    fun `should handle format text output`() {
        val textOutput = """
        Formatting BSL files...
        2 files changed
        Formatting completed successfully
        """.trimIndent()

        val result = parser.parseFormatOutput(textOutput)

        assertNotNull(result)
        assertEquals(true, result["formatted"])
        assertEquals(2, result["filesChanged"])
    }
}
