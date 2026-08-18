package lofod.products.data.remote.response

data class AppReleaseDto(
    val versionCode: Int,
    val versionName: String = "",
    val releasedAt: String = "",
    val downloadPath: String = "",
)
