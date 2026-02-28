package org.example.app.ktb_22_multi_users

fun main() {
    val map = loadImageMap()
    println("Loaded: $map")

    val cat = map["cat"]
    if (cat != null) {
        map["cat"] = cat.copy(fileId = "TEST_FILE_ID_123")
        saveImageMap(map)
        println("Saved with test fileId")
    }
}