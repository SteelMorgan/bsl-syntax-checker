package com.github.steel33ff.mcpbsl.controller

import com.github.steel33ff.mcpbsl.bsl.AnalyzeResult
import com.github.steel33ff.mcpbsl.bsl.BslCliService
import com.github.steel33ff.mcpbsl.dto.AnalyzeRequest
import com.github.steel33ff.mcpbsl.service.PathMappingService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Streaming controller for SSE and NDJSON
 */
@RestController
@RequestMapping("/api/stream")
@Tag(name = "Streaming", description = "Server-Sent Events and NDJSON streaming endpoints")
class StreamingController(
    private val bslCliService: BslCliService,
    private val pathMappingService: PathMappingService
) {

    @PostMapping("/analyze/sse", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(
        summary = "Analyze with SSE streaming",
        description = "Run BSL analysis with Server-Sent Events streaming for real-time diagnostics"
    )
    fun analyzeWithSSE(@RequestBody request: AnalyzeRequest): Flux<ServerSentEvent<Map<String, Any>>> {
        logger.info { "Starting SSE analysis stream for: ${request.srcDir}" }

        return Flux.create { sink ->
            try {
                val containerPath = pathMappingService.translateToContainerPath(request.srcDir)

                // Send start event
                sink.next(
                    ServerSentEvent.builder<Map<String, Any>>()
                        .event("start")
                        .data(mapOf("srcDir" to containerPath.toString()))
                        .build()
                )

                // Run analysis
                val result = bslCliService.analyze(
                    srcDir = containerPath,
                    reporters = request.reporters,
                    language = request.language
                )

                when (result) {
                    is AnalyzeResult.Success -> {
                        val data = result.data

                        // Send diagnostics (in chunks if needed)
                        val diagnostics = data["diagnostics"] as? List<*> ?: emptyList<Any>()
                        diagnostics.forEach { diagnostic ->
                            sink.next(
                                ServerSentEvent.builder<Map<String, Any>>()
                                    .event("diagnostic")
                                    .data(diagnostic as? Map<String, Any> ?: mapOf("error" to "Invalid diagnostic"))
                                    .build()
                            )
                        }

                        // Send summary
                        sink.next(
                            ServerSentEvent.builder<Map<String, Any>>()
                                .event("summary")
                                .data(data["summary"] as? Map<String, Any> ?: mapOf("errors" to 0, "warnings" to 0))
                                .build()
                        )

                        // Send complete event
                        sink.next(
                            ServerSentEvent.builder<Map<String, Any>>()
                                .event("complete")
                                .data(mapOf("status" to "success"))
                                .build()
                        )
                    }
                    is AnalyzeResult.Failure -> {
                        sink.next(
                            ServerSentEvent.builder<Map<String, Any>>()
                                .event("error")
                                .data(mapOf("error" to result.error))
                                .build()
                        )
                    }
                }

                sink.complete()
            } catch (e: Exception) {
                logger.error(e) { "SSE streaming error" }
                sink.next(
                    ServerSentEvent.builder<Map<String, Any>>()
                        .event("error")
                        .data(mapOf("error" to (e.message ?: "Unknown error")))
                        .build()
                )
                sink.complete()
            }
        }.delayElements(Duration.ofMillis(10)) // Small delay for better streaming
    }

    @PostMapping("/analyze/ndjson", produces = ["application/x-ndjson"])
    @Operation(
        summary = "Analyze with NDJSON streaming",
        description = "Run BSL analysis with NDJSON streaming (newline-delimited JSON)"
    )
    fun analyzeWithNDJSON(@RequestBody request: AnalyzeRequest): Flux<String> {
        logger.info { "Starting NDJSON analysis stream for: ${request.srcDir}" }

        return Flux.create { sink ->
            try {
                val containerPath = pathMappingService.translateToContainerPath(request.srcDir)

                // Send start event
                sink.next("""{"event":"start","data":{"srcDir":"$containerPath"}}""")

                // Run analysis
                val result = bslCliService.analyze(
                    srcDir = containerPath,
                    reporters = request.reporters,
                    language = request.language
                )

                when (result) {
                    is AnalyzeResult.Success -> {
                        val data = result.data

                        // Send diagnostics
                        val diagnostics = data["diagnostics"] as? List<*> ?: emptyList<Any>()
                        diagnostics.forEach { diagnostic ->
                            val json = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                                .writeValueAsString(mapOf("event" to "diagnostic", "data" to diagnostic))
                            sink.next(json)
                        }

                        // Send summary
                        val summaryJson = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                            .writeValueAsString(
                                mapOf(
                                    "event" to "summary",
                                    "data" to (data["summary"] ?: mapOf("errors" to 0, "warnings" to 0))
                                )
                            )
                        sink.next(summaryJson)

                        // Send complete
                        sink.next("""{"event":"complete","data":{"status":"success"}}""")
                    }
                    is AnalyzeResult.Failure -> {
                        val errorJson = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                            .writeValueAsString(mapOf("event" to "error", "data" to mapOf("error" to result.error)))
                        sink.next(errorJson)
                    }
                }

                sink.complete()
            } catch (e: Exception) {
                logger.error(e) { "NDJSON streaming error" }
                val errorJson = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                    .writeValueAsString(mapOf("event" to "error", "data" to mapOf("error" to (e.message ?: "Unknown error"))))
                sink.next(errorJson)
                sink.complete()
            }
        }
    }
}

