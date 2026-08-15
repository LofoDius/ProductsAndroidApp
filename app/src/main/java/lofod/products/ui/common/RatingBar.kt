package lofod.products.ui.common

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 5-star rating control mapped to API `rating` 0..10 (half star = 1).
 */
@Composable
fun RatingBar(
    rating: Int,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onRatingChange: ((Int) -> Unit)? = null,
    starSize: Dp = 28.dp,
    filledColor: Color = Color(0xFFFFC107),
    emptyColor: Color = MaterialTheme.colorScheme.outline,
) {
    val coerced = rating.coerceIn(0, 10)
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    fun ratingAt(x: Float): Int {
        if (barWidthPx <= 0f) return coerced
        val fraction = (x / barWidthPx).coerceIn(0f, 1f)
        return (fraction * 10f).roundToInt().coerceIn(0, 10)
    }

    val interactionModifier = if (interactive && onRatingChange != null) {
        Modifier
            .pointerInput(barWidthPx) {
                detectTapGestures { offset ->
                    onRatingChange(ratingAt(offset.x))
                }
            }
            .pointerInput(barWidthPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onRatingChange(ratingAt(offset.x)) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onRatingChange(ratingAt(change.position.x))
                    }
                )
            }
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .onSizeChanged { barWidthPx = it.width.toFloat() }
            .semantics {
                contentDescription = "Рейтинг $coerced из 10"
            }
            .then(interactionModifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { index ->
            val filledThrough = (index + 1) * 2
            val emptyBelow = index * 2
            val fill = when {
                coerced >= filledThrough -> 1f
                coerced > emptyBelow -> 0.5f
                else -> 0f
            }
            HalfStarIcon(
                fill = fill,
                size = starSize,
                filledColor = filledColor,
                emptyColor = emptyColor
            )
        }
    }
}

@Composable
private fun HalfStarIcon(
    fill: Float,
    size: Dp,
    filledColor: Color,
    emptyColor: Color,
) {
    Box(modifier = Modifier.size(size)) {
        Icon(
            imageVector = Icons.Filled.StarBorder,
            contentDescription = null,
            tint = emptyColor,
            modifier = Modifier.fillMaxSize()
        )
        if (fill > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(HorizontalFractionShape(fill.coerceIn(0f, 1f)))
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = filledColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private class HorizontalFractionShape(
    private val fraction: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Rectangle(
        Rect(0f, 0f, size.width * fraction, size.height)
    )
}
