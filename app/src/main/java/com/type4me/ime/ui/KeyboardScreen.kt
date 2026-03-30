package com.type4me.ime.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.type4me.ime.Type4MeInputMethodService
import com.type4me.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun KeyboardScreen(
    isRecording: Boolean,
    recognizedText: String,
    statusText: String,
    permissionGranted: Boolean,
    currentEngine: Type4MeInputMethodService.ASREngine,
    engineStatus: String,
    onMicClick: () -> Unit,
    onCommitClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloseClick: () -> Unit,
    onEngineSwitch: () -> Unit,
    onRefreshPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, BackgroundCard)
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题栏
            HeaderSection()

            Spacer(modifier = Modifier.height(12.dp))

            // 状态卡片
            StatusCard(
                permissionGranted = permissionGranted,
                currentEngine = currentEngine,
                engineStatus = engineStatus,
                statusText = statusText,
                onRefreshPermission = onRefreshPermission,
                onEngineSwitch = onEngineSwitch
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 麦克风大按钮（核心交互）
            MicButton(
                isRecording = isRecording,
                enabled = permissionGranted,
                onClick = onMicClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 识别结果输入框
            ResultTextField(
                text = recognizedText,
                isRecording = isRecording
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 底部操作按钮
            ActionButtons(
                onCommitClick = onCommitClick,
                onSettingsClick = onSettingsClick,
                onCloseClick = onCloseClick,
                hasText = recognizedText.isNotEmpty()
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Type4Me",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 品牌装饰点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryGradientStart, PrimaryGradientEnd)
                    )
                )
        )
    }
}

@Composable
private fun StatusCard(
    permissionGranted: Boolean,
    currentEngine: Type4MeInputMethodService.ASREngine,
    engineStatus: String,
    statusText: String,
    onRefreshPermission: () -> Unit,
    onEngineSwitch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !permissionGranted) { onRefreshPermission() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundElevated.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 权限状态行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                               verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 状态指示灯
                    val indicatorColor = if (permissionGranted) SuccessColor else ErrorColor
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (permissionGranted) "麦克风权限已开启" else "需要麦克风权限",
                        color = if (permissionGranted) SuccessLight else ErrorColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 引擎切换按钮
                EngineChip(
                    engine = currentEngine,
                    status = engineStatus,
                    onClick = onEngineSwitch
                )
            }

            // 状态文字（如果有）
            AnimatedVisibility(visible = statusText.isNotEmpty()) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = TextMuted.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineChip(
    engine: Type4MeInputMethodService.ASREngine,
    status: String,
    onClick: () -> Unit
) {
    val (engineName, engineColor) = when (engine) {
        Type4MeInputMethodService.ASREngine.VOSK_OFFLINE -> "Vosk" to EngineVosk
        Type4MeInputMethodService.ASREngine.SHERPA_ONNX_OFFLINE -> "Sherpa" to EngineSherpa
        Type4MeInputMethodService.ASREngine.GOOGLE_ONLINE -> "Google" to EngineGoogle
    }

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = engineColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(engineColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$engineName · $status",
                color = engineColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MicButton(
    isRecording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // 录音时的脉冲动画
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isRecording) 1f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(if (isRecording) scale else 1f),
        contentAlignment = Alignment.Center
    ) {
        // 外圈光晕（仅在录音时显示）
        if (isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        color = RecordingGradientStart.copy(alpha = 0.3f * alpha)
                    )
            )
        }

        // 主按钮
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) {
                    Color.Transparent
                } else {
                    Color.Unspecified
                },
                disabledContainerColor = ButtonSecondary
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (isRecording) {
                            Brush.radialGradient(
                                colors = listOf(RecordingGradientStart, RecordingGradientEnd)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(PrimaryGradientStart, PrimaryGradientEnd)
                            )
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Rounded.KeyboardVoice,
                    contentDescription = if (isRecording) "停止录音" else "开始录音",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ResultTextField(
    text: String,
    isRecording: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp, max = 120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundDark.copy(alpha = 0.6f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                // 占位符动画
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRecording) {
                        // 录音中动画点
                        val infiniteTransition = rememberInfiniteTransition(label = "dots")
                        repeat(3) { index ->
                            val delay = index * 200
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, delayMillis = delay),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dot_$index"
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGradientStart.copy(alpha = alpha))
                            )
                            if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "正在聆听...",
                            color = PrimaryGradientStart,
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = "识别结果将显示在这里",
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                Text(
                    text = text,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onCommitClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloseClick: () -> Unit,
    hasText: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 上屏按钮
        ActionButton(
            icon = Icons.Default.Send,
            label = "上屏",
            onClick = onCommitClick,
            enabled = hasText,
            primary = true
        )

        // 设置按钮
        ActionButton(
            icon = Icons.Default.Settings,
            label = "设置",
            onClick = onSettingsClick,
            enabled = true,
            primary = false
        )

        // 关闭按钮
        ActionButton(
            icon = Icons.Default.Close,
            label = "关闭",
            onClick = onCloseClick,
            enabled = true,
            primary = false
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (primary && enabled) {
                ButtonPrimary
            } else {
                ButtonSecondary
            },
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) TextPrimary else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = if (enabled) TextSecondary else TextMuted,
            fontSize = 12.sp
        )
    }
}
