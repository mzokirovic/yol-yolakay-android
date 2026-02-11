package com.example.yol_yolakay.feature.inbox

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.ApiErrorResponse
import com.example.yol_yolakay.core.network.model.MessageApiModel
import com.example.yol_yolakay.core.network.model.ThreadApiModel
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class InboxRemoteRepository {

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

    // ---------- Error parsing (pretty) ----------

    private val errJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private suspend fun HttpResponse.throwPrettyError(fallbackPrefix: String): Nothing {
        val fallback = "$fallbackPrefix HTTP ${status.value}"

        val msg = runCatching {
            val text = bodyAsText()
            val parsed = errJson.decodeFromString<ApiErrorResponse>(text)
            parsed.bestMessage()
        }.getOrNull()

        throw Exception(msg?.takeIf { it.isNotBlank() } ?: fallback)
    }

    // ---------- API ----------

    suspend fun listThreads(): List<ThreadApiModel> {
        val resp = BackendClient.client.get("api/inbox")
        if (!resp.status.isSuccess()) resp.throwPrettyError("Inbox")
        return resp.body<ThreadsResponse>().data
    }

    suspend fun getMessages(threadId: String): List<MessageApiModel> {
        val resp = BackendClient.client.get("api/inbox/threads/$threadId")
        if (!resp.status.isSuccess()) resp.throwPrettyError("Messages")
        return resp.body<ThreadMessagesResponse>().messages
    }

    suspend fun sendMessage(threadId: String, text: String) {
        val resp = BackendClient.client.post("api/inbox/threads/$threadId/messages") {
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(text))
        }
        if (!resp.status.isSuccess()) resp.throwPrettyError("SendMessage")
    }

    suspend fun createThread(peerId: String, tripId: String? = null): String? {
        val resp = BackendClient.client.post("api/inbox/threads") {
            contentType(ContentType.Application.Json)
            setBody(CreateThreadRequest(peerId, tripId))
        }
        if (!resp.status.isSuccess()) resp.throwPrettyError("CreateThread")
        return resp.body<CreateThreadResponse>().thread?.id
    }
}
