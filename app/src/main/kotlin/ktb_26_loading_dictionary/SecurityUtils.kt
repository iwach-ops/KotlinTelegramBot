package org.example.app.ktb_26_loading_dictionary

object SecurityUtils {
    private val suspiciousTokens = listOf(
        "'", "\"", ";", "--", "/*", "*/",
        "union", "select", "drop", "delete", "insert", "update"
    )

    fun validateUsername(value: String?): String? {
        val trimmed = value?.trim() ?: return null
        require(trimmed.length <= 64) { "Username too long" }
        require(trimmed.matches(Regex("^[A-Za-z0-9_]+$"))) {
            "Username contains invalid characters"
        }
        return trimmed
    }

    fun validateDictionaryValue(value: String, fieldName: String): String {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "$fieldName must not be blank" }
        require(trimmed.length <= 100) { "$fieldName too long" }
        return trimmed
    }

    fun isSuspicious(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val lowered = value.lowercase()
        return suspiciousTokens.any { lowered.contains(it) }
    }

    fun logSuspiciousInput(source: String, value: String?) {
        if (isSuspicious(value)) {
            println("WARNING suspicious input from $source: $value")
        }
    }
}