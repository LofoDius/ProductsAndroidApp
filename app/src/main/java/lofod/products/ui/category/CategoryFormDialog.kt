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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lofod.products.R
import lofod.products.data.remote.response.CategoryResponse

@Composable
fun CategoryFormDialog(
    viewModel: CategoryFormViewModel,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CategoryFormEvent.Saved -> onSaved()
            }
        }
    }

    if (!state.isVisible) return

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            viewModel.onNewImageSelected(stream.readBytes())
        }
    }

    Dialog(onDismissRequest = {
        viewModel.dismiss()
        onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scroll)
            ) {
                Text(
                    text = if (state.editing != null) {
                        "Редактирование категории"
                    } else {
                        "Добавление категории"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Родительская категория",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    ParentTreePicker(
                        expanded = state.treeExpanded,
                        currentLabel = state.parentLabel,
                        categories = state.ownerCategories,
                        onExpand = viewModel::onTreeExpand,
                        onChooseRoot = { viewModel.onParentChosen(null) },
                        onChoose = viewModel::onParentChosen
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = state.newImagePreview
                            ?: state.existingImage
                            ?: ImageBitmap.imageResource(R.drawable.placeholder),
                        contentDescription = "Картинка категории",
                        modifier = Modifier
                            .height(64.dp)
                            .clickable { launcher.launch("image/*") }
                    )

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(
                            onClick = viewModel::save,
                            enabled = !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Сохранить")
                            }
                        }
                        Spacer(modifier = Modifier.padding(8.dp))
                        Button(
                            onClick = {
                                viewModel.dismiss()
                                onDismiss()
                            },
                            enabled = !state.isSaving
                        ) {
                            Text("Отмена")
                        }
                    }
                }
            }
        }
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
