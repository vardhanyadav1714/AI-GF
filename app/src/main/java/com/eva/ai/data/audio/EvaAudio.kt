package com.eva.ai.data.audio

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import java.io.File

class EvaAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start() {
        cancel()
        val file = File.createTempFile("eva-voice-", ".m4a", context.cacheDir)
        outputFile = file
        val nextRecorder = createMediaRecorder(context)
        recorder = nextRecorder
        try { nextRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(64_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        } } catch (error: Exception) { cancel(); throw error }
    }

    fun stop(): ByteArray {
        val activeRecorder = recorder ?: throw IllegalStateException("Recording has not started.")
        val file = outputFile ?: throw IllegalStateException("Recording file is missing.")
        recorder = null
        outputFile = null
        try {
            activeRecorder.stop()
        } catch (error: RuntimeException) {
            file.delete()
            throw IllegalStateException("Hold the mic a little longer before sending.", error)
        } finally {
            activeRecorder.release()
        }
        return try { file.readBytes() } finally { file.delete() }
    }

    fun level(): Float = (runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0) / 32767f).coerceIn(0f, 1f)

    fun cancel() {
        val activeRecorder = recorder
        recorder = null
        runCatching { activeRecorder?.stop() }
        runCatching { activeRecorder?.release() }
        outputFile?.delete()
        outputFile = null
    }
}

fun createMediaRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        legacyMediaRecorder()
    }

@Suppress("DEPRECATION")
fun legacyMediaRecorder(): MediaRecorder = MediaRecorder()

fun openExternalUrl(context: Context, url: String, onFailure: () -> Unit) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        onFailure()
    }
}

fun audioExtensionForMime(mimeType: String): String =
    when {
        mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
        mimeType.contains("ogg") || mimeType.contains("opus") -> "ogg"
        mimeType.contains("wav") -> "wav"
        else -> "m4a"
    }


