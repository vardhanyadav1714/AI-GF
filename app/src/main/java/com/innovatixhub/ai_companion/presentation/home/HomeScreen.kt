package com.eva.ai.presentation.home

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
fun HomeScreen(
    user: EvaUser,
    companion: CompanionProfile,
    onCompanionSelect: (CompanionProfile) -> Unit,
    onChat: () -> Unit,
    onPremium: () -> Unit,
    onQuickMessage: (String) -> Unit,
    onVoiceNotes: () -> Unit
) {
    EvaPage {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Eva",
                        color = evaText(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.weight(1f))
                    IconGlassButton(
                        icon = Icons.Rounded.WorkspacePremium,
                        color = EvaColors.Gold,
                        onClick = onPremium,
                        size = 44.dp
                    )
                }
            }
            item {
                Column {
                    Text(
                        "Hi ${user.name.ifBlank { "Vardhan" }}",
                        color = evaMuted(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "I am ${companion.name}",
                        color = evaText(),
                        fontSize = 36.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        companion.subtitle,
                        color = evaMuted(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            item {
                HomeCompanionRail(
                    selected = companion,
                    onSelect = onCompanionSelect
                )
            }
            item {
                HeroImageCard(companion, onChat)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeAction(
                        icon = Icons.Rounded.GraphicEq,
                        label = "Voice chat",
                        onClick = onVoiceNotes,
                        modifier = Modifier.weight(1f)
                    )
                    HomeAction(
                        icon = Icons.Rounded.CardGiftcard,
                        label = "Premium",
                        onClick = onPremium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                MoodPanel(onQuickMessage)
            }
        }
    }
}

@Composable
fun HomeCompanionRail(
    selected: CompanionProfile,
    onSelect: (CompanionProfile) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Choose your companion",
                    color = evaText(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Swipe to pick the mood before you chat.",
                    color = evaMuted(),
                    fontSize = 12.sp
                )
            }
            Text(
                selected.personality.substringBefore(","),
                color = EvaColors.Pink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(companionProfiles, key = { it.id }) { profile ->
                HomeCompanionCard(
                    profile = profile,
                    selected = selected.id == profile.id,
                    onClick = { onSelect(profile) }
                )
            }
        }
    }
}

@Composable
fun HomeCompanionCard(
    profile: CompanionProfile,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) EvaColors.Pink.copy(alpha = 0.86f) else evaBorder()
    Row(
        modifier = Modifier
            .width(158.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) EvaColors.Pink.copy(alpha = 0.16f)
                else evaGlass()
            )
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Image(
                painter = painterResource(profile.imageRes),
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(EvaColors.Pink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.name,
                color = evaText(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                profile.tags.take(2).joinToString(" / "),
                color = if (selected) EvaColors.Pink else evaMuted(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HeroImageCard(companion: CompanionProfile, onChat: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(314.dp)
            .clip(RoundedCornerShape(26.dp))
    ) {
        Image(
            painter = painterResource(companion.imageRes),
            contentDescription = companion.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.76f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                companion.homeLine,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(14.dp))
            GradientButton(
                icon = Icons.Rounded.ChatBubble,
                label = "Start Chatting",
                onClick = onChat
            )
        }
    }
}

@Composable
fun HomeAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        radius = 18.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = EvaColors.Pink, modifier = Modifier.size(25.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                color = evaText(),
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MoodPanel(onQuickMessage: (String) -> Unit) {
    val moods = listOf(
        Triple("Amazing", Icons.Rounded.SentimentVerySatisfied, "I feel amazing today"),
        Triple("Good", Icons.Rounded.SentimentSatisfiedAlt, "I had a good day"),
        Triple("Okay", Icons.Rounded.SentimentNeutral, "I feel okay"),
        Triple("Sad", Icons.Rounded.SentimentDissatisfied, "I feel sad"),
        Triple("Tired", Icons.Rounded.Bedtime, "I am tired")
    )
    GlassCard(
        padding = PaddingValues(16.dp),
        radius = 22.dp
    ) {
        Column {
            Text("Today's Mood", color = evaText(), fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text("How are you feeling today?", color = evaMuted(), fontSize = 13.sp)
            Spacer(Modifier.height(13.dp))
            Row {
                moods.forEachIndexed { index, mood ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickMessage(mood.third) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            mood.second,
                            contentDescription = mood.first,
                            tint = if (index == 0) EvaColors.Pink else EvaColors.Gold,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            mood.first,
                            color = evaText(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


