package lofod.products.di

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lofod.products.BuildConfig
import lofod.products.data.remote.AppUpdateApi
import lofod.products.data.remote.AuthApi
import lofod.products.data.remote.AuthInterceptor
import lofod.products.data.remote.CategoryApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    /**
     * Client for the public release endpoints: no [AuthInterceptor], so no token is sent
     * and a 401 elsewhere can never invalidate the session because of an update check.
     * Timeouts are relaxed since the APK download can take a while on a slow network.
     */
    @Provides
    @Singleton
    @AppUpdateNetwork
    fun provideAppUpdateOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BODY would dump APK bytes into logcat.
                    level = HttpLoggingInterceptor.Level.HEADERS
                },
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = createRetrofit(okHttpClient)

    @Provides
    @Singleton
    @AppUpdateNetwork
    fun provideAppUpdateRetrofit(@AppUpdateNetwork okHttpClient: OkHttpClient): Retrofit =
        createRetrofit(okHttpClient)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideCategoryApi(retrofit: Retrofit): CategoryApi = retrofit.create(CategoryApi::class.java)

    @Provides
    @Singleton
    fun provideAppUpdateApi(@AppUpdateNetwork retrofit: Retrofit): AppUpdateApi =
        retrofit.create(AppUpdateApi::class.java)

    private fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(ensureTrailingSlash(BuildConfig.API_URL))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create()
                )
            )
            .build()
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
