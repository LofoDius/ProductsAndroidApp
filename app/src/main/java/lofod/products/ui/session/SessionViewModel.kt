package lofod.products.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lofod.products.data.remote.SessionExpiredNotifier
import lofod.products.data.repository.AuthRepository
import lofod.products.ui.common.ErrorMapper
import javax.inject.Inject

enum class SessionBootstrapState {
    Loading,
    Authenticated,
    Unauthenticated
}

sealed interface SessionNavEvent {
    data object ToLogin : SessionNavEvent
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionExpiredNotifier: SessionExpiredNotifier
) : ViewModel() {

    private val _bootstrapState = MutableStateFlow(SessionBootstrapState.Loading)
    val bootstrapState: StateFlow<SessionBootstrapState> = _bootstrapState.asStateFlow()

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    private val _logoutError = MutableStateFlow<String?>(null)
    val logoutError: StateFlow<String?> = _logoutError.asStateFlow()

    private val _navEvents = MutableSharedFlow<SessionNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<SessionNavEvent> = _navEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            val valid = authRepository.restoreSessionIfValid()
            _bootstrapState.value = if (valid) {
                SessionBootstrapState.Authenticated
            } else {
                SessionBootstrapState.Unauthenticated
            }
        }

        viewModelScope.launch {
            sessionExpiredNotifier.events.collect {
                authRepository.clearSession()
                // During bootstrap, restoreSessionIfValid already decides Authenticated vs not.
                if (_bootstrapState.value == SessionBootstrapState.Loading) {
                    return@collect
                }
                _bootstrapState.value = SessionBootstrapState.Unauthenticated
                _navEvents.emit(SessionNavEvent.ToLogin)
            }
        }
    }

    fun logout() {
        if (_isLoggingOut.value) return
        viewModelScope.launch {
            _isLoggingOut.value = true
            _logoutError.value = null
            try {
                authRepository.logout()
            } catch (e: Exception) {
                // Session is cleared in repository finally; surface message if useful.
                _logoutError.value = ErrorMapper.toMessage(e)
            } finally {
                _isLoggingOut.value = false
                _bootstrapState.value = SessionBootstrapState.Unauthenticated
                _navEvents.emit(SessionNavEvent.ToLogin)
            }
        }
    }

    fun clearLogoutError() {
        _logoutError.value = null
    }
}
