
package org.example.app.ktb_18_word_learning_functionality

import java.io.File
import java.io.IOException

const val MAX_ANSWER_OPTIONS = 4
const val DICTIONARY_FILE = "words.txt"
const val DELIMITER = "|"

class LearnWordsTrainer {
    val dictionary = mutableListOf<Word>()

    fun loadDictionary() {
        val dictionaryFile = File(DICTIONARY_FILE)
        try {
            if (!dictionaryFile.exists()) dictionaryFile.createNewFile()

            dictionary.clear()

            dictionaryFile.readLines().forEach {
                val line = it.split(DELIMITER)
                val word = line.getOrNull(0)?.trim().orEmpty()
                val translate = line.getOrNull(1)?.trim().orEmpty()
                val correctAnswersCount = line.getOrNull(2)?.trim()?.toIntOrNull() ?: 0

                val wordElement = Word(word, translate, correctAnswersCount)

                dictionary.add(wordElement)
            }
        } catch (e: IOException) {
            println("Error reading file: ${e.message}")
        }
    }

    fun saveDictionary() {
        val dictionaryFile = File(DICTIONARY_FILE)
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

    fun getStatistics(): Statistic {
        val total = dictionary.size
        val learned = dictionary.count { it.isLearned() }
        val percent = if (total == 0) 0 else learned * 100 / total
        return Statistic(total, learned, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearned = dictionary.filter { it.correctAnswersCount < MIN_CORRECT_ANSWERS }

        if (notLearned.isEmpty()) {
            println("All the words in the dictionary have been learned.")
            println()
            return null
        }

        val questionWords = if (notLearned.size < MAX_ANSWER_OPTIONS) {
            val learned = dictionary.filter { it.correctAnswersCount >= MIN_CORRECT_ANSWERS }.shuffled()
            notLearned.shuffled().take(MAX_ANSWER_OPTIONS) + learned.take(MAX_ANSWER_OPTIONS - notLearned.size)
        } else {
            notLearned.shuffled().take(MAX_ANSWER_OPTIONS)
        }

        val options = questionWords.shuffled()

        val correctAnswer = options.filter { it.correctAnswersCount < MIN_CORRECT_ANSWERS }.random()

        val correctAnswerId = options.indexOf(correctAnswer) + 1

        return Question(options, correctAnswer, correctAnswerId)
    }

    fun saveCorrectAnswer(correctAnswer: Word) {
        val foundWordId =
            dictionary.indexOfFirst { it.word == correctAnswer.word && it.translate == correctAnswer.translate }

        if (foundWordId != -1) {
            val foundWord = dictionary[foundWordId]
            dictionary[foundWordId] = foundWord.copy(correctAnswersCount = foundWord.correctAnswersCount + 1)
        }

        saveDictionary()
    }

    fun checkAnswer(userAnswerInput: Int, question: Question): Boolean = userAnswerInput == question.correctAnswerId
}

const val MIN_CORRECT_ANSWERS = 3

data class Word(
    val word: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)

fun Word.isLearned(): Boolean = correctAnswersCount >= MIN_CORRECT_ANSWERS