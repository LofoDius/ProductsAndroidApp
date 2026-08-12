package lofod.products.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lofod.products.data.remote.response.MemberResponse
import lofod.products.data.repository.CategoryRepository
import lofod.products.ui.common.ErrorMapper
import javax.inject.Inject

data class MembersUiState(
    val isVisible: Boolean = false,
    val categoryId: String? = null,
    val categoryName: String = "",
    val members: List<MemberResponse> = emptyList(),
    val usernameInput: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MembersUiState())
    val state: StateFlow<MembersUiState> = _state.asStateFlow()

    fun open(categoryId: String, categoryName: String) {
        viewModelScope.launch {
            _state.value = MembersUiState(
                isVisible = true,
                categoryId = categoryId,
                categoryName = categoryName,
                isLoading = true
            )
            loadMembers(categoryId)
        }
    }

    fun dismiss() {
        _state.value = MembersUiState()
    }

    fun onUsernameChange(value: String) {
        _state.update { it.copy(usernameInput = value, errorMessage = null) }
    }

    fun invite() {
        val current = _state.value
        val categoryId = current.categoryId ?: return
        val username = current.usernameInput.trim()
        if (username.isEmpty()) {
            _state.update { it.copy(errorMessage = "Введите username") }
            return
        }
        if (current.isSubmitting) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                categoryRepository.inviteMember(categoryId, username)
                _state.update { it.copy(usernameInput = "", isSubmitting = false) }
                loadMembers(categoryId)
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSubmitting = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }

    fun remove(userId: String) {
        val categoryId = _state.value.categoryId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                categoryRepository.removeMember(categoryId, userId)
                _state.update { it.copy(isSubmitting = false) }
                loadMembers(categoryId)
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSubmitting = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }

    private suspend fun loadMembers(categoryId: String) {
        try {
            val members = categoryRepository.listMembers(categoryId)
            _state.update {
                it.copy(members = members, isLoading = false, errorMessage = null)
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
            }
        }
    }
}
