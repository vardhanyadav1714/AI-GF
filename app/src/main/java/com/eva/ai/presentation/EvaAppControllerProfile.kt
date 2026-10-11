package com.eva.ai.presentation

import com.eva.ai.domain.logic.cleanMessage
import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.ReplyStyle
import java.util.Locale
import com.eva.ai.domain.model.PlaceSuggestion
import kotlinx.coroutines.CancellationException

suspend fun EvaAppController.searchPlaces(query: String): List<PlaceSuggestion> = api.searchPlaces(query)

suspend fun EvaAppController.saveDateOfBirth(dateOfBirth: String, preferredName: String = "", place: PlaceSuggestion? = null, useForBilling: Boolean = false): Boolean {
    if (authState !is AuthState.SignedIn) return false
    if (authBusy) return false
    val owner = (authState as AuthState.SignedIn).user.id
    authBusy = true
    var saved = false
    runCatching {
        api.updateProfile(displayName = null, preferredName = preferredName.takeIf { it.isNotBlank() }, occupation = null,
            dateOfBirth = dateOfBirth, currentPlaceId = place?.id, useLocationForBilling = useForBilling)
    }.onSuccess { user ->
        if ((authState as? AuthState.SignedIn)?.user?.id != owner) return@onSuccess
        if (useForBilling && place?.billingAdministrativeArea != null) {
            settingsStore.saveBillingState(owner, place.billingAdministrativeArea)
            billingAdministrativeArea = place.billingAdministrativeArea
        }
        authState = AuthState.SignedIn(user)
        needsDateOfBirth = false
        saved = true
        notice = "You're all set."
    }.onFailure { error ->
        if (error is CancellationException) { authBusy = false; throw error }
        notice = error.cleanMessage("Could not save your birthday.")
    }
    authBusy = false
    return saved
}

suspend fun EvaAppController.updateProfile(
    displayName: String,
    preferredName: String,
    occupation: String,
    dateOfBirth: String? = null
): Boolean {
    if (authState !is AuthState.SignedIn) return false
    val owner = (authState as AuthState.SignedIn).user.id
    var saved = false
    runCatching {
        api.updateProfile(
            displayName = displayName,
            preferredName = preferredName,
            occupation = occupation,
            dateOfBirth = dateOfBirth
        )
    }.onSuccess { user ->
        if ((authState as? AuthState.SignedIn)?.user?.id != owner) return@onSuccess
        authState = AuthState.SignedIn(user)
        saved = true
        notice = "Profile updated."
    }.onFailure { error ->
        if (error is CancellationException) throw error
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
