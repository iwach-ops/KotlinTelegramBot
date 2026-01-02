package org.example.app.ktb_08_checkthe_correctness_user_answer

import java.io.File

const val MIN_CORRECT_ANSWERS = 3
const val MAX_ANSWER_OPTIONS = 4
const val DICTIONARY_FILE = "words.txt"

fun main() {
    val dictionary = loadDictionary().toMutableList()

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

fun saveDictionary(dictionary: List<Word>) {
    val dictionaryFile = File(DICTIONARY_FILE)
    val content = buildString {
        dictionary.forEach {
            append(it.word)
            append("|")
            append(it.translate)
            append("|")
            append(it.correctAnswersCount)
            append("\n")
        }
    }
    dictionaryFile.writeText(content)
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

        val correctAnswerId = options.indexOf(correctAnswer) + 1

        println()
        println("${correctAnswer.word}:")
        options.forEachIndexed { index, word ->
            println("${index + 1} - ${word.translate}")
        }
        println("--------------")
        println("0 - Menu")

        val userAnswerInput = readln().toIntOrNull()
        if (userAnswerInput == 0) return
        if (userAnswerInput == null || userAnswerInput !in 1..options.size) {
            println("Enter a number from 1 to ${options.size}")
            continue
        }

        if (userAnswerInput == correctAnswerId) {
            println("Right!")


            val idx =
                dictionary.indexOfFirst { it.word == correctAnswer.word && it.translate == correctAnswer.translate }
            if (idx != -1) {
                val old = dictionary[idx]
                dictionary[idx] = old.copy(correctAnswersCount = old.correctAnswersCount + 1)
            }
            saveDictionary(dictionary)
        } else {
            println("Wrong answer! ${correctAnswer.word} - is  ${correctAnswer.translate}")
        }
    }
}

fun loadDictionary(): List<Word> {
    val dictionaryFile = File(DICTIONARY_FILE)
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