package com.github.steel33ff.mcpbsl.controller

import com.github.steel33ff.mcpbsl.bsl.AnalyzeResult
import com.github.steel33ff.mcpbsl.bsl.BslCliService
import com.github.steel33ff.mcpbsl.bsl.FormatResult
import com.github.steel33ff.mcpbsl.dto.*
import com.github.steel33ff.mcpbsl.service.PathMappingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST API controller for BSL operations
 */
@RestController
@RequestMapping("/api")
@Tag(name = "BSL API", description = "BSL Language Server operations: analyze, format")
class BslApiController(
    private val bslCliService: BslCliService,
    private val pathMappingService: PathMappingService
) {

    @PostMapping("/analyze")
    @Operation(
        summary = "Analyze BSL source code",
        description = "Run BSL Language Server analysis on source directory. Returns diagnostics found."
    )
    fun analyze(@RequestBody request: AnalyzeRequest): ResponseEntity<Any> {
        return try {
            val containerPath = pathMappingService.translateToContainerPath(request.srcDir)

            val result = bslCliService.analyze(
                srcDir = containerPath,
                reporters = request.reporters,
                language = request.language
            )

            when (result) {
                is AnalyzeResult.Success -> {
                    val data = result.data
                    val summaryData = data["summary"] as? Map<*, *>
                    val diagnosticsData = data["diagnostics"] as? List<*>
                    
                    val response = AnalyzeResponse(
                        summary = AnalyzeSummary(
                            errors = summaryData?.get("errors") as? Int ?: 0,
                            warnings = summaryData?.get("warnings") as? Int ?: 0
                        ),
                        diagnostics = diagnosticsData?.mapNotNull { diagnostic ->
                            val diag = diagnostic as? Map<*, *>
                            if (diag != null) {
                                Diagnostic(
                                    file = diag["file"] as? String ?: "",
                                    line = diag["line"] as? Int ?: 0,
                                    code = diag["code"] as? String ?: "",
                                    severity = diag["severity"] as? String ?: "info",
                                    message = diag["message"] as? String ?: ""
                                )
                            } else null
                        } ?: emptyList()
                    )
                    ResponseEntity.ok(response)
                }
                is AnalyzeResult.Failure -> {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ErrorResponse(error = result.error, code = "ANALYZE_FAILED"))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(error = e.message ?: "Unknown error", code = "INVALID_REQUEST"))
        }
    }

    @PostMapping("/format")
    @Operation(
        summary = "Format BSL source code",
        description = "Run BSL Language Server formatter on source file or directory"
    )
    fun format(@RequestBody request: FormatRequest): ResponseEntity<Any> {
        return try {
            val containerPath = pathMappingService.translateToContainerPath(request.src)

            val result = bslCliService.format(
                src = containerPath,
                inPlace = request.inPlace
            )

            when (result) {
                is FormatResult.Success -> {
                    val data = result.data
                    val response = FormatResponse(
                        formatted = data["formatted"] as? Boolean ?: true,
                        filesChanged = data["filesChanged"] as? Int ?: 0
                    )
                    ResponseEntity.ok(response)
                }
                is FormatResult.Failure -> {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ErrorResponse(error = result.error, code = "FORMAT_FAILED"))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(error = e.message ?: "Unknown error", code = "INVALID_REQUEST"))
        }
    }
}

