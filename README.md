# Type4Me Android

一个 Android 语音输入 + LLM 文本优化应用。通过输入法模式，将语音实时转换为文字，并支持通过 LLM 进行润色、翻译、总结等智能处理。

## 功能特性

### 核心功能
- **语音输入** - 双引擎支持：Google 在线识别 + SherpaOnnx 离线识别
- **输入法模式** - 标准 Android IME，点击麦克风录音，识别后上屏
- **LLM 优化** - 支持 OpenAI、Gemini、Claude 等多种 LLM 提供商
- **智能降级** - 网络异常时自动切换到 Gemini 免费版
- **上下文变量** - 支持 `{text}`（语音内容）、`{clipboard}`（剪贴板内容）变量

### 支持的 LLM 提供商
- OpenAI (GPT-3.5/4)
- Google Gemini / Gemini 免费版
- Anthropic Claude

### ASR 引擎
- Google SpeechRecognizer（在线，需要网络）
- SherpaOnnx（离线，首次使用时下载模型）

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **数据存储**: DataStore
- **网络**: OkHttp + Gson
- **本地 ASR**: SherpaOnnx

## 项目结构

```
app/src/main/java/com/type4me/
├── Type4MeApplication.kt           # 应用入口
├── core/                           # 核心层
│   ├── domain/                     # 领域模型
│   │   ├── model/                  # 数据模型
│   │   └── repository/             # 仓库接口
│   └── data/                       # 数据层
│       ├── local/                  # 本地存储
│       ├── remote/                 # 远程 API
│       └── repository/             # 仓库实现
├── ime/                            # 输入法模块
│   ├── Type4MeInputMethodService.kt
│   ├── ui/                         # 键盘 UI
│   └── viewmodel/                  # IME ViewModel
├── settings/                       # 设置模块
│   ├── SettingsActivity.kt
│   ├── ui/                         # 设置界面
│   └── viewmodel/                  # 设置 ViewModel
└── util/                           # 工具类
```

## 安装

### 从源码构建

1. 克隆仓库
```bash
git clone https://github.com/joewongjc/type4me-android.git
cd type4me-android
```

2. 使用 Android Studio 打开项目

3. 构建 Release APK
```bash
./gradlew assembleRelease
```

APK 输出路径: `app/build/outputs/apk/release/app-release.apk`

### 系统要求

- Android 10+ (API 29)
- 网络连接（用于在线 ASR 和 LLM 功能）
- 麦克风权限

## 使用指南

### 首次设置

1. 安装应用后，进入系统设置 → 语言和输入法 → 虚拟键盘 → 管理键盘
2. 启用 "Type4Me"
3. 在任意输入框长按 → 切换输入法 → 选择 Type4Me

### 基础使用

1. **录音**: 点击麦克风按钮开始录音
2. **识别**: 录音结束后自动识别文字
3. **上屏**: 点击确认按钮将文字输入到当前输入框
4. **LLM 优化**: 点击"优化"按钮，选择模板进行智能处理

### Prompt 模板

应用内置多种 Prompt 模板：

- **翻译成中文** - 将语音内容翻译成中文
- **润色文字** - 使文字更通顺、专业
- **总结概括** - 提取内容要点
- **智能回复** - 根据剪贴板内容生成回复

支持自定义模板，使用变量：
- `{text}` - 识别的语音内容
- `{clipboard}` - 当前剪贴板内容

### API Key 配置

1. 进入应用设置
2. 选择 LLM 提供商
3. 输入对应的 API Key
4. 点击"测试连接"验证配置

## 隐私说明

- 语音数据仅用于识别，不会上传至第三方（除非使用 Google 在线识别）
- API Key 仅存储在本地设备
- LLM 请求直接发送至对应提供商，不经过中间服务器

## 致谢

- [SherpaOnnx](https://github.com/k2-fsa/sherpa-onnx) - 本地语音识别引擎
- [Type4Me macOS](https://github.com/joewongjc/type4me) - 原版项目灵感

## License

MIT License
