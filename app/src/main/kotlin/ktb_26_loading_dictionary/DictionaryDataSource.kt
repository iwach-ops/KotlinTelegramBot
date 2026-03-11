package org.example.app.ktb_26_loading_dictionary

import java.io.File
import java.sql.DriverManager

class DictionaryDataSource(
    private val dbUrl: String = "jdbc:sqlite:data.db"
) {
    fun updateDictionary(wordsFile: File) {
        if (!wordsFile.exists()) {
            throw IllegalArgumentException("Dictionary file not found: ${wordsFile.path}")
        }

        DriverManager.getConnection(dbUrl).use { connection ->
            val insertStatement = connection.prepareStatement(
                """
                INSERT OR IGNORE INTO words(text, translate)
                VALUES(?, ?)
                """.trimIndent()
            )

            wordsFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { line ->
                    val parts = line.split(DELIMITER)
                    val word = parts.getOrNull(0)?.trim().orEmpty()
                    val translate = parts.getOrNull(1)?.trim().orEmpty()

                    if (word.isBlank() || translate.isBlank()) return@forEach

                    insertStatement.setString(1, word)
                    insertStatement.setString(2, translate)
                    insertStatement.executeUpdate()
                }

            insertStatement.close()
        }
    }
}