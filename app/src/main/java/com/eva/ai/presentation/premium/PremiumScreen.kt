package com.eva.ai.presentation.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eva.ai.BuildConfig
import com.eva.ai.data.audio.openExternalUrl
import com.eva.ai.data.billing.IndiaBillingPolicy
import com.eva.ai.R
import com.eva.ai.domain.model.SubscriptionState
import com.eva.ai.presentation.components.*

@Composable
fun PremiumScreen(
    subscription: SubscriptionState?, busy: Boolean,
    checkoutBusy: Boolean, refreshBusy: Boolean, restoreBusy: Boolean,
    verifyBusy: Boolean, cancelBusy: Boolean,
    onBack: () -> Unit, onContinue: () -> Unit, onRefresh: () -> Unit,
    onRestore: () -> Unit, onCancel: () -> Unit,
    billingState: String?, billingStateRequired: Boolean,
    onBillingStateChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val active = subscription?.active == true
    var confirmCancel by remember { mutableStateOf(false) }
    fun openLink(url: String) = openExternalUrl(context, url) {
        android.widget.Toast.makeText(context, "Could not open your browser.", android.widget.Toast.LENGTH_SHORT).show()
    }
    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Cancel future renewals?") },
        text = { Text("Access continues through your current paid period. No further renewals will be charged.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { confirmCancel = false; onCancel() }) { Text("Cancel renewals") } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep membership") } }
    )
    EvaPage {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                Text("Membership", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Image(painterResource(R.drawable.model_eva_real_v3), "Eva", contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text("Eva Premium", fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                        Text(if (active) "Your membership" else "A little more time together", color = evaMuted(), fontSize = 14.sp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(subscription?.plan?.formattedAmount ?: "INR 499", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Text("per month", fontSize = 14.sp, color = evaMuted())
                    Text(if (active) {
                        if (subscription?.cancelAtPeriodEnd == true) "Active until your paid period ends" else "Membership active"
                    } else "Auto-renews monthly. Cancel anytime.", color = if (active) EvaColors.Green else evaMuted(), fontSize = 13.sp)
                }
                HorizontalDivider(color = evaMuted().copy(alpha = 0.18f))
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Benefit(Icons.Rounded.ChatBubbleOutline, "Every companion", "One membership across your Eva account")
                    Benefit(Icons.Rounded.Psychology, "Shared memory", "Continue your story across companions")
                    Benefit(Icons.Rounded.GraphicEq, "Voice conversations", "Voice notes and audio replies")
                }
                HorizontalDivider(color = evaMuted().copy(alpha = 0.18f))
                if (active) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        subscription?.currentEnd?.let { Text("Paid through ${it.substringBefore('T')}", fontSize = 14.sp) }
                        Text(when (subscription?.provider) { "google_play" -> "Billed through Google Play"; "razorpay" -> "Billed through Razorpay"; else -> "Membership confirmed" }, color = evaMuted(), fontSize = 13.sp)
                        if (subscription?.provider == "google_play") {
                            OutlinedButton(enabled = !busy, onClick = { openLink("https://play.google.com/store/account/subscriptions?sku=eva_premium_monthly&package=com.eva.ai") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Rounded.Settings, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Manage membership")
                            }
                        } else if (subscription?.provider == "razorpay" && subscription.cancelAtPeriodEnd != true) {
                            TextButton(enabled = !busy, onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                BusyIcon(cancelBusy, Icons.Rounded.EventBusy)
                                Spacer(Modifier.width(8.dp)); Text(if (cancelBusy) "Cancelling renewals..." else "Cancel future renewals")
                            }
                        }
                    }
                } else {
                    if (billingStateRequired || billingState != null) {
                        var statesExpanded by remember { mutableStateOf(false) }
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(enabled = !busy, onClick = { statesExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Rounded.LocationOn, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(billingState?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Select your billing state", modifier = Modifier.weight(1f), maxLines = 2)
                                Icon(Icons.Rounded.ExpandMore, null)
                            }
                            DropdownMenu(expanded = statesExpanded, onDismissRequest = { statesExpanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                                IndiaBillingPolicy.administrativeAreas.forEach { state ->
                                    DropdownMenuItem(text = { Text(state.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = {
                                        statesExpanded = false
                                        onBillingStateChanged(state)
                                    })
                                }
                            }
                        }
                    }
                    val confirmingPayment = verifyBusy && !restoreBusy
                    Button(onClick = onContinue, enabled = !busy && (!billingStateRequired || billingState != null), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = EvaColors.Pink), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        BusyIcon(checkoutBusy || confirmingPayment, Icons.Rounded.LockOpen)
                        Spacer(Modifier.width(10.dp))
                        Text(when {
                            checkoutBusy -> "Opening checkout..."
                            confirmingPayment -> "Confirming payment..."
                            billingStateRequired -> "Continue with Razorpay"
                            BuildConfig.ALTERNATIVE_BILLING_ENABLED -> "Choose payment method"
                            else -> "Subscribe with Google Play"
                        }, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    }
                }
                OutlinedButton(onClick = onRestore, enabled = !busy, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    BusyIcon(restoreBusy, Icons.Rounded.Restore); Spacer(Modifier.width(8.dp))
                    Text(if (restoreBusy) "Restoring purchases..." else "Restore purchases")
                }
                TextButton(onClick = onRefresh, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    BusyIcon(refreshBusy, Icons.Rounded.Refresh); Spacer(Modifier.width(8.dp))
                    Text(if (refreshBusy) "Refreshing status..." else "Refresh payment status")
                }
                Text("Cancellation stops future renewals. Access continues through your paid period. No discretionary refunds; legal and provider exceptions apply.", color = evaMuted(), fontSize = 12.sp, lineHeight = 18.sp)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { openLink("https://merigf.com/terms") }) { Text("Terms", fontSize = 12.sp) }
                    TextButton(onClick = { openLink("https://merigf.com/refund-policy") }) { Text("Cancellation & refunds", fontSize = 12.sp) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun BusyIcon(busy: Boolean, icon: ImageVector) {
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(icon, null, Modifier.size(20.dp))
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, detail: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = EvaColors.Pink)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(detail, color = evaMuted(), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
