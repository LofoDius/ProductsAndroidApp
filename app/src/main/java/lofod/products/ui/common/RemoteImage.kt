package lofod.products.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import lofod.products.data.repository.CategoryRepository

suspend fun loadCategoryImageBitmap(
    repository: CategoryRepository,
    imageId: String
): ImageBitmap? = decodeBase64Image {
    repository.getCategoryImage(imageId).image
}

suspend fun loadCardImageBitmap(
    repository: CategoryRepository,
    imageId: String
): ImageBitmap? = decodeBase64Image {
    repository.getCardImage(imageId).image
}

private suspend fun decodeBase64Image(loadBase64: suspend () -> String): ImageBitmap? {
    return try {
        val bytes = Base64.decode(loadBase64(), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun CategoryIcon(
    imageId: String?,
    loadImage: suspend (String) -> ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = null,
    placeholder: ImageVector = Icons.Outlined.Folder
) {
    var bitmap by remember(imageId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageId) {
        val id = imageId ?: run {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = loadImage(id)
    }

    val shape = CircleShape
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = placeholder,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
