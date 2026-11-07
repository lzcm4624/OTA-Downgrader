package com.chumor.downgrader.presentation

import androidx.compose.runtime.Immutable
import com.chumor.downgrader.domain.model.DeviceBrandDetection
import com.chumor.downgrader.domain.model.DeviceInfo
import com.chumor.downgrader.domain.model.DeviceReadError
import com.chumor.downgrader.domain.model.FirmwareDownloadState
import com.chumor.downgrader.domain.model.FirmwarePackage
import com.chumor.downgrader.domain.model.OplusDeviceInfo
import com.chumor.downgrader.domain.model.OplusFirmware
import com.chumor.downgrader.domain.model.OplusQueryError
import com.chumor.downgrader.domain.model.OplusSource
import com.chumor.downgrader.domain.model.QueryError
import com.chumor.downgrader.domain.model.DetectedVendor
import com.chumor.downgrader.domain.model.isSha256Digest

/** 界面上并列的两条通道：vivo 官方降级，与欧加真（OPPO / 一加 / 真我）。 */
enum class FirmwareChannel(val label: String) {
    VIVO("vivo"),
    OPLUS("欧加真"),
}

@Immutable
data class DowngraderUiState(
    val channel: FirmwareChannel = FirmwareChannel.VIVO,
    /** 自动识别结果；界面上的通道默认由它决定。 */
    val detection: DeviceBrandDetection = DeviceBrandDetection.EMPTY,
    val isDetectingBrand: Boolean = true,
    /** 用户手动指定过通道，之后不再被自动识别覆盖。 */
    val channelManual: Boolean = false,
    /** 每次发起查询 +1，界面拿它当入场动画的重播信号。 */
    val motionEpoch: Int = 0,
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
    /** 自动识别推出来的通道；识别不到时按 vivo 走。 */
    val autoChannel: FirmwareChannel
        get() = when (detection.vendor) {
            DetectedVendor.OPLUS -> FirmwareChannel.OPLUS
            else -> FirmwareChannel.VIVO
        }

    val isChannelAuto: Boolean
        get() = !channelManual

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
