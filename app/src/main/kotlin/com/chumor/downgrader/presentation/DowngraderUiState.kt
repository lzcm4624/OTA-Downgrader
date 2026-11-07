package com.chumor.downgrader.presentation

import androidx.compose.runtime.Immutable
import com.chumor.downgrader.domain.model.DeviceInfo
import com.chumor.downgrader.domain.model.DeviceReadError
import com.chumor.downgrader.domain.model.FirmwareDownloadState
import com.chumor.downgrader.domain.model.FirmwarePackage
import com.chumor.downgrader.domain.model.OplusDeviceInfo
import com.chumor.downgrader.domain.model.OplusFirmware
import com.chumor.downgrader.domain.model.OplusQueryError
import com.chumor.downgrader.domain.model.OplusSource
import com.chumor.downgrader.domain.model.QueryError
import com.chumor.downgrader.domain.model.isSha256Digest

/** 界面上并列的两条通道：vivo 官方降级，与欧加真（OPPO / 一加 / 真我）。 */
enum class FirmwareChannel(val label: String) {
    VIVO("vivo"),
    OPLUS("欧加真"),
}

@Immutable
data class DowngraderUiState(
    val channel: FirmwareChannel = FirmwareChannel.VIVO,
    val oplusDevice: OplusDeviceInfo? = null,
    val oplusModelInput: String = "",
    val isQueryingOplus: Boolean = false,
    val oplusStage: String = "",
    val oplusPackages: List<OplusFirmware> = emptyList(),
    val oplusProjectNumber: String? = null,
    val oplusQueryError: OplusQueryError? = null,
    val oplusQueried: Boolean = false,
    val isLoadingDevice: Boolean = false,
    val device: DeviceInfo? = null,
    val deviceError: DeviceReadError? = null,
    val credentialAvailable: Boolean = false,
    val isQuerying: Boolean = false,
    val isSavingCertificate: Boolean = false,
    val savedCertificate: String? = null,
    val firmwareDownload: FirmwareDownloadState = FirmwareDownloadState(),
    val firmware: FirmwarePackage? = null,
    val queryError: QueryError? = null,
) {
    val isDownloadingFirmware: Boolean
        get() = firmwareDownload.status.isActive

    val canQueryPackage: Boolean
        get() = credentialAvailable && !isLoadingDevice && !isQuerying && !isDownloadingFirmware

    val canSaveCertificate: Boolean
        get() = firmware?.certificate?.isNotEmpty() == true && !isSavingCertificate && !isCertificateSaved

    val isCertificateSaved: Boolean
        get() = firmware?.certificate?.isNotEmpty() == true && firmware.certificate == savedCertificate

    val canQueryOplus: Boolean
        get() = oplusModelInput.isNotBlank() && !isQueryingOplus

    val oplusOfficialPackages: List<OplusFirmware>
        get() = oplusPackages.filter { it.source == OplusSource.OFFICIAL }

    val oplusAfterSalePackages: List<OplusFirmware>
        get() = oplusPackages.filter { it.source == OplusSource.AFTER_SALE }

    val canDownloadFirmware: Boolean
        get() = !isDownloadingFirmware && device?.model?.isNotBlank() == true &&
            firmware?.run {
                downloadUrl.isNotBlank() && systemVersion.isNotBlank() &&
                    fileSizeBytes > 0L && sha256.isSha256Digest()
            } == true
}

sealed interface DowngraderUiEffect {
    data class CertificateSaved(val location: String) : DowngraderUiEffect
    data object CertificateSaveFailed : DowngraderUiEffect
    data object FirmwareDownloadCompleted : DowngraderUiEffect
    data object FirmwareDownloadFailed : DowngraderUiEffect
}
