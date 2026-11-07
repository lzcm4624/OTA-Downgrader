package com.chumor.downgrader.data.device

import com.chumor.downgrader.domain.model.DetectedVendor
import com.chumor.downgrader.domain.model.DeviceBrandDetection
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * 机型品牌自动识别：判断这台机器走 vivo 线还是欧加真线。
 *
 * 判定不只看品牌串，还看系统里有没有自家的属性：
 * - vivo / iQOO：ro.product.brand = vivo / iqoo，或存在 ro.vivo.* / sys.vivo.* 属性
 * - OPPO / 一加 / 真我：ro.product.brand = oppo / oneplus / realme，或存在 ro.build.version.oplusrom 之类
 */
class DeviceBrandDetector {

    fun detect(): DeviceBrandDetection {
        val properties = readProperties()
        if (properties.isEmpty()) {
            return DeviceBrandDetection(
                vendor = DetectedVendor.UNKNOWN,
                brandLabel = "",
                code = "",
                model = "",
                basis = "读不到系统属性",
            )
        }

        val brand = properties["ro.product.brand"].orEmpty().trim()
        val manufacturer = properties["ro.product.manufacturer"].orEmpty().trim()
        val deviceCode = sequenceOf(
            "ro.product.name",
            "ro.product.device",
            "ro.product.vendor.device",
        ).mapNotNull { properties[it]?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()
            .orEmpty()
        val model = properties["ro.product.model"].orEmpty().trim()

        val hasVivoProps = properties.keys.any { key ->
            key.startsWith("ro.vivo.") || key.startsWith("sys.vivo.") || key.startsWith("ro.bbk.")
        }
        val hasOplusProps = properties.keys.any { key ->
            key.startsWith("ro.build.version.oplusrom") ||
                key.startsWith("ro.build.version.opporom") ||
                key.startsWith("ro.build.version.realmeui")
        }

        val brandLower = brand.lowercase()
        val manufacturerLower = manufacturer.lowercase()
        val codeLower = deviceCode.lowercase()

        val vivoHit = "vivo" in brandLower || "iqoo" in brandLower ||
            "vivo" in manufacturerLower || "iqoo" in manufacturerLower || hasVivoProps
        val realmeHit = "realme" in brandLower || "realme" in manufacturerLower || "realme" in codeLower
        val onePlusHit = "oneplus" in brandLower || "oneplus" in manufacturerLower ||
            "oneplus" in codeLower
        val oppoHit = "oppo" in brandLower || "oppo" in manufacturerLower || hasOplusProps

        // 两家的属性同时命中时，以品牌串为准；只有属性时按属性走。
        val vendor = when {
            vivoHit && !brandLower.contains("oppo") && !realmeHit && !onePlusHit -> DetectedVendor.VIVO
            brandLower.contains("oppo") -> DetectedVendor.OPLUS
            realmeHit || onePlusHit || oppoHit -> DetectedVendor.OPLUS
            vivoHit -> DetectedVendor.VIVO
            else -> DetectedVendor.UNKNOWN
        }

        val brandLabel = when {
            vendor == DetectedVendor.VIVO ->
                if ("iqoo" in brandLower || "iqoo" in manufacturerLower) "iQOO" else "vivo"

            vendor == DetectedVendor.OPLUS -> when {
                realmeHit -> "真我"
                onePlusHit -> "一加"
                else -> "OPPO"
            }

            else -> ""
        }

        val basis = when (vendor) {
            DetectedVendor.VIVO -> buildString {
                append("品牌属性 ")
                append(brand.ifBlank { manufacturer }.ifBlank { "vivo" })
                if (deviceCode.isNotBlank()) append(" · 代号 $deviceCode")
            }

            DetectedVendor.OPLUS -> buildString {
                append(brandLabel)
                append(" · 代号 ")
                append(deviceCode.ifBlank { "—" })
            }

            DetectedVendor.UNKNOWN -> "没有匹配到 vivo / 欧加真的品牌属性"
        }

        return DeviceBrandDetection(
            vendor = vendor,
            brandLabel = brandLabel,
            code = deviceCode,
            model = model,
            basis = basis,
        )
    }

    private fun readProperties(): Map<String, String> {
        val process = try {
            ProcessBuilder("getprop").redirectErrorStream(true).start()
        } catch (_: IOException) {
            return emptyMap()
        } catch (_: SecurityException) {
            return emptyMap()
        }

        val properties = try {
            BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
                buildMap<String, String> {
                    while (true) {
                        val line = reader.readLine() ?: break
                        val match = PROPERTY_LINE.matchEntire(line) ?: continue
                        put(match.groupValues[1], match.groupValues[2])
                    }
                }
            }
        } catch (_: IOException) {
            process.destroyForcibly()
            return emptyMap()
        }

        try {
            process.waitFor()
        } catch (_: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
        }
        return properties
    }

    private companion object {
        val PROPERTY_LINE = Regex("""^\[([^\]]+)]\s*:\s*\[(.*)]$""")
    }
}
