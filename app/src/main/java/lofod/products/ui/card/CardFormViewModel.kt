package lofod.products.ui.card

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import lofod.products.data.remote.model.PriceLevel
import lofod.products.data.remote.model.QualityLevel
import lofod.products.data.remote.request.CreateCardRequest
import lofod.products.data.remote.response.CardResponse
import lofod.products.data.repository.CategoryRepository
import lofod.products.ui.common.ErrorMapper
import javax.inject.Inject

data class CardFormState(
    val isVisible: Boolean = false,
    val categoryId: String? = null,
    val editing: CardResponse? = null,
    val name: String = "",
    val description: String = "",
    val priceLevel: PriceLevel = PriceLevel.LOW_PRICE,
    val qualityLevel: QualityLevel = QualityLevel.LOW_QUALITY,
    val existingImage: ImageBitmap? = null,
    val newImageBytes: ByteArray? = null,
    val newImagePreview: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface CardFormEvent {
    data class Saved(val cards: List<CardResponse>) : CardFormEvent
}

@HiltViewModel
class CardFormViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CardFormState())
    val state: StateFlow<CardFormState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CardFormEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CardFormEvent> = _events.asSharedFlow()

    fun openCreate(categoryId: String) {
        _state.value = CardFormState(
            isVisible = true,
            categoryId = categoryId,
            editing = null
        )
    }

    fun openEdit(categoryId: String, card: CardResponse) {
        viewModelScope.launch {
            _state.value = CardFormState(
                isVisible = true,
                categoryId = categoryId,
                editing = card,
                name = card.name,
                description = card.description.orEmpty(),
                priceLevel = card.priceLevel,
                qualityLevel = card.qualityLevel,
                isLoading = card.imageId != null
            )
            card.imageId?.let { imageId ->
                try {
                    val base64 = categoryRepository.getCardImage(imageId).image
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    _state.update { it.copy(existingImage = bitmap, isLoading = false) }
                } catch (e: Exception) {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
                    }
                }
            }
        }
    }

    fun dismiss() {
        _state.value = CardFormState()
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value, errorMessage = null) }
    }

    fun onPriceLevelChange(value: PriceLevel) {
        _state.update { it.copy(priceLevel = value) }
    }

    fun onQualityLevelChange(value: QualityLevel) {
        _state.update { it.copy(qualityLevel = value) }
    }

    fun onNewImageSelected(bytes: ByteArray) {
        val preview = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        _state.update {
            it.copy(newImageBytes = bytes, newImagePreview = preview, errorMessage = null)
        }
    }

    fun save() {
        val current = _state.value
        val categoryId = current.categoryId ?: return
        val name = current.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(errorMessage = "Введите название") }
            return
        }
        if (current.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                // Keep existing imageId when editing without a new photo.
                val imageId = when {
                    current.newImageBytes != null ->
                        categoryRepository.uploadCardImage(current.newImageBytes)
                    else -> current.editing?.imageId
                }
                val request = CreateCardRequest(
                    name = name,
                    imageId = imageId,
                    priceLevel = current.priceLevel,
                    qualityLevel = current.qualityLevel,
                    description = current.description.trim().ifEmpty { null }
                )
                val cards = if (current.editing != null) {
                    categoryRepository.updateCard(categoryId, current.editing.cardId, request)
                } else {
                    categoryRepository.createCard(categoryId, request)
                }
                _state.value = CardFormState()
                _events.emit(CardFormEvent.Saved(cards))
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }
}
