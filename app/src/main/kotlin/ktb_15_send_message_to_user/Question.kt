package org.example.app.ktb_15_send_message_to_user

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
