package org.example.app.ktb_15_send_message_to_user

import java.net.http.HttpClient

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val UPDATE_ID_REGEX_PATTERN = """"update_id"\s*:\s*(\d+)"""
const val TEXT_REGEX_PATTERN = """"text"\s*:\s*"((?:\\.|[^"\\])*)""""
const val USER_NAME_REGEX_PATTERN = """"username"\s*:\s*"([^"]+)""""
const val CHAT_ID_REGEX_PATTERN = """"chat"\s*:\s*\{[^}]*"id"\s*:\s*(-?\d+)"""

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0

    val client = HttpClient.newBuilder().build()

    val service = TelegramBotService(botToken)

    while (true) {
        Thread.sleep(2000)

        val updates: String = service.getUpdates(client, botToken, updateId)
        println(updates)

        val updateIdRegex = Regex(UPDATE_ID_REGEX_PATTERN)
        val textRegex = Regex(TEXT_REGEX_PATTERN)
        val userNameRegex = Regex(USER_NAME_REGEX_PATTERN)
        val chatIdRegex = Regex(CHAT_ID_REGEX_PATTERN)

        val userName = userNameRegex.find(updates)?.groupValues?.get(1)
        val text = textRegex.find(updates)?.groupValues?.get(1)
        val chatId = chatIdRegex.find(updates)?.groupValues?.get(1)

        if (chatId != null && text != null) {
            println("$userName: $text: Chat_ID:$chatId")
            service.sendMessage(client, botToken, chatId.toLong(), text)
        }

        val lastUpdateId = updateIdRegex
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.get(1)
            ?.toInt()

        if (lastUpdateId != null) updateId = lastUpdateId + 1
    }
}