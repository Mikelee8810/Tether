package com.relationshipradar.app.connectors.callrecorder

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class WhisperModelStatus(val installed: Boolean, val valid: Boolean, val bytes: Long)

class WhisperModelManager(private val context: Context) {
    val modelFile: File get() = File(context.filesDir, "models/$MODEL_NAME")

    suspend fun status(): WhisperModelStatus = withContext(Dispatchers.IO) {
        val file = modelFile
        WhisperModelStatus(file.isFile, file.isFile && file.length() == MODEL_BYTES && sha1(file) == MODEL_SHA1, file.takeIf(File::isFile)?.length() ?: 0)
    }

    fun install() = OneTimeWorkRequestBuilder<WhisperModelDownloadWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
        .also { WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, it) }

    fun remove() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        modelFile.delete()
        File(modelFile.parentFile, "$MODEL_NAME.download").delete()
    }

    companion object {
        const val MODEL_NAME = "ggml-tiny.en.bin"
        const val MODEL_BYTES = 77_704_715L
        const val MODEL_SHA1 = "c78c86eb1a8faa21b369bcd33207cc90d64ae9df"
        const val MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin"
        const val WORK_NAME = "install_whisper_tiny_en"

        fun sha1(file: File): String {
            val digest = MessageDigest.getInstance("SHA-1")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(256 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

class WhisperModelDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val manager = WhisperModelManager(applicationContext)
        val target = manager.modelFile
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "${WhisperModelManager.MODEL_NAME}.download")
        runCatching {
            val connection = URL(WhisperModelManager.MODEL_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.connect()
            check(connection.responseCode in 200..299) { "Download failed (${connection.responseCode})" }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: WhisperModelManager.MODEL_BYTES
            connection.inputStream.buffered().use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        setProgress(Data.Builder().putInt("percent", (downloaded * 100 / total).toInt().coerceIn(0, 99)).build())
                    }
                }
            }
            connection.disconnect()
            check(partial.length() == WhisperModelManager.MODEL_BYTES) { "Model download is incomplete" }
            check(WhisperModelManager.sha1(partial) == WhisperModelManager.MODEL_SHA1) { "Model integrity check failed" }
            check(partial.renameTo(target)) { "Could not finish model installation" }
            setProgress(Data.Builder().putInt("percent", 100).build())
            Result.success()
        }.getOrElse {
            partial.delete()
            Result.failure(Data.Builder().putString("error", it.message ?: "Download failed").build())
        }
    }
}
