package com.chumor.downgrader.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.chumor.downgrader.data.device.DevicePropertiesReader
import com.chumor.downgrader.data.device.OplusDeviceReader
import com.chumor.downgrader.data.remote.ColorosDowngradeApi
import com.chumor.downgrader.data.remote.OplusOtaApi
import com.chumor.downgrader.data.remote.VivoUpgradeApi
import com.chumor.downgrader.data.repository.DowngraderRepository
import com.chumor.downgrader.data.storage.CertificateSaver

class DowngraderViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(DowngraderViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
        }

        return DowngraderViewModel(
            repository = DowngraderRepository(
                devicePropertiesReader = DevicePropertiesReader(),
                vivoUpgradeApi = VivoUpgradeApi(),
                oplusDeviceReader = OplusDeviceReader(),
                oplusOtaApi = OplusOtaApi(),
                colorosDowngradeApi = ColorosDowngradeApi(),
                certificateSaver = CertificateSaver(),
                workManager = WorkManager.getInstance(appContext),
            ),
        ) as T
    }
}
