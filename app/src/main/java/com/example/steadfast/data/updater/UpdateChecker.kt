package com.example.steadfast.data.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val version: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val releasePageUrl: String
    ) : UpdateCheckResult

    data class UpToDate(
        val currentVersion: String
    ) : UpdateCheckResult

    data class Error(
        val message: String,
        val releasePageUrl: String = "https://github.com/burrowgo/Steadfast/releases"
    ) : UpdateCheckResult
}

interface UpdateChecker {
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult
}

class DefaultUpdateChecker(
    private val repoOwner: String = "burrowgo",
    private val repoName: String = "Steadfast"
) : UpdateChecker {

    override suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val releasesPageUrl = "https://github.com/$repoOwner/$repoName/releases"
        try {
            val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Steadfast-Android-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                return@withContext UpdateCheckResult.Error(
                    message = "No published releases found yet on GitHub.",
                    releasePageUrl = releasesPageUrl
                )
            }
            if (responseCode != 200) {
                return@withContext UpdateCheckResult.Error(
                    message = "GitHub API returned code $responseCode.",
                    releasePageUrl = releasesPageUrl
                )
            }

            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)
            val tagName = json.optString("tag_name", "")
            val remoteVersion = tagName.removePrefix("v").trim()
            val releaseNotes = json.optString("body", "").trim()
            val releasePageUrl = json.optString("html_url", releasesPageUrl)

            var apkDownloadUrl = releasePageUrl
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith("-release.apk") || name.endsWith(".apk")) {
                        val downloadUrl = asset.optString("browser_download_url")
                        if (downloadUrl.isNotBlank()) {
                            apkDownloadUrl = downloadUrl
                            break
                        }
                    }
                }
            }

            if (isNewerVersion(remoteVersion, currentVersion)) {
                UpdateCheckResult.UpdateAvailable(
                    version = remoteVersion,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkDownloadUrl,
                    releasePageUrl = releasePageUrl
                )
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                message = e.localizedMessage ?: "Failed to connect to GitHub.",
                releasePageUrl = releasesPageUrl
            )
        }
    }

    companion object {
        fun isNewerVersion(latest: String, current: String): Boolean {
            val cleanLatest = latest.removePrefix("v").trim()
            val cleanCurrent = current.removePrefix("v").trim()
            if (cleanLatest.isEmpty() || cleanCurrent.isEmpty()) return false

            val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        }
    }
}
