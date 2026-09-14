package com.eva.ai.presentation.settings

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
fun PersonalityModePicker(
    selected: ReplyStyle,
    onSelect: (ReplyStyle) -> Unit
) {
    GlassCard(padding = PaddingValues(16.dp), radius = 22.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "AI personality",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This changes how your companion replies in chat and voice.",
                        color = evaMuted(),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = EvaColors.Pink)
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 2.dp)
            ) {
                items(replyStyles, key = { it.id }) { style ->
                    PersonalityModeChip(
                        style = style,
                        selected = selected.id == style.id,
                        onClick = { onSelect(style) }
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalityModeChip(
    style: ReplyStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(138.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) EvaColors.Pink.copy(alpha = 0.16f)
                else Color.White.copy(alpha = if (isEvaLight()) 0.50f else 0.06f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (selected) EvaColors.Pink.copy(alpha = 0.82f) else evaBorder()
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.Favorite,
                contentDescription = null,
                tint = if (selected) EvaColors.Pink else evaMuted(),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                style.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            style.subtitle,
            color = evaMuted(),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProfileScreen(controller: EvaAppController, user: EvaUser, scope: CoroutineScope) {
    val companion = controller.selectedCompanion
    val displayName = user.name.ifBlank {
        user.email.substringBefore("@").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }.ifBlank { "You" }
    }
    val accountEmail = user.email.ifBlank { "Email not connected" }
    val initials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "U" }

    EvaPage {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Profile",
                        color = evaText(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.weight(1f))
                    IconGlassButton(
                        icon = if (controller.lightMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        onClick = { controller.lightMode = !controller.lightMode }
                    )
                }
            }
            item {
                GlassCard(padding = PaddingValues(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(EvaColors.Gradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initials,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                displayName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                accountEmail,
                                color = evaMuted(),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (controller.subscriptionState?.active == true) {
                                                EvaColors.Gold.copy(alpha = 0.18f)
                                            } else {
                                                EvaColors.Pink.copy(alpha = 0.14f)
                                            }
                                        )
                                        .clickable { controller.premiumOpen = true }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.WorkspacePremium,
                                            contentDescription = null,
                                            tint = if (controller.subscriptionState?.active == true) {
                                                EvaColors.Gold
                                            } else {
                                                EvaColors.Pink
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (controller.subscriptionState?.active == true) {
                                                "Premium"
                                            } else {
                                                "Free plan"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(EvaColors.Pink.copy(alpha = 0.14f))
                                        .clickable { controller.activeTab = EvaTab.Chat }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Chat,
                                            contentDescription = null,
                                            tint = EvaColors.Pink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                StatStrip(
                    chats = controller.messages.count { it.fromUser }.toString(),
                    plan = if (controller.subscriptionState?.active == true) "Premium" else "Free",
                    companion = companion.name
                )
            }
            item {
                PersonalityModePicker(
                    selected = controller.selectedReplyStyle,
                    onSelect = controller::selectReplyStyle
                )
            }
            item {
                GlassCard {
                    Column {
                        Text("About ${companion.name}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            companion.about,
                            color = evaMuted(),
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        ProfileRow(Icons.Rounded.Favorite, "Character vibe", companion.personality)
                        ProfileRow(Icons.Rounded.GraphicEq, "Voice & Style", companion.voiceStyle)
                        ProfileRow(Icons.Rounded.Star, "Interests", companion.interests)
                        ProfileRow(Icons.Rounded.LocationOn, "Born in", "${companion.bornIn} / ${companion.age}")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (controller.lightMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = EvaColors.Pink
                            )
                            Spacer(Modifier.width(14.dp))
                            Text("Light mode", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            Switch(
                                checked = controller.lightMode,
                                onCheckedChange = { controller.lightMode = it }
                            )
                        }
                        ProfileRow(Icons.Rounded.PersonOutline, "Signed in as", user.email.ifBlank { "Email unavailable" })
                        TextButton(
                            onClick = { scope.launch { controller.signOut() } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign out", color = EvaColors.Coral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


