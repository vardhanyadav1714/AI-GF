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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
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
    onGooglePlaySubscribe: () -> Unit,
    onRestorePurchases: () -> Unit = {}
) {
    // No explicit override yet: follow the system theme, including live changes.
    val effectiveLightMode = controller.lightMode ?: !isSystemInDarkTheme()
    AICompanionTheme(darkTheme = !effectiveLightMode, dynamicColor = false) {
        CompositionLocalProvider(LocalEvaLightMode provides effectiveLightMode) {
            EvaSystemBars(lightMode = effectiveLightMode)
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(controller.notice) {
                controller.notice?.let { message ->
                    snackbarHostState.showSnackbar(message)
                    controller.clearNotice()
                }
            }

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { scaffoldPadding ->
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(scaffoldPadding)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
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
                            onGooglePlaySubscribe = onGooglePlaySubscribe,
                            onRestorePurchases = onRestorePurchases
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

    DisposableEffect(lightMode, view) {
        val activity = view.context as? ComponentActivity
        val window = activity?.window
        if (window != null) {
            val bars = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !lightMode }
            activity.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            window.decorView.setBackgroundColor(statusColor.toArgb())

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
    onGooglePlaySubscribe: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    val context = LocalContext.current
    var trialAcknowledged by remember(user.id) { mutableStateOf(controller.settingsStore.trialNoticeSeen(user.id)) }
    var profileEditorOpen by rememberSaveable(user.id) { mutableStateOf(false) }
    val freeRemaining = controller.freeMessagesRemaining
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val membershipVisible = controller.premiumOpen || controller.activeTab == EvaTab.Profile
    LaunchedEffect(user.id, lifecycle, membershipVisible) {
        if (membershipVisible) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                if (!controller.subscriptionBusy) controller.refreshSubscription(silent = true)
                delay(30_000)
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (!controller.needsDateOfBirth && !controller.premiumOpen && shouldShowTrialNotice(
                trialAcknowledged, controller.subscriptionState?.active == true, freeRemaining)) {
            fun acknowledgeTrial() {
                controller.settingsStore.acknowledgeTrialNotice(user.id)
                trialAcknowledged = true
            }
            AlertDialog(
                onDismissRequest = { acknowledgeTrial() },
                title = { Text(if (freeRemaining == 10) "Your first 10 messages are free" else "$freeRemaining free messages remaining") },
                text = { Text("Your 10 free messages include one voice note, shared across all companions. Premium has a daily message allowance and separate voice-input and spoken-reply allowances. Check Membership for current limits. No payment starts unless you subscribe.") },
                confirmButton = { TextButton(onClick = { acknowledgeTrial() }) { Text("Start chatting") } }
            )
        }
        if (controller.needsDateOfBirth) {
            DateOfBirthDialog(
                busy = controller.authBusy,
                onSkip = { scope.launch { controller.signOut() } },
                onSave = { iso, name, place, useForBilling ->
                    scope.launch { controller.saveDateOfBirth(iso, name, place, useForBilling) }
                },
                searchPlaces = { query -> controller.searchPlaces(query) }
            )
        }
        AnimatedContent(
            targetState = EvaShellState(
                tab = controller.activeTab,
                premiumOpen = controller.premiumOpen,
                profileEditorOpen = profileEditorOpen,
                lightMode = controller.lightMode ?: !isSystemInDarkTheme(),
                companionId = controller.selectedCompanion.id
            ),
            transitionSpec = {
                // Direction-aware slide: moving down the tab order slides left,
                // moving back up slides right, so navigation feels spatial.
                val forward = targetState.tab.ordinal >= initialState.tab.ordinal
                val slideSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)
                val fadeSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
                if (forward) {
                    (slideInHorizontally(slideSpec) { it / 10 } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(slideSpec) { -it / 10 } + fadeOut(fadeSpec))
                } else {
                    (slideInHorizontally(slideSpec) { -it / 10 } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(slideSpec) { it / 10 } + fadeOut(fadeSpec))
                }
            },
            label = "eva-shell"
        ) { state ->
            when {
                controller.companionEditorOpen -> CustomCompanionScreen(controller, scope)
                state.premiumOpen -> PremiumScreen(
                    subscription = controller.subscriptionState,
                    busy = controller.subscriptionBusy,
                    checkoutBusy = controller.billingBusy(BillingOperation.Checkout),
                    refreshBusy = controller.billingBusy(BillingOperation.Refresh),
                    restoreBusy = controller.billingBusy(BillingOperation.Restore),
                    verifyBusy = controller.billingBusy(BillingOperation.Verify),
                    cancelBusy = controller.billingBusy(BillingOperation.Cancel),
                    onBack = { controller.premiumOpen = false },
                    onContinue = onGooglePlaySubscribe,
                    onRefresh = {
                        scope.launch { controller.refreshSubscription() }
                    },
                    onRestore = onRestorePurchases,
                    onCancel = { scope.launch { controller.cancelPremiumRenewal() } },
                    billingState = controller.billingAdministrativeArea,
                    billingStateRequired = controller.razorpayBillingStateRequired,
                    onBillingStateChanged = { state ->
                        val owner = (controller.authState as? AuthState.SignedIn)?.user?.id
                        if (owner != null && state in com.eva.ai.data.billing.IndiaBillingPolicy.administrativeAreas) {
                            controller.settingsStore.saveBillingState(owner, state)
                            controller.billingAdministrativeArea = state
                        }
                    }
                )

                state.tab == EvaTab.Home -> HomeScreen(
                    user = user,
                    companion = controller.selectedCompanion,
                    onCompanionSelect = controller::selectCompanion,
                    catalog = companionProfiles + controller.customCompanions.filter { !it.archived }.map { it.profile() },
                    onCreateCompanion = { controller.openCompanionEditor() },
                    onEditCompanion = { controller.openCompanionEditor(controller.selectedCompanion.id) },
                    onChat = { controller.activeTab = EvaTab.Chat },
                    onPremium = { controller.premiumOpen = true },
                    onQuickMessage = { text ->
                        scope.launch { controller.sendMessage(text) }
                    }
                )

                state.tab == EvaTab.Chat -> ChatScreen(controller, scope)
                state.tab == EvaTab.Profile && state.profileEditorOpen -> EditProfileScreen(
                    controller = controller,
                    user = user,
                    scope = scope,
                    onBack = { profileEditorOpen = false }
                )
                state.tab == EvaTab.Profile -> ProfileScreen(
                    controller = controller,
                    user = user,
                    scope = scope,
                    onEditProfile = { profileEditorOpen = true }
                )
            }
        }

        AnimatedVisibility(
            visible = !controller.premiumOpen &&
                controller.activeTab != EvaTab.Chat && !profileEditorOpen && !controller.companionEditorOpen,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EvaBottomNav(
                active = controller.activeTab,
                onSelect = { tab ->
                    controller.activeTab = tab
                    controller.premiumOpen = false
                    profileEditorOpen = false
                }
            )
        }
    }
}

/** Immutable snapshot of everything that can swap the shell screen. */
private data class EvaShellState(
    val tab: EvaTab,
    val premiumOpen: Boolean,
    val profileEditorOpen: Boolean,
    val lightMode: Boolean,
    val companionId: String
)
