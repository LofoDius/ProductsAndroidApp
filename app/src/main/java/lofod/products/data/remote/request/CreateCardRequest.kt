package lofod.products.data.remote.request

import lofod.products.data.remote.model.PriceLevel
import lofod.products.data.remote.model.QualityLevel

data class CreateCardRequest(
    val name: String,
    val imageId: String?,
    val priceLevel: PriceLevel,
    val qualityLevel: QualityLevel,
    val description: String?
)
