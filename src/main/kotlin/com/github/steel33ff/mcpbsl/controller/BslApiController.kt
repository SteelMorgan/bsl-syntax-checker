package com.github.steel33ff.mcpbsl.controller

import com.github.steel33ff.mcpbsl.bsl.AnalyzeResult
import com.github.steel33ff.mcpbsl.bsl.BslCliService
import com.github.steel33ff.mcpbsl.bsl.FormatResult
import com.github.steel33ff.mcpbsl.dto.*
import com.github.steel33ff.mcpbsl.service.PathMappingService
import com.github.steel33ff.mcpbsl.service.PathTypeService
import com.github.steel33ff.mcpbsl.service.PathType
import io.github.oshai.kotlinlogging.KotlinLogging
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
@Tag(name = "BSL API", description = "BSL Language Server operations: analyze, format, path info")
class BslApiController(
    private val bslCliService: BslCliService,
    private val pathMappingService: PathMappingService,
    private val pathTypeService: PathTypeService
) {
    private val logger = KotlinLogging.logger {}

    @PostMapping("/analyze")
    @Operation(
        summary = "Analyze BSL source code",
        description = "Run BSL Language Server analysis on source file or directory. " +
                     "Note: BSL Language Server works with directories only. " +
                     "If a file path is provided, the parent directory will be analyzed. " +
                     "Returns diagnostics found."
    )
    fun analyze(@RequestBody request: AnalyzeRequest): ResponseEntity<Any> {
        logger.info { "Received analyze request for path: ${request.srcDir}" }
        return try {
            val containerPath = pathMappingService.translateToContainerPath(request.srcDir)
            logger.info { "Translated to container path: $containerPath" }

            // Validate that the path exists and is accessible
            if (!pathMappingService.validatePath(containerPath)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse(
                        error = "Path '${request.srcDir}' does not exist or is not accessible",
                        code = "PATH_NOT_FOUND"
                    ))
            }

            val result = bslCliService.analyze(
                srcPath = containerPath,
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

            // Validate that the path exists and is accessible
            if (!pathMappingService.validatePath(containerPath)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse(
                        error = "Path '${request.src}' does not exist or is not accessible",
                        code = "PATH_NOT_FOUND"
                    ))
            }

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

    @GetMapping("/path-info")
    @Operation(
        summary = "Get path information",
        description = "Get information about a file or directory path including type and BSL file detection"
    )
    fun getPathInfo(@RequestParam path: String): ResponseEntity<Any> {
        logger.info { "Received path info request for: $path" }
        return try {
            val containerPath = pathMappingService.translateToContainerPath(path)
            logger.info { "Translated to container path: $containerPath" }

            val pathType = pathTypeService.getPathType(containerPath)
            
            val response = PathInfoResponse(
                type = pathType.name,
                exists = pathType != PathType.NOT_FOUND,
                isBsl = pathType == PathType.BSL_FILE || pathType == PathType.BSL_DIRECTORY,
                sizeBytes = when (pathType) {
                    PathType.BSL_FILE, PathType.FILE -> pathTypeService.getFileSize(containerPath)
                    PathType.BSL_DIRECTORY, PathType.DIRECTORY -> pathTypeService.getDirectorySize(containerPath)
                    else -> null
                },
                bslFileCount = when (pathType) {
                    PathType.BSL_DIRECTORY, PathType.DIRECTORY -> pathTypeService.findBslFiles(containerPath).size
                    else -> null
                },
                containerPath = containerPath.toString()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(error = e.message ?: "Unknown error", code = "INVALID_REQUEST"))
        }
    }
}

