package com.chumor.downgrader.domain.model

import androidx.compose.runtime.Immutable
import java.io.IOException

/** 欧加真（OPPO / 一加 / 真我）品牌归属。 */
enum class OplusBrand(val label: String) {
    OPPO("OPPO"),
    ONEPLUS("一加"),
    REALME("真我"),
    UNKNOWN("欧加真"),
}

/** 欧洲包来源：官方 OTA 版本链，或售后（降级 / 维修）服务器。 */
enum class OplusSource(val label: String) {
    OFFICIAL("官方 OTA"),
    AFTER_SALE("售后降级包"),
}

@Immutable
data class OplusFirmware(
    val version: String,
    val androidVersion: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val md5: String,
    val otaVersion: String,
    val published: String,
    val securityPatch: String,
    val branch: String,
    val source: OplusSource,
    val note: String,
)

@Immutable
data class OplusDeviceInfo(
    val brand: OplusBrand,
    val code: String,
    val model: String,
    val otaVersion: String,
    val androidVersion: String,
    val colorosVersion: String,
) {
    val isRecognized: Boolean
        get() = code.isNotBlank()
}

@Immutable
data class OplusQueryResult(
    val packages: List<OplusFirmware>,
    val projectNumber: String?,
)

sealed interface OplusQueryError {
    data object Network : OplusQueryError
    data class HttpStatus(val code: Int) : OplusQueryError
    data object NotFound : OplusQueryError
    data object InvalidResponse : OplusQueryError
    data object Unexpected : OplusQueryError
}

class OplusQueryException(
    val error: OplusQueryError,
    cause: Throwable? = null,
) : IOException(cause)

/** 从 `RMX3888_16.0.10.500(CN01)` 里取出分支部件名，用于列表排序。 */
fun String.versionSortKey(): Long {
    val core = substringAfter('_', this).substringBefore('(')
    val parts = core.split('.')
    var value = 0L
    for (index in 0 until minOf(parts.size, 4)) {
        value = value * 1_000 + (parts[index].toLongOrNull() ?: 0L)
    }
    return value
}
