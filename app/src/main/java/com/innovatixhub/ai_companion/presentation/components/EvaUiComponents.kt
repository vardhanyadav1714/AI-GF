package com.eva.ai.presentation.components

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eva.ai.domain.model.EvaTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

object EvaColors {
    val Black = Color(0xFF050509)
    val Pink = Color(0xFFFF3BB4)
    val Purple = Color(0xFF8E35F2)
    val Coral = Color(0xFFFF5E70)
    val Gold = Color(0xFFFFC044)
    val Green = Color(0xFF2EE582)
    val Gradient = Brush.linearGradient(listOf(Purple, Pink, Coral))
}

val LocalEvaLightMode = compositionLocalOf { false }
@Composable
fun EvaPage(
    backgroundImage: Int? = null,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalContentColor provides evaText()) {
            if (backgroundImage != null) {
                Image(
                    painter = painterResource(backgroundImage),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (isEvaLight()) 0.13f else 0.10f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                evaPageTop().copy(alpha = if (isEvaLight()) 0.90f else 0.94f),
                                evaPageMid().copy(alpha = if (isEvaLight()) 0.92f else 0.96f),
                                evaPageBottom().copy(alpha = if (isEvaLight()) 0.96f else 0.98f)
                            )
                        )
                    )
            )
            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    radius: Dp = 18.dp,
    glassOverride: Color? = null,
    borderOverride: Color? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        color = glassOverride ?: evaGlass(),
        contentColor = evaText(),
        border = BorderStroke(1.dp, borderOverride ?: evaBorder()),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun IconGlassButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = evaText(),
    size: Dp = 48.dp
) {
    GlassCard(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        padding = PaddingValues(0.dp),
        radius = 18.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.52f))
        }
    }
}

@Composable
fun GradientButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = if (enabled) 6.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) EvaColors.Gradient
                    else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun EvaBottomNav(active: EvaTab, onSelect: (EvaTab) -> Unit) {
    val items = listOf(
        Triple(EvaTab.Home, Icons.Rounded.Home, "Home"),
        Triple(EvaTab.Chat, Icons.Rounded.ChatBubbleOutline, "Chat"),
        Triple(EvaTab.Profile, Icons.Rounded.PersonOutline, "Profile")
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
        padding = PaddingValues(horizontal = 5.dp, vertical = 5.dp),
        radius = 22.dp
    ) {
        Row {
            items.forEach { item ->
                val selected = active == item.first
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) EvaColors.Pink.copy(alpha = 0.16f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(item.first) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        item.second,
                        contentDescription = item.third,
                        tint = if (selected) EvaColors.Pink else evaMuted(),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.third,
                        color = if (selected) EvaColors.Pink else evaMuted(),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun DateChip(label: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassCard(
            padding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            radius = 18.dp
        ) {
            Text(label, color = evaMuted(), fontSize = 12.sp)
        }
    }
}

@Composable
fun VoiceMemoryTile(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.height(148.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(EvaColors.Pink.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.RecordVoiceOver,
                    contentDescription = null,
                    tint = EvaColors.Pink,
                    modifier = Modifier.size(27.dp)
                )
            }
            Column {
                Text("Voice Notes", fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(Modifier.height(5.dp))
                Text("3 saved", color = evaMuted(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MemoryTile(
    imageRes: Int,
    title: String,
    icon: ImageVector,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(18.dp))
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun NoteTile(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.height(148.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text("Late Night Talks", fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("8 May 2024", color = evaMuted())
            Spacer(Modifier.height(14.dp))
            Icon(Icons.Rounded.Chat, contentDescription = null, tint = EvaColors.Purple)
        }
    }
}

@Composable
fun PremiumFeature(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(EvaColors.Gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black)
            Text(body, color = evaMuted(), fontSize = 12.sp)
        }
    }
}

@Composable
fun PriceCard(
    title: String,
    price: String,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(evaGlass())
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) EvaColors.Pink else evaBorder(),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = evaMuted(), fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Spacer(Modifier.height(13.dp))
        Text(
            price,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (tag.isNotBlank()) {
            Spacer(Modifier.height(13.dp))
            Text(
                tag,
                color = if (selected) EvaColors.Pink else evaMuted(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatStrip(chats: String, memories: String, days: String) {
    GlassCard {
        Row {
            StatItem("Chats", chats, Modifier.weight(1f))
            StatItem("Memories", memories, Modifier.weight(1f))
            StatItem("Days Together", days, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = evaMuted(), fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Text(value, fontSize = 23.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ProfileRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = EvaColors.Pink)
        Spacer(Modifier.width(14.dp))
        Text(title, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Text(
            value,
            color = EvaColors.Pink,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.44f)
        )
        if (onClick != null) {
            Spacer(Modifier.width(5.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun CallAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(82.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconGlassButton(
            icon = icon,
            onClick = onClick,
            color = if (active) EvaColors.Pink else Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EvaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction,
    onSend: (() -> Unit)? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    scope.launch {
                        delay(260)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onSend = { onSend?.invoke() }
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = evaText(),
            unfocusedTextColor = evaText(),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = EvaColors.Pink,
            unfocusedIndicatorColor = evaBorder(),
            focusedLabelColor = EvaColors.Pink,
            unfocusedLabelColor = evaMuted()
        )
    )
}

@Composable
fun evaText(): Color = if (isEvaLight()) Color(0xFF17101B) else Color.White

@Composable
fun evaMuted(): Color = if (isEvaLight()) Color(0xFF6E6273) else Color.White.copy(alpha = 0.70f)

@Composable
fun evaPageTop(): Color = if (isEvaLight()) Color(0xFFFFF8FC) else EvaColors.Black

@Composable
fun evaPageMid(): Color = if (isEvaLight()) Color(0xFFF7EFF8) else Color(0xFF120D16)

@Composable
fun evaPageBottom(): Color = if (isEvaLight()) Color.White else EvaColors.Black

@Composable
fun evaGlass(): Color = if (isEvaLight()) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.07f)

@Composable
fun evaBorder(): Color = if (isEvaLight()) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f)

@Composable
fun isEvaLight(): Boolean = LocalEvaLightMode.current

fun formatTime(millis: Long): String = DateFormat.format("H:mm", Date(millis)).toString()

fun formatDateChip(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val messageDay = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        messageDay.isSameLocalDay(today) -> "Today"
        messageDay.isSameLocalDay(yesterday) -> "Yesterday"
        messageDay.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            DateFormat.format("d MMM", Date(millis)).toString()
        else -> DateFormat.format("d MMM yyyy", Date(millis)).toString()
    }
}

private fun Calendar.isSameLocalDay(other: Calendar): Boolean =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "$minutes:${remaining.toString().padStart(2, '0')}"
}



