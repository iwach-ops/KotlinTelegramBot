package org.example.app.ktb_14_parse_string_regex

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0
    while (true) {
        Thread.sleep(2000)

        val updates: String = getUpdates(botToken, updateId)
        println(updates)

        val updateIdRegex = Regex(""""update_id"\s*:\s*(\d+)""")
        val textRegex = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val userNameRegex = Regex(""""username"\s*:\s*"([^"]+)"""")

        val userName = userNameRegex.find(updates)?.groupValues?.get(1)
        val text = textRegex.find(updates)?.groupValues?.get(1)

        if (text != null) println("$userName : $text")

        val lastUpdateId = updateIdRegex
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.get(1)
            ?.toInt()

        if (lastUpdateId != null) updateId = lastUpdateId + 1
    }
}

fun getUpdates(botToken: String, updateId: Int): String {
    val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"

    val client: HttpClient = HttpClient.newBuilder().build()

    val requestGetUpdates = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()

    val responseGetUpdates = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

    return responseGetUpdates.body()
}