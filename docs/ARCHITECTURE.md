# Type4Me Android 技术架构

## 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        应用层 (App Layer)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌───────────────────────────────┐  │
│  │   设置界面      │    │         输入法界面            │  │
│  │ SettingsActivity│    │  Type4MeInputMethodService   │  │
│  └────────┬────────┘    └───────────────┬───────────────┘  │
│           │                              │                  │
│           └──────────────┬───────────────┘                  │
│                          │                                  │
│                   ┌──────▼──────┐                          │
│                   │ Compose UI  │                          │
│                   │KeyboardScreen│                          │
│                   └──────┬──────┘                          │
│                          │                                  │
└──────────────────────────┼──────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                      核心层 (Core Layer)                     │
├──────────────────────────┼──────────────────────────────────┤
│                          │                                  │
│  ┌───────────────────────┼───────────────────────┐         │
│  │                       │                       │         │
│  │   ┌─────────────┐    │    ┌─────────────┐   │         │
│  │   │ VoskEngine  │    │    │SherpaOnnx   │   │         │
│  │   │  (离线)      │◄───┼───►│Engine       │   │         │
│  │   └──────┬──────┘    │    └──────┬──────┘   │         │
│  │          │           │           │          │         │
│  │          │    ┌──────┴───────┐   │          │         │
│  │          └───►│ ASR 管理器    │◄──┘          │         │
│  │               │ Engine切换    │              │         │
│  │               └──────┬───────┘              │         │
│  │                      │                       │         │
│  │               ┌──────┴───────┐              │         │
│  │               │Google Speech │              │         │
│  │               │Recognizer    │              │         │
│  │               │  (在线)       │              │         │
│  │               └──────────────┘              │         │
│  │                                             │         │
│  └─────────────────────────────────────────────┘         │
│                                                            │
└────────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────┼──────────────────────────────────┐
│                    系统层 (System Layer)                     │
├──────────────────────────┼──────────────────────────────────┤
│                          │                                  │
│  ┌──────────────┐  ┌─────┴──────┐  ┌──────────────────┐   │
│  │ InputMethod  │  │ AudioRecord│  │ SpeechRecognizer │   │
│  │   Service    │  │   (录音)    │  │   (Google ASR)   │   │
│  └──────────────┘  └────────────┘  └──────────────────┘   │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

## 数据流图

### 语音识别流程

```
用户点击麦克风
       │
       ▼
┌──────────────┐
│ startRecording│
└──────┬───────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  检查引擎类型  │────►│ VoskEngine   │────►│  开始录音     │
│              │     │              │     │ AudioRecord  │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                 │
                                                 ▼
                                          ┌──────────────┐
                                          │  音频数据处理  │
                                          │ (PCM → Vosk) │
                                          └──────┬───────┘
                                                 │
                    ┌────────────────────────────┘
                    ▼
            ┌──────────────┐
            │  识别结果回调  │
            │  Flow/Callback│
            └──────┬───────┘
                   │
                   ▼
            ┌──────────────┐
            │ 更新 Compose  │
            │  状态 (State) │
            └──────┬───────┘
                   │
                   ▼
            ┌──────────────┐
            │  UI 自动更新  │
            │ KeyboardScreen│
            └──────────────┘
                   │
       用户点击"上屏"
                   │
                   ▼
            ┌──────────────┐
            │ commitText() │
            │  文字输入目标 │
            └──────────────┘
```

## 核心类图

```
┌─────────────────────────────────────────────────────────────┐
│                   Type4MeInputMethodService                  │
├─────────────────────────────────────────────────────────────┤
│ - currentEngine: ASREngine                                  │
│ - speechRecognizer: SpeechRecognizer?                       │
│ - sherpaEngine: SherpaOnnxEngine?                           │
│ - voskEngine: VoskEngine?                                   │
│ - composeRecognizedText: String                             │
│ - composeIsRecording: Boolean                               │
├─────────────────────────────────────────────────────────────┤
│ + onCreateInputView(): View                                 │
│ + startRecording()                                          │
│ + stopRecording()                                           │
│ + toggleEngine()                                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
┌─────────────────┐ ┌─────────────┐ ┌─────────────────┐
│   VoskEngine    │ │SherpaOnnx   │ │Google Speech    │
├─────────────────┤ │  Engine     │ │  Recognizer     │
│ - model: Model  │ ├─────────────┤ ├─────────────────┤
│ - recognizer    │ │ - sherpa:    │ │ - recognizer    │
├─────────────────┤ │   SherpaOnnx│ ├─────────────────┤
│ + initialize()  │ ├─────────────┤ │ + startListening│
│ + startRecording│ │ + initialize│ │ + stopListening │
│ + stopRecording │ │ + startRec()│ └─────────────────┘
│ - processAudio()│ │ + stopRec() │
└─────────────────┘ └─────────────┘
```

## 状态管理

### Compose State 设计

```kotlin
// 输入法服务中的状态（可变状态）
private var composeRecognizedText by mutableStateOf("")
private var composeStatusText by mutableStateOf("")
private var composeIsRecording by mutableStateOf(false)
private var composePermissionGranted by mutableStateOf(false)
private var composeEngineStatus by mutableStateOf("初始化中...")

// 传递给 KeyboardScreen（单向数据流）
KeyboardScreen(
    isRecording = composeIsRecording,        // 录音状态
    recognizedText = composeRecognizedText,  // 识别结果
    statusText = composeStatusText,          // 状态提示
    permissionGranted = composePermissionGranted,
    currentEngine = currentEngine,           // 当前引擎
    engineStatus = composeEngineStatus,      // 引擎状态
    // ... 回调函数
)
```

