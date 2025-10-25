package com.github.steel33ff.mcpbsl.bsl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * Parser for BSL Language Server JSON output
 */
@Component
class BslOutputParser {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    /**
     * Parse BSL analysis JSON output
     */
    fun parseAnalysisOutput(output: String): Map<String, Any> {
        return try {
            logger.debug { "Parsing BSL analysis output: ${output.take(200)}..." }
            
            // Try to parse as JSON first
            val analysisOutput = parseJsonOutput<BslAnalysisOutput>(output)
            
            if (analysisOutput != null) {
                logger.info { "Successfully parsed BSL analysis JSON output" }
                convertAnalysisToMap(analysisOutput)
            } else {
                // Fallback to text parsing if JSON parsing fails
                logger.warn { "JSON parsing failed, falling back to text parsing" }
                parseAnalysisTextOutput(output)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse BSL analysis output" }
            parseAnalysisTextOutput(output)
        }
    }

    /**
     * Parse BSL format JSON output
     */
    fun parseFormatOutput(output: String): Map<String, Any> {
        return try {
            logger.debug { "Parsing BSL format output: ${output.take(200)}..." }
            
            // Try to parse as JSON first
            val formatOutput = parseJsonOutput<BslFormatOutput>(output)
            
            if (formatOutput != null) {
                logger.info { "Successfully parsed BSL format JSON output" }
                convertFormatToMap(formatOutput)
            } else {
                // Fallback to text parsing if JSON parsing fails
                logger.warn { "JSON parsing failed, falling back to text parsing" }
                parseFormatTextOutput(output)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse BSL format output" }
            parseFormatTextOutput(output)
        }
    }

    private inline fun <reified T> parseJsonOutput(output: String): T? {
        return try {
            // Try to find JSON in the output (might be mixed with other text)
            val jsonStart = output.indexOf('{')
            val jsonEnd = output.lastIndexOf('}')
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonString = output.substring(jsonStart, jsonEnd + 1)
                objectMapper.readValue<T>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug(e) { "JSON parsing failed for output" }
            null
        }
    }

    private fun convertAnalysisToMap(analysis: BslAnalysisOutput): Map<String, Any> {
        val fileinfos = analysis.fileinfos ?: emptyList()
        
        // Calculate summary from all diagnostics
        var errors = 0
        var warnings = 0
        var info = 0
        val allDiagnostics = mutableListOf<Map<String, Any>>()
        
        fileinfos.forEach { fileInfo ->
            fileInfo.diagnostics?.forEach { diagnostic ->
                when (diagnostic.severity.lowercase()) {
                    "error" -> errors++
                    "warning" -> warnings++
                    "info" -> info++
                }
                
                allDiagnostics.add(mapOf<String, Any>(
                    "file" to fileInfo.path,
                    "line" to (diagnostic.range?.start?.line ?: 0),
                    "column" to (diagnostic.range?.start?.character ?: 0),
                    "code" to diagnostic.code,
                    "severity" to diagnostic.severity,
                    "message" to diagnostic.message,
                    "range" to (diagnostic.range?.let { range ->
                        mapOf<String, Any>(
                            "start" to mapOf<String, Any>(
                                "line" to range.start.line,
                                "character" to range.start.character
                            ),
                            "end" to mapOf<String, Any>(
                                "line" to range.end.line,
                                "character" to range.end.character
                            )
                        )
                    } ?: emptyMap<String, Any>())
                ))
            }
        }
        
        return mapOf(
            "summary" to mapOf(
                "errors" to errors,
                "warnings" to warnings,
                "info" to info,
                "total" to (errors + warnings + info)
            ),
            "diagnostics" to allDiagnostics,
            "files" to fileinfos.map { fileInfo ->
                mapOf(
                    "file" to fileInfo.path,
                    "diagnostics" to (fileInfo.diagnostics ?: emptyList()).size,
                    "metrics" to fileInfo.metrics?.let { metrics ->
                        mapOf(
                            "procedures" to metrics.procedures,
                            "functions" to metrics.functions,
                            "lines" to metrics.lines,
                            "ncloc" to metrics.ncloc,
                            "comments" to metrics.comments,
                            "statements" to metrics.statements,
                            "cognitiveComplexity" to metrics.cognitiveComplexity,
                            "cyclomaticComplexity" to metrics.cyclomaticComplexity
                        )
                    }
                )
            }
        )
    }

    private fun convertFormatToMap(format: BslFormatOutput): Map<String, Any> {
        return mapOf(
            "formatted" to format.formatted,
            "filesChanged" to format.filesChanged,
            "files" to (format.files ?: emptyList()).map { file ->
                mapOf(
                    "file" to file.file,
                    "formatted" to file.formatted,
                    "hasContent" to (file.content != null)
                )
            }
        )
    }

    /**
     * Fallback text parsing for analysis output
     */
    private fun parseAnalysisTextOutput(output: String): Map<String, Any> {
        logger.debug { "Using text parsing for analysis output" }
        
        val lines = output.lines()
        var errors = 0
        var warnings = 0
        var info = 0
        
        // Try to extract numbers from text output
        lines.forEach { line ->
            when {
                line.contains("error", ignoreCase = true) -> {
                    val match = Regex("(\\d+)\\s*error").find(line)
                    if (match != null) errors = match.groupValues[1].toIntOrNull() ?: 0
                }
                line.contains("warning", ignoreCase = true) -> {
                    val match = Regex("(\\d+)\\s*warning").find(line)
                    if (match != null) warnings = match.groupValues[1].toIntOrNull() ?: 0
                }
                line.contains("info", ignoreCase = true) -> {
                    val match = Regex("(\\d+)\\s*info").find(line)
                    if (match != null) info = match.groupValues[1].toIntOrNull() ?: 0
                }
            }
        }
        
        return mapOf(
            "summary" to mapOf(
                "errors" to errors,
                "warnings" to warnings,
                "info" to info,
                "total" to (errors + warnings + info)
            ),
            "diagnostics" to emptyList<Map<String, Any>>(),
            "rawOutput" to output
        )
    }

    /**
     * Fallback text parsing for format output
     */
    private fun parseFormatTextOutput(output: String): Map<String, Any> {
        logger.debug { "Using text parsing for format output" }
        
        val lines = output.lines()
        var filesChanged = 0
        var formatted = false
        
        // Try to extract information from text output
        lines.forEach { line ->
            when {
                line.contains("formatted", ignoreCase = true) -> formatted = true
                line.contains("changed", ignoreCase = true) -> {
                    val match = Regex("(\\d+)\\s*file").find(line)
                    if (match != null) filesChanged = match.groupValues[1].toIntOrNull() ?: 0
                }
            }
        }
        
        return mapOf(
            "formatted" to formatted,
            "filesChanged" to filesChanged,
            "rawOutput" to output
        )
    }
}
