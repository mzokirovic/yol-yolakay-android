package com.example.yol_yolakay.feature.auth


import com.example.yol_yolakay.core.network.model.AuthResponse
import com.example.yol_yolakay.core.network.model.SendOtpRequest
import com.example.yol_yolakay.core.network.model.VerifyOtpRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class AuthRemoteRepository(private val client: HttpClient) {

    suspend fun sendOtp(phone: String) {
        val resp = client.post("api/auth/otp/send") { setBody(SendOtpRequest(phone)) }
        if (!resp.status.isSuccess()) {
            throw Exception("OTP SEND HTTP ${resp.status.value}: ${resp.bodyAsText()}")
        }
    }

    suspend fun verifyOtp(phone: String, code: String): AuthResponse {
        val resp = client.post("api/auth/otp/verify") { setBody(VerifyOtpRequest(phone, code)) }
        if (!resp.status.isSuccess()) {
            throw Exception("OTP VERIFY HTTP ${resp.status.value}: ${resp.bodyAsText()}")
        }
        return resp.body()
    }
}
