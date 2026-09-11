package com.eva.ai.presentation.auth

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
fun LoadingScreen() {
    EvaPage {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.model_eva_real_v3),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .border(3.dp, EvaColors.Pink, CircleShape)
                )
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(color = EvaColors.Pink)
            }
        }
    }
}

@Composable
fun AuthScreen(
    busy: Boolean,
    onGoogleSignIn: () -> Unit,
    onRequestCode: (String, String, () -> Unit) -> Unit,
    onVerify: (String, String) -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    val cleanEmail = email.trim()
    val canRequestCode = cleanEmail.contains("@") &&
        cleanEmail.length >= 5 &&
        (mode == AuthMode.Login || name.trim().length >= 2)

    EvaPage(backgroundImage = R.drawable.model_eva_real_v3) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 430.dp)
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(EvaColors.Gradient)
                            .padding(3.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.model_eva_real_v3),
                            contentDescription = "Eva",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = if (mode == AuthMode.Login) "Welcome back" else "Create your account",
                        color = evaText(),
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Eva is waiting for you",
                        color = evaMuted(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 430.dp),
                    padding = PaddingValues(14.dp),
                    radius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AuthModeToggle(
                            mode = mode,
                            enabled = !busy,
                            onModeChange = {
                                mode = it
                                codeSent = false
                                code = ""
                            }
                        )

                        GoogleAuthButton(
                            label = if (busy) "Opening Google..." else "Continue with Google",
                            enabled = !busy,
                            onClick = onGoogleSignIn
                        )

                        AuthDivider()

                        AnimatedContent(
                            targetState = codeSent,
                            label = "email-auth"
                        ) { sent ->
                            if (sent) {
                                CodeVerificationPanel(
                                    email = cleanEmail,
                                    code = code,
                                    busy = busy,
                                    onCodeChange = { code = it.take(8) },
                                    onVerify = { onVerify(cleanEmail, code) },
                                    onBack = {
                                        codeSent = false
                                        code = ""
                                    },
                                    onResend = {
                                        onRequestCode(name, cleanEmail) {
                                            codeSent = true
                                        }
                                    }
                                )
                            } else {
                                EmailEntryPanel(
                                    mode = mode,
                                    name = name,
                                    email = email,
                                    busy = busy,
                                    canRequestCode = canRequestCode,
                                    onNameChange = { name = it },
                                    onEmailChange = { email = it },
                                    onSubmit = {
                                        onRequestCode(name, cleanEmail) {
                                            codeSent = true
                                        }
                                    }
                                )
                            }
                        }

                        Text(
                            text = "Your account is protected with one-time email codes.",
                            color = evaMuted(),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthModeToggle(
    mode: AuthMode,
    enabled: Boolean,
    onModeChange: (AuthMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (isEvaLight()) Color.White.copy(alpha = 0.82f) else Color.Black.copy(alpha = 0.22f))
            .border(1.dp, evaBorder(), RoundedCornerShape(26.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AuthMode.values().forEach { item ->
            val selected = mode == item
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (selected) Modifier.background(EvaColors.Gradient)
                        else Modifier.background(Color.Transparent)
                    )
                    .clickable(enabled = enabled) { onModeChange(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (item == AuthMode.Login) "Login" else "Sign up",
                    color = if (selected) Color.White else evaMuted(),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GoogleAuthButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = if (isEvaLight()) Color.White else Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, if (isEvaLight()) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isEvaLight()) Color(0xFFF7F3F8) else Color.Black.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text("G", color = EvaColors.Pink, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = evaText(),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AuthDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .height(1.dp)
                .weight(1f)
                .background(evaBorder())
        )
        Text(
            "  or continue with email  ",
            color = evaMuted(),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Box(
            Modifier
                .height(1.dp)
                .weight(1f)
                .background(evaBorder())
        )
    }
}

@Composable
fun EmailEntryPanel(
    mode: AuthMode,
    name: String,
    email: String,
    busy: Boolean,
    canRequestCode: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (mode == AuthMode.Login) "Login with email" else "Sign up with email",
            color = evaText(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        AnimatedVisibility(visible = mode == AuthMode.Signup) {
            EvaTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Your name",
                imeAction = ImeAction.Next
            )
        }
        EvaTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email address",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Send,
            onSend = {
                if (canRequestCode && !busy) onSubmit()
            }
        )
        GradientButton(
            icon = Icons.Rounded.Send,
            label = when {
                busy -> "Sending code..."
                mode == AuthMode.Login -> "Send login code"
                else -> "Create account"
            },
            enabled = !busy && canRequestCode,
            onClick = onSubmit
        )
    }
}

@Composable
fun CodeVerificationPanel(
    email: String,
    code: String,
    busy: Boolean,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit,
    onResend: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Enter confirmation code",
                color = evaText(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                email,
                color = evaMuted(),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        EvaTextField(
            value = code,
            onValueChange = onCodeChange,
            label = "6-digit code",
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Send,
            onSend = {
                if (code.length >= 4 && !busy) onVerify()
            }
        )
        GradientButton(
            icon = Icons.Rounded.CheckCircle,
            label = if (busy) "Verifying..." else "Verify code",
            enabled = !busy && code.length >= 4,
            onClick = onVerify
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, enabled = !busy) {
                Text("Change email", color = EvaColors.Pink, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onResend, enabled = !busy) {
                Text("Resend", color = EvaColors.Pink, fontWeight = FontWeight.Bold)
            }
        }
    }
}


