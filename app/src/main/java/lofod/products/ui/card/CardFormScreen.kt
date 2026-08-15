package lofod.products.ui.card

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lofod.products.R
import lofod.products.data.remote.model.CustomFieldType
import lofod.products.data.remote.model.PriceLevel
import lofod.products.data.remote.model.QualityLevel
import lofod.products.data.remote.request.CustomFieldDefinitionDto
import lofod.products.data.remote.response.CardResponse
import lofod.products.ui.common.ButtonProgressIndicator
import lofod.products.ui.common.RatingBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFormScreen(
    onSaved: (List<CardResponse>) -> Unit,
    onBack: () -> Unit,
    viewModel: CardFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var priceExpanded by remember { mutableStateOf(false) }
    var qualityExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CardFormEvent.Saved -> onSaved(event.cards)
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
                            "Редактирование оценки"
                        } else {
                            "Создание оценки"
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

                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                ExposedDropdownMenuBox(
                    expanded = priceExpanded,
                    onExpandedChange = { priceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = state.priceLevel.text(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Цена") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = priceExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = priceExpanded,
                        onDismissRequest = { priceExpanded = false }
                    ) {
                        PriceLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.text()) },
                                onClick = {
                                    priceExpanded = false
                                    viewModel.onPriceLevelChange(level)
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = qualityExpanded,
                    onExpandedChange = { qualityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = state.qualityLevel.text(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Качество") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = qualityExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = qualityExpanded,
                        onDismissRequest = { qualityExpanded = false }
                    ) {
                        QualityLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.text()) },
                                onClick = {
                                    qualityExpanded = false
                                    viewModel.onQualityLevelChange(level)
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Рейтинг",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RatingBar(
                    rating = state.rating,
                    interactive = true,
                    onRatingChange = viewModel::onRatingChange
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
                    contentDescription = "Картинка оценки",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { launcher.launch("image/*") }
                        .align(Alignment.CenterHorizontally)
                )

                state.customFields.forEach { field ->
                    val fieldId = field.fieldId ?: return@forEach
                    CustomFieldEditor(
                        definition = field,
                        value = state.customFieldValues[fieldId],
                        onValueChange = { viewModel.onCustomFieldValueChange(fieldId, it) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomFieldEditor(
    definition: CustomFieldDefinitionDto,
    value: String?,
    onValueChange: (String?) -> Unit
) {
    when (definition.type) {
        CustomFieldType.TEXT -> {
            FieldTitle(definition.title)
            OutlinedTextField(
                value = value.orEmpty(),
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        CustomFieldType.NUMBER -> {
            FieldTitle(definition.title)
            OutlinedTextField(
                value = value.orEmpty(),
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        CustomFieldType.BOOLEAN -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = definition.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = value == "true",
                    onCheckedChange = { checked ->
                        onValueChange(if (checked) "true" else "false")
                    }
                )
            }
        }

        CustomFieldType.DATE -> {
            FieldTitle(definition.title)
            DateCustomField(
                value = value,
                onValueChange = onValueChange
            )
        }

        CustomFieldType.COUNTER -> {
            FieldTitle(definition.title)
            CounterCustomField(
                value = value,
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
private fun FieldTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCustomField(
    value: String?,
    onValueChange: (String?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val display = value.orEmpty().ifEmpty { "Выберите дату" }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        val initialMillis = remember(value) {
            value
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    runCatching {
                        LocalDate.parse(it)
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli()
                    }.getOrNull()
                }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onValueChange(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                        showPicker = false
                    }
                ) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CounterCustomField(
    value: String?,
    onValueChange: (String?) -> Unit
) {
    val current = value?.toIntOrNull() ?: 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = { onValueChange((current - 1).toString()) }
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Уменьшить")
        }
        OutlinedTextField(
            value = current.toString(),
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() || (it == '-' && input.indexOf(it) == 0) }
                if (digits.isEmpty() || digits == "-") {
                    onValueChange("0")
                } else {
                    onValueChange(digits.toIntOrNull()?.toString() ?: "0")
                }
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        IconButton(
            onClick = { onValueChange((current + 1).toString()) }
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Увеличить")
        }
    }
}
