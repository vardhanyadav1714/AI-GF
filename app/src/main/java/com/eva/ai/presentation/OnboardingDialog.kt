package com.eva.ai.presentation

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.NumberPicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.eva.ai.domain.model.PlaceSuggestion
import com.eva.ai.presentation.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Calendar
import java.text.DateFormatSymbols
import java.util.Locale

@Composable
fun DateOfBirthDialog(
    busy: Boolean, onSkip: () -> Unit,
    onSave: (String, String, PlaceSuggestion, Boolean) -> Unit,
    searchPlaces: suspend (String) -> List<PlaceSuggestion>
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var name by rememberSaveable { mutableStateOf("") }
    var birthday by rememberSaveable { mutableStateOf<String?>(null) }
    var dateOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var placeId by rememberSaveable { mutableStateOf<String?>(null) }
    var placeLabel by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf<String?>(null) }
    var useForBilling by rememberSaveable { mutableStateOf(true) }
    var results by remember { mutableStateOf(emptyList<PlaceSuggestion>()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query, placeId) {
        results = emptyList(); searching = false; searchError = null
        if (placeId != null || query.trim().length < 2) return@LaunchedEffect
        delay(300)
        searching = true
        try {
            results = searchPlaces(query.trim())
            if (results.isEmpty()) searchError = "No matching city. Try a nearby town."
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            searchError = "Couldn't search cities. Edit the city name to retry."
        } finally { searching = false }
    }
    if (dateOpen) BirthdayWheelDialog(birthday, onDismiss = { dateOpen = false }) {
        birthday = it; dateOpen = false; error = null
    }
    AlertDialog(onDismissRequest = { if (!busy) onSkip() }, shape = RoundedCornerShape(8.dp),
        containerColor = if (isEvaLight()) Color.White else EvaColors.InkHigh,
        title = { Text("A little about you", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("Your name") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { focus.clearFocus(); keyboard?.hide(); dateOpen = true }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Rounded.Cake, null, Modifier.size(18.dp)); Spacer(Modifier.width(10.dp))
                    Text(birthday?.split("-")?.reversed()?.joinToString(" / ") ?: "Choose your birthday", Modifier.weight(1f))
                    Icon(Icons.Rounded.ExpandMore, null)
                }
                OutlinedTextField(query, { query = it.take(80); placeId = null; state = null; error = null },
                    label = { Text("City you live in") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.LocationOn, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = ""; placeId = null; state = null }, enabled = !busy) { Icon(Icons.Rounded.Close, "Clear city") } })
                if (searching) LinearProgressIndicator(Modifier.fillMaxWidth(), color = EvaColors.Pink)
                searchError?.let { Text(it, color = EvaColors.Coral, fontSize = 12.sp) }
                if (results.isNotEmpty()) Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                    results.forEach { place ->
                        ListItem(headlineContent = { Text(place.label.substringBefore(','), fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(place.label.substringAfter(", ", ""), fontSize = 12.sp) },
                            leadingContent = { Icon(Icons.Rounded.LocationOn, null, tint = evaMuted(), modifier = Modifier.size(18.dp)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                                placeId = place.id; placeLabel = place.label; country = place.countryCode; state = place.billingAdministrativeArea
                                query = place.label.substringBefore(','); results = emptyList(); error = null
                                focus.clearFocus(); keyboard?.hide()
                            })
                        HorizontalDivider(color = evaMuted().copy(alpha = 0.15f))
                    }
                }
                if (placeId != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.CheckCircle, "City selected", tint = EvaColors.Green, modifier = Modifier.size(16.dp))
                    Text(placeLabel.substringAfter(", ", placeLabel), color = evaMuted(), fontSize = 12.sp)
                }
                if (state != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(useForBilling, { useForBilling = it }, enabled = !busy)
                    Text("My billing state is ${state!!.lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
                Text("For adults 18 and over.", color = evaMuted(), fontSize = 12.sp)
                TextButton(onClick = { com.eva.ai.data.audio.openExternalUrl(context, "https://www.geonames.org/") {} }, contentPadding = PaddingValues(0.dp)) { Text("Places by GeoNames", fontSize = 11.sp) }
                error?.let { Text(it, color = EvaColors.Coral, fontSize = 12.sp) }
            }
        }, confirmButton = {
            TextButton(enabled = !busy, onClick = {
                when {
                    name.isBlank() -> error = "Enter your name."
                    birthday == null -> error = "Choose your birthday."
                    placeId == null -> error = "Select your city from the results."
                    else -> onSave(birthday!!, name.trim(), PlaceSuggestion(placeId!!, placeLabel, country, state), useForBilling && state != null)
                }
            }) { Text(if (busy) "Saving..." else "Continue") }
        }, dismissButton = { TextButton(enabled = !busy, onClick = onSkip) { Text("Later") } })
}

@Composable
internal fun BirthdayWheelDialog(initial: String?, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val today = remember { Calendar.getInstance() }
    val parts = initial?.split('-')?.mapNotNull { it.toIntOrNull() }
    var year by rememberSaveable { mutableIntStateOf(parts?.getOrNull(0) ?: today.get(Calendar.YEAR) - 25) }
    var month by rememberSaveable { mutableIntStateOf(parts?.getOrNull(1) ?: 1) }
    var day by rememberSaveable { mutableIntStateOf(parts?.getOrNull(2) ?: 1) }
    val days = birthdayDays(year, month)
    LaunchedEffect(days) { if (day > days) day = days }
    val valid = birthdayIso(year, month, day, today)
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(8.dp), title = { Text("Your birthday") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BirthdayWheel("Day", day.coerceAtMost(days), 1, days, null, Modifier.weight(1f)) { day = it }
                BirthdayWheel("Month", month, 1, 12, DateFormatSymbols(Locale.getDefault()).shortMonths.take(12).toTypedArray(), Modifier.weight(1f)) { month = it }
                BirthdayWheel("Year", year, 1900, today.get(Calendar.YEAR) - 18, null, Modifier.weight(1.2f)) { year = it }
            }
            Text("You must be 18 or older.", fontSize = 12.sp, color = evaMuted())
        } }, confirmButton = { TextButton(enabled = valid != null, onClick = { valid?.let(onSelect) }) { Text("Set birthday") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun BirthdayWheel(label: String, value: Int, min: Int, max: Int, labels: Array<String>?, modifier: Modifier, onChange: (Int) -> Unit) {
    val light = isEvaLight()
    val color = evaText().toArgb()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = evaMuted())
        AndroidView(factory = { context: Context ->
            NumberPicker(ContextThemeWrapper(context, if (light) android.R.style.Theme_Material_Light else android.R.style.Theme_Material)).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = false
                setOnValueChangedListener { _, _, next -> onChange(next) }
            }
        }, update = { picker ->
            picker.displayedValues = null; picker.minValue = min; picker.maxValue = max
            picker.displayedValues = labels; picker.value = value.coerceIn(min, max); picker.contentDescription = label
            if (android.os.Build.VERSION.SDK_INT >= 29) picker.textColor = color
        }, modifier = Modifier.fillMaxWidth().height(140.dp))
    }
}
