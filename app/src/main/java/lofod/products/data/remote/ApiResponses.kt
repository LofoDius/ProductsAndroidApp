package lofod.products.data.remote

import retrofit2.HttpException
import retrofit2.Response

fun <T> Response<T>.bodyOrThrow(): T {
    if (isSuccessful) {
        return body() ?: throw IllegalStateException("Empty response body")
    }
    throw HttpException(this)
}

fun Response<*>.ensureSuccess() {
    if (!isSuccessful) {
        throw HttpException(this)
    }
}
