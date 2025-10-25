package com.github.steel33ff.mcpbsl.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.test.context.SpringBootTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class PathTypeServiceTest {

    private val pathTypeService = PathTypeService()

    @Test
    fun `should detect BSL file`(@TempDir tempDir: Path) {
        // Given
        val bslFile = tempDir.resolve("test.bsl")
        Files.writeString(bslFile, "Процедура Тест()\nКонецПроцедуры")

        // When
        val result = pathTypeService.isBslFile(bslFile)

        // Then
        assertTrue(result)
    }

    @Test
    fun `should detect OS file`(@TempDir tempDir: Path) {
        // Given
        val osFile = tempDir.resolve("test.os")
        Files.writeString(osFile, "Процедура Тест()\nКонецПроцедуры")

        // When
        val result = pathTypeService.isBslFile(osFile)

        // Then
        assertTrue(result)
    }

    @Test
    fun `should not detect non-BSL file`(@TempDir tempDir: Path) {
        // Given
        val txtFile = tempDir.resolve("test.txt")
        Files.writeString(txtFile, "Some text")

        // When
        val result = pathTypeService.isBslFile(txtFile)

        // Then
        assertFalse(result)
    }

    @Test
    fun `should detect BSL directory`(@TempDir tempDir: Path) {
        // Given
        val bslFile1 = tempDir.resolve("test1.bsl")
        val bslFile2 = tempDir.resolve("test2.os")
        Files.writeString(bslFile1, "Процедура Тест1()\nКонецПроцедуры")
        Files.writeString(bslFile2, "Процедура Тест2()\nКонецПроцедуры")

        // When
        val result = pathTypeService.isBslDirectory(tempDir)

        // Then
        assertTrue(result)
    }

    @Test
    fun `should not detect non-BSL directory`(@TempDir tempDir: Path) {
        // Given
        val txtFile = tempDir.resolve("test.txt")
        Files.writeString(txtFile, "Some text")

        // When
        val result = pathTypeService.isBslDirectory(tempDir)

        // Then
        assertFalse(result)
    }

    @Test
    fun `should return correct path type for BSL file`(@TempDir tempDir: Path) {
        // Given
        val bslFile = tempDir.resolve("test.bsl")
        Files.writeString(bslFile, "Процедура Тест()\nКонецПроцедуры")

        // When
        val result = pathTypeService.getPathType(bslFile)

        // Then
        assertEquals(PathType.BSL_FILE, result)
    }

    @Test
    fun `should return correct path type for BSL directory`(@TempDir tempDir: Path) {
        // Given
        val bslFile = tempDir.resolve("test.bsl")
        Files.writeString(bslFile, "Процедура Тест()\nКонецПроцедуры")

        // When
        val result = pathTypeService.getPathType(tempDir)

        // Then
        assertEquals(PathType.BSL_DIRECTORY, result)
    }

    @Test
    fun `should return NOT_FOUND for non-existent path`(@TempDir tempDir: Path) {
        // Given
        val nonExistentFile = tempDir.resolve("nonexistent.bsl")

        // When
        val result = pathTypeService.getPathType(nonExistentFile)

        // Then
        assertEquals(PathType.NOT_FOUND, result)
    }

    @Test
    fun `should find BSL files in directory`(@TempDir tempDir: Path) {
        // Given
        val bslFile1 = tempDir.resolve("test1.bsl")
        val bslFile2 = tempDir.resolve("test2.os")
        val txtFile = tempDir.resolve("test.txt")
        Files.writeString(bslFile1, "Процедура Тест1()\nКонецПроцедуры")
        Files.writeString(bslFile2, "Процедура Тест2()\nКонецПроцедуры")
        Files.writeString(txtFile, "Some text")

        // When
        val result = pathTypeService.findBslFiles(tempDir)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains(bslFile1))
        assertTrue(result.contains(bslFile2))
        assertFalse(result.contains(txtFile))
    }

    @Test
    fun `should calculate file size`(@TempDir tempDir: Path) {
        // Given
        val content = "Процедура Тест()\nКонецПроцедуры"
        val bslFile = tempDir.resolve("test.bsl")
        Files.writeString(bslFile, content)

        // When
        val result = pathTypeService.getFileSize(bslFile)

        // Then
        assertEquals(content.toByteArray().size.toLong(), result)
    }

    @Test
    fun `should calculate directory size`(@TempDir tempDir: Path) {
        // Given
        val content1 = "Процедура Тест1()\nКонецПроцедуры"
        val content2 = "Процедура Тест2()\nКонецПроцедуры"
        val bslFile1 = tempDir.resolve("test1.bsl")
        val bslFile2 = tempDir.resolve("test2.bsl")
        Files.writeString(bslFile1, content1)
        Files.writeString(bslFile2, content2)

        // When
        val result = pathTypeService.getDirectorySize(tempDir)

        // Then
        val expectedSize = content1.toByteArray().size + content2.toByteArray().size
        assertEquals(expectedSize.toLong(), result)
    }
}
