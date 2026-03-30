package com.type4me.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.type4me.core.data.remote.GitHubUpdateChecker
import com.type4me.core.data.remote.UpdateManager
import com.type4me.ui.theme.Type4MeTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private lateinit var updateManager: UpdateManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            recreate()
        }
    }

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 用户从设置返回，检查权限
        if (updateManager.canInstallUnknownApps()) {
            // 权限已获取，继续安装
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = UpdateManager(this)

        setContent {
            Type4MeTheme {
                val activity = this@SettingsActivity
                SettingsScreen(
                    activity = activity,
                    updateManager = updateManager,
                    onRequestPermission = { requestPermissions() },
                    onOpenAppSettings = { openAppSettings() },
                    onRequestInstallPermission = {
                        installPermissionLauncher.launch(updateManager.getInstallPermissionIntent())
                    }
                )
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }
}

@Composable
fun SettingsScreen(
    activity: ComponentActivity,
    updateManager: UpdateManager,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRequestInstallPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasRecordAudioPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val updateState by updateManager.updateState.collectAsState()

    LaunchedEffect(Unit) {
        hasRecordAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Type4Me 设置",
                style = MaterialTheme.typography.headlineMedium
            )

            // 更新检查卡片
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "软件更新",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (val state = updateState) {
                        is UpdateManager.UpdateState.Idle -> {
                            Text(text = "当前版本: 1.0.0")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        updateManager.checkUpdate()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("检查更新")
                            }
                        }
                        is UpdateManager.UpdateState.Checking -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            Text(
                                text = "正在检查更新...",
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        is UpdateManager.UpdateState.UpdateAvailable -> {
                            Text(
                                text = "发现新版本: ${state.version}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (state.releaseNotes.isNotBlank()) {
                                Text(
                                    text = state.releaseNotes.take(200),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            if (!updateManager.canInstallUnknownApps()) {
                                Button(
                                    onClick = { onRequestInstallPermission() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("开启安装权限")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        updateManager.startDownload(state.downloadUrl)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("立即更新")
                                }
                            }
                        }
                        is UpdateManager.UpdateState.Downloading -> {
                            LinearProgressIndicator(
                                progress = state.progress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("下载中... ${state.progress}%")
                        }
                        is UpdateManager.UpdateState.Downloaded -> {
                            Text(
                                text = "下载完成",
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { updateManager.installApk(state.file) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("立即安装")
                            }
                        }
                        is UpdateManager.UpdateState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { updateManager.resetState() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("重试")
                            }
                        }
                    }
                }
            }

            // 权限状态卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "权限状态",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "录音权限: ",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (hasRecordAudioPermission) {
                            Text(
                                text = "✓ 已授权",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text(
                                text = "✗ 未授权",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!hasRecordAudioPermission) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                                        activity,
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                    if (shouldShowRationale) {
                                        onRequestPermission()
                                    } else {
                                        onOpenAppSettings()
                                    }
                                } else {
                                    onRequestPermission()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("授予麦克风权限")
                        }

                        Text(
                            text = "注意：如果点击无响应，请在系统设置中手动开启麦克风权限",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // 使用说明
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = """1. 确保已授予麦克风权限
2. 在任意输入框点击输入法切换按钮
3. 选择 Type4Me
4. 点击话筒图标开始录音
5. 说话完成后点击停止
6. 点击"上屏"将文字输入到目标应用""",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 关于
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "关于",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Type4Me v1.0.0\n语音输入 + LLM 文本优化",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
