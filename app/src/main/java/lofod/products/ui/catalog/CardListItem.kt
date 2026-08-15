package lofod.products.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import lofod.products.data.remote.response.CardResponse
import lofod.products.ui.common.RatingBar

@Composable
fun CardListItem(
    card: CardResponse,
    expanded: Boolean,
    canEdit: Boolean,
    loadImage: suspend (String) -> ImageBitmap?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var imageBitmap by remember(card.imageId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(card.imageId) {
        val imageId = card.imageId ?: return@LaunchedEffect
        imageBitmap = loadImage(imageId)
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onToggle)
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(text = card.name, style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Column {
                        Text(
                            text = "${card.priceLevel.text()} · ${card.qualityLevel.text()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RatingBar(
                            rating = card.rating,
                            starSize = 18.dp,
                            interactive = false
                        )
                    }
                },
                leadingContent = imageBitmap?.let { loadedImage ->
                    {
                        Image(
                            bitmap = loadedImage,
                            contentDescription = card.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                },
                trailingContent = if (expanded && canEdit) {
                    {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Редактировать")
                        }
                    }
                } else {
                    null
                }
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 12.dp)) {
                    if (!card.description.isNullOrBlank()) {
                        Text(
                            text = card.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (canEdit) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }
}
