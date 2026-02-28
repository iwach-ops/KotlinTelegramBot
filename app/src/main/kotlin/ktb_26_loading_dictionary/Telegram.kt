package org.example.app.ktb_26_loading_dictionary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED_CALLBACK_DATA = "learnWords_clicked"
const val STATISTIC_CALLBACK_DATA = "statistic_clicked"
const val RESET_CALLBACK_DATA = "reset_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

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

fun maybeSendWordPhoto(
    json: Json,
    service: TelegramBotService,
    chatId: Long,
    wordKey: String,
    imageMap: Map<String, String>,
    cache: PhotoIdStore,
    hasSpoiler: Boolean = false
) {
    val key = wordKey.trim().lowercase()
    val path = imageMap[key] ?: return

    val localFile = File(path)
    if (!localFile.exists()) {
        println("Image not found for '$key': $path")
        return
    }

    val cachedId = cache.get(key)
    if (cachedId != null) {
        println("PHOTO: using cached file_id for $key")
        service.sendPhotoByFileId(chatId, cachedId, hasSpoiler)
        return
    }

    println("PHOTO: uploading local file for $key -> ${localFile.name}")
    val raw = service.sendPhoto(localFile, chatId, hasSpoiler)
    val newId = extractBestPhotoFileId(json, raw)
    if (newId != null) {
        cache.put(key, newId)
        println("PHOTO: saved file_id for $key")
    } else {
        println("PHOTO: upload ok but file_id not extracted for $key")
    }
}


fun checkNextQuestionAndSend(
    json: Json,
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long,
    imageMap: Map<String, String>,
    photoCache: PhotoIdStore,
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
        cache = photoCache,
        hasSpoiler = false
    )

    telegramBotService.sendQuestion(json, chatId, question)
    return question
}

fun handleUpdate(
    update: Update, json: Json, service: TelegramBotService,
    currentQuestions: MutableMap<Long, Question?>,
    trainers: HashMap<Long, LearnWordsTrainer>,
    imageMap: Map<String, String>,
    photoCache: PhotoIdStore,
) {
    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data

    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt").apply { loadDictionary() } }

    if (message?.trim()?.lowercase() == "/photo_test") {
        val cache = PhotoIdStore()

        val key = "cat"
        val localFile = File("app/build/libs/images/cat.png")

        val cachedId = cache.get(key)
        if (cachedId != null) {
            println("USING CACHED file_id for $key: $cachedId")
            service.sendPhotoByFileId(chatId, cachedId, hasSpoiler = false)
            service.sendMessage(chatId, "Used cached fileId")
        } else {
            println("NO CACHE for $key -> uploading file")
            val raw = service.sendPhoto(localFile, chatId, hasSpoiler = false)
            val newId = extractBestPhotoFileId(json, raw)
            println("NEW file_id: $newId")

            if (newId != null) {
                cache.put(key, newId)
                service.sendMessage(chatId, "Uploaded + cached")
            } else {
                service.sendMessage(chatId, "Upload ok, but could not extract fileId")
            }
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
        service.sendMessage(chatId, trainer.getStatistics().printFormat())
        return
    }

    if (data == LEARN_WORDS_CLICKED_CALLBACK_DATA) {
        currentQuestions[chatId] = checkNextQuestionAndSend(json, trainer, service, chatId, imageMap, photoCache)
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
            } else {
                val correctWord = quest.correctAnswer.word
                val correctTranslate = quest.correctAnswer.translate
                service.sendMessage(chatId, "Wrong: $correctWord - $correctTranslate")
            }
            currentQuestions[chatId] = checkNextQuestionAndSend(json, trainer, service, chatId, imageMap, photoCache)
        }
    }

    if (data == RESET_CALLBACK_DATA) {
        trainer.resetProgress()
        service.sendMessage(chatId, "Progress is reset")
    }
}

fun main(args: Array<String>) {
    val botToken = args[0]
    var lastUpdateId = 0L

    val service = TelegramBotService(botToken)

    val json = Json { ignoreUnknownKeys = true }

    val trainers = HashMap<Long, LearnWordsTrainer>()

    val currentQuestions = mutableMapOf<Long, Question?>()

    while (true) {
        Thread.sleep(2000)

        val responseString: String = service.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }

        val imageMap = loadImageMap()
        val photoCache = PhotoIdStore()

        sortedUpdates.forEach { handleUpdate(it, json, service, currentQuestions, trainers, imageMap, photoCache) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}