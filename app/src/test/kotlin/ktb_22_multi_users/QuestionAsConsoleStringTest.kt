package ktb_22_multi_users

import org.example.app.ktb_22_multi_users.Question
import org.example.app.ktb_22_multi_users.Word
import org.example.app.ktb_22_multi_users.asConsoleString
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionAsConsoleStringTest {

    private fun captureStdout(block: () -> Unit): String {
        val originalOut = System.out
        val outBytes = ByteArrayOutputStream()
        val ps = PrintStream(outBytes, true, StandardCharsets.UTF_8)

        System.setOut(ps)
        try {
            block()
        } finally {
            System.setOut(originalOut)
        }


        return outBytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n")
    }

    @Test
    fun printsSomething() {
        val q = Question(
            options = listOf(
                Word("cat", "кошка", 0),
                Word("cat", "собака", 0),
                Word("cat", "дом", 0),
                Word("cat", "машина", 0)
            ),
            correctAnswer = Word("cat", "кошка", 0),
            correctAnswerId = 1
        )

        val out = captureStdout { q.asConsoleString() }

        assertTrue(out.isNotBlank())
        assertTrue(out.contains("cat:"))
    }

    @Test
    fun normalCaseWithFourOptions() {
        val q = Question(
            options = listOf(
                Word("cat", "кошка", 0),
                Word("cat", "собака", 0),
                Word("cat", "дом", 0),
                Word("cat", "машина", 0)
            ),
            correctAnswer = Word("cat", "кошка", 0),
            correctAnswerId = 1
        )

        val out = captureStdout { q.asConsoleString() }

        val expected = (
                "\n" +
                        "cat:\n" +
                        "1 - кошка\n" +
                        "2 - собака\n" +
                        "3 - дом\n" +
                        "4 - машина\n" +
                        "--------------\n" +
                        "0 - Menu\n"
                )

        assertEquals(expected, out)
    }

    @Test
    fun differentOrderOfOptions() {
        val q = Question(
            options = listOf(
                Word("cat", "дом", 0),
                Word("cat", "кошка", 0),
                Word("cat", "машина", 0),
                Word("cat", "собака", 0)
            ),
            correctAnswer = Word("cat", "кошка", 0),
            correctAnswerId = 2
        )

        val out = captureStdout { q.asConsoleString() }

        val expected = (
                "\n" +
                        "cat:\n" +
                        "1 - дом\n" +
                        "2 - кошка\n" +
                        "3 - машина\n" +
                        "4 - собака\n" +
                        "--------------\n" +
                        "0 - Menu\n"
                )

        assertEquals(expected, out)
    }

    @Test
    fun emptyOptionsList() {
        val q = Question(
            options = emptyList(),
            correctAnswer = Word("cat", "кошка", 0),
            correctAnswerId = 1
        )

        val out = captureStdout { q.asConsoleString() }

        val expected = (
                "\n" +
                        "cat:\n" +
                        "--------------\n" +
                        "0 - Menu\n"
                )

        assertEquals(expected, out)
    }

    @Test
    fun tenOptions() {
        val options = (1..10).map { i -> Word("word", "tr$i", 0) }

        val q = Question(
            options = options,
            correctAnswer = Word("word", "tr1", 0),
            correctAnswerId = 1
        )

        val out = captureStdout { q.asConsoleString() }

        assertTrue(out.contains("word:\n"))
        assertTrue(out.contains("1 - tr1\n"))
        assertTrue(out.contains("10 - tr10\n"))
        assertTrue(out.contains("0 - Menu\n"))
    }

    @Test
    fun twoHundredOptionsDoesNotCrash() {
        val options = (1..200).map { i -> Word("word", "tr$i", 0) }

        val q = Question(
            options = options,
            correctAnswer = Word("word", "tr1", 0),
            correctAnswerId = 1
        )

        val out = captureStdout { q.asConsoleString() }

        assertTrue(out.contains("200 - tr200\n"))
    }

    @Test
    fun specialCharactersPrintedAsIs() {
        val q = Question(
            options = listOf(
                Word("x", "кошка|(тест).", 0),
                Word("x", "другое(1).", 0),
                Word("x", "ещё|раз", 0),
                Word("x", "ok.", 0)
            ),
            correctAnswer = Word("cat(.)|", "кошка|(тест).", 0),
            correctAnswerId = 1
        )

        val out = captureStdout { q.asConsoleString() }

        assertTrue(out.contains("cat(.)|:\n"))
        assertTrue(out.contains("1 - кошка|(тест).\n"))
        assertTrue(out.contains("3 - ещё|раз\n"))
    }

    @Test
    fun WordsConsistingOfSpaces() {
        val q = Question(
            options = listOf(
                Word("   ", "   ", 0),
                Word("   ", "x", 0),
            ),
            correctAnswer = Word("   ", "   ", 0),
            correctAnswerId = 1
        )

        val out = captureStdout { q.asConsoleString() }

        assertTrue(out.contains("   :\n"))

        assertTrue(out.contains("1 -    \n"))
    }

    @Test
    fun EmptyStrings() {
        val q = Question(
            options = listOf(Word("", "", 0)),
            correctAnswer = Word("", "", 0),
            correctAnswerId = 1
        )

        val out = captureStdout { q.asConsoleString() }

        val expected = (
                "\n" +
                        ":\n" +
                        "1 - \n" +
                        "--------------\n" +
                        "0 - Menu\n"
                )

        assertEquals(expected, out)
    }
}