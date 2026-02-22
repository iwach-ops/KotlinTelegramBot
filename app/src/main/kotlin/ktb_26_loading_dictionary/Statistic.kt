package org.example.app.ktb_26_loading_dictionary

data class Statistic(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
){
    fun printFormat(): String = "Learned ${learnedCount} from $totalCount words | $percent%"
}