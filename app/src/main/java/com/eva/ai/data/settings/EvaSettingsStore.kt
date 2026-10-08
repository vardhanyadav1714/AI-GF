package com.eva.ai.data.settings

import android.content.Context
import com.eva.ai.domain.model.CompanionProfile
import com.eva.ai.domain.model.ReplyStyle
import com.eva.ai.domain.model.companionById
import com.eva.ai.domain.model.replyStyleById
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EvaSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "eva_app_settings",
        Context.MODE_PRIVATE
    )

    fun selectedCompanion(): CompanionProfile =
        companionById(prefs.getString(KEY_SELECTED_COMPANION_ID, "eva"))

    fun selectedReplyStyle(): ReplyStyle =
        replyStyleById(prefs.getString(KEY_SELECTED_REPLY_STYLE_ID, "natural"))

    /** null = follow the system theme; true/false = explicit user override. */
    fun lightModePref(): Boolean? =
        if (prefs.contains(KEY_LIGHT_MODE)) prefs.getBoolean(KEY_LIGHT_MODE, false) else null

    fun saveSelectedCompanion(companion: CompanionProfile) {
        prefs.edit()
            .putString(KEY_SELECTED_COMPANION_ID, companion.id)
            .apply()
    }

    fun saveSelectedReplyStyle(style: ReplyStyle) {
        prefs.edit()
            .putString(KEY_SELECTED_REPLY_STYLE_ID, style.id)
            .apply()
    }

    fun saveLightMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_LIGHT_MODE, enabled)
            .apply()
    }

    fun billingState(userId: String): String? = prefs.getString(billingStateKey(userId), null)

    fun saveBillingState(userId: String, state: String) {
        prefs.edit().putString(billingStateKey(userId), state).apply()
    }

    fun trialNoticeSeen(userId: String): Boolean = prefs.getBoolean(billingStateKey(userId) + "_trial_seen", false)

    fun acknowledgeTrialNotice(userId: String) {
        prefs.edit().putBoolean(billingStateKey(userId) + "_trial_seen", true).apply()
    }

    private fun billingStateKey(userId: String): String = "billing_state_" +
        java.security.MessageDigest.getInstance("SHA-256").digest(userId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private companion object {
        const val KEY_SELECTED_COMPANION_ID = "selected_companion_id"
        const val KEY_SELECTED_REPLY_STYLE_ID = "selected_reply_style_id"
        const val KEY_LIGHT_MODE = "light_mode"
    }
}
