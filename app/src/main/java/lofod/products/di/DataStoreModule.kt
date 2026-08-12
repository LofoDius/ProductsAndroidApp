package lofod.products.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val SESSION_PREFERENCES_NAME = "session_prefs"

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SESSION_PREFERENCES_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSessionPreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.sessionDataStore
}
