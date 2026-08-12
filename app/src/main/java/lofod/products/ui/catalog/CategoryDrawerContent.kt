package lofod.products.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lofod.products.data.remote.model.CategoryRole
import lofod.products.data.remote.response.CategoryResponse

@Composable
fun CategoryDrawerContent(
    root: CategoryResponse,
    currentCategory: CategoryResponse,
    onSelect: (CategoryResponse) -> Unit,
    onBack: () -> Unit,
    onCreateCategory: () -> Unit,
    onEditCategory: (CategoryResponse) -> Unit,
    onDeleteCategory: (CategoryResponse) -> Unit,
    onOpenMembers: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scroll)
    ) {
        Text(
            text = currentCategory.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (currentCategory.isSyntheticRoot()) {
                "Выберите категорию"
            } else {
                roleLabel(currentCategory.role)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (!currentCategory.isSyntheticRoot()) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Назад")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        val children = if (currentCategory.isSyntheticRoot()) {
            root.subcategories
        } else {
            currentCategory.subcategories
        }

        if (children.isEmpty()) {
            Text(
                text = "Нет подкатегорий",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            children.forEach { child ->
                CategoryRow(
                    category = child,
                    onClick = { onSelect(child) },
                    showOwnerActions = child.canManageCategory(),
                    onEdit = { onEditCategory(child) },
                    onDelete = { onDeleteCategory(child) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (currentCategory.canCreateSubcategory()) {
            TextButton(onClick = onCreateCategory) {
                Text(
                    if (currentCategory.isSyntheticRoot()) {
                        "Создать категорию"
                    } else {
                        "Создать подкатегорию"
                    }
                )
            }
        }

        if (currentCategory.canManageCategory()) {
            TextButton(onClick = { onEditCategory(currentCategory) }) {
                Text("Редактировать категорию")
            }
            TextButton(onClick = { onDeleteCategory(currentCategory) }) {
                Text("Удалить категорию")
            }
        }

        if (currentCategory.canManageMembers()) {
            TextButton(onClick = onOpenMembers) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Участники")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onLogout, enabled = !isLoggingOut) {
            Text(if (isLoggingOut) "Выход…" else "Выйти")
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryResponse,
    onClick: () -> Unit,
    showOwnerActions: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${category.subcategoriesAmount} папок · ${category.cardsAmount} оценок · ${roleLabel(category.role)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showOwnerActions) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}

private fun roleLabel(role: CategoryRole): String = when (role) {
    CategoryRole.OWNER -> "Владелец"
    CategoryRole.MEMBER -> "Участник"
}
