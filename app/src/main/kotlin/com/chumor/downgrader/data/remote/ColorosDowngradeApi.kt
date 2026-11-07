package com.chumor.downgrader.data.remote

import android.util.Base64
import com.chumor.downgrader.domain.model.OplusFirmware
import com.chumor.downgrader.domain.model.OplusQueryError
import com.chumor.downgrader.domain.model.OplusQueryException
import com.chumor.downgrader.domain.model.OplusSource
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
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONException
import org.json.JSONObject

/**
 * ColorOS 售后（降级 / 维修）全量包查询（downgrade.coloros.com/downgrade/query-v3）。
 *
 * 与官方 OTA 的差别：这里用 AES-256-GCM（密文自带 16 字节 tag），加密只出现在
 * protectedKey 与 body 的 deviceId 两处。服务端约一半概率抽风（500 / 解密失败），
 * 所以整包重试，且每次换一套新会话密钥。
 *
 * 项目号（prjNum）来自官方 OTA 响应里的 `oplus_separate_soft`，是两条协议之间唯一的桥。
 */
class ColorosDowngradeApi {
    fun query(model: String, projectNumber: String): List<OplusFirmware> {
        val code = model.trim().uppercase()
        val project = projectNumber.trim()
        if (code.isEmpty() || project.isEmpty()) return emptyList()

        var lastError: OplusQueryError? = null
        repeat(MAX_TRIES) { attempt ->
            if (attempt > 0) {
                try {
                    Thread.sleep(RETRY_DELAY_MILLIS)
                } catch (exception: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw OplusQueryException(OplusQueryError.Unexpected, exception)
                }
            }
            try {
                return queryOnce(code, project)
            } catch (exception: OplusQueryException) {
                // 服务端抖动与「真的没包」在 HTTP 层长得一样，重发几次远好过误报「没有」
                lastError = exception.error
            }
        }
        throw OplusQueryException(lastError ?: OplusQueryError.Unexpected)
    }

    private fun queryOnce(model: String, project: String): List<OplusFirmware> {
        val key = ByteArray(SESSION_KEY_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(GCM_IV_BYTES).also(secureRandom::nextBytes)
        val devicePlain = DEVICE_ID
        val deviceCipher = try {
            gcmEncrypt(key, iv, devicePlain.toByteArray(StandardCharsets.UTF_8))
        } catch (exception: GeneralSecurityException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val protectedKey = try {
            wrapSessionKey(key)
        } catch (exception: GeneralSecurityException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val cipherInfo = try {
            JSONObject()
                .put(
                    "downgrade-server",
                    JSONObject()
                        .put("negotiationVersion", NEGOTIATION_VERSION)
                        .put("protectedKey", protectedKey)
                        .put("version", (System.currentTimeMillis() / 1_000L).toString()),
                )
                .toString()
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val body = try {
            JSONObject()
                .put("model", model)
                .put("nvCarrier", NV_CARRIER)
                .put("prjNum", project)
                .put("serialNo", SERIAL_NO)
                .put("otaVersion", "${model}_11.A")
                .put(
                    "deviceId",
                    JSONObject()
                        .put("cipher", encode(deviceCipher))
                        .put("iv", encode(iv)),
                )
                .toString()
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val response = post(
            url = ENDPOINT,
            body = body,
            headers = mapOf(
                "Content-Type" to "application/json; charset=UTF-8",
                "cipherInfo" to cipherInfo,
                // 请求头里是明文，与 body 的密文是两回事
                "deviceId" to devicePlain,
                "Connection" to "close",
            ),
        )

        val root = try {
            JSONObject(response)
        } catch (exception: JSONException) {
            throw OplusQueryException(OplusQueryError.InvalidResponse, exception)
        }

        val code = try {
            root.get("code").toString().trim().toIntOrNull() ?: -1
        } catch (exception: JSONException) {
            -1
        }
        if (code != SUCCESS_CODE) {
            throw OplusQueryException(OplusQueryError.NotFound)
        }

        val list = root.optJSONObject("data")?.optJSONArray("downgradeVoList")
            ?: throw OplusQueryException(OplusQueryError.NotFound)

        return buildList {
            for (index in 0 until list.length()) {
                val item = list.optJSONObject(index) ?: continue
                add(toFirmware(item))
            }
        }
    }

    private fun toFirmware(item: JSONObject): OplusFirmware {
        val coloros = item.optString("colorosVersion")
        val android = item.optString("androidVersion")
        val otaVersion = item.optString("otaVersion")
        return OplusFirmware(
            version = coloros.ifBlank { android }.ifBlank { "售后全量包" },
            androidVersion = android,
            sizeBytes = item.optLong("fileSize", 0L),
            downloadUrl = item.optString("downloadUrl"),
            md5 = item.optString("fileMd5"),
            otaVersion = otaVersion,
            published = item.optString("publishedTime"),
            securityPatch = "",
            branch = "",
            source = OplusSource.AFTER_SALE,
            note = item.optString("versionIntroduction"),
        )
    }

    private fun post(url: String, body: String, headers: Map<String, String>): String {
        val connection = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = true
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

    private fun wrapSessionKey(key: ByteArray): String {
        val keySpec = X509EncodedKeySpec(decode(PUBLIC_KEY_B64))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return encode(cipher.doFinal(encode(key).toByteArray(StandardCharsets.UTF_8)))
    }

    private fun gcmEncrypt(key: ByteArray, iv: ByteArray, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        // doFinal 的输出就是「密文 || 16 字节 tag」，正是服务端要的拼法
        return cipher.doFinal(plain)
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.DEFAULT)

    private companion object {
        const val ENDPOINT = "https://downgrade.coloros.com/downgrade/query-v3"
        const val SUCCESS_CODE = 200
        const val NV_CARRIER = "10010111"
        const val SERIAL_NO = "a1b2c3d4"
        const val NEGOTIATION_VERSION = 1_636_449_646_204L
        const val SESSION_KEY_BYTES = 32
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val MAX_TRIES = 8
        const val RETRY_DELAY_MILLIS = 600L
        const val CONNECT_TIMEOUT_MILLIS = 20_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

        /** 已建档的查询用 DUID，只参与建档校验，不绑定机身。 */
        const val DEVICE_ID = "498A44DF1BEC4EB19FBCB3A870FCACB4EC7D424979CC9C517FE7B805A1937746"

        val secureRandom = SecureRandom()

        const val PUBLIC_KEY_B64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmeQzr0TIbtwZFnDXgatg6xP9SlNBFho1NTdFQ27SKDF+dBEEfnG9BqRw0na0DUqtpWe2CUtldbU33nnJ0KB6z7y5f+89o9n8mJxIbh952gpskBxyrhCfpYHV5mt/n9Tkm8OcQWLRFou7/XITuZeZejfUTesQjpfOeCaeKyVSoKQc6WuH7NSYq6B37RMyEn/1+vo8XuHEKD84p29KGpyGI7ZeL85iOcwBmOD6+e4yideH2RatA1SzEv/9V8BflaFLAWDuPWUjA2WgfOvy5spYmp/MoMOX4P0d+AkJ9Ms6PUXEUBsbOACmaMFyLCLHmd18+UeGdJR/3I15sXKbJhKerwIDAQAB"
    }
}
