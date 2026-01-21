package org.example.app.ktb_09_refactoring

const val MIN_CORRECT_ANSWERS = 3
const val MAX_ANSWER_OPTIONS = 4
const val DICTIONARY_FILE = "words.txt"
const val DELIMITER = "|"

data class Word(
    val word: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)

fun Word.isLearned(): Boolean = correctAnswersCount >= MIN_CORRECT_ANSWERS