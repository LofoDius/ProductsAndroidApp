package lofod.products.ui.category

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lofod.products.R
import lofod.products.data.remote.model.CustomFieldType
import lofod.products.data.remote.request.CustomFieldDefinitionDto
import lofod.products.data.remote.response.CategoryResponse
import lofod.products.ui.common.ButtonProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CategoryFormEvent.Saved -> onSaved()
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            viewModel.onNewImageSelected(stream.readBytes())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.editing != null) {
                            "Редактирование категории"
                        } else {
                            "Добавление категории"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isSaving) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !state.isSaving && !state.isLoading
                    ) {
                        if (state.isSaving) {
                            ButtonProgressIndicator(size = 18.dp)
                        } else {
                            Text("Сохранить")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading && state.editing == null && state.name.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Родительская категория",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ParentTreePicker(
                    expanded = state.treeExpanded,
                    currentLabel = state.parentLabel,
                    categories = state.ownerCategories,
                    onExpand = viewModel::onTreeExpand,
                    onChooseRoot = { viewModel.onParentChosen(null) },
                    onChoose = viewModel::onParentChosen
                )

                Text(
                    text = "Фото",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Image(
                    bitmap = state.newImagePreview
                        ?: state.existingImage
                        ?: ImageBitmap.imageResource(R.drawable.placeholder),
                    contentDescription = "Картинка категории",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { launcher.launch("image/*") }
                        .align(Alignment.CenterHorizontally)
                )

                CustomFieldsSection(
                    customFields = state.customFields,
                    draftFieldType = state.draftFieldType,
                    draftFieldTitle = state.draftFieldTitle,
                    titleSuggestions = state.draftTitleSuggestions,
                    canAdd = state.canAddCustomField,
                    enabled = !state.isSaving,
                    onTypeChange = viewModel::onDraftFieldTypeChange,
                    onTitleChange = viewModel::onDraftFieldTitleChange,
                    onSuggestionPicked = viewModel::onArchiveSuggestionPicked,
                    onAdd = viewModel::addCustomField,
                    onRemove = viewModel::removeCustomField
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomFieldsSection(
    customFields: List<CustomFieldDefinitionDto>,
    draftFieldType: CustomFieldType,
    draftFieldTitle: String,
    titleSuggestions: List<CustomFieldDefinitionDto>,
    canAdd: Boolean,
    enabled: Boolean,
    onTypeChange: (CustomFieldType) -> Unit,
    onTitleChange: (String) -> Unit,
    onSuggestionPicked: (CustomFieldDefinitionDto) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit
) {
    var typeExpanded by remember { mutableStateOf(false) }
    var titleExpanded by remember { mutableStateOf(false) }

    Text(
        text = "Кастомные поля",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (customFields.isEmpty()) {
        Text(
            text = "Нет активных полей",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        customFields.forEachIndexed { index, field ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = field.title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = field.type.text(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { onRemove(index) },
                    enabled = enabled
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Удалить поле"
                    )
                }
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = typeExpanded,
        onExpandedChange = { if (enabled && canAdd) typeExpanded = it }
    ) {
        OutlinedTextField(
            value = draftFieldType.text(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled && canAdd,
            label = { Text("Тип") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = typeExpanded,
            onDismissRequest = { typeExpanded = false }
        ) {
            CustomFieldType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.text()) },
                    onClick = {
                        typeExpanded = false
                        onTypeChange(type)
                    }
                )
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = titleExpanded && titleSuggestions.isNotEmpty(),
        onExpandedChange = { expanded ->
            if (enabled && canAdd) {
                titleExpanded = expanded && titleSuggestions.isNotEmpty()
            }
        }
    ) {
        OutlinedTextField(
            value = draftFieldTitle,
            onValueChange = {
                onTitleChange(it)
                titleExpanded = true
            },
            enabled = enabled && canAdd,
            label = { Text("Название поля") },
            singleLine = true,
            trailingIcon = {
                if (titleSuggestions.isNotEmpty()) {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = titleExpanded && titleSuggestions.isNotEmpty()
                    )
                }
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = titleExpanded && titleSuggestions.isNotEmpty(),
            onDismissRequest = { titleExpanded = false }
        ) {
            titleSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion.title) },
                    onClick = {
                        titleExpanded = false
                        onSuggestionPicked(suggestion)
                    }
                )
            }
        }
    }

    TextButton(
        onClick = onAdd,
        enabled = enabled && canAdd && draftFieldTitle.isNotBlank()
    ) {
        Text(
            if (canAdd) {
                "Добавить поле"
            } else {
                "Достигнут лимит (10)"
            }
        )
    }
}

@Composable
private fun ParentTreePicker(
    expanded: Boolean,
    currentLabel: String,
    categories: List<CategoryResponse>,
    onExpand: () -> Unit,
    onChooseRoot: () -> Unit,
    onChoose: (CategoryResponse) -> Unit,
    padding: Int = 0
) {
    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "parent-tree"
    ) { isExpanded ->
        if (isExpanded) {
            Column {
                TextButton(onClick = onChooseRoot) {
                    Text("Без родителя (корень)")
                }
                categories.forEach { category ->
                    TextButton(onClick = { onChoose(category) }) {
                        Text(
                            text = category.name,
                            modifier = Modifier.padding(start = (4 + padding).dp)
                        )
                    }
                    if (category.subcategories.isNotEmpty()) {
                        ParentTreePicker(
                            expanded = true,
                            currentLabel = category.name,
                            categories = category.subcategories,
                            onExpand = onExpand,
                            onChooseRoot = onChooseRoot,
                            onChoose = onChoose,
                            padding = padding + 8
                        )
                    }
                }
            }
        } else {
            TextButton(onClick = onExpand) {
                Text(currentLabel)
            }
        }
    }
}
