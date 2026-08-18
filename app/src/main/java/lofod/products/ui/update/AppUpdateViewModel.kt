package lofod.products.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lofod.products.BuildConfig
import lofod.products.data.remote.response.AppReleaseDto
import lofod.products.data.repository.AppUpdateRepository
import java.io.File
import javax.inject.Inject

enum class AppUpdateError {
    DOWNLOAD_FAILED,
    INSTALL_FAILED,
}

data class AppUpdateUiState(
    /** Release newer than the installed build, `null` while unknown or already up to date. */
    val availableRelease: AppReleaseDto? = null,
    val isPromptVisible: Boolean = false,
    val isDownloading: Boolean = false,
    /** 0f..1f, or `null` when the download size is unknown. */
    val downloadProgress: Float? = null,
    val pendingInstallFile: File? = null,
    val error: AppUpdateError? = null,
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
) : ViewModel() {

    private val installedVersionCode: Int = BuildConfig.VERSION_CODE

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private var hasChecked = false
    private var downloadJob: Job? = null

    /** Checks once per process; safe to call from a composable on every recomposition. */
    fun checkForUpdate() {
        if (hasChecked) {
            return
        }
        hasChecked = true
        viewModelScope.launch {
            repository.clearDownloads()
            val release = repository.fetchLatestRelease() ?: return@launch
            if (release.versionCode <= installedVersionCode) {
                return@launch
            }
            val dismissedVersionCode = repository.dismissedVersionCode()
            _uiState.update { state ->
                state.copy(
                    availableRelease = release,
                    isPromptVisible = dismissedVersionCode != release.versionCode,
                )
            }
        }
    }

    fun postponeUpdate() {
        val versionCode = _uiState.value.availableRelease?.versionCode
        _uiState.update { it.copy(isPromptVisible = false) }
        if (versionCode != null) {
            viewModelScope.launch {
                repository.dismissVersion(versionCode)
            }
        }
    }

    fun startUpdate() {
        val release = _uiState.value.availableRelease ?: return
        if (downloadJob?.isActive == true) {
            return
        }
        _uiState.update {
            it.copy(
                isPromptVisible = false,
                isDownloading = true,
                downloadProgress = null,
                pendingInstallFile = null,
                error = null,
            )
        }
        downloadJob = viewModelScope.launch {
            try {
                val apkFile = repository.downloadApk(release) { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }
                }
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        pendingInstallFile = apkFile,
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isDownloading = false, downloadProgress = null) }
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        error = AppUpdateError.DOWNLOAD_FAILED,
                    )
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(isDownloading = false, downloadProgress = null) }
    }

    /** The system installer took over; nothing left for the app to show. */
    fun onInstallLaunched() {
        _uiState.update { it.copy(pendingInstallFile = null) }
    }

    fun onInstallFailed() {
        _uiState.update {
            it.copy(pendingInstallFile = null, error = AppUpdateError.INSTALL_FAILED)
        }
    }

    fun cancelInstall() {
        _uiState.update { it.copy(pendingInstallFile = null) }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }
}
