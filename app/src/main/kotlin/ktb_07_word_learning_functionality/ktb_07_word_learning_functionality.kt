package org.example.app.ktb_07_word_learning_functionality

import java.io.File

const val MIN_CORRECT_ANSWERS = 3
const val MAX_ANSWER_OPTIONS = 4

fun main() {
    val dictionary = loadDictionary().toMutableList()

    while (true) {
        println(
            """
        Menu:
        1 – Learn words
        2 – Statistics
        0 – Exit        
    """.trimIndent()
        )
        val choice = readln().toIntOrNull()

        when (choice) {
            1 -> {
                println("Learn words")
                learnWords(dictionary)
            }

            2 -> {
                println("Statistics")
                val learnedWords = dictionary.filter { it.correctAnswersCount >= MIN_CORRECT_ANSWERS }
                val totalCount = dictionary.size

                val percent = if (totalCount == 0) 0 else learnedWords.size * 100 / totalCount

                println("Learned ${learnedWords.size} from $totalCount words | $percent%")
            }

            0 -> return
            else -> println("Enter the number 1, 2 or 0")
        }
    }
}

fun learnWords(dictionary: MutableList<Word>) {
    while (true) {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < MIN_CORRECT_ANSWERS }

        if (notLearnedList.isEmpty()) {
            println("All the words in the dictionary have been learned.")
            println()
            return
        }

        val questionWords = notLearnedList
            .shuffled()
            .take(MAX_ANSWER_OPTIONS)
            .toMutableList()

         val options = questionWords.shuffled()

        val correctAnswer = options.random()

        println()
        println("${correctAnswer.word}:")
        options.forEachIndexed { index, word ->
            println("${index + 1} - ${word.translate}")
        }

        val answerNumber = readln().toIntOrNull()
        if (answerNumber == null || answerNumber !in 1..options.size) {
            println("Enter a number from 1 to ${options.size}")
            continue
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