package com.example.yol_yolakay.feature.notifications

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.NotificationApiModel
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class NotificationsRemoteRepository(private val userId: String) {

    private fun io.ktor.client.request.HttpRequestBuilder.attachUser() {
        header("x-user-id", userId)
    }

    @Serializable
    private data class ListResponse(
        val success: Boolean = true,
        val data: List<NotificationApiModel> = emptyList()
    )

    @Serializable
    private data class MarkReadResponse(
        val success: Boolean = true,
        val data: NotificationApiModel? = null
    )

    suspend fun list(limit: Int = 50): List<NotificationApiModel> {
        val resp = BackendClient.client.get("api/notifications?limit=$limit") { attachUser() }
        if (!resp.status.isSuccess()) throw Exception("Notifications HTTP ${resp.status.value}: ${resp.bodyAsText()}")
        return resp.body<ListResponse>().data
    }

    suspend fun markRead(id: String): NotificationApiModel? {
        val resp = BackendClient.client.post("api/notifications/$id/read") { attachUser() }
        if (!resp.status.isSuccess()) throw Exception("MarkRead HTTP ${resp.status.value}: ${resp.bodyAsText()}")
        return resp.body<MarkReadResponse>().data
    }

    suspend fun markAllRead() {
        val resp = BackendClient.client.post("api/notifications/read-all") { attachUser() }
        if (!resp.status.isSuccess()) throw Exception("MarkAllRead HTTP ${resp.status.value}: ${resp.bodyAsText()}")
    }
}
