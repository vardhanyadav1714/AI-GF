package com.eva.ai.data.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.File

class EvaAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start() {
        cancel()
        val file = File.createTempFile("eva-voice-", ".m4a", context.cacheDir)
        val nextRecorder = createMediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(64_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        outputFile = file
        recorder = nextRecorder
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
        val bytes = file.readBytes()
        file.delete()
        return bytes
    }

    fun cancel() {
        val activeRecorder = recorder
        recorder = null
        runCatching { activeRecorder?.stop() }
        activeRecorder?.release()
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

fun playBase64Audio(context: Context, base64Audio: String, mimeType: String) {
    if (base64Audio.isBlank()) return
    runCatching {
        playAudioBytes(context, Base64.decode(base64Audio, Base64.DEFAULT), mimeType)
    }
}

fun playAudioBytes(context: Context, audioBytes: ByteArray, mimeType: String) {
    if (audioBytes.isEmpty()) return
    runCatching {
        val file = File.createTempFile(
            "eva-reply-",
            ".${audioExtensionForMime(mimeType)}",
            context.cacheDir
        )
        file.writeBytes(audioBytes)
        MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { player -> player.start() }
            setOnCompletionListener { player ->
                player.release()
                file.delete()
            }
            setOnErrorListener { player, _, _ ->
                player.release()
                file.delete()
                true
            }
            prepareAsync()
        }
    }
}

fun audioExtensionForMime(mimeType: String): String =
    when {
        mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
        mimeType.contains("ogg") || mimeType.contains("opus") -> "ogg"
        mimeType.contains("wav") -> "wav"
        else -> "m4a"
    }


