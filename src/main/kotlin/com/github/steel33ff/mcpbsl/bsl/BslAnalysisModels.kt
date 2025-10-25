package com.github.steel33ff.mcpbsl.bsl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * BSL Language Server analysis output models
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslAnalysisOutput(
    @JsonProperty("date")
    val date: String?,
    
    @JsonProperty("fileinfos")
    val fileinfos: List<BslFileInfo>? = emptyList(),
    
    @JsonProperty("sourceDir")
    val sourceDir: String?
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
    @JsonProperty("range")
    val range: BslRange? = null,
    
    @JsonProperty("severity")
    val severity: String,
    
    @JsonProperty("code")
    val code: String,
    
    @JsonProperty("codeDescription")
    val codeDescription: BslCodeDescription? = null,
    
    @JsonProperty("source")
    val source: String? = null,
    
    @JsonProperty("message")
    val message: String,
    
    @JsonProperty("tags")
    val tags: List<String>? = null,
    
    @JsonProperty("relatedInformation")
    val relatedInformation: Any? = null,
    
    @JsonProperty("data")
    val data: Any? = null
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
data class BslCodeDescription(
    @JsonProperty("href")
    val href: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslFileInfo(
    @JsonProperty("path")
    val path: String,
    
    @JsonProperty("mdoRef")
    val mdoRef: String?,
    
    @JsonProperty("diagnostics")
    val diagnostics: List<BslDiagnostic>? = emptyList(),
    
    @JsonProperty("metrics")
    val metrics: BslFileMetrics? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BslFileMetrics(
    @JsonProperty("procedures")
    val procedures: Int = 0,
    
    @JsonProperty("functions")
    val functions: Int = 0,
    
    @JsonProperty("lines")
    val lines: Int = 0,
    
    @JsonProperty("ncloc")
    val ncloc: Int = 0,
    
    @JsonProperty("comments")
    val comments: Int = 0,
    
    @JsonProperty("statements")
    val statements: Int = 0,
    
    @JsonProperty("cognitiveComplexity")
    val cognitiveComplexity: Int = 0,
    
    @JsonProperty("cyclomaticComplexity")
    val cyclomaticComplexity: Int = 0
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
