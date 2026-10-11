package com.eva.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.eva.ai.data.remote.MeriGfApi
import com.eva.ai.domain.logic.shouldSuppressChatNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class EvaFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var api: MeriGfApi

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val recipient = data["recipientUserId"]
        if (recipient != null && api.savedUser()?.id != recipient) return
        val title = message.notification?.title
            ?: data["title"]
            ?: data["companionName"]?.let { "$it replied" }
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: data["reply"]
            ?: "You have a new message."
        val conversationId = data["conversationId"]

        if (conversationId != null && EvaNotificationCenter.tryDeliverToOpenConversation(conversationId)) {
            return
        }

        EvaNotificationCenter.showCompanionMessage(
            context = this,
            title = title,
            body = body,
            conversationId = conversationId,
            companionId = data["companionId"], recipientUserId = recipient, messageId = data["messageId"]
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        EvaNotificationCenter.saveFcmToken(this, token)
        serviceScope.launch { api.registerFcmDevice() }
    }
}

object EvaNotificationCenter {
    private const val TOKEN_PREFS = "eva_notification_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"
    const val EXTRA_CONVERSATION_ID = "conversationId"

    private val activeConversationId = MutableStateFlow<String?>(null)
    private val appForeground = MutableStateFlow(false)
    private val conversationEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            context.getString(R.string.eva_notifications_channel_id),
            context.getString(R.string.eva_notifications_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Messages and reminders from Eva."
        }
        manager.createNotificationChannel(channel)
    }

    fun saveFcmToken(context: Context, token: String) {
        context.getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    fun savedFcmToken(context: Context): String? =
        context.getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
            ?.takeIf { it.isNotBlank() }

    fun setActiveConversation(conversationId: String?) {
        activeConversationId.value = conversationId
    }

    fun setAppForeground(foreground: Boolean) {
        appForeground.value = foreground
    }

    fun openConversations(): MutableSharedFlow<String> = conversationEvents

    /**
     * Returns true when the given conversation is currently open on screen; the
     * event is emitted so the chat can refresh itself instead of posting a
     * notification for a message the user is already looking at.
     */
    fun tryDeliverToOpenConversation(conversationId: String): Boolean {
        if (!shouldSuppressChatNotification(appForeground.value, activeConversationId.value, conversationId, conversationEvents.subscriptionCount.value > 0)) return false
        return conversationEvents.tryEmit(conversationId)
    }

    fun showCompanionMessage(
        context: Context,
        title: String,
        body: String,
        conversationId: String? = null,
        companionId: String? = null,
        recipientUserId: String? = null,
        messageId: String? = null,
        notificationId: Int = (messageId ?: "$conversationId:$body").hashCode() and Int.MAX_VALUE,
        allowReply: Boolean = true
    ) {
        ensureChannels(context)
        if (!canPostNotifications(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            conversationId?.let { putExtra(EXTRA_CONVERSATION_ID, it) }
            data = Uri.parse("eva-notification://open/${Uri.encode(conversationId ?: "inbox")}")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            context,
            context.getString(R.string.eva_notifications_channel_id)
        )
            .setSmallIcon(R.drawable.ic_notification_eva)
            .setColor(ContextCompat.getColor(context, R.color.eva_notification_color))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (conversationId != null) {
            fun actionIntent(action: String) = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                data = Uri.parse("eva-notification://action/$action/$notificationId")
                putExtra("conversationId", conversationId)
                putExtra("notificationId", notificationId)
                putExtra("companionId", companionId)
                putExtra("recipientUserId", recipientUserId)
            }
            if (allowReply && companionId != null && recipientUserId != null) {
                val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                val reply = PendingIntent.getBroadcast(context, notificationId, actionIntent(NotificationActionReceiver.ACTION_REPLY), PendingIntent.FLAG_UPDATE_CURRENT or mutable)
                builder.addAction(NotificationCompat.Action.Builder(0, "Reply", reply)
                    .addRemoteInput(RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY).setLabel("Your reply").build())
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .setAuthenticationRequired(true).setShowsUserInterface(false).build())
            }
            val dismiss = PendingIntent.getBroadcast(context, notificationId, actionIntent(NotificationActionReceiver.ACTION_DISMISS), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, "Dismiss", dismiss)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission can be revoked after the earlier check.
        }
    }

    fun canPostNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED)
}
