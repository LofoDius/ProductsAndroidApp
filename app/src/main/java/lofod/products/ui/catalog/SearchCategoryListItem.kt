package lofod.products.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import lofod.products.data.remote.response.CategorySearchHit
import lofod.products.ui.common.CategoryIcon

@Composable
fun SearchCategoryListItem(
    hit: CategorySearchHit,
    loadCategoryImage: suspend (String) -> ImageBitmap?,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        ListItem(
            headlineContent = {
                Text(text = hit.name, style = MaterialTheme.typography.titleMedium)
            },
            leadingContent = {
                CategoryIcon(
                    imageId = hit.imageId,
                    loadImage = loadCategoryImage,
                    size = 40.dp,
                    contentDescription = hit.name
                )
            }
        )
    }
}
