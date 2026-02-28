package org.example.app.ktb_22_multi_users

import java.io.File

private const val IMAGE_MAP_FILE = "app/build/libs/images/images_map.txt"
private const val MAP_DELIMITER = "|"

data class ImageInfo(
    val path: String,
    val fileId: String? = null,
)

fun loadImageMap(fileName: String = IMAGE_MAP_FILE): MutableMap<String, ImageInfo> {
    val file = File(fileName)
    if (!file.exists()) return mutableMapOf()

    val map = mutableMapOf<String, ImageInfo>()

    file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { line ->
            val parts = line.split(MAP_DELIMITER)
            val word = parts.getOrNull(0)?.trim().orEmpty()
            val path = parts.getOrNull(1)?.trim().orEmpty()
            val fileId = parts.getOrNull(2)?.trim().orEmpty()

            if (word.isBlank() || path.isBlank()) return@forEach

            map[word] = ImageInfo(
                path = path,
                fileId = fileId.ifBlank { null }
            )
        }

    return map
}

fun saveImageMap(map: Map<String, ImageInfo>, fileName: String = IMAGE_MAP_FILE) {
    val file = File(fileName)

    val content = buildString {
        map.forEach { (word, info) ->
            append(word)
            append(MAP_DELIMITER)
            append(info.path)
            append(MAP_DELIMITER)
            append(info.fileId.orEmpty())
            append("\n")
        }
    }

    file.writeText(content)
}