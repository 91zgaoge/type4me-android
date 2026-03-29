package com.type4me.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.type4me.core.domain.model.DefaultPrompts
import com.type4me.ime.viewmodel.IMEViewModel

@Composable
fun KeyboardScreen(viewModel: IMEViewModel) {
    val state by viewModel.state.collectAsState()
    val currentText by viewModel.currentText.collectAsState()
    val selectedPrompt by viewModel.selectedPrompt.collectAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            when (val s = state) {
                is IMEViewModel.State.Idle -> {
                    IdlePanel(
                        onMicClick = { viewModel.startRecording() },
                        selectedPrompt = selectedPrompt,
                        onPromptSelect = { viewModel.selectPrompt(it) }
                    )
                }
                is IMEViewModel.State.Recording -> {
                    RecordingPanel(
                        onStop = { viewModel.stopRecording() }
                    )
                }
                is IMEViewModel.State.Recognizing -> {
                    RecognizingPanel(text = s.partialText)
                }
                is IMEViewModel.State.Result -> {
                    ResultPanel(
                        text = s.text,
                        isOptimized = s.isOptimized,
                        onCommit = { viewModel.commitText(s.text) },
                        onOptimize = { viewModel.optimizeText(s.text) },
                        onDiscard = { viewModel.reset() }
                    )
                }
                is IMEViewModel.State.Optimizing -> {
                    OptimizingPanel(originalText = s.originalText)
                }
                is IMEViewModel.State.Error -> {
                    ErrorPanel(
                        message = s.message,
                        onDismiss = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
fun IdlePanel(
    onMicClick: () -> Unit,
    selectedPrompt: com.type4me.core.domain.model.PromptTemplate,
    onPromptSelect: (com.type4me.core.domain.model.PromptTemplate) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DefaultPrompts.ALL.forEach { prompt ->
                PromptChip(
                    prompt = prompt,
                    isSelected = prompt.id == selectedPrompt.id,
                    onClick = { onPromptSelect(prompt) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "开始录音",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun PromptChip(
    prompt: com.type4me.core.domain.model.PromptTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = prompt.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun RecordingPanel(onStop: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "正在录音...",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onStop) {
            Text("停止录音")
        }
    }
}

@Composable
fun RecognizingPanel(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "识别中...",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ResultPanel(
    text: String,
    isOptimized: Boolean,
    onCommit: () -> Unit,
    onOptimize: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isOptimized) "优化结果" else "识别结果",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onCommit) {
                Text("上屏")
            }

            if (!isOptimized) {
                Button(onClick = onOptimize) {
                    Text("优化")
                }
            }

            Button(onClick = onDiscard) {
                Text("放弃")
            }
        }
    }
}

@Composable
fun OptimizingPanel(originalText: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "正在优化...",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = originalText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorPanel(message: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "出错了",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onDismiss) {
            Text("确定")
        }
    }
}
