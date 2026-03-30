# Changelog

所有 notable 变更都会记录在此文件。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [1.2.0] - 2024-03-30

### 新增
- 全新 Jetpack Compose 界面设计
  - 现代化渐变配色方案（蓝紫主色调）
  - 大麦克风按钮带脉冲动画效果
  - 状态卡片实时显示权限和引擎状态
  - 引擎切换 Chip（Vosk / Sherpa / Google）
- Vosk 离线语音识别引擎支持
  - 纯离线运行，无需 Google Play 服务
  - 中文语音识别支持
  - 自动检测模型状态
- ComposeView 生命周期管理
- 输入法主题透明背景支持

### 修复
- 修复输入法界面无法显示的问题
- 修复 InputMethodService.Insets 类型引用错误
- 添加延迟引擎初始化避免 onCreate 崩溃
- 添加 FrameLayout 包装确保键盘高度正确

### 变更
- 默认引擎从 Google 在线改为 Vosk 离线（适配中国用户）
- 更新主题配置支持 Compose

## [1.0.1] - 2024-03-29

### 新增
- 基础输入法功能实现
- SherpaOnnx 离线语音识别
- Google SpeechRecognizer 在线识别
- 设置界面
- 麦克风权限处理
- 自动更新功能

## [1.0.0] - 2024-03

### 新增
- 输入法模式基础框架
- Google SpeechRecognizer 在线语音识别
- SherpaOnnx 离线语音识别支持
- 语音输入 → 识别 → 上屏完整流程
- 多 LLM 提供商支持（OpenAI、Gemini、Claude）
- 统一的 LLM 客户端抽象层
- 自动降级策略（主提供商失败 → Gemini 免费版）
- Prompt 模板系统（支持 `{text}`、`{clipboard}` 变量）
- 变量注入防护（模板内容转义）
- 设置界面框架
- API Key 配置界面
- Hilt 依赖注入
- DataStore 本地存储

### 技术债务
- SherpaOnnx 模型动态下载待实现
- 流式 LLM 输出待添加
- 实时流式 ASR 待优化

## [Unreleased]

### 计划
- 添加更多 ASR 引擎选项
- 支持自定义 SherpaOnnx 模型
- 添加语音激活词检测
- 优化离线模型加载速度
- 添加暗黑模式支持
