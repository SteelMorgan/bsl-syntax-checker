package com.github.steel33ff.mcpbsl.transport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.steel33ff.mcpbsl.bsl.AnalyzeResult
import com.github.steel33ff.mcpbsl.bsl.BslCliService
import com.github.steel33ff.mcpbsl.bsl.BslSessionPool
import com.github.steel33ff.mcpbsl.bsl.FormatResult
import com.github.steel33ff.mcpbsl.service.PathMappingService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * HTTP REST API transport for MCP
 */
@RestController
@RequestMapping("/mcp")
// @ConditionalOnProperty(name = ["mcp.transport"], havingValue = "http", matchIfMissing = false)
class HttpTransport(
    private val bslCliService: BslCliService,
    private val sessionPool: BslSessionPool,
    private val pathMappingService: PathMappingService,
    @Value("\${mcp.transport:http}") private val transportMode: String
) {

    private val objectMapper = jacksonObjectMapper()

    init {
        logger.info { "HttpTransport initialized and active - transport mode: $transportMode" }
    }

    @PostMapping
    fun handleMcpRequest(@RequestBody request: Map<String, Any>): Map<String, Any> {
        val method = request["method"] as? String ?: return mapOf("error" to "Missing 'method' field")
        val id = request["id"] ?: ""
        val params = request["params"] as? Map<String, Any> ?: emptyMap()

        logger.info { "Processing MCP HTTP method: $method" }

        return try {
            val result = when (method) {
                "initialize" -> handleInitialize(id, params)
                "tools/list" -> handleToolsList(id, params)
                "tools/call" -> handleToolsCall(id, params)
                "prompts/list" -> handlePromptsList(id, params)
                "resources/list" -> handleResourcesList(id, params)
                else -> {
                    logger.warn { "Unknown MCP method: $method" }
                    return mapOf(
                        "jsonrpc" to "2.0",
                        "id" to id,
                        "error" to mapOf(
                            "code" to -32601,
                            "message" to "Method Not Found",
                            "data" to "Unknown method: $method"
                        )
                    )
                }
            }

            mapOf(
                "jsonrpc" to "2.0",
                "id" to id,
                "result" to result
            )
        } catch (e: Exception) {
            logger.error(e) { "Error processing MCP method: $method" }
            mapOf(
                "jsonrpc" to "2.0",
                "id" to id,
                "error" to mapOf(
                    "code" to -32603,
                    "message" to "Internal Error",
                    "data" to (e.message ?: "Internal server error")
                )
            )
        }
    }

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf("status" to "UP", "transport" to "http")
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
                "tools" to emptyMap<String, Any>(),
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

    private fun handlePromptsList(id: Any?, params: Map<String, Any>): Map<String, Any> {
        logger.info { "MCP prompts/list request received" }
        return mapOf("prompts" to emptyList<Any>())
    }

    private fun handleResourcesList(id: Any?, params: Map<String, Any>): Map<String, Any> {
        logger.info { "MCP resources/list request received" }
        return mapOf("resources" to emptyList<Any>())
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
}
