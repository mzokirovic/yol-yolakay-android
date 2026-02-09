// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/auth/AuthViewModel.kt

package com.example.yol_yolakay.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep { PHONE, CODE }

// Navigatsiya
sealed class AuthEvent {
    object NavigateToHome : AuthEvent()
    object NavigateToCompleteProfile : AuthEvent()
    object None : AuthEvent()
}

data class AuthState(
    val phone: String = "+998", // Default prefix
    val code: String = "",
    val step: AuthStep = AuthStep.PHONE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val event: AuthEvent = AuthEvent.None
)

class AuthViewModel(
    private val repo: AuthRemoteRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    fun setPhone(v: String) {
        _state.update { it.copy(phone = v, error = null) }
    }

    fun setCode(v: String) {
        _state.update { it.copy(code = v, error = null) }
    }

    fun consumeEvent() {
        _state.update { it.copy(event = AuthEvent.None) }
    }

    // Yordamchi: Raqamni tozalash
    private fun formatPhone(raw: String): String {
        // Faqat raqamlarni qoldiramiz
        val digits = raw.filter { it.isDigit() }
        // Agar 998 bilan boshlanmasa, qo'shib qo'yamiz (o'zbekiston uchun)
        return if (digits.startsWith("998")) "+$digits" else "+998$digits"
    }

    fun sendOtp() {
        viewModelScope.launch {
            val s = _state.value

            // Formatlash
            val cleanPhone = s.phone.replace(" ", "").replace("-", "")

            if (cleanPhone.length < 9) { // Juda qisqa bo'lsa
                _state.update { it.copy(error = "Telefon raqam noto'g'ri") }
                return@launch
            }

            // Supabasega ketadigan format (+998...)
            val finalPhone = if (cleanPhone.startsWith("+")) cleanPhone else "+$cleanPhone"

            _state.update { it.copy(isLoading = true, error = null) }

            runCatching { repo.sendOtp(finalPhone) }
                .onSuccess {
                    _state.update { it.copy(isLoading = false, step = AuthStep.CODE, error = null) }
                }
                .onFailure { e ->
                    // 422 xatosi bo'lsa chiroyli ko'rsatamiz
                    val msg = if (e.message?.contains("422") == true)
                        "Raqam formati noto'g'ri yoki SMS limiti tugagan"
                    else e.message ?: "Xatolik yuz berdi"
                    _state.update { it.copy(isLoading = false, error = msg) }
                }
        }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            val s = _state.value
            val cleanPhone = s.phone.replace(" ", "").replace("-", "")
            val finalPhone = if (cleanPhone.startsWith("+")) cleanPhone else "+$cleanPhone"
            val code = s.code.trim()

            if (code.length < 6) {
                _state.update { it.copy(error = "Kod 6 xonali bo'lishi kerak") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val res = repo.verifyOtp(finalPhone, code)

                val access = res.token
                val refresh = res.refreshToken
                val userId = res.userId

                if (!access.isNullOrBlank() && !userId.isNullOrBlank()) {
                    // ✅ MUHIM: suspend save shu yerda real ishlaydi
                    sessionStore.save(access, refresh, userId)

                    val event =
                        if (res.isNewUser) AuthEvent.NavigateToCompleteProfile
                        else AuthEvent.NavigateToHome

                    _state.update { it.copy(isLoading = false, event = event) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Serverdan token kelmadi") }
                }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Kod noto'g'ri yoki eskirgan") }
            }
        }
    }


    fun backToPhone() {
        _state.update { it.copy(step = AuthStep.PHONE, code = "", error = null) }
    }

    companion object {
        // Factory kerak bo'ladi (chunki repo va sessionStore bor)
        fun factory(context: Context, repo: AuthRemoteRepository, session: SessionStore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(repo, session) as T
            }
        }
    }
}