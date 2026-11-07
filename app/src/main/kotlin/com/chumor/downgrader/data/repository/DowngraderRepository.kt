package com.chumor.downgrader.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.chumor.downgrader.data.device.DeviceBrandDetector
import com.chumor.downgrader.data.device.DevicePropertiesReader
import com.chumor.downgrader.data.device.OplusDeviceReader
import com.chumor.downgrader.data.remote.ColorosDowngradeApi
import com.chumor.downgrader.data.remote.OplusOtaApi
import com.chumor.downgrader.data.remote.VivoUpgradeApi
import com.chumor.downgrader.data.storage.CertificateSaver
import com.chumor.downgrader.data.storage.FIRMWARE_DOWNLOAD_EXPECTED_SIZE_BYTES
import com.chumor.downgrader.data.storage.FIRMWARE_DOWNLOAD_PROGRESS_BYTES
import com.chumor.downgrader.data.storage.FIRMWARE_DOWNLOAD_SHA256_TAG_PREFIX
import com.chumor.downgrader.data.storage.FIRMWARE_DOWNLOAD_VERIFYING
import com.chumor.downgrader.data.storage.FIRMWARE_DOWNLOAD_WORK_NAME
import com.chumor.downgrader.data.storage.FirmwareDownloadWorker
import com.chumor.downgrader.data.storage.firmwareDownloadInputData
import com.chumor.downgrader.domain.model.DeviceBrandDetection
import com.chumor.downgrader.domain.model.DeviceReadResult
import com.chumor.downgrader.domain.model.FirmwareDownloadState
import com.chumor.downgrader.domain.model.FirmwareDownloadStatus
import com.chumor.downgrader.domain.model.FirmwarePackage
import com.chumor.downgrader.domain.model.OplusDeviceInfo
import com.chumor.downgrader.domain.model.OplusFirmware
import com.chumor.downgrader.domain.model.OplusQueryException
import com.chumor.downgrader.domain.model.OplusQueryResult
import com.chumor.downgrader.domain.model.downloadFileName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DowngraderRepository(
    private val devicePropertiesReader: DevicePropertiesReader,
    private val deviceBrandDetector: DeviceBrandDetector,
    private val vivoUpgradeApi: VivoUpgradeApi,
    private val oplusDeviceReader: OplusDeviceReader,
    private val oplusOtaApi: OplusOtaApi,
    private val colorosDowngradeApi: ColorosDowngradeApi,
    private val certificateSaver: CertificateSaver,
    private val workManager: WorkManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val firmwareDownload: Flow<FirmwareDownloadState> =
        workManager.getWorkInfosForUniqueWorkFlow(FIRMWARE_DOWNLOAD_WORK_NAME)
            .map { workInfos ->
                val workInfo = workInfos.lastOrNull { !it.state.isFinished } ?: workInfos.lastOrNull()
                FirmwareDownloadState(
                    status = when (workInfo?.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> FirmwareDownloadStatus.Preparing
                        WorkInfo.State.RUNNING -> {
                            if (workInfo.progress.getBoolean(FIRMWARE_DOWNLOAD_VERIFYING, false)) {
                                FirmwareDownloadStatus.Verifying
                            } else {
                                FirmwareDownloadStatus.Downloading
                            }
                        }
                        WorkInfo.State.SUCCEEDED -> FirmwareDownloadStatus.Completed
                        WorkInfo.State.FAILED -> FirmwareDownloadStatus.Failed
                        WorkInfo.State.CANCELLED -> FirmwareDownloadStatus.Cancelled
                        null -> FirmwareDownloadStatus.Idle
                    },
                    sha256 = workInfo?.tags
                        ?.firstOrNull { tag -> tag.startsWith(FIRMWARE_DOWNLOAD_SHA256_TAG_PREFIX) }
                        ?.removePrefix(FIRMWARE_DOWNLOAD_SHA256_TAG_PREFIX),
                    downloadedBytes = workInfo?.progress?.getLong(FIRMWARE_DOWNLOAD_PROGRESS_BYTES, 0L) ?: 0L,
                    totalBytes = workInfo?.progress?.getLong(FIRMWARE_DOWNLOAD_EXPECTED_SIZE_BYTES, 0L) ?: 0L,
                )
            }
            .distinctUntilChanged()

    suspend fun readDevice(): DeviceReadResult = withContext(ioDispatcher) {
        devicePropertiesReader.read()
    }

    suspend fun queryPackage(credential: String): FirmwarePackage = withContext(ioDispatcher) {
        vivoUpgradeApi.query(credential)
    }

    /** 自动识别本机该走哪条通道（vivo / 欧加真）。 */
    suspend fun detectBrand(): DeviceBrandDetection = withContext(ioDispatcher) {
        deviceBrandDetector.detect()
    }

    suspend fun readOplusDevice(): OplusDeviceInfo = withContext(ioDispatcher) {
        oplusDeviceReader.read()
    }

    /** 官方 OTA 版本链回溯：拿到的每个版本都是可以降级回去的完整包。 */
    suspend fun queryOplusOfficial(model: String, otaVersion: String?): OplusQueryResult =
        withContext(ioDispatcher) {
            oplusOtaApi.query(model, otaVersion)
        }

    /** 售后（降级 / 维修）全量包；失败不影响官方结果，交给上层各自处理。 */
    suspend fun queryOplusAfterSale(model: String, projectNumber: String): List<OplusFirmware> =
        withContext(ioDispatcher) {
            colorosDowngradeApi.query(model, projectNumber)
        }

    suspend fun saveCertificate(certificate: String): String = withContext(ioDispatcher) {
        certificateSaver.save(certificate)
    }

    fun downloadFirmware(firmware: FirmwarePackage, deviceModel: String) {
        val request = OneTimeWorkRequestBuilder<FirmwareDownloadWorker>()
            .addTag(FIRMWARE_DOWNLOAD_SHA256_TAG_PREFIX + firmware.sha256.lowercase())
            .setInputData(
                firmwareDownloadInputData(
                    downloadUrl = firmware.downloadUrl,
                    fileName = firmware.downloadFileName(deviceModel),
                    expectedSizeBytes = firmware.fileSizeBytes,
                    expectedSha256 = firmware.sha256,
                ),
            )
            .build()
        workManager.enqueueUniqueWork(FIRMWARE_DOWNLOAD_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun cancelFirmwareDownload() {
        workManager.cancelUniqueWork(FIRMWARE_DOWNLOAD_WORK_NAME)
    }
}
