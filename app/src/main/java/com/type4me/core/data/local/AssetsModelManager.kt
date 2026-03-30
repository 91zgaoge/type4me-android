package com.type4me.core.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Assets 模型管理器
 * 负责将打包在 APK assets 中的模型复制到应用可访问的外部存储
 */
class AssetsModelManager(private val context: Context) {

    companion object {
        private const val TAG = "AssetsModelManager"
        private const val ASSETS_MODEL_DIR = "model/vosk-model-small-cn-0.22"
        private const val TARGET_MODEL_DIR = "vosk-model-small-cn-0.22"

        // 模型关键文件，用于验证完整性
        private val REQUIRED_FILES = listOf(
            "am/final.mdl",
            "conf/model.conf",
            "graph/phones/word_boundary.int"
        )
    }

    /**
     * 检查 assets 中是否包含模型
     */
    fun hasModelInAssets(): Boolean {
        return try {
            context.assets.list(ASSETS_MODEL_DIR)?.isNotEmpty() == true
        } catch (e: Exception) {
            Timber.tag(TAG).d("No model found in assets: ${e.message}")
            false
        }
    }

    /**
     * 检查模型是否已复制到外部存储
     */
    fun isModelExtracted(): Boolean {
        val modelDir = File(context.getExternalFilesDir(null), TARGET_MODEL_DIR)
        if (!modelDir.exists() || !modelDir.isDirectory) return false

        // 验证关键文件是否存在
        return REQUIRED_FILES.all { relativePath ->
            File(modelDir, relativePath).exists()
        }
    }

    /**
     * 获取目标模型路径
     */
    fun getModelPath(): String {
        return File(context.getExternalFilesDir(null), TARGET_MODEL_DIR).absolutePath
    }

    /**
     * 将模型从 assets 复制到外部存储
     * @param progressCallback 进度回调 (0-100)
     */
    suspend fun extractModel(progressCallback: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!hasModelInAssets()) {
                Timber.tag(TAG).w("No model found in assets")
                return@withContext false
            }

            val targetDir = File(context.getExternalFilesDir(null), TARGET_MODEL_DIR)
            targetDir.mkdirs()

            // 获取 assets 中所有文件列表
            val files = listAssetsFiles(ASSETS_MODEL_DIR)
            Timber.tag(TAG).d("Found ${files.size} files in assets")

            var copiedCount = 0

            files.forEach { assetPath ->
                val relativePath = assetPath.removePrefix("$ASSETS_MODEL_DIR/")
                val targetFile = File(targetDir, relativePath)

                // 创建父目录
                targetFile.parentFile?.mkdirs()

                // 复制文件
                copyAssetFile(assetPath, targetFile)

                copiedCount++
                val progress = (copiedCount * 100) / files.size
                progressCallback(progress)

                Timber.tag(TAG).d("Copied: $relativePath ($copiedCount/${files.size})")
            }

            // 验证提取的模型
            if (isModelExtracted()) {
                Timber.tag(TAG).d("Model extracted successfully")
                true
            } else {
                Timber.tag(TAG).e("Model extraction verification failed")
                false
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to extract model from assets")
            false
        }
    }

    /**
     * 递归列出 assets 中的所有文件
     */
    private fun listAssetsFiles(path: String): List<String> {
        val files = mutableListOf<String>()

        try {
            context.assets.list(path)?.forEach { item ->
                val fullPath = "$path/$item"
                val subItems = context.assets.list(fullPath)

                if (subItems.isNullOrEmpty()) {
                    // 是文件
                    files.add(fullPath)
                } else {
                    // 是目录，递归
                    files.addAll(listAssetsFiles(fullPath))
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error listing assets: $path")
        }

        return files
    }

    /**
     * 复制单个文件从 assets 到目标
     */
    private fun copyAssetFile(assetPath: String, targetFile: File) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * 删除已提取的模型
     */
    fun clearExtractedModel() {
        val modelDir = File(context.getExternalFilesDir(null), TARGET_MODEL_DIR)
        modelDir.deleteRecursively()
        Timber.tag(TAG).d("Cleared extracted model")
    }

    /**
     * 获取模型提取状态描述
     */
    fun getStatus(): String {
        return when {
            isModelExtracted() -> "模型已就绪"
            hasModelInAssets() -> "需要解压模型（首次使用）"
            else -> "需要下载模型"
        }
    }
}
