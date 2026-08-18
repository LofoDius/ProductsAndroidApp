package lofod.products.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lofod.products.data.repository.AppUpdatePreferences

private val Context.appUpdateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_update",
)

class AppUpdateDataStore(
    context: Context,
) : AppUpdatePreferences {
    private val dataStore = context.applicationContext.appUpdateDataStore

    override val dismissedUpdateVersionCode: Flow<Int?> = dataStore.data.map { prefs ->
        prefs[Keys.DISMISSED_UPDATE_VERSION_CODE]
    }

    override suspend fun setDismissedUpdateVersionCode(versionCode: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.DISMISSED_UPDATE_VERSION_CODE] = versionCode
        }
    }

    private object Keys {
        val DISMISSED_UPDATE_VERSION_CODE = intPreferencesKey("dismissed_update_version_code")
    }
}
