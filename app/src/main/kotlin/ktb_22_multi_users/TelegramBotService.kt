package org.example.app.ktb_22_multi_users

import kotlinx.serialization.json.Json
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

    fun getUpdates(updateId: Long): String {
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

    fun sendMenu(json: Json, chatId: Long): String {
        val urlSendMessage = "$baseUrl/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Main Menu",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard("$LEARN_WORDS_CLICKED_CALLBACK_DATA", "Learn Words"),
                        InlineKeyboard("$STATISTIC_CALLBACK_DATA", "Statistic"),
                    ),
                    listOf(
                        InlineKeyboard("$RESET_CALLBACK_DATA", "Reset")
                    )
                )
            )
        )

        val requestBodyString = json.encodeToString(requestBody)

        val requestSendMenu = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()

        val responseSendMenu = client.send(requestSendMenu, HttpResponse.BodyHandlers.ofString())

        return responseSendMenu.body()
    }

    fun sendQuestion(json: Json, chatId: Long, question: Question): String {
        val urlSendMessage = "$baseUrl/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.word,
            replyMarkup = ReplyMarkup(
                listOf(question.options.mapIndexed { index, word ->
                    InlineKeyboard(
                        text = word.translate, callbackData = "${CALLBACK_DATA_ANSWER_PREFIX}${index + 1}"
                    )
                })
            )
        )

        val requestBodyString = json.encodeToString(requestBody)

        println("BODY QUESTION:\n$requestBodyString\n")

        val requestSendQuestion = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()

        val responseQuestion = client.send(requestSendQuestion, HttpResponse.BodyHandlers.ofString())
        val resp = responseQuestion.body()
        println("sendQuestion response: $resp\n")
        return resp
    }
}