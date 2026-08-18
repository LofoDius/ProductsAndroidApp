package lofod.products.data.remote

import lofod.products.data.remote.response.AppReleaseDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Public (unauthenticated) release endpoints, so an update stays reachable
 * even before login or after the token expired.
 */
interface AppUpdateApi {
    @GET("app/latest")
    suspend fun getLatestRelease(): AppReleaseDto

    /** [path] is relative to the API base url; the APK is streamed, never buffered whole. */
    @Streaming
    @GET
    suspend fun downloadApk(@Url path: String): ResponseBody
}
