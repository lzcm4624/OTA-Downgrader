package com.chumor.downgrader.data.remote

import android.util.Base64
import com.chumor.downgrader.domain.model.OplusFirmware
import com.chumor.downgrader.domain.model.OplusQueryError
import com.chumor.downgrader.domain.model.OplusQueryException
import com.chumor.downgrader.domain.model.OplusQueryResult
import com.chumor.downgrader.domain.model.OplusSource
import com.chumor.downgrader.domain.model.versionSortKey
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONException
import org.json.JSONObject

/**
 * OPPO / 一加 / 真我 官方 OTA 查询（component-ota-cn.allawntech.com）。
 *
 * 协议要点：请求体用 AES-256-CTR 加密，会话密钥用服务器 RSA 公钥做
 * RSA-OAEP(MGF1-SHA1) 包裹，且被加密的明文是 base64(会话密钥) 这段文本，
 * 不是裸 32 字节密钥。
 *
 * 版本链回溯：把响应里的 `realOtaVersion` 原样作为下一次请求的 otaVersion，
 * 服务器就会返回它的下一个版本。从最老的分支头（`_11.A.01_...`）出发，
 * 能一路走到当前最新，中间经过的每个历史版本都能拿到 —— 这才是能拿来降级的包。
 */
class OplusOtaApi {
    fun query(model: String, preferredOtaVersion: String? = null): OplusQueryResult {
        val code = model.trim().uppercase()
        if (code.isEmpty()) {
            throw OplusQueryException(OplusQueryError.InvalidResponse)
        }

        val collected = LinkedHashMap<String, OplusFirmware>()
        var projectNumber: String? = null
        var failure: OplusQueryError? = null

        val seeds = buildList {
            preferredOtaVersion?.takeIf { it.startsWith(code, ignoreCase = true) }?.let(::add)
            add(chainHeadOf(code, preferredOtaVersion))
            add("${code}_11.A.01_0001_197001010000")
        }.distinct()

        seedLoop@ for (seed in seeds) {
            var cursor = seed
            val visited = HashSet<String>()
            var steps = 0
            while (steps < MAX_CHAIN_STEPS) {
                val data = try {
                    queryOnce(code, cursor)
                } catch (exception: OplusQueryException) {
                    failure = exception.error
                    break
                } ?: break

                val version = data.optString("realVersionName").ifBlank {
                    data.optString("versionName")
                }
                if (version.isNotBlank()) {
                    collected.putIfAbsent(version, toFirmware(data, version))
                }
                if (projectNumber.isNullOrBlank()) {
                    projectNumber = projectNumberOf(data)
                }

                val next = data.optString("realOtaVersion")
                if (next.isBlank() || next == cursor || !visited.add(next)) break
                cursor = next
                steps++
            }
            if (collected.isNotEmpty()) break@seedLoop
        }

        if (collected.isEmpty()) {
            throw OplusQueryException(failure ?: OplusQueryError.NotFound)
        }

        return OplusQueryResult(
            packages = collected.values.sortedByDescending { it.version.versionSortKey() },
            projectNumber = projectNumber,
        )
    }

