package org.example.app.ktb_04_parse_print_dictionary_content

import java.io.File

fun main() {
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

    println(dictionary.joinToString(separator = "\n"))
}

data class Word(
    val word: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)