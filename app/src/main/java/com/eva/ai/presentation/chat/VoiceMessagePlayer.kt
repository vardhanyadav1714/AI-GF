package com.eva.ai.presentation.chat

import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.eva.ai.data.audio.audioExtensionForMime
import com.eva.ai.data.remote.MeriGfApi
import com.eva.ai.domain.model.ChatMessage
import com.eva.ai.presentation.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

// A claim invalidates older downloads so they cannot unexpectedly start playback.
internal object VoicePlayback {
    private var active: ExoPlayer? = null
    private val requests = PlaybackRequestGate()
    fun claim(): Long { active?.pause(); active = null; return requests.next() }
    fun start(player: ExoPlayer, request: Long) {
        if (!requests.isCurrent(request)) return
        active = player
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
    }
    fun stop() { claim() }
    fun release(player: ExoPlayer) { if (active === player) stop() }
}

@Composable
internal fun VoiceMessagePlayer(message: ChatMessage, api: MeriGfApi?, senderLabel: String? = null, timestamp: String? = null) {
    var expanded by remember(message.id) { mutableStateOf(false) }
    val hasTranscript = message.text.isNotBlank() && message.text != "Voice note"
    Column(Modifier.fillMaxWidth().then(if (hasTranscript) Modifier.clickable(
        role = Role.Button,
        onClickLabel = if (expanded) "Hide text" else "Show text",
        onClick = { expanded = !expanded }
    ) else Modifier)) {
        VoiceAudioControls(
            sourceKey = message.id + message.mediaPath,
            title = senderLabel ?: if (message.fromUser) "Voice message" else "Voice reply",
            seconds = message.voiceSeconds,
            mimeType = message.audioMimeType,
            available = message.audioBase64.isNotBlank() || message.mediaPath.isNotBlank(),
            preload = message.audioBase64.isNotBlank(),
            load = {
                if (message.audioBase64.isNotBlank()) Base64.decode(message.audioBase64, Base64.DEFAULT)
                else requireNotNull(api).mediaBytes(message.mediaPath)
            }
        )
        if (hasTranscript) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (message.fromUser) "Transcript" else "Reply", Modifier.weight(1f), color = evaMuted(), fontSize = 11.sp, lineHeight = 14.sp)
                timestamp?.let { Text(it, color = evaMuted(), fontSize = 10.sp, lineHeight = 12.sp) }
                Spacer(Modifier.width(6.dp))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null, tint = evaMuted(), modifier = Modifier.size(18.dp))
            }
            if (expanded) Text(message.text, Modifier.padding(top = 6.dp), color = evaText(), fontSize = 14.sp, lineHeight = 21.sp)
        } else if (timestamp != null) {
            Text(timestamp, Modifier.align(Alignment.End), color = evaMuted(), fontSize = 10.sp, lineHeight = 12.sp)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun VoiceAudioControls(
    sourceKey: String,
    title: String,
    seconds: Int,
    mimeType: String,
    available: Boolean = true,
    preload: Boolean = false,
    enabled: Boolean = true,
    load: suspend () -> ByteArray
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val player = remember(sourceKey) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
            setHandleAudioBecomingNoisy(true)
        }
    }
    var file by remember(sourceKey) { mutableStateOf<File?>(null) }
    var loading by remember(sourceKey) { mutableStateOf(false) }
    var ready by remember(sourceKey) { mutableStateOf(false) }
    var playing by remember(sourceKey) { mutableStateOf(false) }
    var failed by remember(sourceKey) { mutableStateOf(false) }
    var duration by remember(sourceKey) { mutableLongStateOf(0) }
    var position by remember(sourceKey) { mutableLongStateOf(0) }
    var speed by remember(sourceKey) { mutableFloatStateOf(1f) }
    var seek by remember(sourceKey) { mutableStateOf<Float?>(null) }
    DisposableEffect(player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                loading = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    ready = true
                    duration = player.duration.coerceAtLeast(0)
                }
                if (state == Player.STATE_ENDED) position = duration
            }
            override fun onPlayerError(error: PlaybackException) {
                failed = true; ready = false; loading = false
                VoicePlayback.release(player)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { VoicePlayback.stop(); player.pause() }
        }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            VoicePlayback.release(player)
            player.release()
            file?.delete()
        }
    }
    LaunchedEffect(player, playing) {
        while (playing) { position = player.currentPosition; delay(100) }
    }
    val prepare: suspend (Long?) -> Unit = { request ->
        loading = true
        failed = false
        try {
            val bytes = withContext(Dispatchers.IO) { load() }
            require(bytes.isNotEmpty())
            val local = File.createTempFile("eva-audio-", ".${audioExtensionForMime(mimeType)}", context.cacheDir)
            file?.delete()
            file = local
            withContext(Dispatchers.IO) { local.writeBytes(bytes) }
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(local)))
            player.prepare()
            if (request != null && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) VoicePlayback.start(player, request)
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { loading = false; failed = true; ready = false; file?.delete() }
    }
    LaunchedEffect(sourceKey) { if (preload && available) prepare(null) }
    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            enabled = enabled && available && !loading,
            onClick = {
                if (player.isPlaying && !failed) player.pause()
                else {
                    val request = VoicePlayback.claim()
                    if (ready && !failed) VoicePlayback.start(player, request)
                    else scope.launch { prepare(request) }
                }
            },
            modifier = Modifier.size(48.dp).border(1.dp, EvaColors.Pink.copy(alpha = 0.24f), CircleShape),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = EvaColors.Pink, contentColor = EvaColors.Ink,
                disabledContainerColor = EvaColors.Pink.copy(alpha = 0.12f), disabledContentColor = evaMuted()
            )
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = evaMuted())
            else Icon(if (failed) Icons.Rounded.Refresh else if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (failed) "Retry audio" else if (playing) "Pause audio" else "Play audio")
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = evaText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Only the seek track is compact; command buttons retain their touch targets.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Slider(
                value = seek ?: position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                onValueChange = { seek = it },
                onValueChangeFinished = { seek?.let { player.seekTo(it.toLong()); position = it.toLong() }; seek = null },
                valueRange = 0f..duration.coerceAtLeast(1).toFloat(), enabled = enabled && ready && duration > 0,
                modifier = Modifier.fillMaxWidth().height(20.dp),
                colors = SliderDefaults.colors(thumbColor = EvaColors.Pink, activeTrackColor = EvaColors.Pink),
                thumb = {
                    androidx.compose.foundation.layout.Box(Modifier.size(12.dp).then(
                        Modifier.background(if (enabled && ready) EvaColors.Pink else evaMuted(), androidx.compose.foundation.shape.CircleShape)
                    ))
                },
                track = { state ->
                    SliderDefaults.Track(sliderState = state, modifier = Modifier.height(4.dp),
                        colors = SliderDefaults.colors(activeTrackColor = EvaColors.Pink, inactiveTrackColor = evaBorder()),
                        enabled = enabled && ready, drawStopIndicator = null, thumbTrackGapSize = 0.dp)
                }
            )
            }
            Text(when {
                failed -> "Couldn't play. Tap to retry."
                loading -> "Loading..."
                duration > 0 -> "${audioTime((position / 1000).toInt())} / ${audioTime((duration / 1000).toInt())}"
                seconds > 0 -> audioTime(seconds)
                available -> "Voice audio"
                else -> "Audio unavailable"
            }, color = evaMuted(), fontSize = 11.sp, lineHeight = 14.sp)
        }
        TextButton(enabled = enabled && ready, onClick = {
            speed = when (speed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }
            player.setPlaybackSpeed(speed)
        }, modifier = Modifier.size(48.dp).background(EvaColors.Pink.copy(alpha = 0.08f), CircleShape),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = EvaColors.Pink)) {
            Text(if (speed == 1.5f) "1.5x" else "${speed.toInt()}x", fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

internal fun audioTime(seconds: Int): String = String.format(Locale.US, "%d:%02d", seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)
