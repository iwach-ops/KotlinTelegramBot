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

        val st = states.getOrPut(chatId) { UserState() }
        st.messageId = msgId
        st.lastPercent = stats.percent
        st.history.clear()
        st.history.addLast(text)
    }

    fun updateStatistics(chatId: Long, trainer: LearnWordsTrainer) {
        val st = states.getOrPut(chatId) { UserState() }

        if (st.messageId == 0L) {
            showStatistics(chatId, trainer)
            return
        }

        val stats = trainer.getStatistics()
        val from = st.lastPercent
        val to = stats.percent

        animate(chatId, st, stats, from, to)

        val finalText = buildText(to, stats)
        if (safeEdit(chatId, st, finalText)) {
            st.lastPercent = to
            pushHistory(st, finalText)
        }
    }

    fun undo(chatId: Long) {
        val st = states[chatId]
        if (st == null || st.messageId == 0L || st.history.size <= 1) {
            service.sendMessage(chatId, "Nothing to undo.")
            return
        }

        st.history.removeLast()
        val prev = st.history.last()
        safeEdit(chatId, st, prev)
    }

    // -------- helpers --------

    private fun pushHistory(st: UserState, text: String) {
        if (st.history.lastOrNull() != text) {
            st.history.addLast(text)
            while (st.history.size > 20) st.history.removeFirst()
        }
    }

    private fun safeEdit(chatId: Long, st: UserState, text: String): Boolean {
        val raw = service.editMessage(chatId, st.messageId, text)

        val resp = runCatching { json.decodeFromString(EditMessageApiResponse.serializer(), raw) }.getOrNull()
            ?: return raw.contains("MESSAGE_NOT_MODIFIED", ignoreCase = true)

        if (resp.ok) return true

        val desc = resp.description.orEmpty()
        if (desc.contains("MESSAGE_NOT_MODIFIED", ignoreCase = true)) return true

        if (desc.contains("MESSAGE_EDIT_TIME_EXPIRED", ignoreCase = true)) {
            val newId = service.sendMessageAndGetId(json, chatId, text) ?: return false
            st.messageId = newId
            st.history.clear()
            st.history.addLast(text)
            return true
        }
        return false
    }

    private fun animate(chatId: Long, st: UserState, stats: Statistic, from: Int, to: Int) {
        if (from == to) return

        val diff = abs(to - from)
        val step = when {
            diff >= 30 -> 5
            diff >= 10 -> 2
            else -> 1
        }

        val range = if (to > from) (from..to step step) else (from downTo to step step)

        for (p in range) {
            safeEdit(chatId, st, buildText(p, stats))
            Thread.sleep(80)
        }
    }

    private fun bar(percent: Int): String {
        val p = percent.coerceIn(0, 100)
        val filled = p / 10
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