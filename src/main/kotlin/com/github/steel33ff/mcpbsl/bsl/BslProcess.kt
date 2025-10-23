package com.github.steel33ff.mcpbsl.bsl

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Wrapper for BSL Language Server process
 */
class BslProcess(
    private val jarPath: Path,
    private val maxHeap: String,
    private val workspaceDir: Path,
    private val mode: BslMode = BslMode.LSP
) : AutoCloseable {

    private var process: Process? = null
    private var stdoutReader: BufferedReader? = null
    private var stdinWriter: BufferedWriter? = null

    val isAlive: Boolean
        get() = process?.isAlive ?: false

    fun start() {
        if (isAlive) {
            logger.warn { "BSL process already running" }
            return
        }

        logger.info { "Starting BSL Language Server: mode=$mode, workspace=$workspaceDir" }

        val command = buildCommand()
        val processBuilder = ProcessBuilder(command)
            .directory(workspaceDir.toFile())
            .redirectErrorStream(true)

        process = processBuilder.start()

        if (mode == BslMode.LSP || mode == BslMode.WEBSOCKET) {
            stdoutReader = BufferedReader(InputStreamReader(process!!.inputStream))
            stdinWriter = BufferedWriter(OutputStreamWriter(process!!.outputStream))
        }

        logger.info { "BSL Language Server started with PID: ${process!!.pid()}" }
    }

    fun sendRequest(request: String): String? {
        if (!isAlive) {
            throw IllegalStateException("BSL process is not running")
        }

        stdinWriter?.let { writer ->
            writer.write(request)
            writer.newLine()
            writer.flush()

            return stdoutReader?.readLine()
        }

        return null
    }

    fun executeCliCommand(vararg args: String): ProcessResult {
        logger.info { "Executing BSL CLI command: ${args.joinToString(" ")}" }

        val command = buildCommand(*args)
        val processBuilder = ProcessBuilder(command)
            .directory(workspaceDir.toFile())
            .redirectErrorStream(true)

        val cliProcess = processBuilder.start()
        val output = cliProcess.inputStream.bufferedReader().readText()
        val exitCode = cliProcess.waitFor()

        return ProcessResult(exitCode, output)
    }

    private fun buildCommand(vararg extraArgs: String): List<String> {
        val command = mutableListOf(
            "java",
            "-Xmx$maxHeap",
            "-jar",
            jarPath.toString()
        )

        when (mode) {
            BslMode.LSP -> command.add("--lsp")
            BslMode.WEBSOCKET -> {
                command.add("--websocket")
                command.add("--server.port=0") // random port
            }
            BslMode.CLI -> {} // no mode flag for CLI
        }

        command.addAll(extraArgs)
        return command
    }

    override fun close() {
        logger.info { "Stopping BSL Language Server process" }

        stdoutReader?.close()
        stdinWriter?.close()

        process?.let { proc ->
            proc.destroy()
            if (!proc.waitFor(10, TimeUnit.SECONDS)) {
                logger.warn { "BSL process did not terminate gracefully, forcing..." }
                proc.destroyForcibly()
            }
        }

        process = null
    }
}

enum class BslMode {
    LSP,
    WEBSOCKET,
    CLI
}

data class ProcessResult(
    val exitCode: Int,
    val output: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

