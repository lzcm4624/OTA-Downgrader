package com.chumor.downgrader.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chumor.downgrader.data.repository.DowngraderRepository
import com.chumor.downgrader.domain.model.DeviceBrandDetection
import com.chumor.downgrader.domain.model.DeviceReadError
import com.chumor.downgrader.domain.model.DeviceReadException
import com.chumor.downgrader.domain.model.FirmwareDownloadStatus
import com.chumor.downgrader.domain.model.DetectedVendor
import com.chumor.downgrader.domain.model.OplusBrand
import com.chumor.downgrader.domain.model.OplusQueryError
import com.chumor.downgrader.domain.model.OplusQueryException
import com.chumor.downgrader.domain.model.QueryError
import com.chumor.downgrader.domain.model.QueryException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DowngraderViewModel(
    private val repository: DowngraderRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DowngraderUiState())
    val state = mutableState.asStateFlow()

    private val mutableEffects = Channel<DowngraderUiEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var credential: String? = null

    init {
        // Keep background completions in state and queue their UI effects for the next visible session.
        viewModelScope.launch {
            repository.firmwareDownload.collect { download ->
                val wasDownloadingFirmware = mutableState.value.isDownloadingFirmware
                mutableState.update { it.copy(firmwareDownload = download) }
                if (wasDownloadingFirmware) {
                    when (download.status) {
                        FirmwareDownloadStatus.Completed ->
                            mutableEffects.trySend(DowngraderUiEffect.FirmwareDownloadCompleted)
                        FirmwareDownloadStatus.Failed ->
                            mutableEffects.trySend(DowngraderUiEffect.FirmwareDownloadFailed)
                        else -> Unit
                    }
                }
            }
        }
        detectBrand()
        loadDevice()
        loadOplusDevice()
    }

    /**
     * 自动识别机型品牌，直接决定用哪条通道。
     * 用户手动指定过通道就不再被覆盖。
     */
    private fun detectBrand() {
        viewModelScope.launch {
            val detection = try {
                repository.detectBrand()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                DeviceBrandDetection.EMPTY.copy(basis = "识别失败")
            }

            mutableState.update { current ->
                current.copy(
                    detection = detection,
                    isDetectingBrand = false,
                    channel = if (current.channelManual) current.channel else autoChannelOf(detection),
                )
            }
        }
    }

    /** 识别不对时用这个把通道交还给自动识别。 */
    fun useAutoChannel() {
        mutableState.update { current ->
            current.copy(
                channelManual = false,
                channel = autoChannelOf(current.detection),
            )
        }
    }

    private fun autoChannelOf(detection: DeviceBrandDetection): FirmwareChannel =
        if (detection.vendor == DetectedVendor.OPLUS) FirmwareChannel.OPLUS else FirmwareChannel.VIVO

    /** 读欧加真设备信息（品牌、机型代号、ColorOS 版本）。 */
    private fun loadOplusDevice() {
        viewModelScope.launch {
            val device = try {
                repository.readOplusDevice()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            } ?: return@launch

            mutableState.update { current ->
                current.copy(
                    oplusDevice = device,
                    oplusModelInput = current.oplusModelInput.ifBlank { device.code },
                )
            }
        }
    }

    /** 手动指定通道；之后自动识别不再覆盖。 */
    fun selectChannel(channel: FirmwareChannel) {
        mutableState.update { it.copy(channel = channel, channelManual = true) }
    }

    fun updateOplusModel(value: String) {
        mutableState.update { it.copy(oplusModelInput = value.uppercase()) }
    }

    /**
     * 欧加真查询：先从官方 OTA 顺版本链回溯（拿全部历史版本），
     * 再用链上取到的项目号查售后降级包。两段分开上报，界面能看到进展。
     */
    fun queryOplus() {
        val current = mutableState.value
        if (!current.canQueryOplus) return

        val model = current.oplusModelInput.trim().uppercase()
        mutableState.update {
            it.copy(
                isQueryingOplus = true,
                oplusPackages = emptyList(),
                oplusProjectNumber = null,
                oplusQueryError = null,
                oplusQueried = false,
                oplusStage = "正在回溯官方版本链",
                motionEpoch = it.motionEpoch + 1,
            )
        }

        viewModelScope.launch {
            try {
                val official = repository.queryOplusOfficial(model, current.oplusDevice?.otaVersion)
                mutableState.update {
                    it.copy(
                        oplusPackages = official.packages,
                        oplusProjectNumber = official.projectNumber,
                        oplusStage = if (official.projectNumber != null) "正在查询售后降级包" else "",
                    )
                }

                val afterSale = official.projectNumber?.let { project ->
                    try {
                        repository.queryOplusAfterSale(model, project)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        emptyList()
                    }
                }.orEmpty()

                mutableState.update {
                    it.copy(
                        oplusPackages = it.oplusPackages + afterSale,
                        isQueryingOplus = false,
                        oplusStage = "",
                        oplusQueried = true,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: OplusQueryException) {
                mutableState.update {
                    it.copy(
                        isQueryingOplus = false,
                        oplusQueryError = exception.error,
                        oplusStage = "",
                        oplusQueried = true,
                    )
                }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        isQueryingOplus = false,
                        oplusQueryError = OplusQueryError.Unexpected,
                        oplusStage = "",
                        oplusQueried = true,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        credential = null
        mutableEffects.close()
        super.onCleared()
    }

    private fun loadDevice() {
        mutableState.update { it.copy(isLoadingDevice = true) }
        viewModelScope.launch {
            try {
                val result = repository.readDevice()
                val currentCredential = result.credential.takeIf(String::isNotEmpty)
                credential = currentCredential
                mutableState.update {
                    it.copy(
                        isLoadingDevice = false,
                        device = result.info,
                        credentialAvailable = currentCredential != null,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: DeviceReadException) {
                mutableState.update {
                    it.copy(isLoadingDevice = false, deviceError = exception.error)
                }
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(
                        isLoadingDevice = false,
                        deviceError = DeviceReadError.Unavailable,
                    )
                }
            }
        }
    }

    fun queryPackage() {
        // 每查一次就给动效一个重播信号
        val currentCredential = credential ?: return
        if (!mutableState.value.canQueryPackage) return

        mutableState.update {
            it.copy(
                isQuerying = true,
                firmware = null,
                queryError = null,
                motionEpoch = it.motionEpoch + 1,
            )
        }
        viewModelScope.launch {
            try {
                val firmware = repository.queryPackage(currentCredential)
                mutableState.update {
                    it.copy(
                        isQuerying = false,
                        firmware = firmware,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: QueryException) {
                mutableState.update {
                    it.copy(
                        isQuerying = false,
                        queryError = exception.error,
                    )
                }
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(
                        isQuerying = false,
                        queryError = QueryError.Unexpected,
                    )
                }
            }
        }
    }

    fun saveCertificate() {
        val certificate = mutableState.value.firmware?.certificate
            ?.takeIf(String::isNotEmpty)
            ?: return
        if (!mutableState.value.canSaveCertificate) return

        mutableState.update { it.copy(isSavingCertificate = true, savedCertificate = null) }
        viewModelScope.launch {
            val location = try {
                repository.saveCertificate(certificate)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }

            mutableState.update {
                it.copy(
                    isSavingCertificate = false,
                    savedCertificate = certificate.takeIf { location != null },
                )
            }
            mutableEffects.send(
                if (location == null) {
                    DowngraderUiEffect.CertificateSaveFailed
                } else {
                    DowngraderUiEffect.CertificateSaved(location)
                },
            )
        }
    }

    fun downloadFirmware() {
        val currentState = mutableState.value
        val firmware = currentState.firmware ?: return
        val deviceModel = currentState.device?.model?.takeIf(String::isNotBlank) ?: return
        if (!currentState.canDownloadFirmware) return

        repository.downloadFirmware(firmware, deviceModel)
    }

    fun cancelFirmwareDownload() {
        repository.cancelFirmwareDownload()
    }
}
