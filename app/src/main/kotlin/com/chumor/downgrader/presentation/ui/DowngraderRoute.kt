package com.chumor.downgrader.presentation.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.chumor.downgrader.R
import com.chumor.downgrader.presentation.DowngraderUiEffect
import com.chumor.downgrader.presentation.DowngraderViewModel
import kotlinx.coroutines.flow.collect

@Composable
internal fun DowngraderRoute(viewModel: DowngraderViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val systemUpdaterIntent = remember {
        Intent("com.bbk.updater.action.START_UPDATERACTIVITY").setPackage("com.bbk.updater")
    }
    val canOpenSystemUpdater = remember(context) {
        systemUpdaterIntent.resolveActivity(context.packageManager) != null
    }
    var pendingDownloadAccessAction by rememberSaveable {
        mutableStateOf<PendingDownloadAccessAction?>(null)
    }
    val runPendingAccessAction: (PendingDownloadAccessAction) -> Unit = { action ->
        when (action) {
            PendingDownloadAccessAction.SaveCertificate -> viewModel.saveCertificate()
            PendingDownloadAccessAction.DownloadFirmware -> viewModel.downloadFirmware()
        }
    }
    val notificationPermissionNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    val notificationAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        val action = pendingDownloadAccessAction
        pendingDownloadAccessAction = null
        action?.let(runPendingAccessAction)
    }
    val requestNotificationAccess: (PendingDownloadAccessAction) -> Unit = { action ->
        if (action == PendingDownloadAccessAction.DownloadFirmware && notificationPermissionNeeded) {
            pendingDownloadAccessAction = action
            notificationAccessLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            runPendingAccessAction(action)
        }
    }
    val downloadAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val action = pendingDownloadAccessAction
        pendingDownloadAccessAction = null
        if (granted) {
            action?.let(requestNotificationAccess)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.download_storage_permission_denied),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        val action = pendingDownloadAccessAction
        pendingDownloadAccessAction = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            action?.let(requestNotificationAccess)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.download_storage_permission_denied),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val withDownloadAccess: (PendingDownloadAccessAction) -> Unit = { action ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                requestNotificationAccess(action)
            } else {
                pendingDownloadAccessAction = action
                allFilesAccessLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
        } else if (
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationAccess(action)
        } else {
            pendingDownloadAccessAction = action
            downloadAccessLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                val message = when (effect) {
                    is DowngraderUiEffect.CertificateSaved ->
                        context.getString(R.string.certificate_saved, effect.location)

                    DowngraderUiEffect.CertificateSaveFailed ->
                        context.getString(R.string.certificate_save_failed)

                    DowngraderUiEffect.FirmwareDownloadCompleted ->
                        context.getString(R.string.firmware_download_completed)

                    DowngraderUiEffect.FirmwareDownloadFailed ->
                        context.getString(R.string.firmware_download_failed)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    DowngraderScreen(
        modifier = modifier,
        state = state,
        onSelectChannel = viewModel::selectChannel,
        onOplusModelChange = viewModel::updateOplusModel,
        onQueryOplus = viewModel::queryOplus,
        onCopyOplusUrl = { url ->
            copyText(
                context,
                url,
                context.getString(R.string.firmware_download_url),
                context.getString(R.string.firmware_url_copied),
            )
        },
        onOpenOplusUrl = { url ->
            if (url.isNotBlank()) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                    Toast.makeText(context, "没有可用的浏览器", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onQueryPackage = viewModel::queryPackage,
        onSaveCertificate = {
            withDownloadAccess(PendingDownloadAccessAction.SaveCertificate)
        },
        onDownloadFirmware = {
            withDownloadAccess(PendingDownloadAccessAction.DownloadFirmware)
        },
        onCancelFirmwareDownload = viewModel::cancelFirmwareDownload,
        onCopyFirmwareUrl = { url ->
            copyText(
                context,
                url,
                context.getString(R.string.firmware_download_url),
                context.getString(R.string.firmware_url_copied),
            )
        },
        onCopyCertificate = { certificate ->
            copyText(
                context,
                certificate,
                context.getString(R.string.certificate_section_title),
                context.getString(R.string.certificate_copied),
            )
        },
        onOpenSystemUpdater = if (canOpenSystemUpdater) {
            { context.startActivity(systemUpdaterIntent) }
        } else {
            null
        },
    )
}

private enum class PendingDownloadAccessAction {
    SaveCertificate,
    DownloadFirmware,
}

private fun copyText(context: Context, text: String, label: String, copiedMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboard == null) {
        Toast.makeText(context, context.getString(R.string.clipboard_unavailable), Toast.LENGTH_SHORT).show()
        return
    }
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
}
