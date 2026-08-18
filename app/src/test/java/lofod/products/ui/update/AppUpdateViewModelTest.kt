package lofod.products.ui.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import lofod.products.BuildConfig
import lofod.products.MainDispatcherRule
import lofod.products.data.remote.AppUpdateApi
import lofod.products.data.remote.response.AppReleaseDto
import lofod.products.data.repository.AppUpdatePreferences
import lofod.products.data.repository.AppUpdateRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun checkOffersAbsoluteLatestWhenInstalledIsSeveralVersionsBehind() {
        val latest = AppReleaseDto(
            versionCode = BuildConfig.VERSION_CODE + 5,
            versionName = "1.5.0",
            downloadPath = "/app/download",
        )
        val api = FakeAppUpdateApi(latest = latest)
        val viewModel = createViewModel(api)

        viewModel.checkForUpdate()

        val state = viewModel.uiState.value
        assertEquals(latest.versionCode, state.availableRelease?.versionCode)
        assertEquals("/app/download", state.availableRelease?.downloadPath)
        assertTrue(state.isPromptVisible)
        assertEquals(listOf("app/latest"), api.requestedPaths)
    }

    @Test
    fun checkDoesNotOfferUpdateWhenAlreadyOnLatest() {
        val latest = AppReleaseDto(
            versionCode = BuildConfig.VERSION_CODE,
            versionName = "1.0.0",
            downloadPath = "/app/download",
        )
        val viewModel = createViewModel(FakeAppUpdateApi(latest = latest))

        viewModel.checkForUpdate()

        val state = viewModel.uiState.value
        assertNull(state.availableRelease)
        assertFalse(state.isPromptVisible)
    }

    @Test
    fun startUpdateDownloadsOnlyLatestDownloadPath() {
        val latest = AppReleaseDto(
            versionCode = BuildConfig.VERSION_CODE + 3,
            versionName = "2.0.0",
            downloadPath = "/app/download",
        )
        val api = FakeAppUpdateApi(latest = latest, apkBytes = byteArrayOf(1, 2, 3, 4))
        val viewModel = createViewModel(api)

        viewModel.checkForUpdate()
        viewModel.startUpdate()

        assertEquals(listOf("app/download"), api.downloadedPaths)
        assertEquals(
            "products-${latest.versionCode}.apk",
            viewModel.uiState.value.pendingInstallFile?.name,
        )
        assertFalse(viewModel.uiState.value.isDownloading)
    }

    private fun createViewModel(api: FakeAppUpdateApi): AppUpdateViewModel {
        val repository = AppUpdateRepository(
            appUpdateApi = api,
            cacheDir = temporaryFolder.root,
            preferences = FakeAppUpdatePreferences(),
            ioDispatcher = testDispatcher,
        )
        return AppUpdateViewModel(repository)
    }

    private class FakeAppUpdatePreferences : AppUpdatePreferences {
        private val dismissed = MutableStateFlow<Int?>(null)
        override val dismissedUpdateVersionCode = dismissed
        override suspend fun setDismissedUpdateVersionCode(versionCode: Int) {
            dismissed.value = versionCode
        }
    }

    private class FakeAppUpdateApi(
        private val latest: AppReleaseDto?,
        private val apkBytes: ByteArray = byteArrayOf(1),
    ) : AppUpdateApi {
        val requestedPaths = mutableListOf<String>()
        val downloadedPaths = mutableListOf<String>()

        override suspend fun getLatestRelease(): AppReleaseDto {
            requestedPaths += "app/latest"
            return latest ?: error("no release")
        }

        override suspend fun downloadApk(path: String): ResponseBody {
            downloadedPaths += path
            return apkBytes.toResponseBody("application/vnd.android.package-archive".toMediaType())
        }
    }
}
