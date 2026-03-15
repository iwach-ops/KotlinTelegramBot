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
            connection.prepareStatement(
                """
                INSERT OR IGNORE INTO words(text, translate)
                VALUES(?, ?)
                """.trimIndent()
            ).use { insertStatement ->

                wordsFile.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { line ->
                        val parts = line.split(DELIMITER)

                        val rawWord = parts.getOrNull(0)?.trim().orEmpty()
                        val rawTranslate = parts.getOrNull(1)?.trim().orEmpty()

                        SecurityUtils.logSuspiciousInput("dictionary_word", rawWord)
                        SecurityUtils.logSuspiciousInput("dictionary_translate", rawTranslate)

                        val word = runCatching {
                            SecurityUtils.validateDictionaryValue(rawWord, "word")
                        }.getOrNull()

                        val translate = runCatching {
                            SecurityUtils.validateDictionaryValue(rawTranslate, "translate")
                        }.getOrNull()

                        if (word == null || translate == null) return@forEach

                        insertStatement.setString(1, word)
                        insertStatement.setString(2, translate)
                        insertStatement.executeUpdate()
                    }
            }
        }
    }
}