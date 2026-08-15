package lofod.products.ui.category

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
import lofod.products.data.remote.model.CategoryRole
import lofod.products.data.remote.model.CustomFieldType
import lofod.products.data.remote.request.CreateCategoryRequest
import lofod.products.data.remote.request.CustomFieldDefinitionDto
import lofod.products.data.remote.response.CategoryResponse
import lofod.products.data.repository.CategoryRepository
import lofod.products.ui.catalog.CatalogConstants
import lofod.products.ui.common.ErrorMapper
import lofod.products.ui.common.findCategoryById
import lofod.products.ui.navigation.Routes
import javax.inject.Inject

data class CategoryFormState(
    val editing: CategoryResponse? = null,
    val name: String = "",
    val parentId: String? = null,
    val parentLabel: String = "Без родителя (корень)",
    val ownerCategories: List<CategoryResponse> = emptyList(),
    val treeExpanded: Boolean = false,
    val existingImage: ImageBitmap? = null,
    val newImageBytes: ByteArray? = null,
    val newImagePreview: ImageBitmap? = null,
    val customFields: List<CustomFieldDefinitionDto> = emptyList(),
    val customFieldArchive: List<CustomFieldDefinitionDto> = emptyList(),
    val draftFieldType: CustomFieldType = CustomFieldType.TEXT,
    val draftFieldTitle: String = "",
    val pendingRestoreFieldId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val canAddCustomField: Boolean
        get() = customFields.size < MAX_CUSTOM_FIELDS

    val draftTitleSuggestions: List<CustomFieldDefinitionDto>
        get() {
            val query = draftFieldTitle.trim()
            return customFieldArchive
                .filter { it.type == draftFieldType }
                .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }
                .distinctBy { it.fieldId ?: it.title }
        }

    companion object {
        const val MAX_CUSTOM_FIELDS = 10
    }
}

sealed interface CategoryFormEvent {
    data object Saved : CategoryFormEvent
}

