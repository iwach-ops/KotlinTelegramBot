package org.example.app.ktb_22_multi_users

data class Statistic(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
){
    fun printFormat(): String = "Learned ${learnedCount} from $totalCount words | $percent%"
}