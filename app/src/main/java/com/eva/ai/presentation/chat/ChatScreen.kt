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
    var showCompanionProfile by remember(companion.id) { mutableStateOf(false) }
    var reportMessage by remember(companion.id) { mutableStateOf<ChatMessage?>(null) }
    var reportReason by remember { mutableStateOf("unsafe_content") }
    var reportDetails by remember { mutableStateOf("") }
    var reportSending by remember { mutableStateOf(false) }
    reportMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { if (!reportSending) reportMessage = null },
            title = { Text("Report reply") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    listOf("unsafe_content" to "Unsafe or harmful", "sexual_content" to "Sexual content",
                        "minor_safety" to "Child safety", "self_harm" to "Self-harm",
                        "hate_or_harassment" to "Hate or harassment", "wrong_language" to "Wrong language",
                        "other" to "Other").forEach { (value, label) ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = !reportSending) { reportReason = value }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = reportReason == value, onClick = { reportReason = value }, enabled = !reportSending,
                                colors = RadioButtonDefaults.colors(selectedColor = EvaColors.Pink))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                    OutlinedTextField(value = reportDetails, onValueChange = { reportDetails = it.take(1000) },
                        label = { Text("Details (optional)") }, enabled = !reportSending, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = !reportSending, onClick = {
                    if (!reportSending) {
                        reportSending = true
                        scope.launch {
                            try {
                                if (controller.reportAssistantMessage(message, reportReason, reportDetails)) reportMessage = null
                            } finally { reportSending = false }
                        }
                    }
                }) { Text(if (reportSending) "Sending..." else "Send report", color = EvaColors.Pink) }
            },
            dismissButton = { TextButton(enabled = !reportSending, onClick = { reportMessage = null }) { Text("Cancel") } }
        )
    }
    if (showCompanionProfile) CompanionProfileDialog(companion, { showCompanionProfile = false }, { showCompanionProfile = false })
    val listState = rememberLazyListState()
    val imeBottom = WindowInsets.ime.getBottom(density)
    val recorder = remember(context) { EvaAudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recordingLevel by remember { mutableFloatStateOf(0f) }
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
        if (granted && controller.voiceTrialAllowsRecording()) {
            runCatching {
                VoicePlayback.stop()
                recorder.start()
                voicePreview = null
                recordingStartedAt = android.os.SystemClock.elapsedRealtime()
                recordingSeconds = 0
                recording = true
                hapticTick()
            }.onFailure { error ->
                controller.notice = error.cleanMessage("Could not start recording.")
            }
        } else if (!granted) {
            controller.notice = "Microphone permission is needed for voice messages."
        }
    }
    val startRecording: () -> Unit = {
        when {
            controller.sending -> controller.notice = "Wait for ${companion.name} to finish replying first."
            controller.shouldOpenPaywallBeforeSend() -> Unit
            !controller.voiceTrialAllowsRecording() -> Unit
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                runCatching {
                    VoicePlayback.stop()
                    recorder.start()
                    voicePreview = null
                    recordingStartedAt = android.os.SystemClock.elapsedRealtime()
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
                durationSeconds = max(1, ((android.os.SystemClock.elapsedRealtime() - recordingStartedAt) / 1000L).toInt())
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { recorder.cancel(); VoicePlayback.stop() }
    }
    val audioLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(audioLifecycle, companion.id) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && recording) finishRecording()
        }
        audioLifecycle.addObserver(observer)
        onDispose {
            audioLifecycle.removeObserver(observer)
            recorder.cancel()
            recording = false
            voicePreview = null
            VoicePlayback.stop()
        }
    }

    LaunchedEffect(recording) {
        while (recording) {
            recordingSeconds = ((android.os.SystemClock.elapsedRealtime() - recordingStartedAt) / 1000L).toInt()
            recordingLevel = recorder.level()
            delay(100)
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
            val previewCompanionId = companion.id
            VoicePlayback.stop()
            voicePreview = null
            focusManager.clearFocus()
            scope.launch {
                val result = controller.sendVoiceNote(
                    audioBytes = preview.audioBytes,
                    mimeType = preview.mimeType,
                    voiceSeconds = preview.durationSeconds,
                    requestId = preview.requestId
                )
                if (result == null && controller.selectedCompanion.id == previewCompanionId && voicePreview == null) {
                    voicePreview = preview
                }
            }
        }
    }

    EvaPage(backgroundImage = companion.imageRes) {
        Column(Modifier.fillMaxSize()) {
            ChatHeader(
                companion = companion,
                onProfile = { showCompanionProfile = true },
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
                                    onReport = {
                                        reportReason = "unsafe_content"
                                        reportDetails = ""
                                        reportMessage = message
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
                recordingLevel = recordingLevel,
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
                onVoiceCancel = { recorder.cancel(); recording = false; VoicePlayback.stop() },
                onVoiceDelete = { VoicePlayback.stop(); voicePreview = null },
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
    onReport: () -> Unit,
    onProfile: () -> Unit = {}
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
        Box(Modifier.clickable(onClickLabel = "View ${companion.name}'s profile", onClick = onProfile)) {
            GradientAvatar(imageRes = companion.imageRes, size = 44.dp)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f).clickable(onClickLabel = "View companion profile", onClick = onProfile)) {
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
    onReport: (() -> Unit)? = null
) {
    val fromUser = message.fromUser
    val reportable = !fromUser && !message.streaming && message.id.matches(Regex("^[a-fA-F0-9]{24}$")) && onReport != null

    if (message.kind == MessageKind.Voice) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Keep a visible opposite-side gutter, including on narrow phones.
            val bubbleWidth = minOf(240.dp, maxWidth * 0.68f)
            Box(Modifier.fillMaxWidth(), contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
                val shape = RoundedCornerShape(8.dp)
                Column(
                    Modifier.width(bubbleWidth).clip(shape)
                        .background(if (fromUser) EvaColors.Pink.copy(alpha = if (isEvaLight()) 0.10f else 0.16f) else evaGlass())
                        .border(1.dp, if (fromUser) EvaColors.Pink.copy(alpha = 0.28f) else evaBorder(), shape)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    VoiceMessagePlayer(message, api, senderLabel = if (fromUser) "You" else companion.name,
                        timestamp = formatTime(message.createdAtMillis))
                }
                if (reportable) {
                    Box(Modifier.padding(start = bubbleWidth)) { ReplyReportButton(onReport!!) }
                }
            }
        }
        return
    }

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
                    radius = 8.dp
                ) {
                    VoiceMessagePlayer(message, api)
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
            if (reportable) ReplyReportButton(onReport!!)
        }
        return
    }

    val userShape = if (message.kind == MessageKind.Voice) RoundedCornerShape(8.dp) else RoundedCornerShape(
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
                .widthIn(max = if (message.kind == MessageKind.Voice) 344.dp else 300.dp)
                .clip(userShape)
                .background(if (message.kind == MessageKind.Voice) EvaColors.Pink.copy(alpha = if (isEvaLight()) 0.08f else 0.12f) else EvaColors.Pink)
                .border(1.dp, if (message.kind == MessageKind.Voice) EvaColors.Pink.copy(alpha = 0.2f) else Color.Transparent, userShape)
                .padding(start = 15.dp, top = 11.dp, end = 15.dp, bottom = 9.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (api != null && message.mediaMimeType.startsWith("image/")) StoredChatImage(message, api)
            when (message.kind) {
                MessageKind.Voice -> VoiceMessagePlayer(message, api)

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
                color = if (message.kind == MessageKind.Voice) evaMuted() else Color.White.copy(alpha = 0.68f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReplyReportButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.Flag, contentDescription = "Report this reply", tint = evaMuted(), modifier = Modifier.size(16.dp))
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
fun ChatComposer(
    draft: String,
    sending: Boolean,
    recording: Boolean,
    recordingSeconds: Int,
    recordingLevel: Float,
    voicePreview: VoiceRecordingPreview?,
    onDraftChange: (String) -> Unit,
    onInputFocusChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    onVoiceRecord: () -> Unit,
    onVoiceCancel: () -> Unit,
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
                .clip(RoundedCornerShape(if (recording || voicePreview != null) 8.dp else 28.dp))
                .background(
                    if (isEvaLight()) Color.White.copy(alpha = 0.9f)
                    else Color(0xFF17121F).copy(alpha = 0.94f)
                )
                .border(BorderStroke(1.dp, composerBorder), RoundedCornerShape(if (recording || voicePreview != null) 8.dp else 28.dp))
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            when {
                voicePreview != null -> VoicePreviewComposer(
                    preview = voicePreview,
                    sending = sending,
                    onDelete = onVoiceDelete,
                    onSend = onVoiceSend
                )

                recording -> RecordingComposer(
                    seconds = recordingSeconds,
                    level = recordingLevel,
                    onCancel = onVoiceCancel,
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
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = EvaColors.Pink, contentColor = EvaColors.Ink,
                            disabledContainerColor = EvaColors.Pink.copy(alpha = 0.14f), disabledContentColor = evaMuted()),
                        onClick = {
                                if (draft.trim().isNotEmpty()) {
                                    hapticTick()
                                    onSend()
                                } else {
                                    onVoiceRecord()
                                }
                            }
                    ) {
                        if (sending) CircularProgressIndicator(Modifier.size(20.dp), color = EvaColors.Ink, strokeWidth = 2.dp)
                        else Icon(if (draft.trim().isNotEmpty()) Icons.Rounded.Send else Icons.Rounded.Mic,
                            contentDescription = if (draft.trim().isNotEmpty()) "Send message" else "Record voice message")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingComposer(seconds: Int, level: Float, onCancel: () -> Unit, onStop: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel recording", tint = evaMuted())
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(EvaColors.Pink))
                Spacer(Modifier.width(8.dp))
                Text("Recording", color = evaText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { level }, modifier = Modifier.fillMaxWidth().height(4.dp),
                color = EvaColors.Pink, trackColor = EvaColors.Pink.copy(alpha = 0.12f))
            Spacer(Modifier.height(6.dp))
            Text(audioTime(seconds), color = evaMuted(), fontSize = 12.sp)
        }
        FilledIconButton(onClick = onStop, modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = EvaColors.Pink)) {
            Icon(Icons.Rounded.Stop, contentDescription = "Stop and review recording", tint = EvaColors.Ink)
        }
    }
}

@Composable
fun VoicePreviewComposer(
    preview: VoiceRecordingPreview,
    sending: Boolean,
    onDelete: () -> Unit,
    onSend: () -> Unit
) {
    val previewKey = remember(preview) { UUID.randomUUID().toString() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        VoiceAudioControls(sourceKey = previewKey, title = "Your recording", seconds = preview.durationSeconds,
            mimeType = preview.mimeType, preload = true, enabled = !sending, load = { preview.audioBytes })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDelete, enabled = !sending, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Discard recording", tint = evaMuted())
            }
            Spacer(Modifier.weight(1f))
            FilledIconButton(onClick = onSend, enabled = !sending, modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = EvaColors.Pink,
                    disabledContainerColor = EvaColors.Pink.copy(alpha = 0.14f))) {
                if (sending) CircularProgressIndicator(Modifier.size(20.dp), color = EvaColors.Ink, strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Send, contentDescription = "Send recording", tint = EvaColors.Ink)
            }
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
