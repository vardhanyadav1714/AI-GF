package com.eva.ai.presentation.auth

internal enum class AuthAction { Google, SendCode, VerifyCode, ResendCode }

internal fun authActionBusy(busy: Boolean, selected: AuthAction?, action: AuthAction): Boolean = busy && selected == action
