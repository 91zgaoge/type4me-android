# Type4Me Android

一个 Android 语音输入法应用，通过输入法模式将语音实时转换为文字。支持离线和在线语音识别，无需 Google Play 服务即可在中国使用。

## 功能特性

### 核心功能
- **语音输入** - 三引擎支持：Vosk 离线 + SherpaOnnx 离线 + Google 在线识别
- **输入法模式** - 标准 Android IME，点击麦克风录音，识别后上屏
- **离线优先** - 默认使用 Vosk 离线引擎，中国用户无需 Google Play 服务
- **引擎切换** - 支持手动切换语音引擎
- **现代化 UI** - Jetpack Compose 界面，脉冲动画效果

### ASR 引擎对比

| 引擎 | 类型 | 中文支持 | 适用场景 |
|------|------|----------|----------|
| **Vosk** | 离线 | ✅ 优秀 | 中国大陆用户（默认） |
| **SherpaOnnx** | 离线 | ✅ 良好 | 备用离线方案 |
| **Google** | 在线 | ✅ 良好 | 海外用户 |

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3
- **输入法**: InputMethodService + ComposeView
- **本地 ASR**: Vosk (Android), SherpaOnnx
- **在线 ASR**: Google SpeechRecognizer
- **依赖注入**: Hilt
- **异步**: Kotlin Coroutines + Flow
- **日志**: Timber

## 项目结构

```
app/src/main/java/com/type4me/
├── ime/
│   ├── Type4MeInputMethodService.kt    # 输入法核心服务
│   └── ui/
│       └── KeyboardScreen.kt            # Compose 键盘界面
├── core/data/local/
│   ├── VoskEngine.kt                    # Vosk 离线语音识别
│   └── SherpaOnnxEngine.kt              # SherpaOnnx 语音识别
├── settings/
│   └── SettingsActivity.kt              # 设置界面
├── ui/theme/
│   ├── Color.kt                         # 颜色定义（渐变配色）
│   ├── Theme.kt                         # 主题配置
│   └── Type.kt                          # 字体配置
└── Type4MeApplication.kt                # Application 类

app/src/main/res/
├── xml/
│   └── method.xml                       # 输入法配置
├── values/
│   ├── colors.xml                       # 颜色资源
│   └── themes.xml                       # 主题资源
└── drawable/
    └── ic_launcher.xml                  # 应用图标
```

## 安装指南

### 方法一：ADB 安装（推荐）

```bash
# 1. 安装 APK
adb install -r app-debug.apk

# 2. 启用输入法
adb shell ime enable com.type4me/.ime.Type4MeInputMethodService

# 3. 设置为当前输入法
adb shell ime set com.type4me/.ime.Type4MeInputMethodService
```

### 方法二：手动安装

1. 下载 APK 并安装
2. 系统设置 → 语言和输入法 → 虚拟键盘 → 管理键盘
3. 启用 "Type4Me"
4. 在输入框点击 → 切换输入法 → 选择 Type4Me

### 系统要求

- Android 10+ (API 29)
- 麦克风权限
- 存储权限（用于下载离线模型）

## 使用指南

### 首次使用

1. **授予权限** - 首次使用需授予麦克风和存储权限
2. **下载模型** - 点击"需下载"按钮下载 Vosk 中文模型（约 40MB）
3. **开始录音** - 点击大麦克风按钮开始语音识别

### Vosk 模型下载

模型下载地址：https://alphacephei.com/vosk/models

推荐下载：`vosk-model-small-cn-0.22`（约 40MB，中文识别效果好）

**手动安装模型步骤**：
1. 下载模型压缩包
2. 解压到手机存储：`/Android/data/com.type4me/files/vosk-model-small-cn-0.22/`
3. 重启应用

### 界面说明

```
┌─────────────────────────┐
│       Type4Me           │  ← 标题栏
├─────────────────────────┤
│ 🔴 需要麦克风权限        │  ← 状态卡片
│ Vosk · 需下载           │  ← 引擎状态
├─────────────────────────┤
│        [🎤]             │  ← 麦克风按钮（脉冲动画）
├─────────────────────────┤
│ 识别结果将显示在这里     │  ← 结果展示区
├─────────────────────────┤
│ [上屏] [设置] [关闭]    │  ← 操作按钮
└─────────────────────────┘
```

## 开发指南

### 构建项目

```bash
# 编译验证
./gradlew :app:compileDebugKotlin

# 构建 Debug APK
./gradlew :app:assembleDebug

# APK 输出位置
app/build/outputs/apk/debug/app-debug.apk
```

### 发布流程

根据 `.claude/memory/type4me-release-flow.md` 自动执行：

```bash
# 1. 更新版本号 (app/build.gradle.kts)
versionCode = 3
versionName = "1.2.0"

# 2. 构建 APK
./gradlew :app:assembleDebug

# 3. 提交代码
git add -A
git commit -m "版本更新说明"
git push origin master

# 4. 创建 GitHub Release
gh release create v1.2.0 \
  --title "Type4Me v1.2.0 - 全新界面设计" \
  --notes "发布说明" \
  app/build/outputs/apk/debug/app-debug.apk
```

## 核心模块说明

### 1. Type4MeInputMethodService

输入法核心服务，负责：
- 生命周期管理 (`onCreate`, `onCreateInputView`, `onDestroy`)
- 语音识别控制 (开始/停止录音)
- 引擎切换逻辑
- 文本上屏 (`currentInputConnection.commitText`)

### 2. VoskEngine

纯离线语音识别引擎：
- 无需网络，完全本地运行
- 支持中文语音识别
- 模型约 40MB
- 自动检测模型是否已下载

### 3. KeyboardScreen

Jetpack Compose 键盘界面：
- 响应式设计，自动适配屏幕宽度
- 录音脉冲动画
- 状态实时更新
- Material3 组件

## 故障排查

### 问题1：无法调出输入界面

**排查步骤**：
```bash
# 1. 检查输入法是否已注册
adb shell ime list -a | grep type4me

# 2. 查看崩溃日志
adb logcat -s Type4MeIME:D AndroidRuntime:E *:S

# 3. 检查是否已启用
adb shell ime list -e | grep type4me
```

**常见原因**：
- 主题配置问题（已修复为透明主题）
- ComposeView 生命周期问题（已添加 DisposeOnDetachedFromWindow）
- 输入法权限未授予

### 问题2：语音识别失败

**原因**：未下载 Vosk 模型文件

**解决**：点击状态卡片上的"需下载"按钮，或手动下载模型

### 问题3：麦克风无反应

**排查**：
1. 检查麦克风权限是否已授予
2. 检查是否有其他应用占用麦克风
3. 尝试切换引擎（Vosk → Sherpa → Google）

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| v1.2.0 | 2024-03-30 | 全新 Compose 界面，三引擎支持，Vosk 离线 ASR |
| v1.0.1 | 2024-03-29 | 基础功能实现，SherpaOnnx 离线识别 |

## 致谢

- [Vosk](https://github.com/alphacep/vosk-api) - 离线语音识别引擎
- [SherpaOnnx](https://github.com/k2-fsa/sherpa-onnx) - 本地语音识别引擎

## License

MIT License
