package com.chumor.downgrader.domain.model

import androidx.compose.runtime.Immutable

/** 本机厂商归属：决定走哪条查询通道。 */
enum class DetectedVendor(val label: String) {
    VIVO("vivo"),
    OPLUS("欧加真"),
    UNKNOWN("未识别"),
}

/**
 * 自动识别的结果。
 *
 * [basis] 是给用户看的判断依据（品牌串 + 机型代号），
 * 识别错了用户一眼能看出来并手动指定。
 */
@Immutable
data class DeviceBrandDetection(
    val vendor: DetectedVendor,
    val brandLabel: String,
    val code: String,
    val model: String,
    val basis: String,
) {
    val isRecognized: Boolean
        get() = vendor != DetectedVendor.UNKNOWN

    companion object {
        val EMPTY = DeviceBrandDetection(
            vendor = DetectedVendor.UNKNOWN,
            brandLabel = "",
            code = "",
            model = "",
            basis = "正在识别",
        )
    }
}
