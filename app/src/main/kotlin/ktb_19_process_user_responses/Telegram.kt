package org.example.app.ktb_19_process_user_responses

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val UPDATE_ID_REGEX_PATTERN = """"update_id"\s*:\s*(\d+)"""
const val TEXT_REGEX_PATTERN = """"text"\s*:\s*"((?:\\.|[^"\\])*)""""
const val USER_NAME_REGEX_PATTERN = """"username"\s*:\s*"([^"]+)""""
const val CHAT_ID_REGEX_PATTERN = """"chat"\s*:\s*\{[^}]*"id"\s*:\s*(-?\d+)"""
const val CALLBACK_REGEX_PATTERN = """"data"\s*:\s*"((?:\\.|[^"\\])*)""""
const val LEARN_WORDS_CLICKED_CALLBACK_DATA = "learnWords_clicked"
const val STATISTIC_CALLBACK_DATA = "statistic_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Int
): Question? {
    trainer.loadDictionary()

    val question = trainer.getNextQuestion()
    if (question == null) {
        telegramBotService.sendMessage(chatId.toLong(), "All the words in the dictionary have been learned.")
        return null
    } else {
        telegramBotService.sendQuestion(chatId.toLong(), question)
        return question
    }
}

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0

    val service = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()

    val updateIdRegex = Regex(UPDATE_ID_REGEX_PATTERN)
    val textRegex = Regex(TEXT_REGEX_PATTERN)
    val userNameRegex = Regex(USER_NAME_REGEX_PATTERN)
    val chatIdRegex = Regex(CHAT_ID_REGEX_PATTERN)
    val callbackDataRegex = Regex(CALLBACK_REGEX_PATTERN)

    trainer.loadDictionary()

    var currentQuestion: Question? = null

    while (true) {
        Thread.sleep(2000)

        val updates: String = service.getUpdates(updateId)
        println(updates)

        val userName = userNameRegex.find(updates)?.groupValues?.get(1)
        val text = textRegex.find(updates)?.groupValues?.get(1)
        val chatId = chatIdRegex.find(updates)?.groupValues?.get(1)
        val callBackData = callbackDataRegex.find(updates)?.groupValues?.get(1)

        if (text?.lowercase() == "Hello" && chatId != null) {
            println("$userName: $text: Chat_ID:$chatId")
            service.sendMessage(chatId.toLong(), text)
        }

        if (text?.trim()?.lowercase() == "/start" && chatId != null) {
            service.sendMenu(chatId.toLong())
        }

        if (callBackData?.lowercase() == STATISTIC_CALLBACK_DATA && chatId != null) {
            trainer.loadDictionary()
            service.sendMessage(chatId.toLong(), trainer.getStatistics().printFormat())
        }

        if (callBackData == LEARN_WORDS_CLICKED_CALLBACK_DATA && chatId != null) {
            currentQuestion = checkNextQuestionAndSend(trainer, service, chatId.toInt())
            println("current Question: $currentQuestion")
        }

        if (callBackData != null && callBackData.startsWith(CALLBACK_DATA_ANSWER_PREFIX) && chatId != null) {
            println("button pressed: $callBackData")
            val userAnswerIndex = callBackData.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()

            println("userAnswerIndex = $userAnswerIndex")

            val chatIdInt = chatId.toInt()
            val chatIdLong = chatId.toLong()

            val quest = currentQuestion
            if (quest != null) {
                val isCorrect = trainer.checkAnswer(userAnswerIndex, quest)

                if (isCorrect) {
                    service.sendMessage(chatIdLong, "Right!")
                    trainer.saveCorrectAnswer(quest.correctAnswer)
                } else {
                    val correctWord = quest.correctAnswer.word
                    val correctTranslate = quest.correctAnswer.translate
                    service.sendMessage(chatIdLong, "Wrong: $correctWord - $correctTranslate")
                }
                currentQuestion = checkNextQuestionAndSend(trainer, service, chatIdInt)
            }
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