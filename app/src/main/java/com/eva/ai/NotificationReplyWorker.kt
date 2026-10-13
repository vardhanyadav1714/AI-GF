package com.eva.ai

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eva.ai.data.remote.ApiException
import com.eva.ai.data.remote.MeriGfApi
import com.eva.ai.domain.model.companionById
import com.eva.ai.domain.model.replyStyleById
import kotlinx.coroutines.CancellationException

class NotificationReplyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val api = MeriGfApi(applicationContext)
        val user = inputData.getString("recipientUserId") ?: return Result.failure()
        val conversation = inputData.getString("conversationId") ?: return Result.failure()
        val companion = inputData.getString("companionId") ?: return Result.failure()
        val content = inputData.getString("content") ?: return Result.failure()
        val originalId = inputData.getInt("notificationId", 0)
        if (api.savedUser()?.id != user) {
            NotificationManagerCompat.from(applicationContext).cancel(originalId)
            return Result.failure()
        }
        try {
            val profile = if (companion.startsWith("custom_")) api.customCompanions().firstOrNull { it.id == companion && !it.archived }?.profile()
                ?: throw ApiException("Companion not found or archived.", 404) else companionById(companion)
            val result = api.sendMessage(conversation, content, profile, replyStyleById("natural"), requestId = id.toString())
            if (api.savedUser()?.id != user) return Result.failure()
            NotificationManagerCompat.from(applicationContext).cancel(originalId)
            if (!EvaNotificationCenter.tryDeliverToOpenConversation(conversation)) {
                EvaNotificationCenter.showCompanionMessage(applicationContext, profile.name,
                    result.assistantText, conversation, companion, user, result.assistantMessageId)
            }
            return Result.success()
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            val status = (error as? ApiException)?.statusCode
            if (runAttemptCount < 2 && (status == null || status >= 500 || status == 409 || status == 429)) return Result.retry()
            if (api.savedUser()?.id == user) {
                val message = when (status) {
                    402 -> "Your free messages are used. Open Eva to continue with Premium."
                    401 -> "Open Eva to sign in before replying."
                    404 -> "This companion is unavailable or archived. Open Eva to restore it."
                    else -> "Reply could not be confirmed. Open chat before sending again."
                }
                EvaNotificationCenter.showCompanionMessage(applicationContext, inputData.getString("companionName") ?: "Eva", message,
                    conversation, companion, user, notificationId = originalId, allowReply = false)
            }
            return Result.failure()
        }
    }
}
