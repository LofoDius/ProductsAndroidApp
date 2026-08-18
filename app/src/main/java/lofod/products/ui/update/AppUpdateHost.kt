package lofod.products.ui.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import lofod.products.R

/**
 * Dialog host for the in-app updater: checks for a newer release on first composition,
 * then drives the download → permission → install flow.
 */
@Composable
fun AppUpdateHost(
    state: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onStartUpdate: () -> Unit,
    onPostponeUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstallLaunched: () -> Unit,
    onInstallFailed: () -> Unit,
    onCancelInstall: () -> Unit,
    onConsumeError: () -> Unit,
) {
    val context = LocalContext.current
    // canRequestPackageInstalls() is not observable; bump this to re-read it after Settings.
    var installPermissionToken by remember { mutableIntStateOf(0) }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        installPermissionToken++
    }

    LaunchedEffect(Unit) {
        onCheckForUpdate()
    }

    val pendingInstallFile = state.pendingInstallFile
    val canInstallPackages = remember(pendingInstallFile, installPermissionToken) {
        ApkInstaller.canInstallPackages(context)
    }

    LaunchedEffect(pendingInstallFile, canInstallPackages) {
        if (pendingInstallFile == null || !canInstallPackages) {
            return@LaunchedEffect
        }
        if (ApkInstaller.installApk(context, pendingInstallFile)) {
            onInstallLaunched()
        } else {
            onInstallFailed()
        }
    }

    val error = state.error
    val availableRelease = state.availableRelease

    when {
        error != null -> {
            val messageRes = when (error) {
                AppUpdateError.DOWNLOAD_FAILED -> R.string.app_update_error_download
                AppUpdateError.INSTALL_FAILED -> R.string.app_update_error_install
            }
            AlertDialog(
                onDismissRequest = onConsumeError,
                title = { Text(stringResource(R.string.app_update_error_title)) },
                text = { Text(stringResource(messageRes)) },
                confirmButton = {
                    TextButton(onClick = onConsumeError) {
                        Text(stringResource(R.string.app_update_ok))
                    }
                },
            )
        }

        pendingInstallFile != null && !canInstallPackages -> {
            AlertDialog(
                onDismissRequest = onCancelInstall,
                title = { Text(stringResource(R.string.app_update_permission_title)) },
                text = { Text(stringResource(R.string.app_update_permission_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            settingsLauncher.launch(
                                ApkInstaller.unknownSourcesSettingsIntent(context),
                            )
                        },
                    ) {
                        Text(stringResource(R.string.app_update_permission_open_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelInstall) {
                        Text(stringResource(R.string.app_update_cancel))
                    }
                },
            )
        }

        state.isDownloading -> {
            val progress = state.downloadProgress
            AlertDialog(
                onDismissRequest = onCancelDownload,
                title = { Text(stringResource(R.string.app_update_downloading_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (progress == null) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                stringResource(
                                    R.string.app_update_downloading_progress,
                                    (progress * 100).toInt(),
                                ),
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onCancelDownload) {
                        Text(stringResource(R.string.app_update_cancel))
                    }
                },
            )
        }

        state.isPromptVisible && availableRelease != null -> {
            AlertDialog(
                onDismissRequest = onPostponeUpdate,
                title = { Text(stringResource(R.string.app_update_dialog_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.app_update_dialog_message,
                            availableRelease.versionName,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = onStartUpdate) {
                        Text(stringResource(R.string.app_update_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onPostponeUpdate) {
                        Text(stringResource(R.string.app_update_later))
                    }
                },
            )
        }
    }
}
