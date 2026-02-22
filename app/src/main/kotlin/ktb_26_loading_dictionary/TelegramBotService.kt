package org.example.app.ktb_26_loading_dictionary

import kotlinx.serialization.json.Json
import org.example.app.ktb_22_multi_users.TELEGRAM_BASE_URL
import java.io.File
import java.io.InputStream
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
    private val fileBaseUrl = "$TELEGRAM_FILE_BASE_URL$botToken"


    fun downloadFile(filePath: String, targetFile: File) {

        val url = "$fileBaseUrl/$filePath"
        println("download url: $url")

        targetFile.parentFile?.mkdirs()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        val response: HttpResponse<InputStream> =
            client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        println("download status=${response.statusCode()}")

        if (response.statusCode() != 200) {
            throw IllegalStateException("Download failed, status=${response.statusCode()}")
        }

        response.body().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output, 16 * 1024)
            }
        }
    }

    fun getFile(json: Json, fileId: String): GetFileResponse {
        val urlGetFile = "$baseUrl/getFile"

        val requestBody = GetFileRequest(fileId = fileId)
        val requestBodyString = json.encodeToString(requestBody)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(urlGetFile))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return json.decodeFromString<GetFileResponse>(response.body())
    }

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
                listOf(
                    question.options.mapIndexed { index, word ->
                        InlineKeyboard(
                            text = word.translate, callbackData = "${CALLBACK_DATA_ANSWER_PREFIX}${index + 1}"
                        )
                    },
                    listOf(
                        InlineKeyboard(
                            text = "Back to Main Menu",
                            callbackData = BACK_TO_MENU_CALLBACK_DATA
                        )
                    )
                )
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