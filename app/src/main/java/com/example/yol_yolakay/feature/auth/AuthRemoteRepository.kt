// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/auth/AuthRemoteRepository.kt

package com.example.yol_yolakay.feature.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AuthRemoteRepository(private val client: HttpClient) {

    // REQUEST MODELS
    @Serializable data class SendOtpRequest(val phone: String)

    // Backendning verifyOtp controlleri "phone", "token", "type" kutayotgan bo'lishi mumkin
    // Shuning uchun VerifyOtpRequest ishlatmasdan, Map ishlatganimiz ma'qul (pastda ko'rasiz)

    // RESPONSE MODEL
    @Serializable
    data class AuthResponse(
        @SerialName("success") val success: Boolean = true,

        // backend: access_token yuborsa shu tushadi
        @SerialName("access_token") val accessToken: String? = null,

        // backend: token yuborsa shu tushadi
        @SerialName("token") val token: String? = null,

        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("is_new_user") val isNewUser: Boolean = false,
        @SerialName("message") val message: String? = null
    )


    suspend fun sendOtp(phone: String) {
        // 1. Xavfsizlik: Agar "+" belgisi bo'lmasa, majburan qo'shamiz
        val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"

        android.util.Log.d("AuthRepo", "Sending OTP to: $formattedPhone")

        val resp = client.post("api/auth/otp/send") {
            contentType(ContentType.Application.Json)
            setBody(SendOtpRequest(formattedPhone))
        }

        // 2. Agar xato bo'lsa, SABABINI o'qiymiz (bodyAsText)
        if (!resp.status.isSuccess()) {
            val errorBody = resp.bodyAsText() // Serverdan kelgan aniq xato matni
            throw Exception("Server xatosi (${resp.status.value}): $errorBody")
        }
    }

    suspend fun verifyOtp(phone: String, code: String): AuthResponse {
        val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"

        // Backend "code" kutadimi yoki "token"? Supabase odatda "token" ishlatadi.
        // Xavfsizlik uchun Map ishlatamiz
        val reqBody = mapOf(
            "phone" to formattedPhone,
            "token" to code,
            "type" to "sms"
        )

        val resp = client.post("api/auth/otp/verify") {
            contentType(ContentType.Application.Json)
            setBody(reqBody)
        }

        if (!resp.status.isSuccess()) {
            val errorBody = resp.bodyAsText()
            throw Exception("Tasdiqlash xatosi (${resp.status.value}): $errorBody")
        }

        return resp.body()
    }
}