package com.chumor.downgrader.data.device

import com.chumor.downgrader.domain.model.OplusBrand
import com.chumor.downgrader.domain.model.OplusDeviceInfo
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * 读取欧加真设备的品牌、机型代号与当前系统版本。
 *
 * OPPO / 一加 / 真我 的分支扫描与售后查询要的都是「机型代号」（RMX3888、PJZ110 这类），
 * 而 `ro.product.name` 正好就是它；市场名（如「真我 GT7 Pro」）只在界面上给人看。
 */
class OplusDeviceReader {
    fun read(): OplusDeviceInfo {
        val properties = readProperties()
        val brand = detectBrand(properties)
        val code = sequenceOf(
            "ro.product.name",
            "ro.product.device",
            "ro.product.vendor.device",
        ).mapNotNull { properties[it]?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()
            .orEmpty()

        return OplusDeviceInfo(
            brand = brand,
            code = code,
            model = properties["ro.product.model"]?.trim().orEmpty(),
            otaVersion = properties["ro.build.version.ota"]?.trim()
                ?: properties["ro.build.display.id"]?.trim().orEmpty(),
            androidVersion = properties["ro.build.version.release"]?.trim().orEmpty(),
            colorosVersion = sequenceOf(
                "ro.build.version.oplusrom.display",
                "ro.build.version.oplusrom",
                "ro.build.version.opporom",
                "ro.build.version.realmeui",
            ).mapNotNull { properties[it]?.trim()?.takeIf(String::isNotEmpty) }
                .firstOrNull()
                .orEmpty(),
        )
    }

    private fun detectBrand(properties: Map<String, String>): OplusBrand {
        val brand = properties["ro.product.brand"].orEmpty().lowercase()
        val manufacturer = properties["ro.product.manufacturer"].orEmpty().lowercase()
        val device = properties["ro.product.device"].orEmpty().lowercase()
        return when {
            "realme" in brand || "realme" in manufacturer || "realme" in device -> OplusBrand.REALME
            "oneplus" in brand || "oneplus" in manufacturer || "oneplus" in device -> OplusBrand.ONEPLUS
            "oppo" in brand || "oppo" in manufacturer || "oppo" in device -> OplusBrand.OPPO
            // 欧加的系统属性存在，说明是自家机器，只是品牌串没匹配上
            properties.keys.any { it.startsWith("ro.build.version.oplusrom") } -> OplusBrand.OPPO
            else -> OplusBrand.UNKNOWN
        }
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
                        val name = match.groupValues[1]
                        if (name in INTERESTING_PROPERTIES) {
                            put(name, match.groupValues[2])
                        }
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
        val INTERESTING_PROPERTIES = setOf(
            "ro.product.brand",
            "ro.product.manufacturer",
            "ro.product.model",
            "ro.product.name",
            "ro.product.device",
            "ro.product.vendor.device",
            "ro.build.version.ota",
            "ro.build.display.id",
            "ro.build.version.release",
            "ro.build.version.oplusrom.display",
            "ro.build.version.oplusrom",
            "ro.build.version.opporom",
            "ro.build.version.realmeui",
        )
    }
}
