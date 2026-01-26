package org.example.app.ktb_13_request_param_parse

const val MIN_CORRECT_ANSWERS = 3

data class Word(
    val word: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)

fun Word.isLearned(): Boolean = correctAnswersCount >= MIN_CORRECT_ANSWERS