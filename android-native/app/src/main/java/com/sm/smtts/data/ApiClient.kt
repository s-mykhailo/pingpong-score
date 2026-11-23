package com.sm.smtts.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private const val BACKEND_URL =
    "https://script.google.com/macros/s/AKfycbxi_WfYMsZnjewkr4poOwbKzA-wHPOYvsJsv_5tIMrTE1eAMmiEYCIIjgtqFPGBhl9VOA/exec"

/** The server understood the request and rejected it (bad data, auth, etc). Not recoverable by retrying. */
class ApiException(message: String) : Exception(message)

/** The request never reached the server (offline, DNS, timeout). Safe to retry later. */
class NetworkException(cause: Throwable, val isTimeout: Boolean = false) : Exception(cause)

private val KNOWN_API_ERRORS = mapOf(
    "nickname taken" to "Такий нікнейм вже зайнятий, вибери інший.",
    "name required" to "Введи нікнейм.",
    "name too long" to "Нікнейм задовгий (до 40 символів).",
    "invalid code" to "Невірний код.",
    "unauthorized" to "Невірний master key.",
)

fun friendlyMessage(e: Throwable): String = when (e) {
    is NetworkException -> if (e.isTimeout) {
        "Сервер довго відповідає. Спробуйте ще раз за хвилину."
    } else {
        "Немає інтернету. Перевірте з'єднання і спробуйте ще раз."
    }
    is ApiException -> KNOWN_API_ERRORS[e.message] ?: e.message ?: "Сервер відхилив запит."
    else -> "Щось пішло не так."
}

object ApiClient {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val plainText = "text/plain".toMediaType()

    suspend fun call(
        action: String,
        token: String? = null,
        payload: Map<String, Any?> = emptyMap(),
    ): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("action", action)
            if (token != null) put("token", token)
            for ((k, v) in payload) {
                when (v) {
                    null -> {}
                    is JSONObject -> put(k, v)
                    else -> put(k, v)
                }
            }
        }
        val request = Request.Builder()
            .url(BACKEND_URL)
            .post(body.toString().toRequestBody(plainText))
            .build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            throw NetworkException(e, isTimeout = true)
        } catch (e: IOException) {
            throw NetworkException(e)
        }
        val json = response.use {
            val text = it.body?.string() ?: throw NetworkException(IOException("empty response"))
            JSONObject(text)
        }
        if (!json.optBoolean("ok", false)) {
            throw ApiException(json.optString("error", "unknown error"))
        }
        json
    }
}
