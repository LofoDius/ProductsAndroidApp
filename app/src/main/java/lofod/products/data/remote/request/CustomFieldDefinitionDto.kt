package lofod.products.data.remote.request

import lofod.products.data.remote.model.CustomFieldType

data class CustomFieldDefinitionDto(
    val fieldId: String? = null,
    val title: String,
    val type: CustomFieldType,
)
