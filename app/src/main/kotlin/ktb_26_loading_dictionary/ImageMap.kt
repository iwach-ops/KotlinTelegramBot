package org.example.app.ktb_26_loading_dictionary

import java.io.File

private const val MAP_DELIMITER = "|"

fun loadImageMap(file: File = File("app/build/libs/images/images_map.txt")): Map<String, String> {
    if (!file.exists()) return emptyMap()

    val map = mutableMapOf<String, String>()
    file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .forEach { line ->
            val parts = line.split(MAP_DELIMITER, limit = 2)
            if (parts.size < 2) return@forEach
            val key = parts[0].trim().lowercase()
            val path = parts[1].trim()
            if (key.isNotEmpty() && path.isNotEmpty()) {
                map[key] = path
            }
        }
    return map
}