package com.example.yol_yolakay.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Yangi step qo'shildi: PROFILE_MISSING
enum class AuthStep { PHONE, CODE }

// Navigatsiya uchun Eventlar (Single Live Event o'rniga oddiyroq yechim)
sealed class AuthEvent {
    object NavigateToHome : AuthEvent()
    object NavigateToCompleteProfile : AuthEvent()
    object None : AuthEvent()
}

data class AuthState(
    val phone: String = "",
    val code: String = "",
    val step: AuthStep = AuthStep.PHONE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val event: AuthEvent = AuthEvent.None // <-- Navigatsiya boshqaruvi
)

class AuthViewModel(
    private val repo: AuthRemoteRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    fun setPhone(v: String) { _state.value = _state.value.copy(phone = v, error = null) }
    fun setCode(v: String) { _state.value = _state.value.copy(code = v, error = null) }

    // Eventni "iste'mol" qilingandan keyin o'chirish uchun
    fun consumeEvent() { _state.value = _state.value.copy(event = AuthEvent.None) }

    fun sendOtp() {
        viewModelScope.launch {
            val s = _state.value
            val p = s.phone.trim()
            if (p.isBlank()) { _state.value = s.copy(error = "Telefon raqam kiriting"); return@launch }

            _state.value = s.copy(isLoading = true, error = null)
            runCatching { repo.sendOtp(p) }
                .onSuccess {
                    // Muvaffaqiyatli bo'lsa kod kiritishga o'tamiz
                    _state.value = _state.value.copy(isLoading = false, step = AuthStep.CODE)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            val s = _state.value
            val p = s.phone.trim()
            val c = s.code.trim()
            if (c.isBlank()) { _state.value = s.copy(error = "SMS kodni kiriting"); return@launch }

            _state.value = s.copy(isLoading = true, error = null)
            runCatching { repo.verifyOtp(p, c) }
                .onSuccess { res ->
                    val token = res.accessToken
                    val uid = res.userId

                    if (!token.isNullOrBlank() && !uid.isNullOrBlank()) {
                        // 1. Tokenni saqlaymiz
                        sessionStore.save(token, res.refreshToken, uid)

                        // 2. Logic: User yangimi yoki eskami?
                        if (res.isNewUser) {
                            // Agar yangi bo'lsa -> Profil to'ldirishga
                            _state.value = _state.value.copy(isLoading = false, event = AuthEvent.NavigateToCompleteProfile)
                        } else {
                            // Agar eski bo'lsa -> To'g'ridan-to'g'ri Homega
                            _state.value = _state.value.copy(isLoading = false, event = AuthEvent.NavigateToHome)
                        }
                    } else {
                        _state.value = _state.value.copy(isLoading = false, error = res.message ?: "Token kelmadi")
                    }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    fun backToPhone() {
        _state.value = _state.value.copy(step = AuthStep.PHONE, code = "", error = null)
    }
}