    /** 单次 OTA 查询，返回解密后的 data 对象；无包时返回 null。 */
    private fun queryOnce(model: String, otaVersion: String): JSONObject? {
        val key = ByteArray(SESSION_KEY_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(CTR_IV_BYTES).also(secureRandom::nextBytes)

        val protectedKey = try {
            wrapSessionKey(key, PUBLIC_KEY_B64)
        } catch (exception: GeneralSecurityException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val protectedHeader = try {
            JSONObject()
                .put(
                    "SCENE_1",
                    JSONObject()
                        .put("protectedKey", protectedKey)
                        .put("version", versionStamp())
                        .put("negotiationVersion", NEGOTIATION_VERSION),
                )
                .toString()
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val plainBody = try {
            JSONObject()
                .put("mode", "0")
                .put("time", System.currentTimeMillis())
                .put("isRooted", "0")
                .put("isLocked", true)
                .put("type", "0")
                .put("deviceId", "0".repeat(64))
                .put("opex", JSONObject().put("check", true))
                .put("isSuportPki", false)
                .toString()
                .toByteArray(StandardCharsets.UTF_8)
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val payload = try {
            JSONObject()
                .put(
                    "params",
                    JSONObject()
                        .put("cipher", encode(aesCtr(plainBody, key, iv)))
                        .put("iv", encode(iv))
                        .toString(),
                )
                .toString()
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val response = postJson(
            url = "$HOST$UPDATE_ENDPOINT",
            body = payload,
            headers = mapOf(
                "language" to "zh-CN",
                "newLanguage" to "zh-CN",
                "androidVersion" to "unknown",
                "colorOSVersion" to "unknown",
                "romVersion" to "unknown",
                "infVersion" to "1",
                "otaVersion" to otaVersion,
                "model" to model,
                "mode" to "manual",
                "nvCarrier" to NV_CARRIER,
                "pipelineKey" to "ALLNET",
                "operator" to "ALLNET",
                "companyId" to "",
                "version" to "2",
                "deviceId" to randomAlnum(64),
                "protectedKey" to protectedHeader,
            ),
        )

        val root = try {
            JSONObject(response)
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        if (root.optInt("responseCode", -1) != SUCCESS_CODE) return null
        val body = root.optString("body")
        if (body.isBlank()) return null

        val envelope = try {
            JSONObject(body)
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }
        val cipherText = envelope.optString("cipher")
        val responseIv = envelope.optString("iv")
        if (cipherText.isBlank() || responseIv.isBlank()) {
            throw OplusQueryException(OplusQueryError.InvalidResponse)
        }

        val plain = try {
            aesCtr(decode(cipherText), key, decode(responseIv))
        } catch (exception: GeneralSecurityException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        } catch (exception: IllegalArgumentException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        return try {
            JSONObject(String(plain, StandardCharsets.UTF_8))
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }
    }

    private fun toFirmware(data: JSONObject, version: String): OplusFirmware {
        val components = data.optJSONArray("components")
        val packets = components?.optJSONObject(0)?.optJSONObject("componentPackets")
        val otaVersion = data.optString("realOtaVersion").ifBlank { data.optString("otaVersion") }
        val size = maxOf(packets?.optLong("size") ?: 0L, packets?.optLong("sizeV2") ?: 0L)
        return OplusFirmware(
            version = version,
            androidVersion = data.optString("realAndroidVersion").ifBlank {
                data.optString("androidVersion")
            },
            sizeBytes = size,
            downloadUrl = packets?.optString("manualUrl").orEmpty(),
            md5 = packets?.optString("md5").orEmpty(),
            otaVersion = otaVersion,
            published = data.optString("publishedTime"),
            securityPatch = data.optString("securityPatch"),
            branch = branchOf(otaVersion),
            source = OplusSource.OFFICIAL,
            note = "",
        )
    }

    /** 取 `oplus_separate_soft` 五位项目号，售后服务器只认这个。 */
    private fun projectNumberOf(data: JSONObject): String? {
        val header = data.optJSONArray("components")
            ?.optJSONObject(0)
            ?.optJSONObject("componentPackets")
            ?.optJSONObject("vabInfo")
            ?.optJSONObject("data")
            ?.optJSONArray("header")
            ?: return null
        for (index in 0 until header.length()) {
            val line = header.optString(index)
            if (line.startsWith(PROJECT_PREFIX)) {
                return line.removePrefix(PROJECT_PREFIX).trim().takeIf(String::isNotEmpty)
            }
        }
        return null
    }

    private fun postJson(url: String, body: String, headers: Map<String, String>): String {
        val connection = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "*/*")
                headers.forEach { (name, value) -> setRequestProperty(name, value) }
            }
        } catch (exception: IOException) {
            throw OplusQueryException(OplusQueryError.Network, exception)
        }

        try {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(StandardCharsets.UTF_8))
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val text = stream?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (status !in 200..299) {
                throw OplusQueryException(OplusQueryError.HttpStatus(status))
            }
            if (text.isBlank()) {
                throw OplusQueryException(OplusQueryError.InvalidResponse)
            }
            return text
        } catch (exception: OplusQueryException) {
            throw exception
        } catch (exception: IOException) {
            throw OplusQueryException(OplusQueryError.Network, exception)
        } finally {
            connection.disconnect()
        }
    }

    private fun wrapSessionKey(key: ByteArray, publicKeyBase64: String): String {
        val keySpec = X509EncodedKeySpec(decode(publicKeyBase64))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        // 被加密的是 base64(会话密钥) 这段文本，服务端先 base64 解码再当 AES 密钥用
        return encode(cipher.doFinal(encode(key).toByteArray(StandardCharsets.UTF_8)))
    }

    private fun aesCtr(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_CTR_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun versionStamp(): String = (System.currentTimeMillis() * 1_000_000L + VERSION_EPOCH_OFFSET).toString()

    private fun randomAlnum(length: Int): String =
        buildString { repeat(length) { append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]) } }

    private fun branchOf(otaVersion: String): String {
        val letter = otaVersion.split('_').getOrNull(1)?.split('.')?.getOrNull(1).orEmpty()
        return if (letter.length == 1 && letter[0].isLetter()) letter.uppercase() else ""
    }

    private fun chainHeadOf(model: String, preferredOtaVersion: String?): String {
        val letter = preferredOtaVersion?.let(::branchOf)?.takeIf(String::isNotEmpty) ?: "A"
        return "${model}_11.$letter.01_0001_197001010000"
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.DEFAULT)

    private companion object {
        const val HOST = "https://component-ota-cn.allawntech.com"
        const val UPDATE_ENDPOINT = "/update/v6"
        const val SUCCESS_CODE = 200
        const val NV_CARRIER = "10010111"
        const val NEGOTIATION_VERSION = "1615879139745"
        const val PROJECT_PREFIX = "oplus_separate_soft="
        const val SESSION_KEY_BYTES = 32
        const val CTR_IV_BYTES = 16
        const val MAX_CHAIN_STEPS = 16
        const val CONNECT_TIMEOUT_MILLIS = 20_000
        const val READ_TIMEOUT_MILLIS = 25_000
        const val VERSION_EPOCH_OFFSET = 86_400_000_000_000_000L
        const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
        const val AES_CTR_TRANSFORMATION = "AES/CTR/NoPadding"
        const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        val secureRandom = SecureRandom()

        const val PUBLIC_KEY_B64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApXYGXQpNL7gmMzzvajHaoZIHQQvBc2cOEhJc7/tsaO4sT0unoQnwQKfNQCuv7qC1Nu32eCLuewe9LSYhDXr9KSBWjOcCFXVXteLO9WCaAh5hwnUoP/5/Wz0jJwBA+yqs3AaGLA9wJ0+B2lB1vLE4FZNE7exUfwUc03fJxHG9nCLKjIZlrnAAHjRCd8mpnADwfkCEIPIGhnwq7pdkbamZcoZfZud1+fPsELviB9u447C6bKnTU4AaMcR9Y2/uI6TJUTcgyCp+ilgU0JxemrSIPFk3jbCbzamQ6Shkw/jDRzYoXpBRg/2QDkbq+j3ljInu0RHDfOeXf3VBfHSnQ66HCwIDAQAB"
    }
}
