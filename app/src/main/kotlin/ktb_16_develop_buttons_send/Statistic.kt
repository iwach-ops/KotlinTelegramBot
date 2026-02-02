package org.example.app.ktb_16_develop_buttons_send

data class Statistic(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
){
    fun printFormat(): String = "Learned ${learnedCount} from $totalCount words | $percent%"
}