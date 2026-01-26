package org.example.app.ktb_12_token_base_requests

data class Statistic(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
){
    fun printFormat(): String = "Learned ${learnedCount} from $totalCount words | $percent%"
}