@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryFormState())
    val state: StateFlow<CategoryFormState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CategoryFormEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CategoryFormEvent> = _events.asSharedFlow()

    init {
        val categoryId = savedStateHandle.get<String>(Routes.ARG_CATEGORY_ID)
        if (!categoryId.isNullOrBlank()) {
            openEdit(categoryId)
        } else {
            val parentId = savedStateHandle.get<String>(Routes.ARG_PARENT_ID)
            openCreate(parentId)
        }
    }

    private fun openCreate(preferredParentId: String?) {
        viewModelScope.launch {
            _state.value = CategoryFormState(
                editing = null,
                name = "",
                parentId = preferredParentId.takeUnless { it == CatalogConstants.ROOT_ID },
                isLoading = true
            )
            loadOwnerTree(preferredParentId)
        }
    }

    private fun openEdit(categoryId: String) {
        viewModelScope.launch {
            _state.value = CategoryFormState(isLoading = true)
            try {
                val all = categoryRepository.getCategories()
                val category = findCategoryById(categoryId, all)
                if (category == null) {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = "Категория не найдена")
                    }
                    return@launch
                }
                _state.value = CategoryFormState(
                    editing = category,
                    name = category.name,
                    parentId = category.parentId,
                    customFields = category.customFields,
                    customFieldArchive = category.customFieldArchive,
                    isLoading = true
                )
                loadOwnerTree(category.parentId)
                category.imageId?.let { imageId ->
                    try {
                        val base64 = categoryRepository.getCategoryImage(imageId).image
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap =
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        _state.update { it.copy(existingImage = bitmap) }
                    } catch (_: Exception) {
                        // Keep placeholder when image fails to load.
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, errorMessage = null) }
    }

    fun onTreeExpand() {
        _state.update { it.copy(treeExpanded = true) }
    }

    fun onParentChosen(category: CategoryResponse?) {
        _state.update {
            it.copy(
                parentId = category?.categoryId,
                parentLabel = category?.name ?: "Без родителя (корень)",
                treeExpanded = false
            )
        }
    }

    fun onNewImageSelected(bytes: ByteArray) {
        val preview = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        _state.update {
            it.copy(newImageBytes = bytes, newImagePreview = preview, errorMessage = null)
        }
    }

    fun onDraftFieldTypeChange(type: CustomFieldType) {
        _state.update {
            it.copy(
                draftFieldType = type,
                draftFieldTitle = "",
                pendingRestoreFieldId = null,
                errorMessage = null
            )
        }
    }

    fun onDraftFieldTitleChange(value: String) {
        _state.update {
            it.copy(
                draftFieldTitle = value,
                pendingRestoreFieldId = null,
                errorMessage = null
            )
        }
    }

    fun onArchiveSuggestionPicked(field: CustomFieldDefinitionDto) {
        _state.update {
            it.copy(
                draftFieldType = field.type,
                draftFieldTitle = field.title,
                pendingRestoreFieldId = field.fieldId,
                errorMessage = null
            )
        }
    }

    fun addCustomField() {
        val current = _state.value
        if (!current.canAddCustomField) {
            _state.update { it.copy(errorMessage = "Максимум ${CategoryFormState.MAX_CUSTOM_FIELDS} полей") }
            return
        }
        val title = current.draftFieldTitle.trim()
        if (title.isEmpty()) {
            _state.update { it.copy(errorMessage = "Введите название поля") }
            return
        }
        val restoreId = current.pendingRestoreFieldId
        val definition = CustomFieldDefinitionDto(
            fieldId = restoreId,
            title = title,
            type = current.draftFieldType
        )
        _state.update {
            it.copy(
                customFields = it.customFields + definition,
                customFieldArchive = if (restoreId != null) {
                    it.customFieldArchive.filterNot { archived -> archived.fieldId == restoreId }
                } else {
                    it.customFieldArchive
                },
                draftFieldTitle = "",
                pendingRestoreFieldId = null,
                errorMessage = null
            )
        }
    }

    fun removeCustomField(index: Int) {
        _state.update { state ->
            if (index !in state.customFields.indices) return@update state
            val removed = state.customFields[index]
            state.copy(
                customFields = state.customFields.filterIndexed { i, _ -> i != index },
                customFieldArchive = state.customFieldArchive + removed,
                errorMessage = null
            )
        }
    }

    fun save() {
        val current = _state.value
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
                        categoryRepository.uploadCategoryImage(current.newImageBytes)
                    else -> current.editing?.imageId
                }
                val parentId = current.parentId?.takeUnless { it == CatalogConstants.ROOT_ID }
                val request = CreateCategoryRequest(
                    parentId = parentId,
                    name = name,
                    imageId = imageId,
                    customFields = current.customFields
                )
                if (current.editing != null) {
                    categoryRepository.updateCategory(current.editing.categoryId, request)
                } else {
                    categoryRepository.createCategory(request)
                }
                _events.emit(CategoryFormEvent.Saved)
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, errorMessage = ErrorMapper.toMessage(e))
                }
            }
        }
    }

    private suspend fun loadOwnerTree(preferredParentId: String?) {
        try {
            val all = categoryRepository.getCategories()
            val ownerOnly = filterOwnerCategories(all)
            val parentId = preferredParentId?.takeUnless { it == CatalogConstants.ROOT_ID }
            val parentLabel = when {
                parentId == null -> "Без родителя (корень)"
                else -> findCategoryById(parentId, all)?.name ?: "Без родителя (корень)"
            }
            _state.update {
                it.copy(
                    ownerCategories = ownerOnly,
                    parentId = parentId,
                    parentLabel = parentLabel,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(isLoading = false, errorMessage = ErrorMapper.toMessage(e))
            }
        }
    }

    private fun filterOwnerCategories(categories: List<CategoryResponse>): List<CategoryResponse> =
        categories.mapNotNull { category ->
            if (category.role != CategoryRole.OWNER) {
                null
            } else {
                category.copy(subcategories = filterOwnerCategories(category.subcategories))
            }
        }
}
