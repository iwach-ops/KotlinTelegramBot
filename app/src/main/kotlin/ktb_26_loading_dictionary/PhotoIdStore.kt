package org.example.app.ktb_26_loading_dictionary

import java.io.File

class PhotoIdStore(
    private val cacheFile: File = File("app/build/libs/images/photo_id_store.txt")
) {
    private val map = mutableMapOf<String, String>()

    init {
        load()
    }

    fun get(key: String): String? = map[key.lowercase()]

    fun put(key: String, fileId: String) {
        map[key.lowercase()] = fileId
        save()
    }

    private fun load() {
        if (!cacheFile.exists()) return
        cacheFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val parts = line.split("|", limit = 2)
                if (parts.size < 2) return@forEach
                val key = parts[0].trim().lowercase()
                val fileId = parts[1].trim()
                if (key.isNotEmpty() && fileId.isNotEmpty()) map[key] = fileId
            }
    }

    private fun save() {
        val content = map.entries
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key}|${it.value}" }
        cacheFile.writeText(content)
    }
}