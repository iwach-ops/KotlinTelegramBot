package org.example.app.ktb_06_develop_simulator_menu

import java.io.File

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println(
            """
        Menu:
        1 – Learn words
        2 – Statistics
        0 – Exit        
    """.trimIndent()
        )
        val choice = readLine()?.toIntOrNull()

        when (choice) {
            1 -> println("Learn words")
            2 -> {
                println("Statistics")
                val learnedWords = dictionary.filter { it.correctAnswersCount >= 3 }
                val totalCount = dictionary.count()

                val percent = if (totalCount == 0) 0 else learnedWords.size * 100 / totalCount

                println("Learned ${learnedWords.size} from $totalCount words | $percent%")
                println()
            }

            0 -> return
            else -> println("Enter the number 1, 2 or 0")
        }
    }
}

fun loadDictionary(): List<Word> {
    val dictionaryFile = File("words.txt")
    dictionaryFile.createNewFile()

    val dictionary = mutableListOf<Word>()

    dictionaryFile.readLines().forEach {
        val line = it.split("|")
        val word = line.getOrNull(0)?.trim().orEmpty()
        val translate = line.getOrNull(1)?.trim().orEmpty()
        val correctAnswersCount = line.getOrNull(2)?.trim()?.toIntOrNull() ?: 0

        val wordElement = Word(word, translate, correctAnswersCount)

        dictionary.add(wordElement)
    }
    return dictionary
}

data class Word(
    val word: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)