package org.example.app.ktb_16_develop_buttons_send

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class TelegramBotService(
    private val botToken: String,
    private val client: HttpClient = HttpClient.newBuilder().build(),
) {
    private val baseUrl = "$TELEGRAM_BASE_URL$botToken"

    fun getUpdates(updateId: Int): String {
        val urlGetUpdates = "$baseUrl/getUpdates?offset=$updateId"

        val requestGetUpdates = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()

        val responseGetUpdates = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

        return responseGetUpdates.body()
    }

    fun sendMessage(chatId: Long, text: String): String {
        val urlSendMessage = "$baseUrl/sendMessage"

        val formatText = text.trim().take(4096)
        require(formatText.isNotEmpty()) { "text must not be empty" }

        val body = "chat_id=$chatId&text=${URLEncoder.encode(formatText, StandardCharsets.UTF_8)}"

        val requestSendMessage = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val responseSendMessage = client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        return responseSendMessage.body()
    }

    fun sendMenu(chatId: Long): String {
        val urlSendMessage = "$baseUrl/sendMessage"

        val sendMenuBody = """
            {
              "chat_id": $chatId,
              "text": "Main Menu",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "Learn Words",
                      "callback_data": "learnWords_clicked"
                    },
                    {
                      "text": "Statistic",
                      "callback_data": "statistic_clicked"
                    }
                  ]
                ]
              }
            }          
            """.trimIndent()

        val requestSendMenu = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()

        val responseSendMenu = client.send(requestSendMenu, HttpResponse.BodyHandlers.ofString())

        return responseSendMenu.body()
    }

}