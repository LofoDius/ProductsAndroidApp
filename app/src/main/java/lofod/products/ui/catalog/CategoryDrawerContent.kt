package lofod.products.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lofod.products.data.remote.model.CategoryRole
import lofod.products.data.remote.response.CategoryResponse
import lofod.products.ui.common.CategoryIcon

@Composable
fun CategoryDrawerContent(
    root: CategoryResponse,
    currentCategory: CategoryResponse,
    loadCategoryImage: suspend (String) -> ImageBitmap?,
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
            .padding(horizontal = 12.dp)
            .verticalScroll(scroll)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        val isEmptyCatalog =
            currentCategory.isSyntheticRoot() && root.subcategories.isEmpty()

        if (isEmptyCatalog) {
            Text(
                text = "Нет категорий",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            ListItem(
                headlineContent = {
                    Text(
                        text = currentCategory.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                supportingContent = {
                    Text(
                        text = if (currentCategory.isSyntheticRoot()) {
                            "Выберите категорию"
                        } else {
                            roleLabel(currentCategory.role)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingContent = if (!currentCategory.isSyntheticRoot()) {
                    {
                        CategoryIcon(
                            imageId = currentCategory.imageId,
                            loadImage = loadCategoryImage,
                            size = 40.dp,
                            contentDescription = currentCategory.name
                        )
                    }
                } else {
                    null
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            if (!currentCategory.isSyntheticRoot()) {
                NavigationDrawerItem(
                    label = { Text("Назад") },
                    selected = false,
                    onClick = onBack,
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                children.forEach { child ->
                    ListItem(
                        headlineContent = { Text(child.name) },
                        supportingContent = {
                            Text(
                                text = "${child.subcategoriesAmount} папок · ${child.cardsAmount} оценок · ${roleLabel(child.role)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            CategoryIcon(
                                imageId = child.imageId,
                                loadImage = loadCategoryImage,
                                size = 40.dp,
                                contentDescription = child.name
                            )
                        },
                        trailingContent = if (child.canManageCategory()) {
                            {
                                Row {
                                    IconButton(onClick = { onEditCategory(child) }) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = "Редактировать"
                                        )
                                    }
                                    IconButton(onClick = { onDeleteCategory(child) }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Удалить"
                                        )
                                    }
                                }
                            }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(child) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (currentCategory.canCreateSubcategory()) {
            NavigationDrawerItem(
                label = {
                    Text(
                        if (currentCategory.isSyntheticRoot()) {
                            "Создать категорию"
                        } else {
                            "Создать подкатегорию"
                        }
                    )
                },
                selected = false,
                onClick = onCreateCategory,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        if (currentCategory.canManageCategory()) {
            NavigationDrawerItem(
                label = { Text("Редактировать категорию") },
                selected = false,
                onClick = { onEditCategory(currentCategory) },
                icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("Удалить категорию") },
                selected = false,
                onClick = { onDeleteCategory(currentCategory) },
                icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        if (currentCategory.canManageMembers()) {
            NavigationDrawerItem(
                label = { Text("Участники") },
                selected = false,
                onClick = onOpenMembers,
                icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        NavigationDrawerItem(
            label = { Text(if (isLoggingOut) "Выход…" else "Выйти") },
            selected = false,
            onClick = { if (!isLoggingOut) onLogout() },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun roleLabel(role: CategoryRole): String = when (role) {
    CategoryRole.OWNER -> "Владелец"
    CategoryRole.MEMBER -> "Участник"
}
