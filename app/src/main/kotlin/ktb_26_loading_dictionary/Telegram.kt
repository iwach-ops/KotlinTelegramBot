package org.example.app.ktb_26_loading_dictionary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val TELEGRAM_FILE_BASE_URL = "https://api.telegram.org/file/bot"
const val LEARN_WORDS_CLICKED_CALLBACK_DATA = "learnWords_clicked"
const val STATISTIC_CALLBACK_DATA = "statistic_clicked"
const val RESET_CALLBACK_DATA = "reset_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val BACK_TO_MENU_CALLBACK_DATA = "back_to_menu"

@Serializable
data class GetFileRequest(
    @SerialName("file_id")
    val fileId: String
)

@Serializable
data class GetFileResponse(
    val ok: Boolean,
    val result: TelegramFile? = null,
    val description: String? = null,
    @SerialName("error_code")
    val errorCode: Int? = null
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
    @SerialName("file_path")
    val filePath: String,
)

@Serializable
data class Document(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
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
    val ok: Boolean,
    @SerialName("result")
    val result: List<Update> = emptyList(),
    @SerialName("error_code")
    val errorCode: Int? = null,
    val description: String? = null
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
    val document: Document? = null,
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

fun checkNextQuestionAndSend(
    json: Json,
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long
): Question? {

    val question = trainer.getNextQuestion()
    if (question == null) {
        telegramBotService.sendMessage(chatId, "All the words in the dictionary have been learned.")
        return null
    } else {
        telegramBotService.sendQuestion(json, chatId, question)
        return question
    }
}

fun handleUpdate(
    update: Update, json: Json, service: TelegramBotService,
    currentQuestions: MutableMap<Long, Question?>,
    trainers: HashMap<Long, LearnWordsTrainer>
) {

    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data

    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt").apply { loadDictionary() } }

    val document = update.message?.document

    if (document != null) {
        service.sendMessage(chatId, "File received, ${document.fileName}")

        val getFileResponse = service.getFile(json, document.fileId)
        println("GetFile Response: $getFileResponse")

        val filePath = getFileResponse.result?.filePath
        println("path: $filePath")

        if (filePath == null) {
            service.sendMessage(chatId, "Cannot download, file_path is null")
            return
        }

        val targetFile = File("uploads/${document.fileName}")
        service.downloadFile(filePath, targetFile)

        trainer.loadDictionary()
        val addedWordsNumber = trainer.importWordsFromFile(targetFile)

        service.sendMessage(chatId, "Loaded $addedWordsNumber words - ok : ${document.fileName}")

        service.sendMenu(json, chatId)

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
        currentQuestions[chatId] = checkNextQuestionAndSend(json, trainer, service, chatId)
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
            currentQuestions[chatId] = checkNextQuestionAndSend(json, trainer, service, chatId)
        }
    }

    if (data == RESET_CALLBACK_DATA) {
        trainer.resetProgress()
        service.sendMessage(chatId, "Progress is reset")
    }

    if (data == BACK_TO_MENU_CALLBACK_DATA) {
        service.sendMenu(json, chatId)
        return
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
        //println(responseString)
        println("UPDATES JSON:\n$responseString")

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, service, currentQuestions, trainers) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}