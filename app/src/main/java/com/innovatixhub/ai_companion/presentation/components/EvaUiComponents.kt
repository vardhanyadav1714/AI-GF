package com.eva.ai.presentation.components

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.eva.ai.domain.model.EvaTab
import com.eva.ai.ui.theme.EvaInk
import com.eva.ai.ui.theme.EvaInkHigh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

object EvaColors {
    val Black = EvaInk
    val Pink = com.eva.ai.ui.theme.EvaPink
    val Purple = com.eva.ai.ui.theme.EvaPurple
    val Coral = com.eva.ai.ui.theme.EvaCoral
    val Gold = com.eva.ai.ui.theme.EvaGold
    val Green = com.eva.ai.ui.theme.EvaGreen
    val Danger = com.eva.ai.ui.theme.EvaDanger
    val Ink = EvaInk
    val InkHigh = EvaInkHigh
    val Gradient = Brush.linearGradient(listOf(Purple, Pink, Coral))
}

val LocalEvaLightMode = compositionLocalOf { false }

@Composable
fun isEvaLight(): Boolean = LocalEvaLightMode.current

// ── Page scaffold ────────────────────────────────────────────────────────────

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
            // Calm backdrop: a clean neutral gradient. All brand color lives
            // in the accents — never in the canvas.
            val baseTop = evaPageTop()
            val baseMid = evaPageMid()
            val baseBottom = evaPageBottom()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Brush.verticalGradient(listOf(baseTop, baseMid, baseBottom)))
                    }
            )
            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

// ── Surfaces & buttons ───────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    radius: Dp = 20.dp,
    glassOverride: Color? = null,
    borderOverride: Color? = null,
    content: @Composable () -> Unit
) {
    // A faint top-edge highlight gives dark glass cards depth without drop
    // shadows. Skipped entirely in light mode, where it reads as a gray box.
    val drawHighlight = !isEvaLight()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        color = glassOverride ?: evaGlass(),
        contentColor = evaText(),
        border = BorderStroke(1.dp, borderOverride ?: evaBorder()),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .padding(padding)
                .drawBehind {
                    if (drawHighlight) {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.07f),
                                0.4f to Color.Transparent
                            )
                        )
                    }
                }
        ) {
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
    val interactionSource = rememberEvaInteractionSource()
    GlassCard(
        modifier = modifier
            .size(size)
            .pressScale(interactionSource = interactionSource, pressedScale = 0.92f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        padding = PaddingValues(0.dp),
        radius = size / 2
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.5f))
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
    val interactionSource = rememberEvaInteractionSource()
    val hapticTick = rememberEvaHaptic()
    val disabledFill = if (isEvaLight()) {
        Color.Black.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .pressScale(interactionSource = interactionSource, pressedScale = 0.98f)
            .clip(RoundedCornerShape(30.dp))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) {
                hapticTick()
                onClick()
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = if (enabled) 6.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        evaAnimatedGradient()
                    } else {
                        Brush.linearGradient(listOf(disabledFill, disabledFill))
                    }
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) Color.White else evaMuted(),
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                color = if (enabled) Color.White else evaMuted(),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ── Bottom navigation ────────────────────────────────────────────────────────

@Composable
fun EvaBottomNav(active: EvaTab, onSelect: (EvaTab) -> Unit) {
    val hapticTick = rememberEvaHaptic()
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
        radius = 24.dp,
        // Near-opaque fill so content scrolling underneath (especially in
        // landscape) stays readable instead of muddying through the glass.
        glassOverride = if (isEvaLight()) {
            Color.White.copy(alpha = 0.9f)
        } else {
            Color(0xFF17121F).copy(alpha = 0.94f)
        },
        borderOverride = if (isEvaLight()) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.12f)
    ) {
        Row {
            items.forEach { item ->
                val selected = active == item.first
                val interactionSource = rememberEvaInteractionSource()
                val pillColor by animateColorAsState(
                    targetValue = if (selected) EvaColors.Pink.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "nav-pill-${item.third}"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "nav-scale-${item.third}"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .pressScale(interactionSource = interactionSource, pressedScale = 0.94f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(pillColor)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            hapticTick()
                            onSelect(item.first)
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        item.second,
                        contentDescription = item.third,
                        tint = if (selected) EvaColors.Pink else evaMuted(),
                        modifier = Modifier
                            .size(21.dp)
                            .scale(iconScale)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.third,
                        color = if (selected) EvaColors.Pink else evaMuted(),
                        fontSize = 10.5.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── Motion components ────────────────────────────────────────────────────────

/** Three softly breathing dots. The "companion is thinking" cue. */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    color: Color = evaMuted()
) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 170)
                ),
                label = "typing-dot-$index"
            )
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

/** Status dot with a gentle halo pulse while the connection is live. */
@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    pulse: Boolean = true
) {
    if (!pulse) {
        Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "pulse")
    val halo by transition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-halo"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-alpha"
    )
    Box(
        modifier.size(size * 2.4f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size * halo)
                .clip(CircleShape)
                .background(color.copy(alpha = haloAlpha))
        )
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Voice waveform whose bars breathe while recording or preparing to send. */
@Composable
fun AnimatedWaveform(
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    height: Dp = 22.dp
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier = modifier.height(height),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(barCount) { index ->
            val scale by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 480 + (index % 4) * 90,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 65)
                ),
                label = "wave-bar-$index"
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height((height * (0.28f + 0.72f * scale)))
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// ── Touch feel ───────────────────────────────────────────────────────────────

