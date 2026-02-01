package com.example.yol_yolakay.feature.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.feature.publish.model.TripDto
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PublishViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState = _uiState.asStateFlow()

    // --- Ma'lumotlarni yangilash ---
    fun onFromChange(v: String) = _uiState.update { it.copy(fromLocation = v) }
    fun onToChange(v: String) = _uiState.update { it.copy(toLocation = v) }
    fun onDateChange(v: LocalDate) = _uiState.update { it.copy(date = v) }
    fun onTimeChange(v: LocalTime) = _uiState.update { it.copy(time = v) }
    fun onPassengersChange(v: Int) = _uiState.update { it.copy(passengers = v.coerceIn(1, 8)) }
    fun onPriceChange(v: String) = _uiState.update { it.copy(price = v.filter { char -> char.isDigit() }) }

    // --- Navigatsiya ---
    fun onNext() {
        val steps = PublishStep.values()
        val currentOrd = _uiState.value.currentStep.ordinal

        // Agar oxirgi qadamdan oldin bo'lsa -> Keyingisiga o'tish
        if (currentOrd < steps.lastIndex) {
            _uiState.update { it.copy(currentStep = steps[currentOrd + 1]) }
        } else {
            // Agar oxirgi qadam (PREVIEW) bo'lsa -> Serverga jo'natish!
            onPublish()
        }
    }

    fun onBack() {
        val steps = PublishStep.values()
        val currentOrd = _uiState.value.currentStep.ordinal
        if (currentOrd > 0) {
            _uiState.update { it.copy(currentStep = steps[currentOrd - 1]) }
        }
    }

    // --- BACKEND BILAN ALOQA ---
    private fun onPublish() {
        val currentState = _uiState.value

        viewModelScope.launch {
            try {
                // 1. Ma'lumotni Backend tushunadigan tilga (DTO) o'giramiz
                val tripDto = TripDto(
                    fromLocation = currentState.fromLocation,
                    toLocation = currentState.toLocation,

                    // Koordinatalar (Hozircha Toshkent-Samarqand markazlari)
                    // Keyinchalik bularni Map orqali haqiqiy locationdan olamiz
                    fromLat = 41.3111,
                    fromLng = 69.2401,
                    toLat = 39.6542,
                    toLng = 66.9597,

                    date = currentState.date.format(DateTimeFormatter.ISO_DATE),
                    time = currentState.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                    price = currentState.price.toDoubleOrNull() ?: 0.0,
                    seats = currentState.passengers,
                    driverId = "0ed9def9-ae36-422d-a6c1-fb45dbd9b90a" // Vaqtincha ID
                )

                println("🚀 Serverga yuborilyapti: $tripDto")

                // 2. Render serveriga POST so'rov yuboramiz
                val response = BackendClient.client.post("api/trips/publish") {
                    contentType(ContentType.Application.Json)
                    setBody(tripDto)
                }

                // 3. Natijani tekshiramiz
                if (response.status.value in 200..299) {
                    println("✅ Muvaffaqiyatli! Server javobi: ${response.status}")
                    // TODO: Bu yerda foydalanuvchini Bosh Sahifaga qaytarish logikasi bo'ladi
                } else {
                    println("❌ Xatolik: Server ${response.status} qaytardi")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println("⚠️ Internet yoki Server xatosi: ${e.message}")
            }
        }
    }
}