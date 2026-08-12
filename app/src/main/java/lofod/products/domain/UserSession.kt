package lofod.products.domain

data class UserSession(
    val userId: String,
    val username: String,
    val token: String
)
