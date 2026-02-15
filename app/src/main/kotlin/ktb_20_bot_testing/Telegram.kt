package org.example.app.ktb_20_bot_testing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED_CALLBACK_DATA = "learnWords_clicked"
const val STATISTIC_CALLBACK_DATA = "statistic_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

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

fun checkNextQuestionAndSend(
    json: Json,
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long
): Question? {
    trainer.loadDictionary()

    val question = trainer.getNextQuestion()
    if (question == null) {
        telegramBotService.sendMessage(chatId, "All the words in the dictionary have been learned.")
        return null
    } else {
        telegramBotService.sendQuestion(json, chatId, question)
        return question
    }
}

fun main(args: Array<String>) {
    val botToken = args[0]
    var lastUpdateId = 0L

    val service = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()

    val json = Json {
        ignoreUnknownKeys = true
    }

    trainer.loadDictionary()

    var currentQuestion: Question? = null

    while (true) {
        Thread.sleep(2000)

        val responseString: String = service.getUpdates(lastUpdateId)
        println(responseString)
        val response: Response = json.decodeFromString(responseString)
        val updates = response.result
        val firstUpdate = updates.firstOrNull() ?: continue
        val updateId = firstUpdate.updateId
        lastUpdateId = updateId + 1

        val message = firstUpdate.message?.text
        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        val data = firstUpdate.callbackQuery?.data

        if (message?.lowercase() == "Hello" && chatId != null) {
            println(" $message: Chat_ID:$chatId")
            service.sendMessage(chatId, message)
        }

        if (message?.trim()?.lowercase() == "/start" && chatId != null) {
            service.sendMenu(json, chatId)
        }

        if (data?.lowercase() == STATISTIC_CALLBACK_DATA && chatId != null) {
            trainer.loadDictionary()
            service.sendMessage(chatId, trainer.getStatistics().printFormat())
        }

        if (data == LEARN_WORDS_CLICKED_CALLBACK_DATA && chatId != null) {
            currentQuestion = checkNextQuestionAndSend(json, trainer, service, chatId)
            println("current Question: $currentQuestion")
        }

        if (data != null && data.startsWith(CALLBACK_DATA_ANSWER_PREFIX) && chatId != null) {
            println("button pressed: $data")
            val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()

            println("userAnswerIndex = $userAnswerIndex")

            val quest = currentQuestion
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
                currentQuestion = checkNextQuestionAndSend(json, trainer, service, chatId)
            }
        }
    }
}