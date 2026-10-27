package com.eva.ai.data.audio

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.File
import com.eva.ai.domain.logic.VoiceRecordingPolicy

data class RecordedAudio(val bytes: ByteArray, val durationSeconds: Int)

class EvaAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    @Volatile private var durationLimitReached = false
    private var maxSeconds = VoiceRecordingPolicy.DEFAULT_MAX_SECONDS

    fun start(maxDurationSeconds: Int = VoiceRecordingPolicy.DEFAULT_MAX_SECONDS) {
        cancel()
        maxSeconds = VoiceRecordingPolicy.maxSeconds(maxDurationSeconds)
        durationLimitReached = false
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
            setMaxDuration(maxSeconds * 1000)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) durationLimitReached = true
            }
            setOutputFile(file.absolutePath)
            prepare()
            start()
        } } catch (error: Exception) { cancel(); throw error }
    }

    fun stop(): RecordedAudio {
        val activeRecorder = recorder ?: throw IllegalStateException("Recording has not started.")
        val file = outputFile ?: throw IllegalStateException("Recording file is missing.")
        recorder = null
        outputFile = null
        try {
            activeRecorder.stop()
        } catch (error: RuntimeException) {
            // The platform may already have stopped at its hard duration limit.
            if (!durationLimitReached) {
                file.delete()
                throw IllegalStateException("Hold the mic a little longer before sending.", error)
            }
        } finally {
            activeRecorder.release()
        }
        return try {
            val metadata = MediaMetadataRetriever()
            val durationMillis = try {
                metadata.setDataSource(file.absolutePath)
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: throw IllegalStateException("Could not read that recording. Please try again.")
            } finally { metadata.release() }
            check(durationMillis > 0 && durationMillis <= maxSeconds * 1000L + 500L) {
                "Voice notes can be up to $maxSeconds seconds. Please record a shorter note."
            }
            val bytes = file.readBytes()
            check(bytes.isNotEmpty()) { "The recording was empty. Please try again." }
            RecordedAudio(bytes, (durationMillis / 1000L).toInt().coerceIn(1, maxSeconds))
        } finally { file.delete() }
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


