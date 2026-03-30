package com.type4me.core.data.remote

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File

/**
 * 应用更新管理器
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val UPDATE_FILE_NAME = "type4me-update.apk"
    }

    private val updateChecker = GitHubUpdateChecker()
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        data class UpdateAvailable(
            val version: String,
            val releaseNotes: String,
            val downloadUrl: String
        ) : UpdateState()
        data class Downloading(val progress: Int) : UpdateState()
        data class Downloaded(val file: File) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    /**
     * 检查更新
     */
    suspend fun checkUpdate() {
        _updateState.value = UpdateState.Checking

        val releaseInfo = updateChecker.checkForUpdate()

        if (releaseInfo != null) {
            _updateState.value = UpdateState.UpdateAvailable(
                version = releaseInfo.version,
                releaseNotes = releaseInfo.releaseNotes,
                downloadUrl = releaseInfo.downloadUrl
            )
        } else {
            _updateState.value = UpdateState.Idle
        }
    }

    /**
     * 使用系统 DownloadManager 下载更新
     */
    fun startDownload(downloadUrl: String) {
        try {
            _updateState.value = UpdateState.Downloading(0)

            // 删除旧文件
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            File(downloadDir, UPDATE_FILE_NAME).delete()

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Type4Me 更新")
                setDescription("正在下载新版本...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, UPDATE_FILE_NAME)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            Timber.tag(TAG).d("Download started, ID: $downloadId")

            // 注册广播接收器监听下载完成
            registerDownloadReceiver(downloadId)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start download")
            _updateState.value = UpdateState.Error("下载失败: ${e.message}")
        }
    }

    /**
     * 注册下载完成广播接收器
     */
    private fun registerDownloadReceiver(downloadId: Long) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)

                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)

                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                            val apkFile = File(downloadDir, UPDATE_FILE_NAME)
                            if (apkFile.exists()) {
                                _updateState.value = UpdateState.Downloaded(apkFile)
                            } else {
                                _updateState.value = UpdateState.Error("下载文件不存在")
                            }
                        } else {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            _updateState.value = UpdateState.Error("下载失败: $reason")
                        }
                    }
                    cursor.close()
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * 安装 APK
     * @param apkFile APK 文件
     */
    fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android 7.0+ 使用 FileProvider
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }

                setDataAndType(uri, "application/vnd.android.package-archive")
            }

            context.startActivity(intent)
            Timber.tag(TAG).d("Install intent started")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to install APK")
            _updateState.value = UpdateState.Error("安装失败: ${e.message}")
        }
    }

    /**
     * 检查是否有安装未知应用的权限
     */
    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * 获取安装未知应用的设置意图
     */
    fun getInstallPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}
