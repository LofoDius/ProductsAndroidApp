package lofod.products.data.repository

import kotlinx.coroutines.flow.Flow

interface AppUpdatePreferences {
    /** Version code the user postponed, or `null` if nothing was postponed. */
    val dismissedUpdateVersionCode: Flow<Int?>

    suspend fun setDismissedUpdateVersionCode(versionCode: Int)
}
