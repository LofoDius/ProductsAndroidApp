package lofod.products.api.request

import lofod.products.api.model.PriceLevel
import lofod.products.api.model.QualityLevel

data class CreateCardRequest(
    val name: String,
    val imageId: String?,
    val priceLevel: PriceLevel,
    val qualityLevel: QualityLevel,
    val description: String?
)
