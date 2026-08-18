package lofod.products.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import lofod.products.data.local.AppUpdateDataStore
import lofod.products.data.remote.AppUpdateApi
import lofod.products.data.repository.AppUpdatePreferences
import lofod.products.data.repository.AppUpdateRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppUpdateModule {

    @Provides
    @Singleton
    fun provideAppUpdatePreferences(
        @ApplicationContext context: Context,
    ): AppUpdatePreferences = AppUpdateDataStore(context)

    @Provides
    @Singleton
    fun provideAppUpdateRepository(
        appUpdateApi: AppUpdateApi,
        @ApplicationContext context: Context,
        preferences: AppUpdatePreferences,
    ): AppUpdateRepository = AppUpdateRepository(
        appUpdateApi = appUpdateApi,
        cacheDir = context.cacheDir,
        preferences = preferences,
    )
}
