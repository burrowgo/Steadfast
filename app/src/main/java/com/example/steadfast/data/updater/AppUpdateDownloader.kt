package com.example.steadfast.data.updater

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float // 0f..1f, or -1f if indeterminate
    ) : DownloadState
    data class Success(val apkFile: File) : DownloadState
    data class Error(val message: String) : DownloadState
}

interface AppUpdateDownloader {
    fun downloadUpdate(downloadUrl: String, version: String): Flow<DownloadState>
}

class DefaultAppUpdateDownloader(private val context: Context) : AppUpdateDownloader {

    override fun downloadUpdate(downloadUrl: String, version: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(bytesDownloaded = 0L, totalBytes = -1L, progress = -1f))

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Clean up previous APK files and partial downloads
        try {
            updatesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk") || file.name.endsWith(".tmp")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}

        val tempFile = File(updatesDir, "steadfast-v$version.apk.tmp")
        val targetFile = File(updatesDir, "steadfast-v$version.apk")

        try {
            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 10

            while (redirects < maxRedirects) {
                currentCoroutineContext().ensureActive()
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Steadfast-Android-App")
                    setRequestProperty("Accept-Encoding", "identity")
                }

                val responseCode = conn.responseCode
                if (responseCode in listOf(
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        HttpURLConnection.HTTP_SEE_OTHER,
                        307,
                        308
                    )
                ) {
                    val location = conn.getHeaderField("Location")
                        ?: throw IOException("Redirect response without Location header")
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(url, location).toString()
                    }
                    conn.disconnect()
                    redirects++
                    continue
                }

                if (responseCode !in 200..299) {
                    conn.disconnect()
                    throw IOException("HTTP error code $responseCode")
                }

                connection = conn
                break
            }

            val finalConn = connection ?: throw IOException("Exceeded maximum redirects ($redirects)")
            val totalBytes = finalConn.contentLengthLong.coerceAtLeast(-1L)

            var bytesDownloaded = 0L
            val buffer = ByteArray(8192)
            var lastEmitTime = 0L

            finalConn.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime >= 100L || (totalBytes > 0 && bytesDownloaded == totalBytes)) {
                            lastEmitTime = now
                            val progress = if (totalBytes > 0) {
                                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            emit(DownloadState.Downloading(bytesDownloaded, totalBytes, progress))
                        }
                    }
                    output.flush()
                }
            }

            finalConn.disconnect()

            if (tempFile.length() == 0L) {
                throw IOException("Downloaded file is empty")
            }
            if (totalBytes > 0 && bytesDownloaded < totalBytes) {
                throw IOException("Incomplete download: received $bytesDownloaded of $totalBytes bytes")
            }

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            emit(DownloadState.Success(targetFile))
        } catch (e: CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            emit(DownloadState.Error(e.localizedMessage ?: "Failed to download update"))
        }
    }.flowOn(Dispatchers.IO)
}
