package com.github.steel33ff.mcpbsl.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request to analyze BSL code
 */
@Schema(description = "Analyze BSL source code request")
data class AnalyzeRequest(
    @Schema(description = "Host absolute path to source file or directory", example = "D:\\Projects\\MyProject\\Module.bsl")
    val srcDir: String,

    @Schema(description = "List of reporters", example = "[\"json\"]", defaultValue = "[\"json\"]")
    val reporters: List<String> = listOf("json"),

    @Schema(description = "Language for diagnostics", example = "ru", allowableValues = ["ru", "en"], defaultValue = "ru")
    val language: String = "ru",

    @Schema(description = "Enable streaming mode (SSE/NDJSON)", defaultValue = "false")
    val stream: Boolean = false
)

/**
 * Response from analyze operation
 */
@Schema(description = "Analysis result")
data class AnalyzeResponse(
    @Schema(description = "Summary of analysis")
    val summary: AnalyzeSummary,

    @Schema(description = "List of diagnostics found")
    val diagnostics: List<Diagnostic>
)

@Schema(description = "Analysis summary")
data class AnalyzeSummary(
    @Schema(description = "Number of errors", example = "5")
    val errors: Int,

    @Schema(description = "Number of warnings", example = "12")
    val warnings: Int
)

@Schema(description = "Diagnostic issue")
data class Diagnostic(
    @Schema(description = "File path", example = "Module.bsl")
    val file: String,

    @Schema(description = "Line number", example = "42")
    val line: Int,

    @Schema(description = "Diagnostic code", example = "LineLength")
    val code: String,

    @Schema(description = "Severity", example = "warning", allowableValues = ["error", "warning", "info"])
    val severity: String,

    @Schema(description = "Diagnostic message", example = "Line is too long")
    val message: String
)

/**
 * Request to format BSL code
 */
@Schema(description = "Format BSL source code request")
data class FormatRequest(
    @Schema(description = "Host absolute path to file or directory", example = "D:\\Projects\\Module.bsl")
    val src: String,

    @Schema(description = "Format in place or return formatted content", defaultValue = "true")
    val inPlace: Boolean = true
)

/**
 * Response from format operation
 */
@Schema(description = "Format result")
data class FormatResponse(
    @Schema(description = "Formatting was successful")
    val formatted: Boolean,

    @Schema(description = "Number of files changed", example = "3")
    val filesChanged: Int
)

/**
 * Request to start a BSL session
 */
@Schema(description = "Start BSL session request")
data class SessionStartRequest(
    @Schema(description = "Host absolute path to project directory", example = "D:\\Projects\\MyProject")
    val projectPath: String
)

/**
 * Response from session start
 */
@Schema(description = "Session start result")
data class SessionStartResponse(
    @Schema(description = "Session ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val sessionId: String,

    @Schema(description = "Normalized project path", example = "/workspaces/MyProject")
    val project: String
)

/**
 * Response from session status query
 */
@Schema(description = "Session status")
data class SessionStatusResponse(
    @Schema(description = "Session status", example = "ready", allowableValues = ["ready", "starting", "stopped"])
    val status: String,

    @Schema(description = "Uptime in seconds", example = "120")
    val uptimeSeconds: Long
)

/**
 * Response from session stop
 */
@Schema(description = "Session stop result")
data class SessionStopResponse(
    @Schema(description = "Session was stopped successfully")
    val stopped: Boolean
)

/**
 * Path information response
 */
@Schema(description = "Path information")
data class PathInfoResponse(
    @Schema(description = "Path type", example = "BSL_FILE", allowableValues = ["BSL_FILE", "BSL_DIRECTORY", "FILE", "DIRECTORY", "NOT_FOUND", "UNKNOWN"])
    val type: String,

    @Schema(description = "Path exists")
    val exists: Boolean,

    @Schema(description = "Is BSL file or directory")
    val isBsl: Boolean,

    @Schema(description = "File size in bytes (for files)")
    val sizeBytes: Long? = null,

    @Schema(description = "Number of BSL files (for directories)")
    val bslFileCount: Int? = null,

    @Schema(description = "Container path")
    val containerPath: String
)

/**
 * Error response
 */
@Schema(description = "Error response")
data class ErrorResponse(
    @Schema(description = "Error message")
    val error: String,

    @Schema(description = "Error code", example = "PATH_MAPPING_ERROR")
    val code: String? = null
)

