package com.eva.ai.presentation.premium

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.eva.ai.data.billing.normalizeBillingPhone

@Composable
fun ManualPhoneDialog(onDismiss: () -> Unit, onContinue: (String) -> Unit) {
    var phone by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val normalized = normalizeBillingPhone(phone)
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
    fun proceed() { attempted = true; normalized?.let(onContinue) }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(8.dp), title = { Text("Phone number") },
        text = { OutlinedTextField(value = phone, onValueChange = { phone = it.take(24); attempted = false },
            label = { Text("Mobile number") }, placeholder = { Text("10-digit number or +country code") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { proceed() }), modifier = Modifier.fillMaxWidth().focusRequester(focus),
            isError = attempted && normalized == null,
            supportingText = { if (attempted && normalized == null) Text("Enter a valid mobile number.") }) },
        confirmButton = { TextButton(onClick = { proceed() }) { Text("Continue to Razorpay") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
