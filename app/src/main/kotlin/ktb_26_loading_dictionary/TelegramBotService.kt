package org.example.app.ktb_26_loading_dictionary

import kotlinx.serialization.json.Json
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Random
import java.nio.file.Files
import java.nio.file.Path

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

    fun sendPhotoByFileId(chatId: Long, fileId: String, hasSpoiler: Boolean = false): String {
        val urlSendPhoto = "$baseUrl/sendPhoto"

        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = fileId
        data["has_spoiler"] = hasSpoiler

        val boundary = java.math.BigInteger(35, java.util.Random()).toString()

        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(urlSendPhoto))
            .postMultipartFormData(boundary, data)
            .build()

        val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendPhoto(file: File, chatId: Long, hasSpoiler: Boolean = false): String {
        require(file.exists()) { "File not found: ${file.absolutePath}" }

        val urlSendPhoto = "$baseUrl/sendPhoto"

        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file
        data["has_spoiler"] = hasSpoiler

        val boundary = BigInteger(35, Random()).toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(urlSendPhoto))
            .postMultipartFormData(boundary, data)
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun HttpRequest.Builder.postMultipartFormData(
        boundary: String,
        data: Map<String, Any>
    ): HttpRequest.Builder {
        val byteArrays = ArrayList<ByteArray>()
        val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

        for ((key, value) in data) {
            byteArrays.add(separator)

            when (value) {
                is File -> {
                    val path = Path.of(value.toURI())
                    val mimeType = Files.probeContentType(path) ?: "application/octet-stream"

                    byteArrays.add(
                        "\"$key\"; filename=\"${path.fileName}\"\r\nContent-Type: $mimeType\r\n\r\n"
                            .toByteArray(StandardCharsets.UTF_8)
                    )
                    byteArrays.add(Files.readAllBytes(path))
                    byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
                }

                else -> {
                    byteArrays.add(
                        "\"$key\"\r\n\r\n$value\r\n".toByteArray(StandardCharsets.UTF_8)
                    )
                }
            }
        }

        byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))

        this.header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))

        return this
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
                        InlineKeyboard(callbackData = GO_TO_STATS_CALLBACK_DATA, text = "📊 Statistic"),
                        InlineKeyboard(callbackData = MENU_CALLBACK_DATA, text = "🏠 Menu"),
                        InlineKeyboard(callbackData = UNDO_CALLBACK_DATA, text = "↩️ Undo"),
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

    fun sendMessageAndGetId(json: Json, chatId: Long, text: String): Long? {
        val raw = sendMessage(chatId, text)
        return extractMessageId(json, raw)
    }

    fun editMessage(chatId: Long, messageId: Long, message: String): String {
        val url = "$baseUrl/editMessageText"

        val formatText = message.trim().take(4096)
        require(formatText.isNotEmpty()) { "message must not be empty" }

        val body =
            "chat_id=$chatId" +
                    "&message_id=$messageId" +
                    "&text=${URLEncoder.encode(formatText, StandardCharsets.UTF_8)}"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: Long, text: String, replyToMessageId: Long? = null): String {
        val urlSendMessage = "$baseUrl/sendMessage"

        val formatText = text.trim().take(4096)
        require(formatText.isNotEmpty()) { "text must not be empty" }

        val body = buildString {
            append("chat_id=$chatId")
            append("&text=${URLEncoder.encode(formatText, StandardCharsets.UTF_8)}")
            if (replyToMessageId != null) {
                append("&reply_to_message_id=$replyToMessageId")
                append("&allow_sending_without_reply=true")
            }
        }

        val requestSendMessage = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val responseSendMessage = client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())
        return responseSendMessage.body()
    }
}