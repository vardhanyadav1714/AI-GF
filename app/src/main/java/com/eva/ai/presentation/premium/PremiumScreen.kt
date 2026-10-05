package com.eva.ai.presentation.premium

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
import com.eva.ai.BuildConfig
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
fun PremiumScreen(
    subscription: SubscriptionState?,
    busy: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRefresh: () -> Unit,
    onGooglePlay: (() -> Unit)? = null,
    googlePlayBusy: Boolean = false,
    onRestore: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var confirmCancel by remember { mutableStateOf(false) }
    if (confirmCancel) {
        AlertDialog(onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel future renewals?") },
            text = { Text("Your current paid period remains available until its end date.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; onCancel() }) { Text("Cancel renewals") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep membership") } })
    }
    val plan = subscription?.plan ?: SubscriptionPlan(
        planId = "eva_premium_monthly",
        name = "Eva Premium Monthly",
        amount = 49900,
        formattedAmount = "INR 499",
        currency = "INR",
        interval = "monthly"
    )
    val active = subscription?.active == true

    EvaPage(backgroundImage = R.drawable.model_eva_real_v3) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 30.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconGlassButton(icon = Icons.Rounded.ArrowBackIosNew, onClick = onBack)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("EVA Premium", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Unlock the full experience", color = evaMuted())
                    }
                    Spacer(Modifier.width(48.dp))
                }
            }
            item {
                Image(
                    painter = painterResource(R.drawable.model_eva_real_v3),
                    contentDescription = "Eva Premium",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(26.dp))
                )
                GlassCard(
                    modifier = Modifier
                        .offset(y = (-22).dp)
                        .fillMaxWidth()
                ) {
                    Column {
                        PremiumFeature(Icons.Rounded.Chat, "Premium Membership", "One monthly membership for your account")
                        PremiumFeature(Icons.Rounded.Psychology, "Shared Memory", "Eva remembers you across every companion")
                        PremiumFeature(Icons.Rounded.GraphicEq, "Voice Notes", "Send voice and hear audio replies")
                        PremiumFeature(Icons.Rounded.Star, "Custom Personality", "Make Eva your way")
                        PremiumFeature(Icons.Rounded.Verified, "Google Play Billing", "Secure monthly subscription")
                    }
                }
            }
            item {
                // One honest plan summary instead of a single plan pretending
                // to be a selectable list.
                GlassCard(padding = PaddingValues(16.dp), radius = 20.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                plan.name,
                                color = evaText(),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${plan.formattedAmount} / month",
                                color = evaText(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Billed monthly · cancel anytime",
                                color = evaMuted(),
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (active) EvaColors.Green.copy(alpha = 0.16f)
                                    else EvaColors.Gold.copy(alpha = 0.16f)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (active) "ACTIVE" else "READY",
                                color = if (active) EvaColors.Green else EvaColors.Gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            item {
                GradientButton(
                    icon = Icons.Rounded.LockOpen,
                    label = when {
                        active -> "Premium Active"
                        busy -> "Opening Checkout"
                        BuildConfig.ALTERNATIVE_BILLING_ENABLED -> "Choose payment method"
                        else -> "Continue with Google Play"
                    },
                    enabled = !busy && !active,
                    onClick = onGooglePlay ?: onContinue
                )
            }
            item {
                val context = LocalContext.current
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Renews monthly until cancelled. Cancel before the next renewal to stop the next charge. If renewal has already been charged, cancellation stops later renewals. Access continues through the paid period. No discretionary refunds; legal and provider exceptions apply.",
                        color = evaMuted(), fontSize = 12.sp, lineHeight = 18.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = {
                        openExternalUrl(context, "https://merigf.com/refund-policy") {
                            android.widget.Toast.makeText(context, "Visit merigf.com/refund-policy", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }) { Text("Cancellation & Refund Policy", color = EvaColors.Pink) }
                }
            }
            item {
                onGooglePlay?.let {
                    GradientButton(
                        icon = Icons.Rounded.PlayCircle,
                        label = when {
                            googlePlayBusy -> "Checking purchases"
                            else -> "Restore purchases"
                        },
                        enabled = !googlePlayBusy,
                        onClick = onRestore
                    )
                }
            }
            item {
                val context = LocalContext.current
                subscription?.currentEnd?.let { end ->
                    Text("Current period ends ${end.substringBefore('T')}", color = evaMuted(), modifier = Modifier.fillMaxWidth())
                }
                if (subscription?.cancelAtPeriodEnd == true) {
                    Text("Future renewals are cancelled", color = evaMuted())
                } else if (subscription?.provider == "razorpay" && active) {
                    TextButton(onClick = { confirmCancel = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel future renewals")
                    }
                }
                if (subscription?.provider == "google_play") {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/account/subscriptions?sku=eva_premium_monthly&package=com.eva.ai"))) }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manage in Google Play")
                    }
                }
                Text(
                    if (busy) "Checking subscription..." else "Refresh Subscription Status",
                    color = EvaColors.Pink.copy(alpha = 0.9f),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy, onClick = onRefresh)
                )
            }
        }
    }
}

