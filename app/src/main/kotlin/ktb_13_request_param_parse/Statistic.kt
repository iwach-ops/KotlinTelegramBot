package org.example.app.ktb_13_request_param_parse

data class Statistic(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
){
    fun printFormat(): String = "Learned ${learnedCount} from $totalCount words | $percent%"
}