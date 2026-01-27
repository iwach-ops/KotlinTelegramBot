package org.example.app.ktb_13_request_param_parse

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val START_UPDATE_ID_PATTERN = "update_id"
const val END_UPDATE_ID_PATTERN = ",\n\"message\""

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0
    while (true) {
        Thread.sleep(2000)

        val updates: String = getUpdates(botToken, updateId)
        println(updates)

        var startUpdateId = updates.lastIndexOf(START_UPDATE_ID_PATTERN)
        val endUpdateId = updates.lastIndexOf(END_UPDATE_ID_PATTERN)

        if (startUpdateId == -1 || endUpdateId == -1) continue

        startUpdateId += START_UPDATE_ID_PATTERN.length + 2

        val updateIdString = updates.substring(startUpdateId, endUpdateId)

        updateId = updateIdString.toInt() + 1
    }
}

fun getUpdates(botToken: String, updateId: Int): String {
    val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"

    val client: HttpClient = HttpClient.newBuilder().build()

    val requestGetUpdates = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()

    val responseGetUpdates = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

    return responseGetUpdates.body()
}