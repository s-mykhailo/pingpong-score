package com.sm.smtts.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AvailableUpdate(val version: String, val url: String)

private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/s-mykhailo/pingpong-score/releases/latest"
private const val TAG_PREFIX = "smtts-v"

object UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Returns the newer release if one exists, or null on any error or when up to date.
     *  Never throws - a failed version check must stay invisible to the user. */
    suspend fun checkForUpdate(currentVersion: String): AvailableUpdate? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "")
                if (!tag.startsWith(TAG_PREFIX) || htmlUrl.isEmpty()) return@withContext null
                val latestVersion = tag.removePrefix(TAG_PREFIX)
                if (isNewer(latestVersion, currentVersion)) {
                    AvailableUpdate(latestVersion, htmlUrl)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }
}
