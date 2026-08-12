package lofod.products.data.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory session token cache for synchronous OkHttp interceptors.
 * Kept in sync with [SessionDataStore] by [lofod.products.data.repository.AuthRepository]
 * and [lofod.products.ProductsApp] on startup.
 */
@Singleton
class SessionTokenHolder @Inject constructor() {
    @Volatile
    var token: String? = null
}
