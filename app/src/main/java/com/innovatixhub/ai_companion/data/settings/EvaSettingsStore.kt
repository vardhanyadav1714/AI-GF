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

    private companion object {
        const val KEY_SELECTED_COMPANION_ID = "selected_companion_id"
        const val KEY_SELECTED_REPLY_STYLE_ID = "selected_reply_style_id"
    }
}
