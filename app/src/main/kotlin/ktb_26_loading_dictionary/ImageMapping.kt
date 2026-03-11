package org.example.app.ktb_26_loading_dictionary

import kotlinx.serialization.json.Json
import java.io.File

private const val IMAGE_MAP_FILE = "app/build/libs/images/images_map.txt"
private const val MAP_DELIMITER = "|"

data class ImageInfo(
    val path: String,
    val fileId: String? = null,
)

fun maybeSendWordPhoto(
    json: Json,
    service: TelegramBotService,
    chatId: Long,
    wordKey: String,
    imageMap: MutableMap<String, ImageInfo>,
    hasSpoiler: Boolean = false
) {
    val key = wordKey.trim().lowercase()
    val info = imageMap[key] ?: return

    val localFile = File(info.path)
    if (!localFile.exists()) {
        println("Image not found for '$key': ${localFile.absolutePath}")
        return
    }

    val cachedId = info.fileId
    if (cachedId != null) {
        println("PHOTO: using cached file_id for $key")
        service.sendPhotoByFileId(chatId, cachedId, hasSpoiler)
        return
    }

    println("PHOTO: uploading local file for $key -> ${localFile.name}")
    val raw = service.sendPhoto(localFile, chatId, hasSpoiler)
    println("sendPhoto raw: $raw")

    val newId = extractBestPhotoFileId(json, raw)
    if (newId != null) {
        imageMap[key] = info.copy(fileId = newId)
        saveImageMap(imageMap)
        println("PHOTO: saved file_id for $key into images_map.txt")
    } else {
        println("PHOTO: upload ok but file_id not extracted for $key")
    }
}

fun loadImageMap(fileName: String = IMAGE_MAP_FILE): MutableMap<String, ImageInfo> {
    val file = File(fileName)
    println("IMAGE_MAP_FILE: ${file.absolutePath}")
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

    println("IMAGE MAP LOADED: ${map.size} entries")
    return map
}

fun saveImageMap(map: Map<String, ImageInfo>, fileName: String = IMAGE_MAP_FILE) {
    val file = File(fileName)

    file.parentFile?.mkdirs()

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