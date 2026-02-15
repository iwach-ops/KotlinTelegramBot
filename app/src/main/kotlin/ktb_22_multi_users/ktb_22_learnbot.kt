package org.example.app.ktb_22_multi_users

fun main() {
    val trainer = LearnWordsTrainer("words.txt")
    trainer.loadDictionary()

    while (true) {
        println(
            """
        Menu:
        1 - Learn words
        2 - Statistics
        0 - Exit        
    """.trimIndent()
        )
        val choice = readln().toIntOrNull()

        when (choice) {
            1 -> {
                println("Learn words")
                learnWords(trainer)
            }

            2 -> {
                println("Statistics")
                println(trainer.getStatistics().printFormat())
            }

            0 -> return
            else -> println("Enter the number 1, 2 or 0")
        }
    }
}

fun learnWords(trainer: LearnWordsTrainer) {
    while (true) {

        val question = trainer.getNextQuestion() ?: return

        question.asConsoleString()

        val userAnswerInput = readln().toIntOrNull()
        if (userAnswerInput == 0) return
        if (userAnswerInput == null || userAnswerInput !in 1..question.options.size) {
            println("Enter a number from 1 to ${question.options.size}")
            continue
        }

        if (trainer.checkAnswer(userAnswerInput, question)) {
            println("Right!")
            trainer.saveCorrectAnswer(question.correctAnswer)
        } else {
            println("Wrong answer! ${question.correctAnswer.word} - is  ${question.correctAnswer.translate}")
        }
    }
}