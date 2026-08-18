package lofod.products.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import lofod.products.data.remote.AppUpdateApi
import lofod.products.data.remote.response.AppReleaseDto
import retrofit2.HttpException
import java.io.File
import java.io.IOException

class AppUpdateRepository(
    private val appUpdateApi: AppUpdateApi,
    private val cacheDir: File,
    private val preferences: AppUpdatePreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Latest published release, or `null` when the server has none (404) or is unreachable. */
    suspend fun fetchLatestRelease(): AppReleaseDto? = withContext(ioDispatcher) {
        try {
            appUpdateApi.getLatestRelease()
        } catch (_: HttpException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun dismissedVersionCode(): Int? = preferences.dismissedUpdateVersionCode.first()

    suspend fun dismissVersion(versionCode: Int) {
        preferences.setDismissedUpdateVersionCode(versionCode)
    }

    /**
     * Streams the APK into the cache dir, replacing any previously downloaded file.
     * [onProgress] receives 0f..1f, or `null` while the total size is unknown.
     */
    suspend fun downloadApk(
        release: AppReleaseDto,
        onProgress: (Float?) -> Unit,
    ): File = withContext(ioDispatcher) {
        val dir = apkDir()
        deleteDownloadedApks(dir)
        val target = File(dir, "products-${release.versionCode}.apk")
        val body = appUpdateApi.downloadApk(relativeDownloadPath(release.downloadPath))
        try {
            body.use { responseBody ->
                val totalBytes = responseBody.contentLength()
                responseBody.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                        var copied = 0L
                        // Reported per whole percent only, to avoid a recomposition per chunk.
                        var reportedPercent = -1
                        onProgress(if (totalBytes > 0L) 0f else null)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) {
                                break
                            }
                            output.write(buffer, 0, read)
                            copied += read
                            if (totalBytes > 0L) {
                                val percent = (copied * 100 / totalBytes).toInt().coerceIn(0, 100)
                                if (percent != reportedPercent) {
                                    reportedPercent = percent
                                    onProgress(percent / 100f)
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
        if (target.length() == 0L) {
            target.delete()
            throw IOException("Empty APK response")
        }
        target
    }

    /** Removes leftovers from previous runs so the cache never keeps stale APKs. */
    suspend fun clearDownloads() {
        withContext(ioDispatcher) {
            deleteDownloadedApks(apkDir())
        }
    }

    private fun apkDir(): File = File(cacheDir, APK_DIR_NAME).also { it.mkdirs() }

    private fun deleteDownloadedApks(dir: File) {
        dir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        /** Must match the `cache-path` entry in `res/xml/file_paths.xml`. */
        const val APK_DIR_NAME = "app_updates"

        private const val DEFAULT_DOWNLOAD_PATH = "app/download"
        private const val DOWNLOAD_BUFFER_BYTES = 16 * 1024

        /**
         * Retrofit resolves a leading-slash path against the host root, which would drop any
         * path prefix of the base url, so the server path is made relative.
         */
        internal fun relativeDownloadPath(downloadPath: String): String {
            val trimmed = downloadPath.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed
            }
            return trimmed.removePrefix("/").ifEmpty { DEFAULT_DOWNLOAD_PATH }
        }
    }
}
