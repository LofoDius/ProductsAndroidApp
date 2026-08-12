package lofod.products.data.remote.response

import lofod.products.data.remote.model.PriceLevel
import lofod.products.data.remote.model.QualityLevel

data class CardResponse(
    val cardId: String,
    val categoryId: String,
    val name: String,
    val imageId: String? = null,
    val priceLevel: PriceLevel,
    val qualityLevel: QualityLevel,
    val description: String?,
)
