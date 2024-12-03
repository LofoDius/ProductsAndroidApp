package lofod.products.api.response

import lofod.products.api.model.PriceLevel
import lofod.products.api.model.QualityLevel

data class CardResponse(
    val cardId: String,
    val categoryId: String,
    val name: String,
    val imageId: String? = null,
    val priceLevel: PriceLevel,
    val qualityLevel: QualityLevel,
    val description: String?,
)
