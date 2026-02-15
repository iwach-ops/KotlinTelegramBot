package org.example.app.ktb_20_bot_testing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.Int
import kotlin.String
import kotlinx.serialization.decodeFromString


fun main() {

    val json = Json {
        ignoreUnknownKeys = true
    }
    val responseString = """
        {
          "ok": true,
          "result": [
            {
              "update_id": 499165494,
              "message": {
                "message_id": 455,
                "from": {
                  "id": 1362578887,
                  "is_bot": false,
                  "first_name": "Ivan",
                  "username": "ivan_wach",
                  "language_code": "de"
                },
                "chat": {
                  "id": 1362578887,
                  "first_name": "Ivan",
                  "username": "ivan_wach",
                  "type": "private"
                },
                "date": 1770799679,
                "text": "/start",
                "entities": [
                  {
                    "offset": 0,
                    "length": 6,
                    "type": "bot_command"
                  }
                ]
              }
            }
          ]
        }
    """.trimIndent()
/*
        val word = Json.encodeToString(
            Word(
                "Hallo",
                "Hello",
                0,
            )
        )
        println(word)

        val wordObject = Json.decodeFromString<Word>(word)
        println(wordObject)

*/


    val response = json.decodeFromString<Response>(responseString)

    println(response)
}