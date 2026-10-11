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
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
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
                        fontWeight = FontWeight.Bold,
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
                fontWeight = FontWeight.Bold,
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
fun ProfileScreen(controller: EvaAppController, user: EvaUser, scope: CoroutineScope, onEditProfile: () -> Unit = {}) {
    val context = LocalContext.current
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
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1f))
                    IconGlassButton(
                        icon = if (controller.lightMode == true) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        description = if (controller.lightMode == true) "Use dark mode" else "Use light mode",
                        onClick = { controller.applyLightMode(!(controller.lightMode == true)) }
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                displayName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                accountEmail,
                                color = evaMuted(),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            fontWeight = FontWeight.Bold
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
                                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditProfile).padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = EvaColors.Pink)
                    Text("Edit profile", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = evaMuted())
                }
            }
            item {
                PersonalityModePicker(
                    selected = controller.selectedReplyStyle,
                    onSelect = controller::selectReplyStyle
                )
            }
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column {
                        Text("About ${companion.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                                if (controller.lightMode == true) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = EvaColors.Pink
                            )
                            Spacer(Modifier.width(14.dp))
                            Text("Light mode", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Switch(
                                checked = controller.lightMode == true,
                                onCheckedChange = { controller.applyLightMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EvaColors.Pink,
                                    uncheckedThumbColor = evaMuted(),
                                    uncheckedTrackColor = evaGlass(),
                                    uncheckedBorderColor = evaBorder()
                                )
                            )
                        }
                        ProfileRow(Icons.Rounded.PersonOutline, "Signed in as", user.email.ifBlank { "Email unavailable" })
                        TextButton(onClick = {
                            openExternalUrl(context, "https://api.merigf.com/account/delete") { controller.notice = "Could not open account deletion." }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete account and data", color = EvaColors.Pink)
                        }
                        TextButton(onClick = {
                            openExternalUrl(context, "https://merigf.com/privacy") { controller.notice = "Could not open privacy policy." }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Privacy policy") }
                        TextButton(
                            onClick = { scope.launch { controller.signOut() } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign out", color = EvaColors.Pink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileScreen(controller: EvaAppController, user: EvaUser, scope: CoroutineScope, onBack: () -> Unit) {
    var changed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    fun close() {
        if (saving) return
        if (changed) confirmDiscard = true else onBack()
    }
    BackHandler { close() }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard changes?") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } }
    )
    EvaPage {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconGlassButton(Icons.Rounded.ArrowBack, "Back", onClick = { close() })
                Spacer(Modifier.width(12.dp))
                Text("Edit profile", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                ProfileEditor(controller, user, scope, onSaved = onBack,
                    onDirtyChanged = { changed = it }, onSavingChanged = { saving = it })
            }
        }
    }
}

@Composable
fun ProfileEditor(controller: EvaAppController, user: EvaUser, scope: CoroutineScope,
    onSaved: () -> Unit = {}, onDirtyChanged: (Boolean) -> Unit = {}, onSavingChanged: (Boolean) -> Unit = {}) {
    var editName by rememberSaveable(user.id, user.name) { mutableStateOf(user.name) }
    var editPreferred by rememberSaveable(user.id, user.preferredName) { mutableStateOf(user.preferredName.orEmpty()) }
    var editOccupation by rememberSaveable(user.id, user.occupation) { mutableStateOf(user.occupation.orEmpty()) }
    var birthday by rememberSaveable(user.id, user.dateOfBirth) { mutableStateOf(user.dateOfBirth?.take(10)) }
    var dateOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val changed = editName.trim() != user.name || editPreferred.trim() != user.preferredName.orEmpty() ||
        editOccupation.trim() != user.occupation.orEmpty() || birthday != user.dateOfBirth?.take(10)
    LaunchedEffect(changed) { onDirtyChanged(changed) }
    LaunchedEffect(saving) { onSavingChanged(saving) }
    if (dateOpen) BirthdayWheelDialog(birthday, onDismiss = { dateOpen = false }) {
        birthday = it; dateOpen = false
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            EvaTextField(
                value = editName,
                onValueChange = { if (!saving) editName = it.take(80) },
                label = "Your name",
                enabled = !saving,
                imeAction = ImeAction.Next
            )
            Spacer(Modifier.height(8.dp))
            EvaTextField(
                value = editPreferred,
                onValueChange = { if (!saving) editPreferred = it.take(80) },
                label = "What Eva calls you",
                enabled = !saving,
                imeAction = ImeAction.Next
            )
            Spacer(Modifier.height(8.dp))
            EvaTextField(
                value = editOccupation,
                onValueChange = { if (!saving) editOccupation = it.take(120) },
                label = "Occupation",
                enabled = !saving,
                imeAction = ImeAction.Done
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { focus.clearFocus(); dateOpen = true }, enabled = !saving,
                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Rounded.Cake, null, Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(birthday?.split('-')?.reversed()?.joinToString(" / ") ?: "Add birthday", Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore, null)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(Icons.Rounded.Check, "Save changes", enabled = changed && editName.isNotBlank(), busy = saving) {
                focus.clearFocus()
                scope.launch {
                    saving = true
                    try {
                        if (controller.updateProfile(editName.trim(), editPreferred.trim(), editOccupation.trim(), birthday)) onSaved()
                    } finally { saving = false }
                }
            }
        }
    }
}
