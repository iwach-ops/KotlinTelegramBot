package org.example.app.ktb_15_send_message_to_user

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

    fun getUpdates(client: HttpClient, botToken: String, updateId: Int): String {
        val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"

        val requestGetUpdates = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()

        val responseGetUpdates = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

        return responseGetUpdates.body()
    }

    fun sendMessage(client: HttpClient, botToken: String, chatId: Long, text: String): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"

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
}