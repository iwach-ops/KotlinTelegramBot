package org.example.app.ktb_12_token_base_requests

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {
    val botToken = args[0]
    val urlGetMe = "https://api.telegram.org/bot$botToken/getMe"
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates"

    val builder: HttpClient.Builder = HttpClient.newBuilder()
    val client: HttpClient = builder.build()

    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()

    val requestGetUpdates = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()

    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    val responseGetUpdates = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

    println(response.body())

    println(responseGetUpdates.body())
}