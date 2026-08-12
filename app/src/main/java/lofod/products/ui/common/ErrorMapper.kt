package lofod.products.ui.common

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

object ErrorMapper {
    fun toMessage(throwable: Throwable): String = when (throwable) {
        is HttpException -> httpMessage(throwable)
        is IOException -> "Нет соединения с сервером"
        else -> throwable.message ?: "Неизвестная ошибка"
    }

    private fun httpMessage(exception: HttpException): String {
        val bodyText = try {
            exception.response()?.errorBody()?.string()?.trim().orEmpty()
        } catch (_: Exception) {
            ""
        }
        parseApiMessage(bodyText)?.let { return it }
        if (bodyText.isNotEmpty()) return bodyText
        return when (exception.code()) {
            401 -> "Требуется авторизация"
            403 -> "Недостаточно прав"
            404 -> "Не найдено"
            409 -> "Конфликт данных"
            else -> "Ошибка сервера: ${exception.code()}"
        }
    }

    private fun parseApiMessage(bodyText: String): String? {
        if (bodyText.isEmpty() || !bodyText.startsWith("{")) return null
        return try {
            JSONObject(bodyText).optString("message").trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}

fun findCategoryById(
    categoryId: String,
    categories: List<lofod.products.data.remote.response.CategoryResponse>
): lofod.products.data.remote.response.CategoryResponse? {
    categories.forEach {
        if (it.categoryId == categoryId) {
            return it
        } else if (it.subcategories.isNotEmpty()) {
            val foundCategory = findCategoryById(categoryId, it.subcategories)
            if (foundCategory != null) {
                return foundCategory
            }
        }
    }
    return null
}
