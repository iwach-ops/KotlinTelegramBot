package org.example.app.ktb_26_loading_dictionary

import kotlinx.serialization.json.Json
import java.util.ArrayDeque
import kotlin.math.abs

private data class UserState(
    var messageId: Long = 0L,
    var lastPercent: Int = 0,
    val history: ArrayDeque<String> = ArrayDeque()
)

class DynamicMessage(
    private val json: Json,
    private val service: TelegramBotService,
) {
    private val states = mutableMapOf<Long, UserState>()

    fun showStatistics(chatId: Long, trainer: LearnWordsTrainer) {
        val stats = trainer.getStatistics()
        val text = buildText(stats.percent, stats)

        val msgId = service.sendMessageAndGetId(json, chatId, text) ?: return

        val state = states.getOrPut(chatId) { UserState() }
        state.messageId = msgId
        state.lastPercent = stats.percent
        state.history.clear()
        state.history.addLast(text)
    }

    fun updateStatistics(chatId: Long, trainer: LearnWordsTrainer) {
        val state = states.getOrPut(chatId) { UserState() }

        if (state.messageId == 0L) {
            showStatistics(chatId, trainer)
            return
        }

        val stats = trainer.getStatistics()
        val from = state.lastPercent
        val to = stats.percent

        animate(chatId, state, stats, from, to)

        val finalText = buildText(to, stats)
        if (safeEdit(chatId, state, finalText)) {
            state.lastPercent = to
            pushHistory(state, finalText)
        }
    }

    fun undo(chatId: Long) {
        val state = states[chatId]
        if (state == null || state.messageId == 0L || state.history.size <= 1) {
            service.sendMessage(chatId, "Nothing to undo.")
            return
        }

        state.history.removeLast()
        val prev = state.history.last()
        safeEdit(chatId, state, prev)
    }

    private fun pushHistory(state: UserState, text: String) {
        if (state.history.lastOrNull() != text) {
            state.history.addLast(text)
            while (state.history.size > 20) state.history.removeFirst()
        }
    }

    private fun safeEdit(chatId: Long, state: UserState, text: String): Boolean {
        val raw = service.editMessage(chatId, state.messageId, text)

        val resp = runCatching { json.decodeFromString(EditMessageApiResponse.serializer(), raw) }.getOrNull()
            ?: return raw.contains("MESSAGE_NOT_MODIFIED", ignoreCase = true)

        if (resp.ok) return true

        val desc = resp.description.orEmpty()
        if (desc.contains("MESSAGE_NOT_MODIFIED", ignoreCase = true)) return true

        if (desc.contains("MESSAGE_EDIT_TIME_EXPIRED", ignoreCase = true)) {
            val newId = service.sendMessageAndGetId(json, chatId, text) ?: return false
            state.messageId = newId
            state.history.clear()
            state.history.addLast(text)
            return true
        }
        return false
    }

    private fun animate(chatId: Long, state: UserState, stats: Statistic, from: Int, to: Int) {
        if (from == to) return

        val diff = abs(to - from)
        val step = when {
            diff >= 30 -> 5
            diff >= 10 -> 2
            else -> 1
        }

        val range = if (to > from) (from..to step step) else (from downTo to step step)

        for (progress in range) {
            safeEdit(chatId, state, buildText(progress, stats))
            Thread.sleep(80)
        }
    }

    private fun bar(percent: Int): String {
        val progress = percent.coerceIn(0, 100)
        val filled = progress / 10
        return "█".repeat(filled) + "▒".repeat(10 - filled)
    }

    private fun buildText(percent: Int, stats: Statistic): String =
        """
        📊 Statistic

        ${stats.printFormat()}

        Progress: $percent%
        [${bar(percent)}]

        /undo
        """.trimIndent()

    fun statsMessageId(chatId: Long): Long? =
        states[chatId]?.messageId?.takeIf { it != 0L }
}