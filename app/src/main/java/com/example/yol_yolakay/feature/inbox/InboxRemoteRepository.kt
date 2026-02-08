package com.example.yol_yolakay.feature.inbox

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.MessageApiModel
import com.example.yol_yolakay.core.network.model.ThreadApiModel
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class InboxRemoteRepository { // 🚨 Argument olib tashlandi

    // 🚨 attachUser() funksiyasi kerak emas, BackendClient o'zi hal qiladi

    @Serializable
    private data class ThreadsResponse(
        val success: Boolean = true,
        val data: List<ThreadApiModel> = emptyList()
    )

    @Serializable
    private data class ThreadMessagesResponse(
        val success: Boolean = true,
        val messages: List<MessageApiModel> = emptyList()
    )

    @Serializable
    data class CreateThreadRequest(val peerId: String, val tripId: String? = null)

    @Serializable
    private data class RawThreadApiModel(val id: String)

    @Serializable
    private data class CreateThreadResponse(
        val success: Boolean = true,
        val thread: RawThreadApiModel? = null
    )

    @Serializable
    private data class SendMessageRequest(val text: String)

    suspend fun listThreads(): List<ThreadApiModel> {
        val resp = BackendClient.client.get("api/inbox")
        if (!resp.status.isSuccess()) {
            throw Exception("Inbox HTTP ${resp.status.value}: ${resp.bodyAsText()}")
        }
        return resp.body<ThreadsResponse>().data
    }

    suspend fun getMessages(threadId: String): List<MessageApiModel> {
        val resp = BackendClient.client.get("api/inbox/threads/$threadId")
        if (!resp.status.isSuccess()) {
            throw Exception("Messages HTTP ${resp.status.value}: ${resp.bodyAsText()}")
        }
        return resp.body<ThreadMessagesResponse>().messages
    }

    suspend fun sendMessage(threadId: String, text: String) {
        val resp = BackendClient.client.post("api/inbox/threads/$threadId/messages") {
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(text))
        }
        if (!resp.status.isSuccess()) {
            throw Exception("SendMessage HTTP ${resp.status.value}: ${resp.bodyAsText()}")
        }
    }

    suspend fun createThread(peerId: String, tripId: String? = null): String? {
        val resp = BackendClient.client.post("api/inbox/threads") {
            contentType(ContentType.Application.Json)
            setBody(CreateThreadRequest(peerId, tripId))
        }
        if (!resp.status.isSuccess()) {
            throw Exception("CreateThread HTTP ${resp.status.value}: ${resp.bodyAsText()}")
        }
        return resp.body<CreateThreadResponse>().thread?.id
    }
}