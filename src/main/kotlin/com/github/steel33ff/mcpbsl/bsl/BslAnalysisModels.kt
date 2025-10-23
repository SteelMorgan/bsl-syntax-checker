package com.github.steel33ff.mcpbsl.bsl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * BSL Language Server analysis output models
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslAnalysisOutput(
    @JsonProperty("summary")
    val summary: BslAnalysisSummary?,
    
    @JsonProperty("diagnostics")
    val diagnostics: List<BslDiagnostic>? = emptyList(),
    
    @JsonProperty("files")
    val files: List<BslFileAnalysis>? = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslAnalysisSummary(
    @JsonProperty("errors")
    val errors: Int = 0,
    
    @JsonProperty("warnings")
    val warnings: Int = 0,
    
    @JsonProperty("info")
    val info: Int = 0,
    
    @JsonProperty("total")
    val total: Int = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslDiagnostic(
    @JsonProperty("file")
    val file: String,
    
    @JsonProperty("line")
    val line: Int,
    
    @JsonProperty("column")
    val column: Int = 0,
    
    @JsonProperty("code")
    val code: String,
    
    @JsonProperty("severity")
    val severity: String,
    
    @JsonProperty("message")
    val message: String,
    
    @JsonProperty("range")
    val range: BslRange? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslRange(
    @JsonProperty("start")
    val start: BslPosition,
    
    @JsonProperty("end")
    val end: BslPosition
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslPosition(
    @JsonProperty("line")
    val line: Int,
    
    @JsonProperty("character")
    val character: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslFileAnalysis(
    @JsonProperty("file")
    val file: String,
    
    @JsonProperty("diagnostics")
    val diagnostics: List<BslDiagnostic>? = emptyList()
)

/**
 * BSL Language Server format output models
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslFormatOutput(
    @JsonProperty("formatted")
    val formatted: Boolean = false,
    
    @JsonProperty("filesChanged")
    val filesChanged: Int = 0,
    
    @JsonProperty("files")
    val files: List<BslFormattedFile>? = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslFormattedFile(
    @JsonProperty("file")
    val file: String,
    
    @JsonProperty("formatted")
    val formatted: Boolean = false,
    
    @JsonProperty("content")
    val content: String? = null
)
