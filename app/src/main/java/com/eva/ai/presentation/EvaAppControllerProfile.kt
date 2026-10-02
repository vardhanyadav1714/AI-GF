package com.eva.ai.presentation

import com.eva.ai.domain.logic.cleanMessage
import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.ReplyStyle
import java.util.Locale

suspend fun EvaAppController.saveDateOfBirth(dateOfBirth: String, preferredName: String = ""): Boolean {
    if (authState !is AuthState.SignedIn) return false
    if (authBusy) return false
    authBusy = true
    var saved = false
    runCatching {
        api.updateProfile(displayName = null, preferredName = preferredName.takeIf { it.isNotBlank() }, occupation = null, dateOfBirth = dateOfBirth)
    }.onSuccess { user ->
        authState = AuthState.SignedIn(user)
        needsDateOfBirth = false
        saved = true
        notice = "Birthday saved. You're all set."
    }.onFailure { error ->
        notice = error.cleanMessage("Could not save your birthday.")
    }
    authBusy = false
    return saved
}

suspend fun EvaAppController.updateProfile(
    displayName: String,
    preferredName: String,
    occupation: String
): Boolean {
    if (authState !is AuthState.SignedIn) return false
    var saved = false
    runCatching {
        api.updateProfile(
            displayName = displayName,
            preferredName = preferredName,
            occupation = occupation
        )
    }.onSuccess { user ->
        authState = AuthState.SignedIn(user)
        saved = true
        notice = "Profile updated."
    }.onFailure { error ->
        notice = error.cleanMessage("Could not update your profile.")
    }
    return saved
}

fun EvaAppController.selectReplyStyle(style: ReplyStyle) {
    if (selectedReplyStyle.id == style.id) return
    selectedReplyStyle = style
    settingsStore.saveSelectedReplyStyle(style)
    notice = "${selectedCompanion.name} will keep replies ${style.label.lowercase(Locale.getDefault())}."
}
