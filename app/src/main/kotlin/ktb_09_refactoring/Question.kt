package org.example.app.ktb_09_refactoring

data class Question(
    val options: List<Word>,
    val correctAnswer: Word,
    val correctAnswerId: Int,
)