package com.eva.ai.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eva.ai.R
import com.eva.ai.domain.model.CompanionProfile
import org.json.JSONObject

@Composable
fun CompanionProfileDialog(companion: CompanionProfile, onDismiss: () -> Unit, onChat: () -> Unit) {
    val context = LocalContext.current
    val story = remember(companion.id) {
        runCatching { JSONObject(context.assets.open("companion-stories.json").bufferedReader().use { it.readText() }).optJSONObject(companion.id) }.getOrNull()
    }
    val additionalPhoto = when (companion.id) {
        "eva" -> R.drawable.gallery_eva_01
        "riya" -> R.drawable.gallery_riya_01
        "mira" -> R.drawable.gallery_mira_01
        "aria" -> R.drawable.gallery_aria_01
        "sera" -> R.drawable.gallery_sera_01
        "nova" -> R.drawable.gallery_nova_01
        else -> null
    }
    val photos = listOfNotNull(companion.imageRes, additionalPhoto)
    var selected by remember(companion.id) { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Close companion profile", tint = evaText()) }
                    Text("About ${companion.name}", color = evaText(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item {
                        Image(painterResource(photos[selected]), "${companion.name}, AI portrait ${selected + 1} of ${photos.size}",
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(RoundedCornerShape(8.dp)))
                        if (photos.size > 1) Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selected = (selected - 1 + photos.size) % photos.size }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Previous portrait", tint = evaText())
                            }
                            Spacer(Modifier.weight(1f))
                            photos.forEachIndexed { index, photo ->
                                Image(painterResource(photo), "View portrait ${index + 1}", contentScale = ContentScale.Crop,
                                    modifier = Modifier.padding(horizontal = 5.dp).size(48.dp).clip(RoundedCornerShape(6.dp)).clickable { selected = index })
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { selected = (selected + 1) % photos.size }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Next portrait", tint = evaText())
                            }
                        }
                    }
                    item {
                        Text(companion.name, color = evaText(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("AI companion | AI-generated portraits", color = evaMuted(), fontSize = 12.sp)
                        val detail = if (story != null) "${story.optInt("age")} | ${story.optString("city")}" else "${companion.age} | Your private character"
                        Text(detail, color = evaMuted(), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    item {
                        Text(story?.optString("title") ?: "Their story", color = evaText(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(story?.optString("backstory") ?: companion.about.ifBlank { "Your companion's story is still being written." },
                            color = evaText(), fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                    item {
                        Text("Little joys", color = evaText(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(companion.interests, color = evaMuted(), fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
                Button(onClick = onChat, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = EvaColors.Pink)) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Chat with ${companion.name}")
                }
            }
        }
    }
}
