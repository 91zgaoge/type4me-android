# Type4Me 模型打包指南

## 概述

Type4Me 现在支持将 Vosk 语音识别模型打包进 APK。首次启动时，应用会自动将模型从 APK assets 解压到外部存储。

## 模型信息

- **模型名称**: `vosk-model-small-cn-0.22`
- **模型大小**: 约 40MB
- **支持语言**: 中文
- **模型来源**: https://alphacephei.com/vosk/models

## 打包步骤

### 1. 下载模型

```bash
# 下载中文小模型
wget https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip
```

### 2. 解压模型

```bash
unzip vosk-model-small-cn-0.22.zip
```

解压后得到 `vosk-model-small-cn-0.22/` 目录，结构如下：
```
vosk-model-small-cn-0.22/
├── am/
│   └── final.mdl
├── conf/
│   ├── mfcc.conf
│   └── model.conf
├── graph/
│   ├── disambig_tid.int
│   ├── phones/
│   │   ├── align_lexicon.int
│   │   ├── disambig.int
│   │   ├── optional_silence.csl
│   │   ├── optional_silence.int
│   │   ├── rebuild_tree
│   │   ├── sets.int
│   │   ├── sets.txt
│   │   ├── silence.csl
│   │   ├── silence.int
│   │   ├── word_boundary.int
│   │   └── words.txt
│   └── word_boundary.int
├── ivector/
│   ├── final.dubm
│   ├── final.ie
│   ├── final.mat
│   ├── global_cmvn.stats
│   ├── online_cmvn.conf
│   └── splice.conf
└── README
```

### 3. 放置到项目目录

```bash
# 移动模型到 assets 目录
mv vosk-model-small-cn-0.22/* app/src/main/assets/model/vosk-model-small-cn-0.22/
```

确保目录结构为：
```
app/src/main/assets/model/
└── vosk-model-small-cn-0.22/
    ├── am/
    ├── conf/
    ├── graph/
    └── ivector/
```

### 4. 构建 APK

```bash
./gradlew :app:assembleDebug
```

## 代码说明

### AssetsModelManager

负责将模型从 APK assets 复制到外部存储：

```kotlin
val assetsModelManager = AssetsModelManager(context)

// 检查 assets 中是否有模型
if (assetsModelManager.hasModelInAssets()) {
    // 解压模型
    assetsModelManager.extractModel { progress ->
        // 更新进度 UI
    }
}
```

### VoskEngine 集成

```kotlin
// 检查是否需要从 assets 解压
if (voskEngine.needsExtraction()) {
    // 解压模型
    voskEngine.extractModelFromAssets { progress -> }
}

// 检查外部存储中是否有模型
if (voskEngine.isModelInExternalStorage()) {
    // 初始化引擎
    voskEngine.initialize()
}
```

## 使用流程

1. **首次启动**: 检测到 assets 中有模型但外部存储没有，显示"需解压"
2. **点击引擎切换**: 开始解压模型，显示进度（约 5 秒）
3. **解压完成**: 自动初始化 Vosk 引擎
4. **后续启动**: 直接加载外部存储的模型，无需解压

## APK 大小

| 配置 | 大小 |
|------|------|
| 不含模型 | ~20 MB |
| 含模型 | ~60 MB |

## 注意事项

1. **Google Play 限制**: Google Play 限制 APK 大小为 100MB，含模型后约 60MB，仍在限制内
2. **国内分发**: 国内应用商店通常支持更大的 APK，无需担心
3. **模型更新**: 如需更新模型，需要重新打包 APK
4. **存储权限**: 首次启动需要存储权限来解压模型

## 替代方案

如果不想打包模型，可以：

1. 手动下载模型并解压到 `/Android/data/com.type4me/files/vosk-model-small-cn-0.22/`
2. 应用内下载（需要网络权限，中国大陆可能访问困难）
