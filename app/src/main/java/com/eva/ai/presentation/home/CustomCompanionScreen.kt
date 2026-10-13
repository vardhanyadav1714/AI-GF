package com.eva.ai.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eva.ai.domain.model.*
import com.eva.ai.presentation.EvaAppController
import com.eva.ai.presentation.components.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CustomCompanionScreen(controller: EvaAppController, scope: CoroutineScope) {
    val existing = controller.customCompanions.firstOrNull { it.id == controller.editingCompanionId }
    var form by remember(controller.editingCompanionId) { mutableStateOf(existing ?: CustomCompanion()) }
    var ageText by remember(controller.editingCompanionId) { mutableStateOf(form.age.toString()) }
    var archiveConfirmation by remember { mutableStateOf(false) }
    val busy = controller.companionSaveBusy
    BackHandler(enabled = !busy) { controller.companionEditorOpen = false }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EvaColors.Pink, cursorColor = EvaColors.Pink,
        focusedLabelColor = EvaColors.Pink)
    EvaPage {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = !busy, onClick = { controller.companionEditorOpen = false }) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = evaText())
                }
                Text(if (existing == null) "Create companion" else "Edit companion", color = evaText(), fontWeight = FontWeight.Bold)
            }
            if (controller.customCompanions.any { it.archived }) {
                Text("Archived companions", color = evaMuted())
                controller.customCompanions.filter { it.archived }.forEach { archived ->
                    TextButton(enabled = !busy, onClick = { controller.openCompanionEditor(archived.id) }) {
                        Text(archived.name, color = EvaColors.Pink)
                    }
                }
            }
            Text("Portrait", color = evaText(), fontWeight = FontWeight.SemiBold)
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            companionPortraits.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { avatar ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(
                                if (form.avatarId == avatar.id) 2.dp else 0.dp,
                                if (form.avatarId == avatar.id) EvaColors.Pink else androidx.compose.ui.graphics.Color.Transparent)) {
                                Image(painterResource(avatar.imageRes), "Choose ${avatar.name} portrait",
                                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).clickable(enabled = !busy) { form = form.copy(avatarId = avatar.id) },
                                    contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
            }
            OutlinedTextField(form.name, { if (it.length <= 40) form = form.copy(name = it) }, label = { Text("Name") },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            OutlinedTextField(ageText, { if (it.length <= 3 && it.all(Char::isDigit)) ageText = it }, label = { Text("Age (18 or older)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            Text("Personality", color = evaText(), fontWeight = FontWeight.SemiBold)
            customCharacterTraits.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) { row.forEach { trait ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = trait in form.traits, enabled = !busy && (trait in form.traits || form.traits.size < 4),
                            colors = CheckboxDefaults.colors(checkedColor = EvaColors.Pink), onCheckedChange = { checked ->
                                form = form.copy(traits = if (checked) form.traits + trait else form.traits - trait)
                            })
                        Text(trait, color = evaText())
                    }
                } }
            }
            CharacterOption("Language", listOf("Automatic", "English", "Hindi", "Hinglish"), form.language, busy) { form = form.copy(language = it) }
            CharacterOption("Conversation style", listOf("natural", "soft", "flirty", "direct", "deep"), form.conversationStyle, busy) { form = form.copy(conversationStyle = it) }
            OutlinedTextField(form.interests, { if (it.length <= 200) form = form.copy(interests = it) }, label = { Text("Interests") },
                enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            OutlinedTextField(form.backstory, { if (it.length <= 600) form = form.copy(backstory = it) }, label = { Text("Backstory") },
                minLines = 3, maxLines = 6, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            controller.companionSaveError?.let { Text(it, color = EvaColors.Pink) }
            val age = ageText.toIntOrNull()
            Button(onClick = { scope.launch { controller.saveCompanion(form.copy(age = age ?: 24, archived = false)) } },
                enabled = !busy && form.name.isNotBlank() && form.traits.isNotEmpty() && age != null && age in 18..100,
                colors = ButtonDefaults.buttonColors(containerColor = EvaColors.Pink), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "Saving..." else if (form.archived) "Restore companion" else "Save companion")
            }
            if (existing != null && !form.archived) TextButton(enabled = !busy, onClick = { archiveConfirmation = true }) {
                Text("Archive companion", color = EvaColors.Pink)
            }
        }
    }
    if (archiveConfirmation) AlertDialog(onDismissRequest = { archiveConfirmation = false }, title = { Text("Archive companion?") },
        text = { Text("Your chat history will be kept. You can restore this companion later.") },
        confirmButton = { TextButton(onClick = { archiveConfirmation = false; scope.launch { controller.saveCompanion(form.copy(age = ageText.toIntOrNull() ?: form.age, archived = true)) } }) { Text("Archive", color = EvaColors.Pink) } },
        dismissButton = { TextButton(onClick = { archiveConfirmation = false }) { Text("Cancel") } })
}

@Composable
private fun CharacterOption(label: String, options: List<String>, selected: String, busy: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = evaText(), fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EvaColors.Pink), shape = RoundedCornerShape(8.dp)) {
                Text(selected.replaceFirstChar { it.uppercase() })
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = { expanded = false; onSelect(option) }) }
            }
        }
    }
}
