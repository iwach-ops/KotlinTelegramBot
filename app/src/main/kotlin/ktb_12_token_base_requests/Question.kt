package org.example.app.ktb_12_token_base_requests

data class Question(
    val options: List<Word>,
    val correctAnswer: Word,
    val correctAnswerId: Int,
)

fun Question.asConsoleString(){
    println()
    println("${correctAnswer.word}:")
    options.forEachIndexed { index, word ->
        println("${index + 1} - ${word.translate}")
    }
    println("--------------")
    println("0 - Menu")
}
