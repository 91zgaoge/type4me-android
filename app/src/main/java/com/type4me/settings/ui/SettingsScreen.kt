package com.type4me.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.type4me.core.domain.model.ASREngine
import com.type4me.core.domain.model.LLMProvider
import com.type4me.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Type4Me 设置") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ASR 设置
            Text(
                text = "语音识别",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("优先使用离线识别") },
                supportingContent = { Text("需要下载模型文件") },
                trailingContent = {
                    Switch(
                        checked = settings.preferOffline,
                        onCheckedChange = { viewModel.setPreferOffline(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("自动下载模型") },
                trailingContent = {
                    Switch(
                        checked = settings.autoDownloadModel,
                        onCheckedChange = { viewModel.setAutoDownloadModel(it) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // LLM 设置
            Text(
                text = "大语言模型",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("首选提供商") },
                supportingContent = { Text(settings.primaryProvider.displayName()) }
            )

            ListItem(
                headlineContent = { Text("OpenAI API Key") },
                supportingContent = { Text(if (settings.openAiKey.isNotEmpty()) "已设置" else "未设置") }
            )

            ListItem(
                headlineContent = { Text("Gemini API Key") },
                supportingContent = { Text(if (settings.geminiKey.isNotEmpty()) "已设置" else "未设置") }
            )

            ListItem(
                headlineContent = { Text("Claude API Key") },
                supportingContent = { Text(if (settings.claudeKey.isNotEmpty()) "已设置" else "未设置") }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 通用设置
            Text(
                text = "通用",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("自动优化") },
                supportingContent = { Text("识别完成后自动发送到 LLM 优化") },
                trailingContent = {
                    Switch(
                        checked = settings.autoOptimize,
                        onCheckedChange = { viewModel.setAutoOptimize(it) }
                    )
                }
            )
        }
    }
}

fun LLMProvider.displayName(): String = when (this) {
    LLMProvider.OPENAI -> "OpenAI"
    LLMProvider.GEMINI -> "Gemini"
    LLMProvider.CLAUDE -> "Claude"
    LLMProvider.GEMINI_FREE -> "Gemini 免费版"
}