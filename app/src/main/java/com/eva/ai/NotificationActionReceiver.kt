package com.eva.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.eva.ai.data.remote.MeriGfApi
import com.eva.ai.domain.model.companionById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val conversation = intent.getStringExtra("conversationId") ?: return
        val notificationId = intent.getIntExtra("notificationId", 0)
        if (intent.action == ACTION_DISMISS) {
            NotificationManagerCompat.from(context).cancel(notificationId)
            return
        }
        if (intent.action != ACTION_REPLY) return
        val user = intent.getStringExtra("recipientUserId") ?: return
        if (MeriGfApi(context).savedUser()?.id != user) return
        val text = notificationReplyText(RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_REPLY)) ?: return
        val companion = intent.getStringExtra("companionId") ?: return
        val work = OneTimeWorkRequestBuilder<NotificationReplyWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf("conversationId" to conversation, "recipientUserId" to user,
                "companionId" to companion, "content" to text, "notificationId" to notificationId))
            .addTag("eva-notification-replies")
            .build()
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EvaNotificationCenter.showCompanionMessage(appContext, companionById(companion).name, "Sending your reply...", conversation,
                    companion, user, notificationId = notificationId, allowReply = false)
                WorkManager.getInstance(appContext).enqueueUniqueWork("eva-reply-$user-$notificationId", ExistingWorkPolicy.KEEP, work)
                    .result.get(8, TimeUnit.SECONDS)
            } catch (_: Exception) {
                if (MeriGfApi(appContext).savedUser()?.id == user) {
                    EvaNotificationCenter.showCompanionMessage(appContext, companionById(companion).name,
                        "Open chat to check whether your reply was sent.", conversation, companion, user,
                        notificationId = notificationId, allowReply = false)
                }
            } finally { pending.finish() }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.eva.ai.NOTIFICATION_REPLY"
        const val ACTION_DISMISS = "com.eva.ai.NOTIFICATION_DISMISS"
        const val KEY_REPLY = "eva_reply_text"
    }
}
