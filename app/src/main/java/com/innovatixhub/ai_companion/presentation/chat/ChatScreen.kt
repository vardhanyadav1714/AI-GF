package com.eva.ai.presentation.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.eva.ai.R
import com.eva.ai.data.audio.*
import com.eva.ai.data.remote.*
import com.eva.ai.data.settings.*
import com.eva.ai.domain.logic.*
import com.eva.ai.domain.model.*
import com.eva.ai.presentation.*
import com.eva.ai.presentation.auth.*
import com.eva.ai.presentation.call.*
import com.eva.ai.presentation.chat.*
import com.eva.ai.presentation.components.*
import com.eva.ai.presentation.home.*
import com.eva.ai.presentation.memories.*
import com.eva.ai.presentation.premium.*
import com.eva.ai.presentation.settings.*
import com.eva.ai.ui.theme.AICompanionTheme
import kotlinx.coroutines.CoroutineScope
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
    val companion = controller.selectedCompanion
    val listState = rememberLazyListState()
    val recorder = remember(context) { EvaAudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var voicePreview by remember { mutableStateOf<VoiceRecordingPreview?>(null) }
    var composerFocused by remember { mutableStateOf(false) }
    val chatMessages = controller.messages.toList()
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
        composerFocused
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
                onCall = { controller.callOpen = true },
                onReport = {
                    scope.launch {
                        controller.reportLastAssistantReply()
                    }
                }
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (controller.chatsLoading) {
                    item(key = "messages-loading") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = EvaColors.Pink,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
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
                        MessageBubble(
                            message = message,
                            companion = companion,
                            onPlayAudio = { audioMessage ->
                                if (audioMessage.audioBase64.isNotBlank()) {
                                    playBase64Audio(
                                        context = context,
                                        base64Audio = audioMessage.audioBase64,
                                        mimeType = audioMessage.audioMimeType.ifBlank { "audio/mp4" }
                                    )
                                }
                            }
                        )
                    }
                }
                item(key = "chat-tail-space") {
                    Spacer(Modifier.height(2.dp))
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
                onAttachment = controller::sendAttachment,
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
    onCall: () -> Unit,
    onReport: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconGlassButton(
            icon = Icons.Rounded.ArrowBackIosNew,
            onClick = onBack,
            size = 42.dp
        )
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(companion.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(companion.name, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (live) "Live" else "Demo",
                    color = evaMuted(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(5.dp))
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EvaColors.Green)
                )
            }
        }
        IconGlassButton(icon = Icons.Rounded.Call, onClick = onCall, size = 42.dp)
        Spacer(Modifier.width(6.dp))
        Box {
            IconGlassButton(
                icon = Icons.Rounded.MoreVert,
                onClick = { menuOpen = true },
                size = 42.dp
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = evaGlass()
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
    onPlayAudio: (ChatMessage) -> Unit = {}
) {
    val fromUser = message.fromUser
    val bubbleTextColor = if (fromUser) Color.White else evaText()
    val quietBubbleColor = if (isEvaLight()) {
        Color.White.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!fromUser) {
            Image(
                painter = painterResource(companion.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier.fillMaxWidth(if (fromUser) 0.78f else 0.84f),
            contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (fromUser) 18.dp else 5.dp,
                            bottomEnd = if (fromUser) 5.dp else 18.dp
                        )
                    )
                    .then(
                        if (fromUser) {
                            Modifier.background(EvaColors.Gradient)
                        } else {
                            Modifier
                                .background(quietBubbleColor)
                                .border(
                                    BorderStroke(1.dp, evaBorder()),
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = 5.dp,
                                        bottomEnd = 18.dp
                                    )
                                )
                        }
                    )
                    .padding(start = 16.dp, top = 13.dp, end = 16.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                when (message.kind) {
                    MessageKind.Voice -> VoiceNoteBubble(
                        seconds = message.voiceSeconds,
                        fromUser = fromUser,
                        companionName = companion.name,
                        canPlay = message.audioBase64.isNotBlank(),
                        onPlay = { onPlayAudio(message) }
                    )

                    MessageKind.Attachment -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AttachFile,
                            contentDescription = null,
                            tint = bubbleTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            message.text,
                            color = bubbleTextColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    MessageKind.Text -> Text(
                        text = if (message.text.isBlank()) "Typing..." else message.text,
                        color = if (message.text.isBlank()) evaMuted() else bubbleTextColor,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = if (fromUser) FontWeight.Bold else FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    formatTime(message.createdAtMillis),
                    color = if (fromUser) Color.White.copy(alpha = 0.62f) else evaMuted(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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
                Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
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
    onAttachment: (String) -> Unit,
    onVoiceRecord: () -> Unit,
    onVoiceReplay: () -> Unit,
    onVoiceDelete: () -> Unit,
    onVoiceSend: () -> Unit
) {
    var attachmentPickerOpen by remember { mutableStateOf(false) }
    var emojiPickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .animateContentSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 10.dp)
    ) {
        AnimatedVisibility(visible = emojiPickerOpen) {
            PickerStrip(
                items = listOf(
                    "\uD83D\uDC95",
                    "\uD83D\uDC96",
                    "\uD83D\uDE0A",
                    "\uD83D\uDE0D",
                    "\uD83E\uDD70",
                    "\u2728",
                    "\uD83D\uDE18",
                    "\uD83E\uDD17"
                ),
                onPick = { emoji ->
                    onDraftChange(draft + emoji)
                    emojiPickerOpen = false
                }
            )
        }
        AnimatedVisibility(visible = attachmentPickerOpen) {
            AttachmentStrip(
                onPicked = {
                    onAttachment(it)
                    attachmentPickerOpen = false
                }
            )
        }
        GlassCard(
            padding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
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
                    IconButton(onClick = { emojiPickerOpen = !emojiPickerOpen }) {
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
                            .onFocusEvent { onInputFocusChange(it.isFocused) },
                        minLines = 1,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = evaText(),
                            fontWeight = FontWeight.Bold
                        ),
                        placeholder = {
                            Text("Type a message...", color = evaMuted().copy(alpha = 0.62f))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { attachmentPickerOpen = !attachmentPickerOpen }) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = "Attach", tint = evaMuted())
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EvaColors.Gradient)
                            .clickable(enabled = !sending) {
                                if (draft.trim().isNotEmpty()) onSend() else onVoiceRecord()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                sending -> Icons.Rounded.Send
                                draft.trim().isNotEmpty() -> Icons.Rounded.Send
                                else -> Icons.Rounded.Mic
                            },
                            contentDescription = null,
                            tint = Color.White
                        )
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
            .height(66.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53945).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53945))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recording", color = evaText(), fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(formatDuration(seconds), color = EvaColors.Coral, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(7.dp))
            AudioLevelBars(color = EvaColors.Coral.copy(alpha = 0.82f))
        }
        Box(
            modifier = Modifier
                .height(46.dp)
                .width(98.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFE53945), EvaColors.Coral)))
                .clickable(onClick = onStop),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Finish recording",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Done", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
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
            .height(66.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
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
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AudioLevelBars(color = EvaColors.Pink.copy(alpha = 0.74f))
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
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(EvaColors.Gradient)
                .clickable(enabled = !sending, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Send,
                contentDescription = "Send recording",
                tint = Color.White
            )
        }
    }
}

@Composable
fun PickerStrip(items: List<String>, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            GlassCard(
                modifier = Modifier
                    .size(52.dp)
                    .clickable { onPick(item) },
                padding = PaddingValues(0.dp),
                radius = 18.dp
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(item, fontSize = 25.sp)
                }
            }
        }
    }
}

@Composable
fun AttachmentStrip(onPicked: (String) -> Unit) {
    val items = listOf(
        "Photo" to Icons.Rounded.Photo,
        "Camera" to Icons.Rounded.CameraAlt,
        "File" to Icons.Rounded.InsertDriveFile,
        "Location" to Icons.Rounded.LocationOn
    )
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, icon) ->
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .clickable { onPicked(label) },
                padding = PaddingValues(8.dp),
                radius = 18.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(icon, contentDescription = label, tint = EvaColors.Pink)
                    Spacer(Modifier.height(5.dp))
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
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

