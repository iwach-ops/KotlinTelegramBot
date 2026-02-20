package ktb_22_multi_users

import org.example.app.ktb_22_multi_users.LearnWordsTrainer
import org.example.app.ktb_22_multi_users.Question
import org.example.app.ktb_22_multi_users.Statistic
import org.example.app.ktb_22_multi_users.Word
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearnWordsTrainerTest {

    @Test
    fun testStatisticsWith4wordsOf7() {
        val trainer = LearnWordsTrainer("src/test/4_words_of_7.txt").apply { loadDictionary() }

        assertEquals(
            Statistic(totalCount = 7, learnedCount = 4, percent = 57),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test statistics with corrupted file`() {

        val trainer = LearnWordsTrainer("src/test/corrupted_words.txt").apply { loadDictionary() }

        assertEquals(
            Statistic(totalCount = 4, learnedCount = 1, percent = 25),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test getNextQuestion() with 5 unlearned words`() {
        val trainer = LearnWordsTrainer("src/test/5_unlearned.txt").apply { loadDictionary() }

        val question = assertNotNull(
            trainer.getNextQuestion(),
            "Question must not be null when there are unlearned words"
        )

        assertEquals(4, question.options.size)
        assertTrue(question.correctAnswerId in 1..4)
    }

    @Test
    fun `test getNextQuestion() with 1 unlearned word`() {

        val trainer = LearnWordsTrainer("src/test/1_unlearned.txt").apply { loadDictionary() }

        val question = assertNotNull(
            trainer.getNextQuestion(),
            "Question must not be null when there is 1 unlearned word"
        )

        assertEquals(4, question.options.size)

        assertEquals("cat", question.correctAnswer.word)
        assertEquals("кошка", question.correctAnswer.translate)
    }

    @Test
    fun `test getNextQuestion() with all words learned`() {

        val trainer = LearnWordsTrainer("src/test/all_learned.txt").apply { loadDictionary() }

        val q = trainer.getNextQuestion()

        assertNull(q)
    }

    @Test
    fun `test checkAnswer() with true`() {
        val trainer = LearnWordsTrainer("dummy.txt") // файл тут не нужен

        val q = Question(
            options = listOf(
                Word("cat", "кошка", 0),
                Word("dog", "собака", 0),
                Word("house", "дом", 0),
                Word("car", "машина", 0)
            ),
            correctAnswer = Word("dog", "собака", 0),
            correctAnswerId = 2
        )

        val result = trainer.checkAnswer(2, q)

        assertTrue(result)
    }

    @Test
    fun `test checkAnswer() with false`() {
        val trainer = LearnWordsTrainer("dummy.txt")

        val q = Question(
            options = listOf(
                Word("cat", "кошка", 0),
                Word("dog", "собака", 0),
                Word("house", "дом", 0),
                Word("car", "машина", 0)
            ),
            correctAnswer = Word("dog", "собака", 0),
            correctAnswerId = 2
        )

        val result = trainer.checkAnswer(3, q)

        assertFalse(result)
    }

    @Test
    fun `test resetProgress() with 2 words in dictionary`() {
        val trainer = LearnWordsTrainer("src/test/reset_2_words.txt").apply { loadDictionary() }

        trainer.resetProgress()

        trainer.loadDictionary()

        assertEquals(2, trainer.dictionary.size)
        assertEquals(0, trainer.dictionary[0].correctAnswersCount)
        assertEquals(0, trainer.dictionary[1].correctAnswersCount)

        assertEquals(
            Statistic(totalCount = 2, learnedCount = 0, percent = 0),
            trainer.getStatistics()
        )
    }
}