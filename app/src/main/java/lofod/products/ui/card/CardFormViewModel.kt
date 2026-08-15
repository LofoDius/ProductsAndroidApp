package lofod.products.ui.card

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
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
import lofod.products.data.remote.model.CustomFieldType
import lofod.products.data.remote.model.PriceLevel
import lofod.products.data.remote.model.QualityLevel
import lofod.products.data.remote.request.CreateCardRequest
import lofod.products.data.remote.request.CustomFieldDefinitionDto
import lofod.products.data.remote.request.CustomFieldValueDto
import lofod.products.data.remote.response.CardResponse
import lofod.products.data.repository.CategoryRepository
import lofod.products.ui.common.ErrorMapper
import lofod.products.ui.common.findCategoryById
import javax.inject.Inject

data class CardFormState(
    val categoryId: String? = null,
    val editing: CardResponse? = null,
    val name: String = "",
    val description: String = "",
    val priceLevel: PriceLevel = PriceLevel.LOW_PRICE,
    val qualityLevel: QualityLevel = QualityLevel.LOW_QUALITY,
    val rating: Int = 0,
    val customFields: List<CustomFieldDefinitionDto> = emptyList(),
    val customFieldValues: Map<String, String?> = emptyMap(),
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
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(CardFormState())
    val state: StateFlow<CardFormState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CardFormEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CardFormEvent> = _events.asSharedFlow()

    init {
        val categoryId = checkNotNull(savedStateHandle.get<String>("categoryId"))
        val cardId = savedStateHandle.get<String>("cardId")
        if (cardId.isNullOrBlank()) {
            openCreate(categoryId)
        } else {
            openEdit(categoryId, cardId)
        }
    }

    private fun openCreate(categoryId: String) {
        viewModelScope.launch {
            _state.value = CardFormState(categoryId = categoryId, isLoading = true)
            try {
                val customFields = loadActiveCustomFields(categoryId)
                _state.update {
                    it.copy(
                        customFields = customFields,
                        customFieldValues = defaultsFor(customFields),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }

    private fun openEdit(categoryId: String, cardId: String) {
        viewModelScope.launch {
            _state.value = CardFormState(
                categoryId = categoryId,
                isLoading = true
            )
            try {
                val customFields = loadActiveCustomFields(categoryId)
                val card = categoryRepository.getCard(categoryId, cardId)
                val valuesById = card.customFieldValues.associate { it.fieldId to it.value }
                _state.update {
                    it.copy(
                        editing = card,
                        name = card.name,
                        description = card.description.orEmpty(),
                        priceLevel = card.priceLevel,
                        qualityLevel = card.qualityLevel,
                        rating = card.rating.coerceIn(0, 10),
                        customFields = customFields,
                        customFieldValues = defaultsFor(customFields, valuesById),
                        isLoading = card.imageId != null
                    )
                }
                card.imageId?.let { imageId ->
                    try {
                        val base64 = categoryRepository.getCardImage(imageId).image
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap =
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        _state.update { it.copy(existingImage = bitmap, isLoading = false) }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
                        }
                    }
                } ?: _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }

    private suspend fun loadActiveCustomFields(categoryId: String): List<CustomFieldDefinitionDto> {
        val all = categoryRepository.getCategories()
        val category = findCategoryById(categoryId, all)
            ?: throw IllegalStateException("Категория не найдена")
        return category.customFields.filter { !it.fieldId.isNullOrBlank() }
    }

    private fun defaultsFor(
        fields: List<CustomFieldDefinitionDto>,
        existing: Map<String, String?> = emptyMap()
    ): Map<String, String?> = fields.mapNotNull { field ->
        val fieldId = field.fieldId ?: return@mapNotNull null
        fieldId to resolveValue(field.type, existing[fieldId])
    }.toMap()

    private fun resolveValue(type: CustomFieldType, existing: String?): String? {
        if (existing != null) return existing
        return when (type) {
            CustomFieldType.TEXT -> ""
            CustomFieldType.NUMBER -> ""
            CustomFieldType.BOOLEAN -> "false"
            CustomFieldType.DATE -> null
            CustomFieldType.COUNTER -> "0"
        }
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

    fun onRatingChange(value: Int) {
        _state.update { it.copy(rating = value.coerceIn(0, 10)) }
    }

    fun onCustomFieldValueChange(fieldId: String, value: String?) {
        _state.update {
            it.copy(
                customFieldValues = it.customFieldValues + (fieldId to value),
                errorMessage = null
            )
        }
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
                    rating = current.rating.coerceIn(0, 10),
                    description = current.description.trim().ifEmpty { null },
                    customFieldValues = serializeCustomFieldValues(
                        current.customFields,
                        current.customFieldValues
                    )
                )
                val cards = if (current.editing != null) {
                    categoryRepository.updateCard(categoryId, current.editing.cardId, request)
                } else {
                    categoryRepository.createCard(categoryId, request)
                }
                _events.emit(CardFormEvent.Saved(cards))
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }

    private fun serializeCustomFieldValues(
        fields: List<CustomFieldDefinitionDto>,
        values: Map<String, String?>
    ): List<CustomFieldValueDto> = fields.mapNotNull { field ->
        val fieldId = field.fieldId ?: return@mapNotNull null
        CustomFieldValueDto(
            fieldId = fieldId,
            value = serializeValue(field.type, values[fieldId])
        )
    }

    private fun serializeValue(type: CustomFieldType, raw: String?): String? = when (type) {
        CustomFieldType.TEXT -> raw.orEmpty()
        CustomFieldType.NUMBER -> raw?.takeIf { it.isNotBlank() }
        CustomFieldType.BOOLEAN -> if (raw == "true") "true" else "false"
        CustomFieldType.DATE -> raw?.takeIf { it.isNotBlank() }
        CustomFieldType.COUNTER -> raw?.toIntOrNull()?.toString() ?: "0"
    }
}
