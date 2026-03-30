package com.type4me.core.data.remote

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import com.type4me.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * GitHub Release 更新检查器
 */
class GitHubUpdateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "GitHubUpdate"
        private const val GITHUB_API_URL = "https://api.github.com/repos/91zgaoge/type4me-android/releases/latest"
    }

    /**
     * 检查更新
     * @return 如果有更新返回 ReleaseInfo，否则返回 null
     */
    suspend fun checkForUpdate(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("Checking for updates...")

            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).e("Failed to check update: ${response.code}")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val release = gson.fromJson(body, GitHubRelease::class.java)

                val latestVersion = release.tagName.removePrefix("v")
                val currentVersion = BuildConfig.VERSION_NAME
                Timber.tag(TAG).d("Latest version: $latestVersion, Current: $currentVersion")

                if (isNewerVersion(latestVersion, currentVersion)) {
                    // 查找 APK 文件
                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                        ?: return@withContext null

                    ReleaseInfo(
                        version = latestVersion,
                        downloadUrl = apkAsset.browserDownloadUrl,
                        releaseNotes = release.body,
                        publishedAt = release.publishedAt
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking for update")
            null
        }
    }

    /**
     * 下载 APK
     * @param context 上下文
     * @param downloadUrl 下载链接
     * @param progressCallback 下载进度回调 (下载字节数, 总字节数)
     * @return 下载完成的文件，失败返回 null
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        progressCallback: (downloaded: Long, total: Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("Downloading APK from: $downloadUrl")

            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).e("Download failed: ${response.code}")
                    return@withContext null
                }

                val totalBytes = response.body?.contentLength() ?: -1
                val inputStream = response.body?.byteStream() ?: return@withContext null

                // 保存到下载目录
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                val apkFile = File(downloadsDir, "type4me-update.apk")

                FileOutputStream(apkFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0) {
                            progressCallback(downloadedBytes, totalBytes)
                        }
                    }
                }

                Timber.tag(TAG).d("APK downloaded to: ${apkFile.absolutePath}")
                apkFile
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error downloading APK")
            null
        }
    }

    /**
     * 比较版本号
     * @param latest 最新版本
     * @param current 当前版本
     * @return true 如果 latest > current
     */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toInt() }
            val currentParts = current.split(".").map { it.toInt() }

            for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error comparing versions")
            false
        }
    }

    data class ReleaseInfo(
        val version: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val publishedAt: String
    )

    // GitHub API 响应数据类
    private data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("body") val body: String,
        @SerializedName("published_at") val publishedAt: String,
        @SerializedName("assets") val assets: List<Asset>
    )

    private data class Asset(
        @SerializedName("name") val name: String,
        @SerializedName("browser_download_url") val browserDownloadUrl: String,
        @SerializedName("size") val size: Long
    )
}
