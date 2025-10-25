package com.github.steel33ff.mcpbsl.bsl

import com.github.steel33ff.mcpbsl.config.BslServerProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

/**
 * Service for executing BSL Language Server CLI commands
 */
@Service
class BslCliService(
    private val properties: BslServerProperties,
    private val outputParser: BslOutputParser
) {

    /**
     * Run analysis on source file or directory
     */
    fun analyze(
        srcPath: Path,
        reporters: List<String> = listOf("json"),
        language: String = "ru",
        outputDir: Path? = null
    ): AnalyzeResult {
        logger.info { "Running BSL analysis: srcPath=$srcPath, reporters=$reporters, language=$language" }

        val args = mutableListOf(
            "--analyze",
            "--configuration", createConfigJson(language)
        )

        // Determine if srcPath is a file or directory and use appropriate parameter
        if (srcPath.toFile().isFile) {
            args.add("--src")
            args.add(srcPath.toString())
        } else {
            args.add("--srcDir")
            args.add(srcPath.toString())
        }

        reporters.forEach { reporter ->
            args.add("--reporter")
            args.add(reporter)
        }

        outputDir?.let {
            args.add("--outputDir")
            args.add(it.toString())
        }

        return try {
            // Use parent directory as working directory if srcPath is a file
            val workDir = if (srcPath.toFile().isFile) {
                srcPath.parent
            } else {
                srcPath
            }
            
            // Always specify output directory to avoid writing to read-only /workspaces
            val tempOutputDir = outputDir ?: Path("/tmp/bsl-reports")
            args.add("--outputDir")
            args.add(tempOutputDir.toString())
            
            val result = executeCliCommand(workDir, *args.toTypedArray())

            if (result.isSuccess) {
                logger.info { "Analysis completed successfully" }
                AnalyzeResult.success(outputParser.parseAnalysisOutput(result.output))
            } else {
                logger.error { "Analysis failed: ${result.output}" }
                AnalyzeResult.failure(result.output)
            }
        } catch (e: Exception) {
            logger.error(e) { "Analysis execution failed" }
            AnalyzeResult.failure(e.message ?: "Unknown error")
        }
    }

    /**
     * Format source file or directory
     */
    fun format(
        src: Path,
        inPlace: Boolean = true
    ): FormatResult {
        logger.info { "Running BSL format: src=$src, inPlace=$inPlace" }

        val args = mutableListOf(
            "--format",
            "--src", src.toString()
        )

        return try {
            val result = executeCliCommand(src.parent, *args.toTypedArray())

            if (result.isSuccess) {
                logger.info { "Format completed successfully" }
                FormatResult.success(outputParser.parseFormatOutput(result.output))
            } else {
                logger.error { "Format failed: ${result.output}" }
                FormatResult.failure(result.output)
            }
        } catch (e: Exception) {
            logger.error(e) { "Format execution failed" }
            FormatResult.failure(e.message ?: "Unknown error")
        }
    }

    private fun executeCliCommand(workDir: Path, vararg args: String): ProcessResult {
        val process = BslProcess(
            jarPath = Path(properties.jarPath),
            maxHeap = properties.jvm.maxHeap,
            workspaceDir = workDir,
            mode = BslMode.CLI
        )

        return try {
            process.executeCliCommand(*args)
        } finally {
            process.close()
        }
    }

    private fun createConfigJson(language: String): String {
        // Temporary config file with language setting
        return """{"language":"$language"}"""
    }

}

sealed class AnalyzeResult {
    data class Success(val data: Map<String, Any>) : AnalyzeResult()
    data class Failure(val error: String) : AnalyzeResult()

    companion object {
        fun success(data: Map<String, Any>) = Success(data)
        fun failure(error: String) = Failure(error)
    }
}

sealed class FormatResult {
    data class Success(val data: Map<String, Any>) : FormatResult()
    data class Failure(val error: String) : FormatResult()

    companion object {
        fun success(data: Map<String, Any>) = Success(data)
        fun failure(error: String) = Failure(error)
    }
}

