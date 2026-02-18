package com.example.yol_yolakay.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.session.SessionStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep { PHONE, CODE }

sealed class AuthEvent {
    data object NavigateHome : AuthEvent()
    data object NavigateCompleteProfile : AuthEvent()
    data object None : AuthEvent()
}

data class AuthState(
    val step: AuthStep = AuthStep.PHONE,

    // phone
    val phoneRaw: String = "+998",
    val phoneE164: String = "",

    // code
    val code: String = "",

    // ui
    val isLoading: Boolean = false,
    val error: String? = null,

    // resend
    val resendSecondsLeft: Int = 0,
    val canResend: Boolean = true,

    val event: AuthEvent = AuthEvent.None
)

class AuthViewModel(
    private val repo: AuthRemoteRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    fun consumeEvent() = _state.update { it.copy(event = AuthEvent.None) }

    fun setPhone(v: String) {
        _state.update { it.copy(phoneRaw = v, error = null) }
    }

    fun setCode(v: String) {
        val digits = v.filter(Char::isDigit).take(6)
        _state.update { it.copy(code = digits, error = null) }

        // UX: 6 ta bo‘lsa auto verify
        if (digits.length == 6) verifyOtp()
    }

    fun backToPhone() {
        _state.update {
            it.copy(
                step = AuthStep.PHONE,
                code = "",
                error = null,
                resendSecondsLeft = 0,
                canResend = true
            )
        }
    }

    fun sendOtp() {
        val phone = normalizeToE164(_state.value.phoneRaw)
        if (phone == null) {
            _state.update { it.copy(error = "Telefon raqami noto‘g‘ri") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, phoneE164 = phone) }

            runCatching { repo.sendOtp(phone) }
                .onSuccess {
                    _state.update { it.copy(isLoading = false, step = AuthStep.CODE, code = "") }
                    startResendTimer(40) // Uber uslub: 30–60s oralig‘ida
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = humanize(e.message)) }
                }
        }
    }

    fun resendOtp() {
        val phone = _state.value.phoneE164.ifBlank { normalizeToE164(_state.value.phoneRaw) ?: "" }
        if (phone.isBlank()) return
        if (!_state.value.canResend) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repo.sendOtp(phone) }
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    startResendTimer(40)
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = humanize(e.message)) }
                }
        }
    }

    fun verifyOtp() {
        val s = _state.value
        val phone = s.phoneE164.ifBlank { normalizeToE164(s.phoneRaw) ?: "" }
        val code = s.code.trim()

        if (phone.isBlank()) {
            _state.update { it.copy(error = "Telefon raqami noto‘g‘ri") }
            return
        }
        if (code.length != 6) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            runCatching { repo.verifyOtp(phone, code) }
                .onSuccess { res ->
                    val access = res.accessToken
                    val userId = res.userId
                    if (access.isNullOrBlank() || userId.isNullOrBlank()) {
                        _state.update { it.copy(isLoading = false, error = "Serverdan token kelmadi") }
                        return@onSuccess
                    }

                    sessionStore.save(access, res.refreshToken, userId)

                    val ev =
                        if (res.isNewUser) AuthEvent.NavigateCompleteProfile
                        else AuthEvent.NavigateHome

                    _state.update { it.copy(isLoading = false, event = ev) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = humanize(e.message)) }
                }
        }
    }

    private fun startResendTimer(seconds: Int) {
        viewModelScope.launch {
            _state.update { it.copy(canResend = false, resendSecondsLeft = seconds) }
            var t = seconds
            while (t > 0) {
                delay(1000)
                t--
                _state.update { it.copy(resendSecondsLeft = t) }
            }
            _state.update { it.copy(canResend = true) }
        }
    }

    private fun normalizeToE164(raw: String): String? {
        val digits = raw.filter(Char::isDigit)
        // Uzbek default: +998XXXXXXXXX (9 ta raqamdan keyin)
        return when {
            digits.startsWith("998") && digits.length >= 12 -> "+${digits.take(12)}"
            digits.length == 9 -> "+998$digits"
            raw.startsWith("+") && digits.length in 10..15 -> "+$digits"
            else -> null
        }
    }

    private fun humanize(msg: String?): String {
        val m = msg.orEmpty().lowercase()
        return when {
            m.contains("429") || m.contains("too many") -> "Juda ko‘p urinish. Birozdan keyin qayta urinib ko‘ring."
            m.contains("401") || m.contains("unauthorized") -> "Sessiya yaroqsiz. Qayta kiring."
            m.contains("422") -> "Raqam formati noto‘g‘ri yoki limit tugagan."
            m.contains("invalid") || m.contains("wrong") -> "Kod noto‘g‘ri yoki eskirgan."
            msg.isNullOrBlank() -> "Xatolik yuz berdi"
            else -> msg.take(220)
        }
    }

    companion object {
        fun factory(repo: AuthRemoteRepository, session: SessionStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(repo, session) as T
                }
            }
    }
}
