package com.github.steel33ff.mcpbsl.transport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.steel33ff.mcpbsl.bsl.AnalyzeResult
import com.github.steel33ff.mcpbsl.bsl.BslCliService
import com.github.steel33ff.mcpbsl.bsl.BslSessionPool
import com.github.steel33ff.mcpbsl.bsl.FormatResult
import com.github.steel33ff.mcpbsl.service.PathMappingService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}

/**
 * MCP stdio transport implementation
 * 
 * Reads JSON requests from stdin, processes them, and writes JSON responses to stdout.
 * Activated when running with --stdio or when TRANSPORT_MODE=stdio
 */
@Component
// @ConditionalOnProperty(name = ["mcp.transport"], havingValue = "stdio", matchIfMissing = true)
class StdioTransport(
    private val bslCliService: BslCliService,
    private val sessionPool: BslSessionPool,
    private val pathMappingService: PathMappingService,
    @Value("\${mcp.transport:stdio}") private val transportMode: String
) : CommandLineRunner {

    private val objectMapper = jacksonObjectMapper()

    override fun run(vararg args: String) {
        logger.info { "StdioTransport initialized and active - transport mode: $transportMode" }
        logger.info { "Starting MCP stdio transport mode" }

        val reader = BufferedReader(InputStreamReader(System.`in`))

        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        processRequest(line)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Stdio transport error" }
            writeError("Transport error: ${e.message}")
        }

        logger.info { "Stdio transport shutting down" }
    }

    private fun processRequest(jsonLine: String) {
        try {
            val request = objectMapper.readValue<Map<String, Any>>(jsonLine)
            val method = request["method"] as? String ?: run {
                writeJsonRpcError(request["id"], -32600, "Invalid Request", "Missing 'method' field")
                return
            }
            val id = request["id"]
            val params = request["params"] as? Map<String, Any> ?: emptyMap()

            logger.debug { "Processing MCP method: $method" }

            val response = when (method) {
                "initialize" -> handleInitialize(id, params)
                "tools/list" -> handleToolsList(id, params)
                "tools/call" -> handleToolsCall(id, params)
                else -> {
                    logger.warn { "Unknown MCP method: $method" }
                    writeJsonRpcError(id, -32601, "Method Not Found", "Unknown method: $method")
                    return
                }
            }

            writeJsonRpcResponse(id, response)
        } catch (e: Exception) {
            logger.error(e) { "Error processing request: $jsonLine" }
            writeJsonRpcError(null, -32603, "Internal Error", "Request processing error: ${e.message}")
        }
    }

    private fun handleAnalyze(params: Map<String, Any>): Map<String, Any> {
        val srcDir = params["srcDir"] as? String ?: return mapOf("error" to "Missing srcDir parameter")
        val reporters = (params["reporters"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("json")
        val language = params["language"] as? String ?: "ru"

        return try {
            val containerPath = pathMappingService.translateToContainerPath(srcDir)
            val result = bslCliService.analyze(containerPath, reporters, language)

            when (result) {
                is AnalyzeResult.Success -> {
                    val data = result.data
                    val summaryData = data["summary"] as? Map<*, *>
                    val diagnosticsData = data["diagnostics"] as? List<*>
                    
                    val content = buildString {
                        appendLine("🔍 **Анализ кода 1C завершен**")
                        appendLine()
                        
                        if (summaryData != null) {
                            val errors = summaryData["errors"] as? Int ?: 0
                            val warnings = summaryData["warnings"] as? Int ?: 0
                            
                            appendLine("📊 **Результаты анализа:**")
                            appendLine("- ❌ Ошибки: $errors")
                            appendLine("- ⚠️ Предупреждения: $warnings")
                            appendLine()
                        }
                        
                        if (!diagnosticsData.isNullOrEmpty()) {
                            appendLine("📋 **Детали:**")
                            diagnosticsData.take(10).forEach { diagnostic ->
                                val diag = diagnostic as? Map<*, *>
                                if (diag != null) {
                                    val file = diag["file"] as? String ?: ""
                                    val line = diag["line"] as? Int ?: 0
                                    val severity = diag["severity"] as? String ?: "info"
                                    val message = diag["message"] as? String ?: ""
                                    val code = diag["code"] as? String ?: ""
                                    
                                    val icon = when (severity) {
                                        "error" -> "❌"
                                        "warning" -> "⚠️"
                                        else -> "ℹ️"
                                    }
                                    
                                    appendLine("$icon **$file:$line** - $message")
                                    if (code.isNotEmpty()) {
                                        appendLine("   Код: `$code`")
                                    }
                                }
                            }
                            
                            if (diagnosticsData.size > 10) {
                                appendLine("... и еще ${diagnosticsData.size - 10} проблем")
                            }
                        }
                    }
                    
                    mapOf(
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to content
                            )
                        )
                    )
                }
                is AnalyzeResult.Failure -> mapOf(
                    "content" to listOf(
                        mapOf(
                            "type" to "text",
                            "text" to "❌ **Ошибка анализа:** ${result.error}"
                        )
                    )
                )
            }
        } catch (e: Exception) {
            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to "❌ **Ошибка:** ${e.message ?: "Analysis failed"}"
                    )
                )
            )
        }
    }

    private fun handleFormat(params: Map<String, Any>): Map<String, Any> {
        val src = params["src"] as? String ?: return mapOf("error" to "Missing src parameter")
        val inPlace = params["inPlace"] as? Boolean ?: true

        return try {
            val containerPath = pathMappingService.translateToContainerPath(src)
            val result = bslCliService.format(containerPath, inPlace)

            when (result) {
                is FormatResult.Success -> {
                    val data = result.data
                    val filesChanged = data["filesChanged"] as? Int ?: 0
                    val formatted = data["formatted"] as? Boolean ?: true
                    
                    val content = buildString {
                        appendLine("✨ **Форматирование кода 1C завершено**")
                        appendLine()
                        appendLine("📊 **Результаты:**")
                        appendLine("- 📁 Файлов изменено: $filesChanged")
                        appendLine("- ✅ Форматирование: ${if (formatted) "успешно" else "не выполнено"}")
                    }
                    
                    mapOf(
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to content
                            )
                        )
                    )
                }
                is FormatResult.Failure -> mapOf(
                    "content" to listOf(
                        mapOf(
                            "type" to "text",
                            "text" to "❌ **Ошибка форматирования:** ${result.error}"
                        )
                    )
                )
            }
        } catch (e: Exception) {
            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to "❌ **Ошибка:** ${e.message ?: "Format failed"}"
                    )
                )
            )
        }
    }

    private fun handleSessionStart(params: Map<String, Any>): Map<String, Any> {
        val projectPath = params["projectPath"] as? String ?: return mapOf("error" to "Missing projectPath parameter")

        return try {
            val containerPath = pathMappingService.translateToContainerPath(projectPath)
            val session = sessionPool.createSession(containerPath)

            val content = buildString {
                appendLine("🚀 **Сессия BSL Language Server запущена**")
                appendLine()
                appendLine("📊 **Информация о сессии:**")
                appendLine("- 🆔 ID сессии: `${session.id}`")
                appendLine("- 📁 Проект: `$containerPath`")
                appendLine("- ⏰ Время запуска: ${java.time.Instant.now()}")
            }

            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to content
                    )
                )
            )
        } catch (e: Exception) {
            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to "❌ **Ошибка запуска сессии:** ${e.message ?: "Session start failed"}"
                    )
                )
            )
        }
    }

    private fun handleSessionStatus(params: Map<String, Any>): Map<String, Any> {
        val sessionId = params["sessionId"] as? String ?: return mapOf("error" to "Missing sessionId parameter")

        val session = sessionPool.getSession(sessionId)
        return if (session != null) {
            val status = if (session.isAlive) "🟢 активна" else "🔴 остановлена"
            val uptime = session.getUptimeSeconds()
            
            val content = buildString {
                appendLine("📊 **Статус сессии BSL Language Server**")
                appendLine()
                appendLine("🆔 **ID сессии:** `$sessionId`")
                appendLine("📈 **Статус:** $status")
                appendLine("⏱️ **Время работы:** ${uptime}с")
            }

            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to content
                    )
                )
            )
        } else {
            mapOf(
                "content" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to "❌ **Сессия не найдена:** `$sessionId`"
                    )
                )
            )
        }
    }

    private fun handleSessionStop(params: Map<String, Any>): Map<String, Any> {
        val sessionId = params["sessionId"] as? String ?: return mapOf("error" to "Missing sessionId parameter")

        val stopped = sessionPool.stopSession(sessionId)
        val content = if (stopped) {
            buildString {
                appendLine("🛑 **Сессия BSL Language Server остановлена**")
                appendLine()
                appendLine("🆔 **ID сессии:** `$sessionId`")
                appendLine("✅ **Результат:** успешно остановлена")
            }
        } else {
            buildString {
                appendLine("❌ **Ошибка остановки сессии**")
                appendLine()
                appendLine("🆔 **ID сессии:** `$sessionId`")
                appendLine("⚠️ **Результат:** сессия не найдена или уже остановлена")
            }
        }

        return mapOf(
            "content" to listOf(
                mapOf(
                    "type" to "text",
                    "text" to content
                )
            )
        )
    }

    private fun handleInitialize(id: Any?, params: Map<String, Any>): Map<String, Any> {
        logger.info { "MCP initialize request received" }
        return mapOf(
            "protocolVersion" to "2024-11-05",
            "capabilities" to mapOf(
                "tools" to mapOf("listChanged" to true),
                "prompts" to emptyMap<String, Any>(),
                "resources" to emptyMap<String, Any>(),
                "logging" to emptyMap<String, Any>()
            ),
            "serverInfo" to mapOf(
                "name" to "MCP BSL Server",
                "version" to "0.1.0"
            )
        )
    }

    private fun handleToolsList(id: Any?, params: Map<String, Any>): Map<String, Any> {
        logger.info { "MCP tools/list request received" }
        val tools = listOf(
            mapOf(
                "name" to "bslcheck_analyze",
                "description" to "Analyze BSL source code for errors and warnings. Works with directories containing BSL files. IMPORTANT: All paths must be absolute and start with '/workspaces/'.",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "srcDir" to mapOf(
                            "type" to "string",
                            "description" to "Source directory path containing BSL files to analyze. MUST be absolute path starting with '/workspaces/' (e.g., '/workspaces/project/src'). Paths outside /workspaces/ will be rejected."
                        ),
                        "reporters" to mapOf(
                            "type" to "array",
                            "items" to mapOf("type" to "string"),
                            "description" to "Reporters to use (json, console, etc.)",
                            "default" to listOf("json")
                        ),
                        "language" to mapOf(
                            "type" to "string",
                            "description" to "Language for messages (ru, en)",
                            "default" to "ru"
                        )
                    ),
                    "required" to listOf("srcDir")
                )
            ),
            mapOf(
                "name" to "bslcheck_format",
                "description" to "Format BSL source code. Works with directories containing BSL files. IMPORTANT: All paths must be absolute and start with '/workspaces/'.",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "src" to mapOf(
                            "type" to "string",
                            "description" to "Source directory path containing BSL files to format. MUST be absolute path starting with '/workspaces/' (e.g., '/workspaces/project/src'). Paths outside /workspaces/ will be rejected."
                        ),
                        "inPlace" to mapOf(
                            "type" to "boolean",
                            "description" to "Format files in place",
                            "default" to true
                        )
                    ),
                    "required" to listOf("src")
                )
            ),
            mapOf(
                "name" to "bslcheck_session_start",
                "description" to "Start BSL Language Server session for a project. IMPORTANT: All paths must be absolute and start with '/workspaces/'.",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "projectPath" to mapOf(
                            "type" to "string",
                            "description" to "Project path for the session. MUST be absolute path starting with '/workspaces/' (e.g., '/workspaces/project'). Paths outside /workspaces/ will be rejected."
                        )
                    ),
                    "required" to listOf("projectPath")
                )
            ),
            mapOf(
                "name" to "bslcheck_session_status",
                "description" to "Get BSL Language Server session status",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "sessionId" to mapOf(
                            "type" to "string",
                            "description" to "Session ID to check"
                        )
                    ),
                    "required" to listOf("sessionId")
                )
            ),
            mapOf(
                "name" to "bslcheck_session_stop",
                "description" to "Stop BSL Language Server session",
                "inputSchema" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "sessionId" to mapOf(
                            "type" to "string",
                            "description" to "Session ID to stop"
                        )
                    ),
                    "required" to listOf("sessionId")
                )
            )
        )
        return mapOf("tools" to tools)
    }

    private fun handleToolsCall(id: Any?, params: Map<String, Any>): Map<String, Any> {
        val toolName = params["name"] as? String ?: return mapOf("error" to "Missing tool name")
        val arguments = params["arguments"] as? Map<String, Any> ?: emptyMap()

        logger.info { "MCP tools/call request for tool: $toolName" }

        return when (toolName) {
            "bslcheck_analyze" -> handleAnalyze(arguments)
            "bslcheck_format" -> handleFormat(arguments)
            "bslcheck_session_start" -> handleSessionStart(arguments)
            "bslcheck_session_status" -> handleSessionStatus(arguments)
            "bslcheck_session_stop" -> handleSessionStop(arguments)
            else -> mapOf("error" to "Unknown tool: $toolName")
        }
    }

    private fun writeJsonRpcResponse(id: Any?, result: Map<String, Any>) {
        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to result
        )
        val json = objectMapper.writeValueAsString(response)
        println(json)
        System.out.flush()
    }

    private fun writeJsonRpcError(id: Any?, code: Int, message: String, data: String? = null) {
        val error = mapOf(
            "code" to code,
            "message" to message
        ) + if (data != null) mapOf("data" to data) else emptyMap()
        
        val response = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "error" to error
        )
        val json = objectMapper.writeValueAsString(response)
        println(json)
        System.out.flush()
    }

    private fun writeResponse(response: Map<String, Any>) {
        val json = objectMapper.writeValueAsString(response)
        println(json)
        System.out.flush()
    }

    private fun writeError(message: String) {
        writeResponse(mapOf("error" to message))
    }
}

