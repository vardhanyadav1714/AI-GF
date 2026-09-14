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
fun PremiumScreen(
    subscription: SubscriptionState?,
    busy: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRefresh: () -> Unit,
    onGooglePlay: (() -> Unit)? = null,
    googlePlayBusy: Boolean = false
) {
    val plan = subscription?.plan ?: SubscriptionPlan(
        planId = "plan_TRv3HKpujDyFoS",
        name = "Eva Premium Monthly",
        amount = 29900,
        formattedAmount = "INR 299",
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
                        Text("EVA Premium", fontSize = 22.sp, fontWeight = FontWeight.Black)
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
                        .clip(RoundedCornerShape(18.dp))
                )
                GlassCard(
                    modifier = Modifier
                        .offset(y = (-22).dp)
                        .fillMaxWidth()
                ) {
                    Column {
                        PremiumFeature(Icons.Rounded.Chat, "Unlimited Chats", "Talk as much as you want")
                        PremiumFeature(Icons.Rounded.GraphicEq, "Voice Notes", "Send voice and hear audio replies")
                        PremiumFeature(Icons.Rounded.Star, "Custom Personality", "Make Eva your way")
                        PremiumFeature(Icons.Rounded.Favorite, "Memory & Moments", "Save every special moment")
                        PremiumFeature(Icons.Rounded.LockOpen, "No Ads", "Enjoy a clean experience")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceCard(
                        title = "Monthly",
                        price = plan.formattedAmount,
                        tag = "MONTHLY PLAN",
                        selected = true,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        radius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                if (active) Icons.Rounded.Verified else Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = if (active) EvaColors.Green else EvaColors.Gold,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (active) "Active" else "Ready",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                subscription?.status?.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                } ?: "Not subscribed",
                                color = evaMuted(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
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
                        else -> "Continue ${plan.formattedAmount}"
                    },
                    enabled = !busy && !active,
                    onClick = onContinue
                )
            }
            item {
                onGooglePlay?.let { onPlay ->
                    GradientButton(
                        icon = Icons.Rounded.PlayCircle,
                        label = when {
                            active -> "Premium Active"
                            googlePlayBusy -> "Opening Google Play"
                            else -> "Pay with Google Play"
                        },
                        enabled = !active && !googlePlayBusy,
                        onClick = onPlay
                    )
                }
            }
            item {
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


