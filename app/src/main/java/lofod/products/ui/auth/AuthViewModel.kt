package lofod.products.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lofod.products.data.repository.AuthRepository
import lofod.products.ui.common.ErrorMapper
import javax.inject.Inject

data class AuthFormState(
    val username: String = "",
    val password: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AuthNavEvent {
    data object ToCatalog : AuthNavEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(AuthFormState())
    val loginState: StateFlow<AuthFormState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthFormState())
    val registerState: StateFlow<AuthFormState> = _registerState.asStateFlow()

    private val _navEvents = MutableSharedFlow<AuthNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<AuthNavEvent> = _navEvents.asSharedFlow()

    fun onLoginUsernameChange(value: String) {
        _loginState.update {
            it.copy(username = value, usernameError = null, errorMessage = null)
        }
    }

    fun onLoginPasswordChange(value: String) {
        _loginState.update {
            it.copy(password = value, passwordError = null, errorMessage = null)
        }
    }

    fun onRegisterUsernameChange(value: String) {
        _registerState.update {
            it.copy(username = value, usernameError = null, errorMessage = null)
        }
    }

    fun onRegisterPasswordChange(value: String) {
        _registerState.update {
            it.copy(password = value, passwordError = null, errorMessage = null)
        }
    }

    fun login() {
        val current = _loginState.value
        val usernameError = AuthValidation.validateUsername(current.username)
        val passwordError = AuthValidation.validatePassword(current.password)
        if (usernameError != null || passwordError != null) {
            _loginState.update {
                it.copy(usernameError = usernameError, passwordError = passwordError)
            }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.login(current.username.trim(), current.password)
                _loginState.update { it.copy(isLoading = false) }
                _navEvents.emit(AuthNavEvent.ToCatalog)
            } catch (e: Exception) {
                _loginState.update {
                    it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }

    fun register() {
        val current = _registerState.value
        val usernameError = AuthValidation.validateUsername(current.username)
        val passwordError = AuthValidation.validatePassword(current.password)
        if (usernameError != null || passwordError != null) {
            _registerState.update {
                it.copy(usernameError = usernameError, passwordError = passwordError)
            }
            return
        }

        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.register(current.username.trim(), current.password)
                _registerState.update { it.copy(isLoading = false) }
                _navEvents.emit(AuthNavEvent.ToCatalog)
            } catch (e: Exception) {
                _registerState.update {
                    it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }
}