### 状态流转

```
                    ┌─────────────────┐
         ┌─────────►│   Idle (空闲)    │
         │          │ 未录音状态      │
         │          └────────┬────────┘
         │                   │ 点击麦克风
         │                   ▼
         │          ┌─────────────────┐
         │          │  Recording (录音)│◄──────┐
         │          │ 显示脉冲动画    │       │
         │          └────────┬────────┘       │
         │                   │                │
         │    ┌──────────────┼───────────────┘
         │    │              │ 识别中
         │    │              ▼
         │    │     ┌─────────────────┐
         │    │     │  Recognizing    │
         │    │     │  (识别中)        │
         │    │     └────────┬────────┘
         │    │              │
         │    │              ▼
         │    │     ┌─────────────────┐
         └────┴────►│   Result (结果)  │
                   │ 显示识别文字    │
                   └─────────────────┘
```

## 引擎对比

| 特性 | Vosk | SherpaOnnx | Google |
|------|------|------------|--------|
| **运行模式** | 离线 | 离线 | 在线 |
| **中文支持** | 优秀 | 良好 | 良好 |
| **模型大小** | ~40MB | ~100MB | 无 |
| **响应速度** | 快 | 快 | 依赖网络 |
| **中国可用** | ✅ | ✅ | ❌ |
| **首次使用** | 需下载模型 | 需下载模型 | 即用 |
| **准确率** | 高 | 高 | 高 |

## 关键设计决策

### 1. 为什么选择 Compose 而非传统 View 系统？

- **声明式 UI**：更简洁的代码，状态驱动界面更新
- **动画支持**：内置动画 API，实现脉冲效果简单
- **主题系统**：Material3 组件，现代化外观
- **性能**：IME 窗口生命周期复杂，Compose 自动处理重组

### 2. 为什么默认使用 Vosk 而非 Google SpeechRecognizer？

- **中国可用性**：Google 服务在中国大陆不可用
- **隐私**：纯离线，语音数据不上传
- **稳定性**：不受网络状况影响
- **成本**：无 API 调用费用

### 3. 引擎初始化策略

```kotlin
// 延迟初始化，避免 onCreate 阻塞
handler.postDelayed({
    sherpaEngine = SherpaOnnxEngine(this)
    voskEngine = VoskEngine(this)
    initGoogleSpeechRecognizer()
}, 100)
```

**原因**：
- IME 创建频繁，快速启动是关键
- 模型加载可能耗时，不应阻塞主线程
- 用户首次可能不需要语音功能

### 4. ComposeView 生命周期

```kotlin
setViewCompositionStrategy(
    ViewCompositionStrategy.DisposeOnDetachedFromWindow
)
```

**原因**：IME 窗口频繁 attach/detach，需要正确释放 Compose 资源，避免内存泄漏。

## 性能优化

### 1. 音频缓冲区设计

```kotlin
private val bufferSize = AudioRecord.getMinBufferSize(
    SAMPLE_RATE,
    AudioFormat.CHANNEL_IN_MONO,
    AudioFormat.ENCODING_PCM_16BIT
) * 2  // 双倍缓冲区避免溢出
```

### 2. 协程作用域

```kotlin
private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
```

- `SupervisorJob`：子协程失败不影响其他
- `Dispatchers.Main`：UI 更新在主线程
- 音频处理在 IO 线程（VoskEngine 内部）

### 3. 状态更新批量处理

识别结果通过 Flow 收集，避免频繁 UI 刷新：

```kotlin
scope.launch {
    voskEngine?.partialResult?.collect { text ->
        if (text.isNotEmpty()) {
            composeRecognizedText = text
        }
    }
}
```

## 安全考虑

### 1. 权限检查

```kotlin
private val hasRecordPermission: Boolean
    get() = try {
        applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }
```

### 2. 异常处理

所有引擎操作都包裹 try-catch，避免崩溃：

```kotlin
try {
    speechRecognizer?.startListening(intent)
} catch (e: Exception) {
    // 降级到离线引擎
    currentEngine = ASREngine.SHERPA_ONNX_OFFLINE
    startSherpaRecording()
}
```

## 扩展性设计

### 添加新的 ASR 引擎

1. 实现引擎接口：
```kotlin
interface ASREngine {
    fun initialize(): Boolean
    fun startRecording(): Boolean
    fun stopRecording()
    fun isModelReady(): Boolean
}
```

2. 在 `Type4MeInputMethodService.ASREngine` 枚举中添加新类型

3. 在 `toggleEngine()` 中添加切换逻辑

4. 在 `startRecording()` 中添加启动逻辑

## 调试技巧

### 1. 查看 IME 日志

```bash
adb logcat -s Type4MeIME:D *:S
```

### 2. 检查输入法状态

```bash
# 列出所有输入法
adb shell ime list -a

# 列出已启用输入法
adb shell ime list -e

# 查看当前默认输入法
adb shell settings get secure default_input_method
```

### 3. 强制停止输入法

```bash
adb shell am force-stop com.type4me
```

### 4. 模拟输入法切换

```bash
adb shell ime set com.type4me/.ime.Type4MeInputMethodService
```
