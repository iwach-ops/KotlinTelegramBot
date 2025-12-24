package org.example.app.ktb_02_primary_dictionary

import java.io.File

fun main() {
    val dictionaryFile = File("dictionary.txt")

    dictionaryFile.createNewFile()
    dictionaryFile.appendText("hello привет\n")
    dictionaryFile.appendText("dog собака\n")
    dictionaryFile.appendText("cat кошка\n")

    dictionaryFile.readLines().forEach { println(it) }
}
