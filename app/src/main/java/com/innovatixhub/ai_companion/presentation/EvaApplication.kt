package com.eva.ai.presentation

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
fun EvaApplication(
    controller: EvaAppController,
    scope: CoroutineScope,
    onGoogleSignIn: () -> Unit,
    onGooglePlaySubscribe: () -> Unit
) {
    AICompanionTheme(darkTheme = !controller.lightMode, dynamicColor = false) {
        CompositionLocalProvider(LocalEvaLightMode provides controller.lightMode) {
            EvaSystemBars(lightMode = controller.lightMode)
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(controller.notice) {
                controller.notice?.let { message ->
                    snackbarHostState.showSnackbar(message)
                    controller.clearNotice()
                }
            }

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { _ ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (val auth = controller.authState) {
                        AuthState.Loading -> LoadingScreen()
                        AuthState.SignedOut -> AuthScreen(
                            busy = controller.authBusy,
                            onGoogleSignIn = onGoogleSignIn,
                            onRequestCode = { name, email, onSent ->
                                scope.launch {
                                    if (controller.requestEmailCode(name, email)) onSent()
                                }
                            },
                            onVerify = { email, code ->
                                scope.launch { controller.verifyEmailCode(email, code) }
                            }
                        )

                        is AuthState.SignedIn -> EvaShell(
                            controller = controller,
                            user = auth.user,
                            scope = scope,
                            onGooglePlaySubscribe = onGooglePlaySubscribe
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EvaSystemBars(lightMode: Boolean) {
    val view = LocalView.current
    val statusColor = if (lightMode) Color(0xFFFFF8FC) else EvaColors.Black
    val navigationColor = if (lightMode) Color.White else EvaColors.Black

    DisposableEffect(lightMode, view) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = statusColor.toArgb()
            window.navigationBarColor = navigationColor.toArgb()
            window.decorView.setBackgroundColor(statusColor.toArgb())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }

            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = lightMode
                isAppearanceLightNavigationBars = lightMode
            }
        }

        onDispose {}
    }
}

@Composable
fun EvaShell(
    controller: EvaAppController,
    user: EvaUser,
    scope: CoroutineScope,
    onGooglePlaySubscribe: () -> Unit
) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = "${controller.activeTab}-${controller.premiumOpen}-${controller.callOpen}-${controller.lightMode}-${controller.selectedCompanion.id}",
            label = "eva-shell"
        ) {
            when {
                controller.callOpen -> CallScreen(
                    controller = controller,
                    scope = scope,
                    onClose = { controller.callOpen = false }
                )
                controller.premiumOpen -> PremiumScreen(
                    subscription = controller.subscriptionState,
                    busy = controller.subscriptionBusy,
                    googlePlayBusy = controller.subscriptionBusy,
                    onBack = { controller.premiumOpen = false },
                    onContinue = {
                        scope.launch {
                            val checkoutUrl = controller.startPremiumSubscription()
                            if (!checkoutUrl.isNullOrBlank()) {
                                openExternalUrl(
                                    context = context,
                                    url = checkoutUrl,
                                    onFailure = { controller.notice = "Could not open Razorpay checkout." }
                                )
                            }
                        }
                    },
                    onRefresh = {
                        scope.launch { controller.refreshSubscription() }
                    },
                    onGooglePlay = onGooglePlaySubscribe
                )

                controller.activeTab == EvaTab.Home -> HomeScreen(
                    user = user,
                    companion = controller.selectedCompanion,
                    onCompanionSelect = controller::selectCompanion,
                    onChat = { controller.activeTab = EvaTab.Chat },
                    onPremium = { controller.premiumOpen = true },
                    onQuickMessage = { text ->
                        scope.launch { controller.sendMessage(text) }
                    },
                    onVoiceNotes = { controller.callOpen = true }
                )

                controller.activeTab == EvaTab.Chat -> ChatScreen(controller, scope)
                controller.activeTab == EvaTab.Profile -> ProfileScreen(
                    controller = controller,
                    user = user,
                    scope = scope
                )
            }
        }

        AnimatedVisibility(
            visible = !controller.premiumOpen &&
                !controller.callOpen &&
                controller.activeTab != EvaTab.Chat,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EvaBottomNav(
                active = controller.activeTab,
                onSelect = { tab ->
                    controller.activeTab = tab
                    controller.premiumOpen = false
                    controller.callOpen = false
                }
            )
        }
    }
}

