package com.eva.ai.presentation.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthActionTest {
    @Test fun sendingEmailDoesNotAnimateGoogleOrVerification() {
        assertTrue(authActionBusy(true, AuthAction.SendCode, AuthAction.SendCode))
        assertFalse(authActionBusy(true, AuthAction.SendCode, AuthAction.Google))
        assertFalse(authActionBusy(true, AuthAction.SendCode, AuthAction.VerifyCode))
    }

    @Test fun resendingDoesNotAnimateVerification() {
        assertTrue(authActionBusy(true, AuthAction.ResendCode, AuthAction.ResendCode))
        assertFalse(authActionBusy(true, AuthAction.ResendCode, AuthAction.VerifyCode))
    }

    @Test fun finishingAnActionClearsItsProgress() {
        AuthAction.entries.forEach { assertFalse(authActionBusy(false, it, it)) }
    }

    @Test fun noSelectionDoesNotAttributeBackgroundWorkToAnyButton() {
        AuthAction.entries.forEach { assertFalse(authActionBusy(true, null, it)) }
    }
}
