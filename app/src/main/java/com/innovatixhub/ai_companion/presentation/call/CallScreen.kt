package com.eva.ai.presentation.call

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
import android.text.format.DateFormat
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
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

@Composable
fun CallScreen(
    controller: EvaAppController,
    scope: CoroutineScope,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val companion = controller.selectedCompanion
    val recorder = remember(context) { EvaAudioRecorder(context) }
    var callSeconds by remember { mutableIntStateOf(0) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var speaker by remember { mutableStateOf(true) }
    val callStatus = when {
        controller.sending -> "${companion.name} is replying"
        recording -> "Listening to you"
        else -> "Live voice call"
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                recorder.start()
                recordingStartedAt = System.currentTimeMillis()
                recordingSeconds = 0
                recording = true
            }.onFailure { error ->
                controller.notice = error.cleanMessage("Could not start recording.")
            }
        } else {
            controller.notice = "Microphone permission is needed for calls."
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
                controller.notice = error.cleanMessage("Could not send that recording.")
            }
            .getOrNull()
        recording = false
        if (audioBytes != null) {
            scope.launch {
                controller.sendVoiceNote(
                    audioBytes = audioBytes,
                    voiceSeconds = max(1, ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt()),
                    stayInCall = true
                )?.let { result ->
                    playBase64Audio(context, result.audioBase64, result.audioMimeType)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            callSeconds += 1
        }
    }

    LaunchedEffect(recording) {
        while (recording) {
            recordingSeconds = max(1, ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt())
            delay(250)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(companion.imageRes),
            contentDescription = "${companion.name} call",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconGlassButton(
                    icon = Icons.Rounded.ArrowBackIosNew,
                    onClick = onClose,
                    color = Color.White
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(companion.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(formatDuration(callSeconds), color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp)
                }
                IconGlassButton(
                    icon = if (controller.sending) Icons.Rounded.GraphicEq else Icons.Rounded.RecordVoiceOver,
                    onClick = {},
                    color = if (controller.sending) EvaColors.Pink else Color.White
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CallAvatar(companion = companion, active = recording || controller.sending)
                Spacer(Modifier.height(20.dp))
                Text(companion.name, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                CallStatusPill(status = callStatus, active = recording || controller.sending)
                Spacer(Modifier.height(16.dp))
                Text(
                    if (recording) formatDuration(recordingSeconds) else formatDuration(callSeconds),
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                radius = 28.dp,
                glassOverride = Color.Black.copy(alpha = 0.34f),
                borderOverride = Color.White.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallAction(
                        icon = when {
                            controller.sending -> Icons.Rounded.GraphicEq
                            recording -> Icons.Rounded.CheckCircle
                            else -> Icons.Rounded.Mic
                        },
                        label = when {
                            controller.sending -> "Replying"
                            recording -> "Done"
                            else -> "Talk"
                        },
                        active = recording || controller.sending,
                        onClick = {
                            if (recording) finishRecording() else startRecording()
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53945))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CallEnd,
                            contentDescription = "End call",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    CallAction(
                        icon = Icons.Rounded.VolumeUp,
                        label = if (speaker) "Speaker" else "Earpiece",
                        active = speaker,
                        onClick = { speaker = !speaker }
                    )
                }
            }
        }
    }
}

@Composable
fun CallAvatar(companion: CompanionProfile, active: Boolean) {
    Box(
        modifier = Modifier.size(178.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(178.dp)
                .clip(CircleShape)
                .background(
                    if (active) EvaColors.Pink.copy(alpha = 0.16f)
                    else Color.White.copy(alpha = 0.08f)
                )
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .border(
                    BorderStroke(3.dp, if (active) EvaColors.Pink else Color.White.copy(alpha = 0.45f)),
                    CircleShape
                )
        ) {
            Image(
                painter = painterResource(companion.imageRes),
                contentDescription = companion.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CallStatusPill(status: String, active: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), RoundedCornerShape(28.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) EvaColors.Pink else EvaColors.Green)
        )
        Spacer(Modifier.width(8.dp))
        Text(status, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}


