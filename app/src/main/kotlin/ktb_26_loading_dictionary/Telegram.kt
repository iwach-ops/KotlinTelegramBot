package org.example.app.ktb_26_loading_dictionary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED_CALLBACK_DATA = "learnWords_clicked"
const val STATISTIC_CALLBACK_DATA = "statistic_clicked"
const val RESET_CALLBACK_DATA = "reset_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val GO_TO_STATS_CALLBACK_DATA = "go_to_stats"
const val MENU_CALLBACK_DATA = "menu_clicked"
const val UNDO_CALLBACK_DATA = "undo_clicked"

@Serializable
data class SendPhotoResponse(
    val ok: Boolean,
    val result: SendPhotoResult? = null
)

@Serializable
data class SendPhotoResult(
    @SerialName("photo")
    val photo: List<PhotoSize> = emptyList()
)

@Serializable
data class PhotoSize(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long? = null,
    val width: Int,
    val height: Int
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,

    )

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

fun extractBestPhotoFileId(json: Json, raw: String): String? {
    val resp = json.decodeFromString<SendPhotoResponse>(raw)
    val photos = resp.result?.photo ?: return null
    return photos.maxByOrNull { it.fileSize ?: 0 }?.fileId
        ?: photos.lastOrNull()?.fileId
}

fun checkNextQuestionAndSend(
    json: Json,
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long,
    imageMap: MutableMap<String, ImageInfo>,
): Question? {

    val question = trainer.getNextQuestion()
    if (question == null) {
        telegramBotService.sendMessage(chatId, "All the words in the dictionary have been learned.")
        return null
    }

    maybeSendWordPhoto(
        json = json,
        service = telegramBotService,
        chatId = chatId,
        wordKey = question.correctAnswer.word,
        imageMap = imageMap,
        hasSpoiler = false
    )

    telegramBotService.sendQuestion(json, chatId, question)
    return question
}

fun handleUpdate(
    update: Update, json: Json, service: TelegramBotService,
    currentQuestions: MutableMap<Long, Question?>,
    trainers: HashMap<Long, LearnWordsTrainer>,
    imageMap: MutableMap<String, ImageInfo>,
    dynamicMessage: DynamicMessage,
) {
    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data

    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt").apply { loadDictionary() } }

    if (message?.trim()?.lowercase() == "/photo_test") {
        val key = "cat"
        val info = imageMap[key]

        if (info == null) {
            service.sendMessage(chatId, "No image mapping for '$key' in images_map.txt")
            return
        }

        val localFile = File(info.path)
        if (!localFile.exists()) {
            service.sendMessage(chatId, "Image file not found: ${localFile.path}")
            return
        }

        if (info.fileId != null) {
            println("USING CACHED file_id for $key: ${info.fileId}")
            service.sendPhotoByFileId(chatId, info.fileId, hasSpoiler = false)
            service.sendMessage(chatId, "Used cached fileId")
            return
        }

        println("NO fileId for $key -> uploading file: ${localFile.path}")
        val raw = service.sendPhoto(localFile, chatId, hasSpoiler = false)
        val newId = extractBestPhotoFileId(json, raw)

        if (newId != null) {
            imageMap[key] = info.copy(fileId = newId)
            saveImageMap(imageMap)
            service.sendMessage(chatId, "Uploaded + saved fileId to images_map.txt")
        } else {
            service.sendMessage(chatId, "Upload ok, but could not extract fileId")
        }
        return
    }

    if (message?.trim()?.lowercase() == "hello") {
        println(" $message: Chat_ID:$chatId")
        service.sendMessage(chatId, message)
        return
    }

    if (message?.trim()?.lowercase() == "/start") {
        service.sendMenu(json, chatId)
        return
    }

    if (data?.lowercase() == STATISTIC_CALLBACK_DATA) {
        //service.sendMessage(chatId, trainer.getStatistics().printFormat())
        dynamicMessage.showStatistics(chatId, trainer)
        return
    }

    if (data == LEARN_WORDS_CLICKED_CALLBACK_DATA) {
        currentQuestions[chatId] = checkNextQuestionAndSend(json, trainer, service, chatId, imageMap)
        return
    }

    if (data != null && data.startsWith(CALLBACK_DATA_ANSWER_PREFIX)) {
        println("button pressed: $data")
        val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
            ?: return

        val quest = currentQuestions[chatId]
        if (quest != null) {
            val isCorrect = trainer.checkAnswer(userAnswerIndex, quest)

            if (isCorrect) {
                service.sendMessage(chatId, "Right!")
                trainer.saveCorrectAnswer(quest.correctAnswer)

                dynamicMessage.updateStatistics(chatId, trainer)
            } else {
                val correctWord = quest.correctAnswer.word
                val correctTranslate = quest.correctAnswer.translate
                service.sendMessage(chatId, "Wrong: $correctWord - $correctTranslate")
            }
            currentQuestions[chatId] = checkNextQuestionAndSend(json, trainer, service, chatId, imageMap)
        }
    }

    if (data == RESET_CALLBACK_DATA) {
        trainer.resetProgress()
        service.sendMessage(chatId, "Progress is reset")
    }

    if (message?.trim()?.lowercase() == "/undo") {
        dynamicMessage.undo(chatId)
        return
    }

    // 1) Jump to stats
    if (data == GO_TO_STATS_CALLBACK_DATA) {
        val statsId = dynamicMessage.statsMessageId(chatId)

        if (statsId != null) {
            service.sendMessage(
                chatId,
                "⬆️ Click on Reply, in order to reach the Statistic.",
                replyToMessageId = statsId
            )
        } else {
            service.sendMessage(chatId, "No Statistic. Please  click on Statistic in Menu ")
            service.sendMenu(json, chatId)
        }
        return
    }

    if (data == MENU_CALLBACK_DATA) {
        service.sendMenu(json, chatId)
        return
    }

    if (data == UNDO_CALLBACK_DATA) {
        dynamicMessage.undo(chatId)
        return
    }
}

fun main(args: Array<String>) {
    val botToken = args[0]
    var lastUpdateId = 0L
    val imageMap = loadImageMap()
    val service = TelegramBotService(botToken)

    val json = Json { ignoreUnknownKeys = true }

    val dynamicMessage = DynamicMessage(json, service)

    val trainers = HashMap<Long, LearnWordsTrainer>()

    val currentQuestions = mutableMapOf<Long, Question?>()

    while (true) {
        Thread.sleep(2000)

        val responseString: String = service.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }

        sortedUpdates.forEach { handleUpdate(it, json, service, currentQuestions, trainers, imageMap, dynamicMessage) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

@Serializable
data class SendMessageApiResponse(
    val ok: Boolean,
    val result: TelegramMessageResult? = null,
    val description: String? = null,
    @SerialName("error_code") val errorCode: Int? = null
)

@Serializable
data class TelegramMessageResult(
    @SerialName("message_id") val messageId: Long
)

@Serializable
data class EditMessageApiResponse(
    val ok: Boolean,
    val result: JsonElement? = null, // Telegram kann Message oder true liefern
    val description: String? = null,
    @SerialName("error_code") val errorCode: Int? = null
)

fun extractMessageId(json: Json, raw: String): Long? =
    json.decodeFromString<SendMessageApiResponse>(raw).result?.messageId