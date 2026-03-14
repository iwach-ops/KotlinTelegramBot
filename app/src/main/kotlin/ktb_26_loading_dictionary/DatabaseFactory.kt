package org.example.app.ktb_26_loading_dictionary

import java.sql.DriverManager

object DatabaseFactory {
    private const val DEFAULT_DB_URL = "jdbc:sqlite:data.db"

    fun initDatabase(dbUrl: String = DEFAULT_DB_URL) {
        DriverManager.getConnection(dbUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username VARCHAR,
                        created_at TIMESTAMP NOT NULL,
                        chat_id INTEGER NOT NULL UNIQUE
                    );
                    """.trimIndent()
                )

                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS words (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        text VARCHAR NOT NULL UNIQUE,
                        translate VARCHAR NOT NULL
                    );
                    """.trimIndent()
                )

                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS user_answers (
                        user_id INTEGER NOT NULL,
                        word_id INTEGER NOT NULL,
                        correct_answer_count INTEGER NOT NULL DEFAULT 0,
                        updated_at TIMESTAMP NOT NULL,
                        PRIMARY KEY (user_id, word_id),
                        FOREIGN KEY (user_id) REFERENCES users(id),
                        FOREIGN KEY (word_id) REFERENCES words(id)
                    );
                    """.trimIndent()
                )
            }
        }
    }
}