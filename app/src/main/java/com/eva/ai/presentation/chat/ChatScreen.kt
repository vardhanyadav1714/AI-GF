package com.eva.ai.presentation.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.relocation.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.eva.ai.EvaNotificationCenter
import com.eva.ai.R
import com.eva.ai.data.audio.*
import com.eva.ai.data.remote.*
import com.eva.ai.data.settings.*
import com.eva.ai.domain.logic.*
import com.eva.ai.domain.model.*
import com.eva.ai.presentation.*
import com.eva.ai.presentation.auth.*
import com.eva.ai.presentation.chat.*
import com.eva.ai.presentation.components.*
import com.eva.ai.presentation.home.*
import com.eva.ai.presentation.premium.*
import com.eva.ai.presentation.settings.*
import com.eva.ai.ui.theme.AICompanionTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import kotlin.math.max

@Composable
fun ChatScreen(controller: EvaAppController, scope: CoroutineScope) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val companion = controller.selectedCompanion
    val listState = rememberLazyListState()
    var loadingAudioId by remember { mutableStateOf<String?>(null) }
    val imeBottom = WindowInsets.ime.getBottom(density)
    val recorder = remember(context) { EvaAudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var voicePreview by remember { mutableStateOf<VoiceRecordingPreview?>(null) }
    var composerFocused by remember { mutableStateOf(false) }
    val hapticTick = rememberEvaHaptic()

    DisposableEffect(controller.selectedConversationId) {
        EvaNotificationCenter.setActiveConversation(controller.selectedConversationId)
        val eventJob = scope.launch {
            EvaNotificationCenter.openConversations().collect {
                controller.reloadCurrentConversation()
            }
        }
        onDispose {
            EvaNotificationCenter.setActiveConversation(null)
            eventJob.cancel()
        }
    }

    val chatMessages = controller.messages.toList()
    val keyboardVisible = imeBottom > 0
    val tailSpace = when {
        voicePreview != null || recording -> 30.dp
        keyboardVisible || composerFocused -> 44.dp
        else -> 14.dp
    }
    val chatListItemCount = (if (controller.chatsLoading) 1 else 0) +
        chatMessages.size +
        chatMessages.dateGroupCount() +
        1
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                recorder.start()
                voicePreview = null
                recordingStartedAt = System.currentTimeMillis()
                recordingSeconds = 0
                recording = true
                hapticTick()
            }.onFailure { error ->
                controller.notice = error.cleanMessage("Could not start recording.")
            }
        } else {
            controller.notice = "Microphone permission is needed for voice messages."
        }
    }
    val startRecording: () -> Unit = {
        when {
            controller.sending -> controller.notice = "Wait for ${companion.name} to finish replying first."
            controller.shouldOpenPaywallBeforeSend() -> Unit
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                runCatching {
                    recorder.start()
                    voicePreview = null
                    recordingStartedAt = System.currentTimeMillis()
                    recordingSeconds = 0
                    recording = true
                    hapticTick()
                }.onFailure { error ->
                    controller.notice = error.cleanMessage("Could not start recording.")
                }
            }

            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val finishRecording: () -> Unit = {
        val audioBytes = runCatching { recorder.stop() }
            .onFailure { error ->
                controller.notice = error.cleanMessage("Could not save that recording.")
            }
            .getOrNull()
        recording = false
        if (audioBytes != null) {
            hapticTick()
            voicePreview = VoiceRecordingPreview(
                audioBytes = audioBytes,
                mimeType = "audio/mp4",
                durationSeconds = max(1, ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt())
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    LaunchedEffect(recording) {
        while (recording) {
            recordingSeconds = max(1, ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt())
            delay(250)
        }
    }

    LaunchedEffect(
        controller.messages.size,
        controller.chatsLoading,
        controller.sending,
        recording,
        voicePreview != null,
        composerFocused,
        imeBottom
    ) {
        if (chatMessages.isEmpty()) return@LaunchedEffect

        delay(if (composerFocused) 280 else 90)
        listState.animateScrollToItem(max(0, chatListItemCount - 1))
    }

    val submitVoicePreview: () -> Unit = {
        val preview = voicePreview
        if (preview != null) {
            voicePreview = null
            focusManager.clearFocus()
            scope.launch {
                controller.sendVoiceNote(
                    audioBytes = preview.audioBytes,
                    mimeType = preview.mimeType,
                    voiceSeconds = preview.durationSeconds
                )?.let { result ->
                    playBase64Audio(context, result.audioBase64, result.audioMimeType)
                }
            }
        }
    }

    EvaPage(backgroundImage = companion.imageRes) {
        Column(Modifier.fillMaxSize()) {
            ChatHeader(
                companion = companion,
                live = controller.backendLive,
                onBack = { controller.activeTab = EvaTab.Home },
                onReport = {
                    scope.launch {
                        controller.reportLastAssistantReply()
                    }
                }
            )
            controller.freeMessagesRemaining?.let { remaining ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (remaining > 0) "$remaining free message${if (remaining == 1) "" else "s"} left" else "Your free messages are used", modifier = Modifier.weight(1f), color = evaMuted(), fontSize = 13.sp)
                    TextButton(onClick = { controller.premiumOpen = true }, enabled = !controller.sending) { Text("View Premium") }
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (controller.chatsLoading) {
                    item(key = "messages-loading") {
                        ChatSkeleton()
                    }
                }
                if (!controller.chatsLoading && chatMessages.isEmpty()) {
                    item(key = "chat-empty") {
                        ChatEmptyState(
                            companion = companion,
                            onStarter = { text ->
                                scope.launch { controller.sendMessage(text) }
                            }
                        )
                    }
                }
                var previousDateLabel: String? = null
                chatMessages.forEach { message ->
                    val dateLabel = formatDateChip(message.createdAtMillis)
                    if (dateLabel != previousDateLabel) {
                        item(key = "date-${dateLabel}-${message.id}") {
                            DateChip(dateLabel)
                        }
                        previousDateLabel = dateLabel
                    }
                    item(key = message.id) {
                        Box(Modifier.animateItem()) {
                            val popScale = remember(message.id) { Animatable(0.94f) }
                            LaunchedEffect(popScale) {
                                popScale.animateTo(
                                    1f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                            Box(
                                Modifier.graphicsLayer {
                                    scaleX = popScale.value
                                    scaleY = popScale.value
                                }
                            ) {
                                MessageBubble(
                                    message = message,
                                    companion = companion,
                                    api = controller.api,
                                    onPlayAudio = { audioMessage ->
                                        if (audioMessage.audioBase64.isNotBlank()) {
                                            playBase64Audio(
                                                context = context,
                                                base64Audio = audioMessage.audioBase64,
                                                mimeType = audioMessage.audioMimeType.ifBlank { "audio/mp4" }
                                            )
                                        } else if (audioMessage.mediaPath.isNotBlank() && loadingAudioId == null) {
                                            scope.launch {
                                                loadingAudioId = audioMessage.id
                                                try {
                                                    val bytes = controller.api.mediaBytes(audioMessage.mediaPath)
                                                    playAudioBytes(context, bytes, audioMessage.audioMimeType)
                                                } catch (error: CancellationException) { throw error }
                                                catch (_: Exception) { controller.notice = "Voice recording could not be loaded. Please try again." }
                                                finally { loadingAudioId = null }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                item(key = "chat-tail-space") {
                    Spacer(Modifier.height(tailSpace))
                }
            }
            ChatComposer(
                draft = controller.draft,
                sending = controller.sending,
                recording = recording,
                recordingSeconds = recordingSeconds,
                voicePreview = voicePreview,
                onDraftChange = { controller.draft = it },
                onInputFocusChange = { composerFocused = it },
                onSend = {
                    focusManager.clearFocus()
                    scope.launch { controller.sendMessage() }
                },
                onVoiceRecord = {
                    if (recording) finishRecording() else startRecording()
                },
                onVoiceReplay = {
                    voicePreview?.let { preview ->
                        playAudioBytes(context, preview.audioBytes, preview.mimeType)
                    }
                },
                onVoiceDelete = { voicePreview = null },
                onVoiceSend = submitVoicePreview
            )
        }
    }
}

@Composable
fun ChatHeader(
    companion: CompanionProfile,
    live: Boolean,
    onBack: () -> Unit,
    onReport: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconGlassButton(
            icon = Icons.Rounded.ArrowBackIosNew,
            description = "Back to chats",
            onClick = onBack,
            size = 42.dp
        )
        Spacer(Modifier.width(8.dp))
        GradientAvatar(imageRes = companion.imageRes, size = 44.dp)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(companion.name, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (live) "Live" else "Disconnected",
                    color = if (live) EvaColors.Green.copy(alpha = 0.9f) else evaMuted(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                PulsingDot(
                    color = if (live) EvaColors.Green else evaMuted(),
                    size = 7.dp,
                    pulse = live
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box {
            IconGlassButton(
                icon = Icons.Rounded.MoreVert,
                description = "Chat options",
                onClick = { menuOpen = true },
                size = 42.dp
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = if (isEvaLight()) Color.White else EvaColors.InkHigh,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 3.dp
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Report reply", color = evaText(), fontWeight = FontWeight.Bold)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = EvaColors.Coral)
                    },
                    onClick = {
                        menuOpen = false
                        onReport()
                    }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    companion: CompanionProfile,
    api: MeriGfApi? = null,
    onPlayAudio: (ChatMessage) -> Unit = {}
) {
    val fromUser = message.fromUser

    if (!fromUser) {
        // Modern AI-chat layout: assistant replies are plain text sitting
        // directly on the backdrop — no bubble, no per-message avatar.
        // Only voice replies keep a small card, because a player needs a
        // surface to live on.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            if (api != null && message.mediaMimeType.startsWith("image/")) StoredChatImage(message, api)
            when (message.kind) {
                MessageKind.Voice -> GlassCard(
                    padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    radius = 18.dp
                ) {
                    VoiceNoteBubble(
                        seconds = message.voiceSeconds,
                        fromUser = false,
                        companionName = companion.name,
                        canPlay = message.audioBase64.isNotBlank() || message.mediaPath.isNotBlank(),
                        onPlay = { onPlayAudio(message) }
                    )
                }

                MessageKind.Text -> if (message.text.isBlank()) {
                    TypingIndicator(modifier = Modifier.padding(vertical = 6.dp))
                } else {
                    Text(
                        text = message.text,
                        color = evaText(),
                        fontSize = 15.5.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(end = 28.dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                formatTime(message.createdAtMillis),
                color = evaMuted().copy(alpha = 0.7f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        return
    }

    // User messages keep a compact gradient bubble, aligned right.
    val userShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 20.dp,
        bottomEnd = 6.dp
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(userShape)
                .background(EvaColors.Gradient)
                .padding(start = 15.dp, top = 11.dp, end = 15.dp, bottom = 9.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (api != null && message.mediaMimeType.startsWith("image/")) StoredChatImage(message, api)
            when (message.kind) {
                MessageKind.Voice -> VoiceNoteBubble(
                    seconds = message.voiceSeconds,
                    fromUser = true,
                    companionName = companion.name,
                    canPlay = message.audioBase64.isNotBlank() || message.mediaPath.isNotBlank(),
                    onPlay = { onPlayAudio(message) }
                )

                MessageKind.Text -> Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                formatTime(message.createdAtMillis),
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StoredChatImage(message: ChatMessage, api: MeriGfApi) {
    var bitmap by remember(message.mediaPath) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(message.mediaPath) { mutableStateOf(false) }
    var attempt by remember(message.mediaPath) { mutableStateOf(0) }
    LaunchedEffect(message.mediaPath, attempt) {
        failed = false
        try {
            val bytes = api.mediaBytes(message.mediaPath)
            bitmap = withContext(Dispatchers.IO) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                require(options.outWidth > 0 && options.outHeight > 0)
                options.inJustDecodeBounds = false
                options.inSampleSize = 1
                while (options.outWidth / options.inSampleSize > 1024 || options.outHeight / options.inSampleSize > 1024) options.inSampleSize *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap() ?: error("Invalid image")
            }
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { failed = true }
    }
    val image = bitmap
    if (image != null) Image(image, contentDescription = "Chat attachment", modifier = Modifier.widthIn(max = 280.dp).heightIn(max = 320.dp), contentScale = ContentScale.Fit)
    else if (failed) IconButton(onClick = { attempt++ }) { Icon(Icons.Rounded.Refresh, contentDescription = "Retry loading image") }
    else CircularProgressIndicator(modifier = Modifier.size(24.dp))
}

@Composable
fun VoiceNoteBubble(
    seconds: Int,
    fromUser: Boolean,
    companionName: String,
    canPlay: Boolean,
    onPlay: () -> Unit
) {
    val iconColor = if (fromUser) Color.White else EvaColors.Pink
    val textColor = if (fromUser) Color.White else evaText()
    val label = if (fromUser) "Voice sent" else "$companionName audio"
    val meta = if (fromUser) "Delivered" else "Tap to listen"
    Row(
        modifier = Modifier
            .width(224.dp)
            .clickable(enabled = canPlay, onClick = onPlay),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (fromUser) Color.White.copy(alpha = 0.22f)
                    else EvaColors.Pink.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = iconColor)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (canPlay) EvaColors.Green else textColor.copy(alpha = 0.35f))
                )
            }
            Spacer(Modifier.height(7.dp))
            AudioLevelBars(color = textColor.copy(alpha = if (fromUser) 0.88f else 0.72f))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDuration(seconds),
                    color = textColor.copy(alpha = 0.74f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    meta,
                    color = textColor.copy(alpha = 0.58f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            if (fromUser) Icons.Rounded.CheckCircle else Icons.Rounded.VolumeUp,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AudioLevelBars(color: Color) {
    val bars = listOf(8, 13, 18, 11, 20, 15, 9, 17, 12, 19, 10)
    Row(
        modifier = Modifier.height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        bars.forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun ChatComposer(
    draft: String,
    sending: Boolean,
    recording: Boolean,
    recordingSeconds: Int,
    voicePreview: VoiceRecordingPreview?,
    onDraftChange: (String) -> Unit,
    onInputFocusChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    onVoiceRecord: () -> Unit,
    onVoiceReplay: () -> Unit,
    onVoiceDelete: () -> Unit,
    onVoiceSend: () -> Unit
) {
    var emojiPickerOpen by remember { mutableStateOf(false) }
    var inputFocused by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val composerScope = rememberCoroutineScope()
    val hapticTick = rememberEvaHaptic()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .animateContentSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, top = 4.dp, end = 14.dp, bottom = 8.dp)
    ) {
        AnimatedVisibility(visible = emojiPickerOpen) {
            PickerStrip(
                items = listOf(
                    "\uD83D\uDC96", "\uD83D\uDC95", "\uD83E\uDD70", "\uD83D\uDE0D", "\uD83D\uDE18", "\uD83D\uDE0A",
                    "\uD83E\uDD7A", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE02", "\uD83D\uDE48", "\uD83E\uDEE3",
                    "\u2728", "\uD83D\uDD25", "\uD83C\uDF39", "\uD83D\uDC8B", "\uD83E\uDD17", "\uD83C\uDF89",
                    "\uD83D\uDC4D", "\uD83D\uDE4F", "\uD83D\uDCAF", "\uD83C\uDF19", "\u2B50", "\u2615"
                ),
                onPick = { emoji ->
                    onDraftChange(draft + emoji)
                }
            )
        }
        val composerBorder by animateColorAsState(
            targetValue = when {
                inputFocused -> EvaColors.Pink.copy(alpha = 0.55f)
                isEvaLight() -> Color.Black.copy(alpha = 0.08f)
                else -> Color.White.copy(alpha = 0.09f)
            },
            animationSpec = tween(durationMillis = 220),
            label = "composer-border"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (isEvaLight()) Color.White.copy(alpha = 0.9f)
                    else Color(0xFF17121F).copy(alpha = 0.94f)
                )
                .border(BorderStroke(1.dp, composerBorder), RoundedCornerShape(28.dp))
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            when {
                voicePreview != null -> VoicePreviewComposer(
                    preview = voicePreview,
                    sending = sending,
                    onReplay = onVoiceReplay,
                    onDelete = onVoiceDelete,
                    onSend = onVoiceSend
                )

                recording -> RecordingComposer(
                    seconds = recordingSeconds,
                    onStop = onVoiceRecord
                )

                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            // Drop text focus so the keyboard and the emoji
                            // panel never fight for the bottom of the screen.
                            if (!emojiPickerOpen) focusManager.clearFocus()
                            emojiPickerOpen = !emojiPickerOpen
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SentimentSatisfiedAlt,
                            contentDescription = "Emoji",
                            tint = evaMuted()
                        )
                    }
                    TextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier
                            .weight(1f)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusEvent { focusState ->
                                inputFocused = focusState.isFocused
                                onInputFocusChange(focusState.isFocused)
                                if (focusState.isFocused) {
                                    composerScope.launch {
                                        delay(260)
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                        minLines = 1,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = evaText(),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        ),
                        placeholder = {
                            Text("Message...", color = evaMuted().copy(alpha = 0.62f))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (!sending && draft.trim().isNotEmpty()) onSend() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    FilledIconButton(
                        modifier = Modifier.size(50.dp), enabled = !sending,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = EvaColors.Action, contentColor = Color.White,
                            disabledContainerColor = EvaColors.Action, disabledContentColor = Color.White),
                        onClick = {
                                if (draft.trim().isNotEmpty()) {
                                    hapticTick()
                                    onSend()
                                } else {
                                    onVoiceRecord()
                                }
                            }
                    ) {
                        if (sending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(if (draft.trim().isNotEmpty()) Icons.Rounded.Send else Icons.Rounded.Mic,
                            contentDescription = if (draft.trim().isNotEmpty()) "Send message" else "Record voice message")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingComposer(seconds: Int, onStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(EvaColors.Danger.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(EvaColors.Danger)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recording", color = evaText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(formatDuration(seconds), color = EvaColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(4.dp))
            AnimatedWaveform(color = EvaColors.Danger.copy(alpha = 0.82f), height = 20.dp)
        }
        Button(
            onClick = onStop, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 92.dp),
            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = EvaColors.Action, contentColor = Color.White)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun VoicePreviewComposer(
    preview: VoiceRecordingPreview,
    sending: Boolean,
    onReplay: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(EvaColors.Pink.copy(alpha = 0.14f))
                .clickable(enabled = !sending, onClick = onReplay),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Replay", tint = EvaColors.Pink)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (sending) "Sending voice" else "Voice ready",
                color = evaText(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedWaveform(color = EvaColors.Pink.copy(alpha = 0.74f), height = 20.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    formatDuration(preview.durationSeconds),
                    color = evaMuted(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        IconButton(onClick = onDelete, enabled = !sending) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete recording", tint = evaMuted())
        }
        FilledIconButton(
            onClick = onSend, enabled = !sending, modifier = Modifier.size(50.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = EvaColors.Action, contentColor = Color.White,
                disabledContainerColor = EvaColors.Action, disabledContentColor = Color.White)
        ) {
            if (sending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(
                Icons.Rounded.Send,
                contentDescription = "Send recording",
                tint = Color.White
            )
        }
    }
}

@Composable
fun PickerStrip(items: List<String>, onPick: (String) -> Unit) {
    val hapticTick = rememberEvaHaptic()
    Column(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.chunked(6).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val interactionSource = rememberEvaInteractionSource()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .pressScale(interactionSource = interactionSource, pressedScale = 0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(evaGlass())
                            .border(BorderStroke(1.dp, evaBorder()), RoundedCornerShape(16.dp))
                            .clickable(interactionSource = interactionSource, indication = null) {
                                hapticTick()
                                onPick(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

/** Welcoming empty state with one-tap ice-breakers for fresh conversations. */
@Composable
fun ChatEmptyState(companion: CompanionProfile, onStarter: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(EvaColors.Gradient)
                .padding(3.dp)
        ) {
            Image(
                painter = painterResource(companion.imageRes),
                contentDescription = companion.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Say hi to ${companion.name}",
            color = evaText(),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Break the ice with one of these",
            color = evaMuted(),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        listOf(
            "Hey ${companion.name}! How was your day?",
            "I missed you",
            "Tell me something interesting"
        ).forEach { starter ->
            val interactionSource = rememberEvaInteractionSource()
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(interactionSource = interactionSource, pressedScale = 0.97f)
                    .clickable(interactionSource = interactionSource, indication = null) { onStarter(starter) },
                padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                radius = 18.dp
            ) {
                Text(starter, color = evaText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Shimmering skeleton shown while history loads, in the shape of real bubbles. */
@Composable
fun ChatSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonRow(isUser = false, widthFraction = 0.62f)
        SkeletonRow(isUser = true, widthFraction = 0.46f)
        SkeletonRow(isUser = false, widthFraction = 0.5f)
    }
}

@Composable
private fun SkeletonRow(isUser: Boolean, widthFraction: Float) {
    if (isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                Modifier
                    .fillMaxWidth(widthFraction)
                    .height(40.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 6.dp
                        )
                    )
                    .evaShimmer()
            )
        }
    } else {
        // Assistant skeleton mirrors the plain-text layout: bare text bars,
        // no bubble box.
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth(widthFraction)
                    .height(15.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .evaShimmer()
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth(widthFraction * 0.6f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .evaShimmer()
            )
        }
    }
}

private fun List<ChatMessage>.dateGroupCount(): Int {
    var count = 0
    var previousDateLabel: String? = null
    forEach { message ->
        val dateLabel = formatDateChip(message.createdAtMillis)
        if (dateLabel != previousDateLabel) {
            count += 1
            previousDateLabel = dateLabel
        }
    }
    return count
}
