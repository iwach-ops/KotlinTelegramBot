package org.example.app.ktb_26_loading_dictionary

import java.sql.DriverManager
import java.time.LocalDateTime

class DatabaseUserDictionary(
    private val chatId: Long,
    private val username: String? = null,
    private val dbUrl: String = "jdbc:sqlite:data.db"
) : IUserDictionary {

    override fun getNumOfLearnedWords(): Int {
        DriverManager.getConnection(dbUrl).use { connection ->
            val userId = getOrCreateUserId(connection)

            connection.prepareStatement(
                """
                SELECT COUNT(*)
                FROM user_answers ua
                WHERE ua.user_id = ?
                  AND ua.correct_answer_count >= ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setInt(2, MIN_CORRECT_ANSWERS)

                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    override fun getSize(): Int {
        DriverManager.getConnection(dbUrl).use { connection ->
            connection.prepareStatement(
                """
                SELECT COUNT(*)
                FROM words
                """.trimIndent()
            ).use { statement ->
                val resultSet = statement.executeQuery()
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    override fun getLearnedWords(): List<Word> {
        DriverManager.getConnection(dbUrl).use { connection ->
            val userId = getOrCreateUserId(connection)

            connection.prepareStatement(
                """
                SELECT w.text, w.translate, ua.correct_answer_count
                FROM words w
                JOIN user_answers ua ON ua.word_id = w.id
                WHERE ua.user_id = ?
                  AND ua.correct_answer_count >= ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setInt(2, MIN_CORRECT_ANSWERS)

                val resultSet = statement.executeQuery()
                val result = mutableListOf<Word>()

                while (resultSet.next()) {
                    result.add(
                        Word(
                            word = resultSet.getString("text"),
                            translate = resultSet.getString("translate"),
                            correctAnswersCount = resultSet.getInt("correct_answer_count")
                        )
                    )
                }

                return result
            }
        }
    }

    override fun getUnlearnedWords(): List<Word> {
        DriverManager.getConnection(dbUrl).use { connection ->
            val userId = getOrCreateUserId(connection)

            connection.prepareStatement(
                """
                SELECT 
                    w.text,
                    w.translate,
                    COALESCE(ua.correct_answer_count, 0) AS correct_answer_count
                FROM words w
                LEFT JOIN user_answers ua
                    ON ua.word_id = w.id AND ua.user_id = ?
                WHERE COALESCE(ua.correct_answer_count, 0) < ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setInt(2, MIN_CORRECT_ANSWERS)

                val resultSet = statement.executeQuery()
                val result = mutableListOf<Word>()

                while (resultSet.next()) {
                    result.add(
                        Word(
                            word = resultSet.getString("text"),
                            translate = resultSet.getString("translate"),
                            correctAnswersCount = resultSet.getInt("correct_answer_count")
                        )
                    )
                }

                return result
            }
        }
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        DriverManager.getConnection(dbUrl).use { connection ->
            val userId = getOrCreateUserId(connection)
            val wordId = getWordId(connection, word)
                ?: throw IllegalArgumentException("Word not found in database: $word")

            connection.prepareStatement(
                """
                INSERT INTO user_answers(user_id, word_id, correct_answer_count, updated_at)
                VALUES(?, ?, ?, ?)
                ON CONFLICT(user_id, word_id)
                DO UPDATE SET
                    correct_answer_count = excluded.correct_answer_count,
                    updated_at = excluded.updated_at
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, userId)
                statement.setLong(2, wordId)
                statement.setInt(3, correctAnswersCount)
                statement.setString(4, LocalDateTime.now().toString())
                statement.executeUpdate()
            }
        }
    }

    override fun resetUserProgress() {
        DriverManager.getConnection(dbUrl).use { connection ->
            val userId = getOrCreateUserId(connection)

            connection.prepareStatement(
                """
                DELETE FROM user_answers
                WHERE user_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, userId)
                statement.executeUpdate()
            }
        }
    }

    private fun getOrCreateUserId(connection: java.sql.Connection): Long {
        connection.prepareStatement(
            """
            SELECT id
            FROM users
            WHERE chat_id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, chatId)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                return resultSet.getLong("id")
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO users(username, created_at, chat_id)
            VALUES(?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, username)
            statement.setString(2, LocalDateTime.now().toString())
            statement.setLong(3, chatId)
            statement.executeUpdate()
        }

        connection.prepareStatement(
            """
            SELECT id
            FROM users
            WHERE chat_id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, chatId)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                return resultSet.getLong("id")
            }
        }

        throw IllegalStateException("Could not create or find user for chat_id=$chatId")
    }

    private fun getWordId(connection: java.sql.Connection, word: String): Long? {
        connection.prepareStatement(
            """
            SELECT id
            FROM words
            WHERE text = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, word)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getLong("id") else null
        }
    }
}