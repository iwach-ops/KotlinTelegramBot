package org.example.app.ktb_26_loading_dictionary

import java.io.File
import java.io.IOException

class FileUserDictionary(
    private val fileName: String,
    private val learningThreshold: Int = MIN_CORRECT_ANSWERS,
) : IUserDictionary {

    private val dictionary = try {
        loadDictionary()
    } catch (e: Exception) {
        throw IllegalArgumentException("Incorrect file")
    }

    override fun getNumOfLearnedWords(): Int =
        dictionary.count { it.correctAnswersCount >= learningThreshold }

    override fun getSize(): Int =
        dictionary.size

    override fun getLearnedWords(): List<Word> =
        dictionary.filter { it.correctAnswersCount >= learningThreshold }

    override fun getUnlearnedWords(): List<Word> =
        dictionary.filter { it.correctAnswersCount < learningThreshold }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        dictionary.find { it.word == word }?.correctAnswersCount = correctAnswersCount
        saveDictionary()
    }

    override fun resetUserProgress() {
        dictionary.forEach { it.correctAnswersCount = 0 }
        saveDictionary()
    }

    private fun loadDictionary(): MutableList<Word> {
        val dictionaryFile = File(fileName)

        try {
            if (!dictionaryFile.exists()) {
                val template = File(DICTIONARY_FILE)
                if (template.exists() && template.absolutePath != dictionaryFile.absolutePath) {
                    template.copyTo(dictionaryFile, overwrite = false)
                } else {
                    dictionaryFile.createNewFile()
                }
            }

            val result = mutableListOf<Word>()

            dictionaryFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach {
                    val line = it.split(DELIMITER)
                    val word = line.getOrNull(0)?.trim().orEmpty()
                    val translate = line.getOrNull(1)?.trim().orEmpty()
                    val correctAnswersCount = line.getOrNull(2)?.trim()?.toIntOrNull() ?: 0

                    result.add(Word(word, translate, correctAnswersCount))
                }

            return result
        } catch (e: IOException) {
            throw IllegalArgumentException("Error reading file: ${e.message}")
        }
    }

    private fun saveDictionary() {
        val dictionaryFile = File(fileName)
        val content = buildString {
            dictionary.forEach {
                append(it.word)
                append(DELIMITER)
                append(it.translate)
                append(DELIMITER)
                append(it.correctAnswersCount)
                append("\n")
            }
        }
        dictionaryFile.writeText(content)
    }
}