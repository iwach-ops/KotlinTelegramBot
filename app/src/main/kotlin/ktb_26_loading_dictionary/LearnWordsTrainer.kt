package org.example.app.ktb_26_loading_dictionary

import kotlinx.serialization.Serializable

const val MAX_ANSWER_OPTIONS = 4
const val DICTIONARY_FILE = "words.txt"
const val DELIMITER = "|"
const val MIN_CORRECT_ANSWERS = 3

class LearnWordsTrainer(
    private val userDictionary: IUserDictionary
) {
    fun resetProgress() {
        userDictionary.resetUserProgress()
    }

    fun getStatistics(): Statistic {
        val total = userDictionary.getSize()
        val learned = userDictionary.getNumOfLearnedWords()
        val percent = if (total == 0) 0 else learned * 100 / total
        return Statistic(total, learned, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearned = userDictionary.getUnlearnedWords()

        if (notLearned.isEmpty()) {
            println("All the words in the dictionary have been learned.")
            println()
            return null
        }

        val questionWords = if (notLearned.size < MAX_ANSWER_OPTIONS) {
            val learned = userDictionary.getLearnedWords().shuffled()
            notLearned.shuffled().take(MAX_ANSWER_OPTIONS) +
                    learned.take(MAX_ANSWER_OPTIONS - notLearned.size)
        } else {
            notLearned.shuffled().take(MAX_ANSWER_OPTIONS)
        }

        val options = questionWords.shuffled()
        val correctAnswer = options.filter { it.correctAnswersCount < MIN_CORRECT_ANSWERS }.random()
        val correctAnswerId = options.indexOf(correctAnswer) + 1

        return Question(options, correctAnswer, correctAnswerId)
    }

    fun saveCorrectAnswer(correctAnswer: Word) {
        val newCorrectAnswersCount = correctAnswer.correctAnswersCount + 1
        userDictionary.setCorrectAnswersCount(correctAnswer.word, newCorrectAnswersCount)
    }

    fun checkAnswer(userAnswerInput: Int, question: Question): Boolean =
        userAnswerInput == question.correctAnswerId
}

@Serializable
data class Word(
    val word: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

fun Word.isLearned(): Boolean = correctAnswersCount >= MIN_CORRECT_ANSWERS