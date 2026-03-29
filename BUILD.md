# 构建指南

## 环境要求

- JDK 17+
- Android SDK (API 29-34)
- Android Studio (推荐) 或命令行工具

## 本地构建步骤

### 1. 克隆项目

```bash
git clone https://github.com/joewongjc/type4me-android.git
cd type4me-android
```

### 2. 配置 Android SDK

设置环境变量：

```bash
export ANDROID_HOME=/path/to/android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

或在 `local.properties` 文件中指定：

```properties
sdk.dir=/path/to/android/sdk
```

### 3. 构建 Release APK

```bash
./gradlew assembleRelease
```

APK 输出路径：`app/build/outputs/apk/release/app-release.apk`

### 4. 构建 Debug APK

```bash
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 签名配置

Release 版本需要配置签名。在 `app/build.gradle.kts` 中添加：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "type4me"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## 常见问题

### Gradle 下载失败

如果首次构建时下载 Gradle 失败，可手动下载并放置到：
- Linux/Mac: `~/.gradle/wrapper/dists/`
- Windows: `%USERPROFILE%\.gradle\wrapper\dists\`

### Android SDK 缺失组件

运行以下命令安装所需组件：

```bash
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
```

### 内存不足

在 `gradle.properties` 中添加：

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

## CI/CD 构建

### GitHub Actions

项目已配置 `.github/workflows/build.yml`，推送标签时自动构建：

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Docker 构建

```bash
docker run --rm -v $(pwd):/project -w /project \
  -e ANDROID_HOME=/opt/android-sdk \
  thyrlian/android-sdk \
  ./gradlew assembleRelease
```

## 安装 APK

### 通过 ADB

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 手动安装

1. 将 APK 传输到手机
2. 允许"安装未知来源应用"
3. 点击 APK 安装

## 发布到 Google Play

1. 生成签名密钥：
```bash
keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias type4me
```

2. 构建 App Bundle：
```bash
./gradlew bundleRelease
```

3. 在 Google Play Console 上传 `app/build/outputs/bundle/release/app-release.aab`
