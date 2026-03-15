package org.example.app.ktb_26_loading_dictionary

import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseUserDictionaryTest {

    private val dbUrl = "jdbc:sqlite:test.db"

    @Test
    fun `sql-like username should not break users table`() {
        File("test.db").delete()

        try {
            DatabaseFactory.initDatabase(dbUrl)

            val dict = DatabaseUserDictionary(
                chatId = 1001L,
                username = "'; DROP TABLE users; --",
                dbUrl = dbUrl
            )

            dict.getNumOfLearnedWords()

            DriverManager.getConnection(dbUrl).use { connection ->
                val resultSet = connection.createStatement()
                    .executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
                assertTrue(resultSet.next())
            }
        } finally {
            File("test.db").delete()
        }
    }

    @Test
    fun `sql-like dictionary entry should not break words table`() {
        File("test.db").delete()

        val file = File("test_words.txt")
        try {
            DatabaseFactory.initDatabase(dbUrl)
            file.writeText("'; DROP TABLE words; --|перевод\ncat|кот")

            val dictionarySource = DictionaryDataSource(dbUrl)
            dictionarySource.updateDictionary(file)

            DriverManager.getConnection(dbUrl).use { connection ->
                val resultSet = connection.createStatement()
                    .executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='words'")
                assertTrue(resultSet.next())
            }
        } finally {
            file.delete()
            File("test.db").delete()
        }
    }

    @Test
    fun `validate username should reject invalid characters`() {
        assertFailsWith<IllegalArgumentException> {
            SecurityUtils.validateUsername("bad name!!!")
        }
    }

    @Test
    fun `normal dictionary entry should still be inserted`() {
        File("test.db").delete()

        val file = File("test_words.txt")

        try {
            DatabaseFactory.initDatabase(dbUrl)
            file.writeText("cat|кот")

            val dictionarySource = DictionaryDataSource(dbUrl)
            dictionarySource.updateDictionary(file)

            DriverManager.getConnection(dbUrl).use { connection ->
                val resultSet = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM words WHERE text = 'cat'")
                assertTrue(resultSet.next())
                assertTrue(resultSet.getInt(1) == 1)
            }
        } finally {
            file.delete()
            File("test.db").delete()
        }
    }
}