/** Springy press-down scale. Pair with clickable(interactionSource, indication = null). */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "eva-press-scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** A fresh InteractionSource per component; standard companion to pressScale. */
@Composable
fun rememberEvaInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

/** Slowly drifting brand gradient: the "liquid" accent for primary CTAs. */
@Composable
fun evaAnimatedGradient(): Brush {
    val transition = rememberInfiniteTransition(label = "eva-brand-gradient")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eva-brand-shift"
    )
    val span = 520f
    return Brush.linearGradient(
        colors = listOf(EvaColors.Purple, EvaColors.Pink, EvaColors.Coral, EvaColors.Purple),
        start = Offset(-span + (span * 2f) * shift, 40f),
        end = Offset(span * shift, 620f)
    )
}

/** Moving sheen for skeleton loaders. Draw a base fill first, then the sheen. */
@Composable
fun Modifier.evaShimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "eva-shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eva-shimmer-progress"
    )
    val base = if (isEvaLight()) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f)
    val sheen = if (isEvaLight()) Color.Black.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.13f)
    return this.drawBehind {
        drawRect(base)
        val start = size.width * progress
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, sheen, Color.Transparent),
                start = Offset(start, 0f),
                end = Offset(start + size.width * 0.7f, size.height)
            )
        )
    }
}

/** Staggered slide-up + fade entrance for hero and greeting text groups. */
@Composable
fun StaggeredAppear(
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 360, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
            ) { it / 4 }
    ) {
        content()
    }
}

// ── Haptics ──────────────────────────────────────────────────────────────────

/** Light, consistent tick used for sends, selections and record start/stop. */
@Composable
fun rememberEvaHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}

// ── Small shared pieces ──────────────────────────────────────────────────────

/** Avatar wrapped in the brand gradient ring — the app's presence marker. */
@Composable
fun GradientAvatar(imageRes: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(EvaColors.Gradient)
            .padding(2.dp)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
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
fun PremiumFeature(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EvaColors.Gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
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
    val interactionSource = rememberEvaInteractionSource()
    Column(
        modifier = modifier
            .height(130.dp)
            .pressScale(interactionSource = interactionSource, pressedScale = 0.97f)
            .clip(RoundedCornerShape(18.dp))
            .background(evaGlass())
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) EvaColors.Pink else evaBorder(),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = evaMuted(), fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(13.dp))
        Text(
            price,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (tag.isNotBlank()) {
            Spacer(Modifier.height(13.dp))
            Text(
                tag,
                color = if (selected) EvaColors.Pink else evaMuted(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatStrip(chats: String, plan: String, companion: String) {
    GlassCard {
        Row {
            StatItem("Chats", chats, Modifier.weight(1f))
            StatItem("Plan", plan, Modifier.weight(1f))
            StatItem("Companion", companion, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = evaMuted(), fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Text(
            value,
            fontSize = if (value.length > 7) 16.sp else 22.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(
            value,
            color = EvaColors.Pink,
            fontWeight = FontWeight.SemiBold,
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
            fontWeight = FontWeight.SemiBold,
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
    onSend: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .then(modifier)
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

// ── Semantic colors ──────────────────────────────────────────────────────────

@Composable
fun evaText(): Color = if (isEvaLight()) Color(0xFF17101B) else Color.White

@Composable
fun evaMuted(): Color = if (isEvaLight()) Color(0xFF6E6273) else Color.White.copy(alpha = 0.70f)

@Composable
fun evaPageTop(): Color = if (isEvaLight()) Color(0xFFFAFAFA) else EvaInk

@Composable
fun evaPageMid(): Color = if (isEvaLight()) Color(0xFFF4F4F5) else Color(0xFF121216)

@Composable
fun evaPageBottom(): Color = if (isEvaLight()) Color.White else EvaInk

@Composable
fun evaGlass(): Color = if (isEvaLight()) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.07f)

@Composable
fun evaBorder(): Color = if (isEvaLight()) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f)

// ── Formatting helpers ───────────────────────────────────────────────────────

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
