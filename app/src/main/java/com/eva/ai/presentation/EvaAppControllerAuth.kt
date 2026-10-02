package com.eva.ai.presentation

import android.net.Uri
import com.eva.ai.data.remote.ApiException
import com.eva.ai.domain.model.AuthState
import com.eva.ai.domain.model.EvaTab
import com.eva.ai.domain.logic.cleanMessage

suspend fun EvaAppController.requestEmailCode(name: String, email: String): Boolean {
    val cleanEmail = email.trim()
    if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
        notice = "Enter a valid email first."
        return false
    }

    authBusy = true
    val sent = runCatching {
        api.requestEmailCode(name = name.trim(), email = cleanEmail)
    }.onSuccess {
        notice = "Confirmation code sent to $cleanEmail."
    }.onFailure { error ->
        notice = error.cleanMessage("Could not send the email code.")
    }.isSuccess
    authBusy = false
    return sent
}

suspend fun EvaAppController.verifyEmailCode(email: String, code: String) {
    val cleanCode = code.trim()
    if (cleanCode.length < 4) {
        notice = "Enter the confirmation code."
        return
    }

    authBusy = true
    runCatching {
        api.verifyEmailCode(email = email.trim(), code = cleanCode)
    }.onSuccess { session ->
        authState = AuthState.SignedIn(session.user)
        needsDateOfBirth = session.user.dateOfBirth == null
        loadChats()
        refreshSubscription(silent = true)
        syncDeviceToken()
        notice = welcomeNotice(session.user.name)
    }.onFailure { error ->
        notice = error.cleanMessage("That code could not be verified.")
    }
    authBusy = false
}

suspend fun EvaAppController.signInWithGoogle(idToken: String) {
    authBusy = true
    runCatching {
        api.signInWithGoogle(idToken)
    }.onSuccess { session ->
        authState = AuthState.SignedIn(session.user)
        needsDateOfBirth = session.user.dateOfBirth == null
        loadChats()
        refreshSubscription(silent = true)
        syncDeviceToken()
        notice = welcomeNotice(session.user.name)
    }.onFailure { error ->
        notice = error.cleanMessage("Google sign-in failed.")
    }
    authBusy = false
}

fun EvaAppController.googleSignInUri(): Uri = api.googleSignInUri()

suspend fun EvaAppController.acceptAuthRedirect(uri: Uri) {
    authBusy = true
    val error = uri.getQueryParameter("error")
    if (!error.isNullOrBlank()) {
        notice = error
        authBusy = false
        return
    }

    runCatching {
        val accessToken = uri.getQueryParameter("accessToken")
        val refreshToken = uri.getQueryParameter("refreshToken")
        val code = uri.getQueryParameter("code")
        when {
            !accessToken.isNullOrBlank() -> api.saveRedirectSession(accessToken, refreshToken)
            !code.isNullOrBlank() -> api.exchangeGoogleCode(code)
            else -> throw ApiException("Google sign-in did not return a session.")
        }
    }.onSuccess { session ->
        authState = AuthState.SignedIn(session.user)
        needsDateOfBirth = session.user.dateOfBirth == null
        loadChats()
        refreshSubscription(silent = true)
        syncDeviceToken()
        notice = welcomeNotice(session.user.name)
    }.onFailure { redirectError ->
        notice = redirectError.cleanMessage("Google sign-in could not be completed.")
    }
    authBusy = false
}

suspend fun EvaAppController.signOut() {
    runCatching { api.unregisterFcmDevice() }
    api.clearSession()
    authState = AuthState.SignedOut
    needsDateOfBirth = false
    messages.clear()
    conversations.clear()
    subscriptionState = null
    selectedConversationId = null
    pendingConversationId = null
    activeTab = EvaTab.Home
    premiumOpen = false
    backendLive = false
}

suspend fun EvaAppController.syncDeviceToken() {
    if (authState !is AuthState.SignedIn) return
    runCatching { api.registerFcmDevice() }
}
