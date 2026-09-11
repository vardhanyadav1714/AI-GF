package com.eva.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException as GoogleApiException
import com.google.firebase.messaging.FirebaseMessaging
import com.eva.ai.presentation.EvaAppController
import com.eva.ai.presentation.EvaApplication
import com.eva.ai.presentation.components.EvaColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var controller: EvaAppController
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            EvaNotificationCenter.ensureChannels(this)
        } else if (::controller.isInitialized) {
            controller.notice = "Notifications are off. You can enable them later from app settings."
        }
    }
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (!::controller.isInitialized) return@registerForActivityResult

        val accountResult = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(GoogleApiException::class.java)
        }
        accountResult.onSuccess { account ->
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                controller.authBusy = false
                controller.notice = "Google did not return a sign-in token."
            } else {
                lifecycleScope.launch {
                    try {
                        controller.signInWithGoogle(idToken)
                    } finally {
                        controller.authBusy = false
                    }
                }
            }
        }.onFailure {
            controller.authBusy = false
            controller.notice = if (result.resultCode == Activity.RESULT_CANCELED) {
                "Google sign-in was cancelled."
            } else {
                "Google sign-in failed."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = EvaColors.Black.toArgb()
        window.navigationBarColor = EvaColors.Black.toArgb()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setupNotifications()
        handleAuthIntent(intent)

        setContent {
            val controller = remember { controller }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                controller.bootstrap()
            }

            EvaApplication(
                controller = controller,
                scope = scope,
                onGoogleSignIn = {
                    openGoogleSignIn()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::controller.isInitialized) {
            lifecycleScope.launch {
                controller.refreshSubscription(silent = true)
            }
        }
    }

    private fun openGoogleSignIn() {
        controller.authBusy = true
        runCatching {
            val webClientId = googleWebClientId()
            if (webClientId.isBlank()) {
                controller.authBusy = false
                controller.notice = "Google sign-in needs the Web OAuth client ID in Firebase."
                return
            }
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
                .build()
            googleSignInLauncher.launch(GoogleSignIn.getClient(this, options).signInIntent)
        }.onFailure {
            controller.authBusy = false
            controller.notice = "Google sign-in failed."
        }
    }

    private fun googleWebClientId(): String {
        val resourceId = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (resourceId == 0) return ""
        return getString(resourceId)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "ai-companion" || uri.host != "auth") return
        lifecycleScope.launch {
            controller.acceptAuthRedirect(uri)
        }
    }

    private fun setupNotifications() {
        EvaNotificationCenter.ensureChannels(this)
        askNotificationPermissionIfNeeded()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            EvaNotificationCenter.saveFcmToken(this, token)
        }
        FirebaseMessaging.getInstance().subscribeToTopic("eva_users")
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}



