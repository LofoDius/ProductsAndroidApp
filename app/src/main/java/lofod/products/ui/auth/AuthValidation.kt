package lofod.products.ui.auth

/**
 * Client-side auth form rules.
 * Min password length is 6 (product choice for T06; server may enforce its own rules later).
 */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 6

    fun validateUsername(username: String): String? {
        if (username.isBlank()) return "Введите имя пользователя"
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.isBlank()) return "Введите пароль"
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "Пароль должен быть не короче $MIN_PASSWORD_LENGTH символов"
        }
        return null
    }
}
