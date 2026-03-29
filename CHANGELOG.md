# Changelog

所有 notable 变更都会记录在此文件。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

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
