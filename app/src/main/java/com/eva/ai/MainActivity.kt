package com.eva.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.eva.ai.data.billing.IndiaBillingPolicy
import com.eva.ai.data.billing.normalizeBillingPhone
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.eva.ai.presentation.EvaAppController
import com.eva.ai.presentation.acceptAuthRedirect
import com.eva.ai.presentation.openConversationFromNotification
import com.eva.ai.presentation.refreshSubscription
import com.eva.ai.presentation.signInWithGoogle
import com.eva.ai.presentation.googleSignInUri
import com.eva.ai.presentation.BillingOperation
import com.eva.ai.presentation.withBillingOperation
import com.eva.ai.presentation.startPremiumSubscription
import com.eva.ai.presentation.userChoiceBillingEnabled
import com.eva.ai.presentation.syncDeviceToken
import com.eva.ai.presentation.verifyGooglePlayPurchase
import com.eva.ai.presentation.verifyRazorpayPurchase
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import com.eva.ai.presentation.EvaApplication
import com.eva.ai.presentation.components.EvaColors
import com.eva.ai.data.billing.PlayBillingManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.eva.ai.domain.model.AuthState
import java.security.SecureRandom

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    @Inject
    lateinit var controller: EvaAppController
    private val credentialManager by lazy { CredentialManager.create(this) }
    private val purchaseVerificationMutex = Mutex()
    private var razorpaySubscriptionId: String? = null
    private var razorpayAccountId: String? = null
    private data class RazorpayChoice(val token: String, val owner: String)
    private var pendingRazorpayChoice: RazorpayChoice? = null
    private var phoneHintChoice: RazorpayChoice? = null
    private var phoneHintTicket: Long? = null
    private val phoneHintLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val phone = if (result.resultCode == RESULT_OK && result.data != null) runCatching {
            Identity.getSignInClient(this).getPhoneNumberFromIntent(result.data!!)
        }.getOrNull() else null
        finishPhoneHint(phone)
    }
    private val playBilling: PlayBillingManager by lazy {
        PlayBillingManager(
            context = this,
            onAlternativeBilling = { token, country ->
                if (country == "IN") prepareRazorpayChoice(token)
            },
            onPurchased = { purchaseToken, productId ->
                lifecycleScope.launch {
                    purchaseVerificationMutex.withLock {
                    val verified = controller.verifyGooglePlayPurchase(purchaseToken, productId)
                    if (verified) {
                        playBilling.acknowledge(purchaseToken)
                    }
                    }
                }
            },
            onRestoreError = { message -> controller.notice = message },
            accountId = { (controller.authState as? AuthState.SignedIn)?.user?.id }
        )
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            EvaNotificationCenter.ensureChannels(this)
        } else if (::controller.isInitialized) {
            controller.notice = "Notifications are off. You can enable them later from app settings."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        razorpaySubscriptionId = savedInstanceState?.getString("razorpaySubscriptionId")
        razorpayAccountId = savedInstanceState?.getString("razorpayAccountId")
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setupNotifications()
        playBilling.connect()
        handleAuthIntent(intent)
        handleConversationIntent(intent)

        setContent {
            val controller = remember { controller }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                controller.bootstrap()
            }
            LaunchedEffect(controller.authState) {
                val owner = (controller.authState as? AuthState.SignedIn)?.user?.id
                controller.billingAdministrativeArea = owner?.let { controller.settingsStore.billingState(it) }
                    ?.takeIf { it in IndiaBillingPolicy.administrativeAreas }
                if (pendingRazorpayChoice?.owner != owner) {
                    pendingRazorpayChoice = null
                    controller.razorpayBillingStateRequired = false
                }
                if (owner != null) playBilling.restorePurchases()
            }
            LaunchedEffect(controller.billingAdministrativeArea, controller.razorpayBillingStateRequired) {
                if (controller.razorpayBillingStateRequired && controller.billingAdministrativeArea != null && !controller.subscriptionBusy) {
                    requestBillingPhoneHint()
                }
            }
            LaunchedEffect(controller.premiumOpen) {
                if (!controller.premiumOpen && controller.razorpayBillingStateRequired) {
                    pendingRazorpayChoice = null
                    controller.razorpayBillingStateRequired = false
                }
            }

            EvaApplication(
                controller = controller,
                scope = scope,
                onGoogleSignIn = {
                    openGoogleSignIn()
                },
                onGooglePlaySubscribe = {
                    controller.notice = null
                    if (pendingRazorpayChoice != null && !controller.subscriptionBusy) {
                        requestBillingPhoneHint()
                    } else if (!controller.subscriptionBusy) lifecycleScope.launch {
                        val enabled = controller.withBillingOperation(BillingOperation.Checkout) {
                            controller.userChoiceBillingEnabled()
                        }
                        playBilling.launchSubscribe(this@MainActivity, enabled) { message ->
                            controller.notice = message
                        }
                    }
                },
                onRestorePurchases = {
                    if (!controller.subscriptionBusy) lifecycleScope.launch {
                        controller.withBillingOperation(BillingOperation.Restore) {
                            try {
                                val purchases = withTimeout(20_000) {
                                    suspendCancellableCoroutine<List<com.android.billingclient.api.Purchase>> { continuation ->
                                        playBilling.queryOwnedPurchases { purchases, error ->
                                            if (continuation.isActive) {
                                                if (error != null) continuation.resumeWithException(IllegalStateException(error))
                                                else continuation.resume(purchases)
                                            }
                                        }
                                    }
                                }
                                purchaseVerificationMutex.withLock {
                                    purchases.forEach { purchase ->
                                        if (controller.verifyGooglePlayPurchase(purchase.purchaseToken, PlayBillingManager.PREMIUM_PRODUCT_ID)) {
                                            playBilling.acknowledge(purchase.purchaseToken)
                                        }
                                    }
                                }
                                val refreshed = controller.refreshSubscription(silent = true)
                                if (refreshed) controller.notice = if (controller.subscriptionState?.active == true)
                                    "Your membership is restored." else "No active membership was found."
                                else controller.notice = "Could not refresh your membership. Please try again."
                            } catch (error: Exception) {
                                if (error is CancellationException && error !is kotlinx.coroutines.TimeoutCancellationException) throw error
                                controller.notice = "Could not restore purchases. Please try again."
                            }
                        }
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
        handleConversationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        EvaNotificationCenter.setAppForeground(true)
        if (::controller.isInitialized) {
            lifecycleScope.launch {
                controller.refreshSubscription(silent = true)
                playBilling.restorePurchases()
            }
        }
    }

    override fun onPause() {
        EvaNotificationCenter.setAppForeground(false)
        super.onPause()
    }

    override fun onDestroy() {
        phoneHintTicket?.let { controller.endBillingOperation(it) }
        phoneHintTicket = null
        playBilling.close()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("razorpaySubscriptionId", razorpaySubscriptionId)
        outState.putString("razorpayAccountId", razorpayAccountId)
        super.onSaveInstanceState(outState)
    }

    private fun prepareRazorpayChoice(token: String) {
        val userId = (controller.authState as? AuthState.SignedIn)?.user?.id ?: return
        pendingRazorpayChoice = RazorpayChoice(token, userId)
        controller.billingAdministrativeArea = controller.settingsStore.billingState(userId)
            ?.takeIf { it in IndiaBillingPolicy.administrativeAreas }
        if (controller.billingAdministrativeArea != null) requestBillingPhoneHint()
        else {
            controller.razorpayBillingStateRequired = true
            controller.premiumOpen = true
        }
    }

    private fun requestBillingPhoneHint() {
        val choice = pendingRazorpayChoice ?: return
        if (phoneHintTicket != null || choice.owner != (controller.authState as? AuthState.SignedIn)?.user?.id) return
        val state = controller.billingAdministrativeArea ?: return
        if (state !in IndiaBillingPolicy.administrativeAreas) return
        controller.razorpayBillingStateRequired = false
        phoneHintTicket = controller.beginBillingOperation(BillingOperation.Checkout)
        phoneHintChoice = choice
        // Google's consent-based selector needs no phone/SMS permissions.
        Identity.getSignInClient(this)
            .getPhoneNumberHintIntent(GetPhoneNumberHintIntentRequest.builder().build())
            .addOnSuccessListener(this) { pendingIntent ->
                if (phoneHintChoice != choice || phoneHintTicket == null) return@addOnSuccessListener
                if (pendingRazorpayChoice != choice) {
                    finishPhoneHint(null)
                    return@addOnSuccessListener
                }
                runCatching { phoneHintLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build()) }
                    .onFailure { finishPhoneHint(null) }
            }
            .addOnFailureListener(this) { if (phoneHintChoice == choice) finishPhoneHint(null) }
    }

    private fun finishPhoneHint(phone: String?) {
        phoneHintTicket?.let { controller.endBillingOperation(it) }
        phoneHintTicket = null
        val choice = phoneHintChoice ?: return
        phoneHintChoice = null
        if (pendingRazorpayChoice != choice) return
        pendingRazorpayChoice = null
        val state = controller.billingAdministrativeArea ?: return
        if (choice.owner != (controller.authState as? AuthState.SignedIn)?.user?.id) return
        openRazorpaySubscription(choice.token, state, normalizeBillingPhone(phone))
    }

    private fun openRazorpaySubscription(externalTransactionToken: String, billingAdministrativeArea: String, phone: String? = null) {
        if (!BuildConfig.ALTERNATIVE_BILLING_ENABLED || externalTransactionToken.isBlank() || razorpaySubscriptionId != null) return
        val user = (controller.authState as? AuthState.SignedIn)?.user ?: return
        lifecycleScope.launch {
            val checkout = controller.startPremiumSubscription(externalTransactionToken, "IN", billingAdministrativeArea) ?: return@launch
            if ((controller.authState as? AuthState.SignedIn)?.user?.id != user.id) return@launch
            if (!checkout.keyId.startsWith("rzp_") || !checkout.subscriptionId.startsWith("sub_")) {
                controller.notice = "Native checkout is not configured yet. Please try again later."
                return@launch
            }
            razorpaySubscriptionId = checkout.subscriptionId
            razorpayAccountId = user.id
            runCatching {
                Checkout.preload(applicationContext)
                val prefill = JSONObject().put("email", user.email).put("name", user.name)
                if (phone != null) prefill.put("contact", phone)
                val options = JSONObject()
                    .put("name", "Eva")
                    .put("description", "Monthly Premium subscription - renews until cancelled")
                    .put("subscription_id", checkout.subscriptionId)
                    .put("prefill", prefill)
                    .put("allow_rotation", true)
                    .put("theme", JSONObject().put("color", "#b72c50"))
                Checkout().apply { setKeyID(checkout.keyId) }.open(this@MainActivity, options)
            }.onFailure {
                razorpaySubscriptionId = null
                razorpayAccountId = null
                controller.notice = "Could not open Razorpay checkout. Please try again."
            }
        }
    }

    override fun onPaymentSuccess(paymentId: String?, paymentData: PaymentData?) {
        val subscriptionId = razorpaySubscriptionId
        val accountId = razorpayAccountId
        val signature = paymentData?.signature
        razorpaySubscriptionId = null
        razorpayAccountId = null
        if (paymentId.isNullOrBlank() || signature.isNullOrBlank() || subscriptionId.isNullOrBlank() ||
            accountId != (controller.authState as? AuthState.SignedIn)?.user?.id) {
            controller.notice = "Sign in with the purchasing account and refresh payment status to confirm your payment."
            return
        }
        lifecycleScope.launch {
            purchaseVerificationMutex.withLock {
                controller.verifyRazorpayPurchase(paymentId, subscriptionId, signature)
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        razorpaySubscriptionId = null
        razorpayAccountId = null
        controller.notice = if (code == Checkout.PAYMENT_CANCELED) "Checkout cancelled. Refresh payment status if your bank shows a debit."
            else "Payment was not confirmed. Refresh payment status before trying again."
        // Provider responses can contain personal payment information; do not log them.
    }

    private fun openGoogleSignIn() {
        if (controller.authBusy) return
        lifecycleScope.launch {
            controller.authBusy = true
            val idToken = runCatching { requestGoogleIdToken() }
                .onFailure { error ->
                    if (error is CancellationException) {
                        controller.authBusy = false
                        throw error
                    }
                    // Exception messages and credentials may contain account information.
                    Log.w("EvaAuth", "Credential sign-in failed: ${error.javaClass.simpleName}")
                    controller.notice = when (error) {
                        is GetCredentialCancellationException -> null
                        is GoogleIdTokenParsingException -> "Google sign-in response could not be read."
                        else -> "Google sign-in could not start. Check Firebase OAuth setup."
                    }
                    if (error is GetCredentialCancellationException) offerBrowserSignIn()
                }
                .getOrNull()
            controller.authBusy = false
            if (!idToken.isNullOrBlank()) {
                controller.signInWithGoogle(idToken)
            }
        }
    }

    private fun offerBrowserSignIn() {
        if (isFinishing || isDestroyed) return
        android.app.AlertDialog.Builder(this)
            .setTitle("Continue with Google?")
            .setMessage("The account picker did not complete sign-in.")
            .setPositiveButton("Continue in browser") { _, _ ->
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, controller.googleSignInUri()))
                }.onFailure {
                    controller.notice = "Could not open your browser. Please try again."
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun requestGoogleIdToken(): String {
        val webClientId = runCatching { getString(R.string.default_web_client_id) }
            .getOrDefault("")
            .trim()
        if (webClientId.isBlank()) {
            throw IllegalStateException("Missing Firebase web OAuth client ID.")
        }

        val googleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(generateNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()
        val result = credentialManager.getCredential(
            context = this@MainActivity,
            request = request
        )
        val credential = result.credential
        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw IllegalStateException("Google sign-in returned an unsupported credential.")
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "ai-companion" || uri.host != "auth") return
        intent.data = null
        lifecycleScope.launch {
            controller.acceptAuthRedirect(uri)
        }
    }

    private fun handleConversationIntent(intent: Intent?) {
        val conversationId = intent?.getStringExtra(EvaNotificationCenter.EXTRA_CONVERSATION_ID) ?: return
        intent.removeExtra(EvaNotificationCenter.EXTRA_CONVERSATION_ID)
        lifecycleScope.launch {
            controller.openConversationFromNotification(conversationId)
        }
    }

    private fun setupNotifications() {
        EvaNotificationCenter.ensureChannels(this)
        askNotificationPermissionIfNeeded()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            EvaNotificationCenter.saveFcmToken(this, token)
            lifecycleScope.launch {
                controller.syncDeviceToken()
            }
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


