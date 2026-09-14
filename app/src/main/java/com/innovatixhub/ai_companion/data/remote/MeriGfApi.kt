package com.eva.ai.data.remote

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.eva.ai.EvaNotificationCenter
import com.eva.ai.R
import com.eva.ai.domain.model.AuthSession
import com.eva.ai.domain.model.ChatMessage
import com.eva.ai.domain.model.CompanionProfile
import com.eva.ai.domain.model.ConversationPreview
import com.eva.ai.domain.model.EvaUser
import com.eva.ai.domain.model.MessageKind
import com.eva.ai.domain.model.ReplyStyle
import com.eva.ai.domain.model.SendMessageResult
import com.eva.ai.domain.model.SubscriptionCheckout
import com.eva.ai.domain.model.SubscriptionPlan
import com.eva.ai.domain.model.SubscriptionState
import com.eva.ai.domain.model.VoiceSendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class MeriGfApi @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("meri_gf_session", Context.MODE_PRIVATE)
    private val baseUrl = context.getString(R.string.backend_base_url).trimEnd('/')

    fun savedAccessToken(): String? = prefs.getString("access_token", null)

    private fun savedRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun hasSavedSession(): Boolean =
        !savedAccessToken().isNullOrBlank() || !savedRefreshToken().isNullOrBlank()

    fun savedUser(): EvaUser? {
        val id = prefs.getString("user_id", null)?.takeIf { it.isNotBlank() } ?: return null
        return EvaUser(
            id = id,
            name = prefs.getString("user_name", null)?.takeIf { it.isNotBlank() } ?: "Vardhan",
            email = prefs.getString("user_email", null).orEmpty(),
            avatarUrl = prefs.getString("user_avatar_url", null)?.takeIf { it.isNotBlank() }
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun googleSignInUri(): Uri {
        val redirectUri = "ai-companion://auth/google"
        val encodedRedirect = URLEncoder.encode(redirectUri, Charsets.UTF_8.name())
        return Uri.parse("$baseUrl/auth/google/start?redirectUri=$encodedRedirect")
    }

    suspend fun exchangeGoogleCode(code: String): AuthSession {
        val data = requestObject(
            method = "POST",
            path = "/auth/google/mobile/exchange",
            body = JSONObject().put("code", code),
            authorized = false
        )
        return saveSession(data)
    }

    suspend fun saveRedirectSession(accessToken: String, refreshToken: String?): AuthSession {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()

        val user = runCatching { me() }
            .getOrElse {
                EvaUser(id = "google-user", name = "Vardhan", email = "")
            }
        saveUser(user)
        return AuthSession(accessToken, refreshToken, user)
    }

    suspend fun requestEmailCode(name: String, email: String) {
        requestJson(
            method = "POST",
            path = "/auth/email/start",
            body = JSONObject()
                .put("name", name)
                .put("email", email),
            authorized = false
        )
    }

    suspend fun verifyEmailCode(email: String, code: String): AuthSession {
        val data = requestObject(
            method = "POST",
            path = "/auth/email/verify",
            body = JSONObject()
                .put("email", email)
                .put("code", code),
            authorized = false
        )
        return saveSession(data)
    }

    suspend fun signInWithGoogle(idToken: String): AuthSession {
        val data = requestObject(
            method = "POST",
            path = "/auth/google",
            body = JSONObject().put("idToken", idToken),
            authorized = false
        )
        return saveSession(data)
    }

    suspend fun me(): EvaUser {
        val data = requestObject(method = "GET", path = "/auth/me")
        return parseUser(data.optJSONObject("user") ?: data).also(::saveUser)
    }

    suspend fun subscriptionStatus(sync: Boolean = false): SubscriptionState {
        val data = requestObject(
            method = if (sync) "POST" else "GET",
            path = if (sync) "/subscriptions/sync" else "/subscriptions/me",
            body = if (sync) JSONObject() else null
        )
        return parseSubscriptionState(data)
    }

    suspend fun startSubscription(): SubscriptionCheckout {
        val data = requestObject(
            method = "POST",
            path = "/subscriptions/checkout",
            body = JSONObject()
        )
        val subscription = parseSubscriptionState(data.optJSONObject("subscription") ?: data)
        val checkout = data.optJSONObject("checkout")
        return SubscriptionCheckout(
            subscription = subscription,
            checkoutUrl = checkout?.bestString("checkoutUrl", "shortUrl", default = "").orEmpty()
        )
    }

    suspend fun verifyGooglePlay(purchaseToken: String, productId: String): SubscriptionState {
        val data = requestObject(
            method = "POST",
            path = "/subscriptions/google/verify",
            body = JSONObject()
                .put("purchaseToken", purchaseToken)
                .put("productId", productId)
        )
        return parseSubscriptionState(data.optJSONObject("subscription") ?: data)
    }

    suspend fun conversations(): List<ConversationPreview> {
        val data = requestJson(method = "GET", path = "/conversations")
        val array = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("conversations") ?: JSONArray()
            else -> JSONArray()
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    ConversationPreview(
                        id = item.bestString("id", "_id", default = UUID.randomUUID().toString()),
                        title = item.optString("title", "Eva"),
                        companionId = item.optString("companionId", "eva"),
                        lastMessageAt = item.optString("lastMessageAt", "")
                    )
                )
            }
        }
    }

    suspend fun messages(conversationId: String): List<ChatMessage> {
        val data = requestJson(
            method = "GET",
            path = "/conversations/${conversationId.urlPath()}/messages"
        )
        val array = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("messages") ?: JSONArray()
            else -> JSONArray()
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val role = item.optString("role", item.optString("sender", "assistant"))
                val content = item.bestString("content", "text", default = "")
                if (content.isBlank()) continue
                add(
                    ChatMessage(
                        id = item.bestString("id", "_id", default = UUID.randomUUID().toString()),
                        text = content,
                        fromUser = role.equals("user", ignoreCase = true),
                        createdAtMillis = item.bestTimestampMillis(
                            "createdAt",
                            "created_at",
                            "timestamp",
                            "time"
                        ) ?: System.currentTimeMillis(),
                        kind = MessageKind.Text
                    )
                )
            }
        }
    }

    suspend fun sendMessage(
        conversationId: String?,
        content: String,
        companion: CompanionProfile,
        replyStyle: ReplyStyle
    ): SendMessageResult {
        val id = conversationId ?: createConversation(companion, replyStyle)
        val data = requestObject(
            method = "POST",
            path = "/conversations/${id.urlPath()}/messages",
            body = JSONObject()
                .put("role", "user")
                .put("content", content)
                .put("companionId", companion.id)
                .put("personalityMode", replyStyle.id)
                .put("personalityPrompt", replyStyle.instruction)
                .put("companionContext", companionContextJson(companion, replyStyle))
        )
        val reply = data.bestString("reply", "assistantReply", "content", default = "")
            .ifBlank {
                val messageObject = data.optJSONObject("assistantMessage")
                    ?: data.optJSONObject("replyMessage")
                    ?: data.optJSONArray("messages")?.lastObjectWithRole("assistant")
                messageObject?.bestString("content", "text", default = "").orEmpty()
            }
        return SendMessageResult(
            conversationId = data.optString("conversationId", id),
            assistantText = reply
        )
    }

    suspend fun sendVoiceMessage(
        conversationId: String?,
        audioBytes: ByteArray,
        mimeType: String,
        companion: CompanionProfile,
        replyStyle: ReplyStyle
    ): VoiceSendResult {
        val body = JSONObject()
            .put("conversationId", conversationId.orEmpty())
            .put("companionId", companion.id)
            .put("personalityMode", replyStyle.id)
            .put("personalityPrompt", replyStyle.instruction)
            .put("companionContext", companionContextJson(companion, replyStyle))
            .put("audioBase64", Base64.encodeToString(audioBytes, Base64.NO_WRAP))
            .put("mimeType", mimeType)
        val data = requestObject(
            method = "POST",
            path = "/voice/chat",
            body = body
        )
        val audio = data.optJSONObject("audio")
        val reply = data.bestString("reply", "assistantReply", "content", default = "")
            .ifBlank {
                val messageObject = data.optJSONObject("assistantMessage")
                    ?: data.optJSONObject("replyMessage")
                    ?: data.optJSONArray("messages")?.lastObjectWithRole("assistant")
                messageObject?.bestString("content", "text", default = "").orEmpty()
            }
        return VoiceSendResult(
            conversationId = data.bestString("conversationId", default = conversationId.orEmpty()),
            transcript = data.bestString("transcript", "text", default = "Voice note"),
            assistantText = reply,
            audioBase64 = audio?.bestString("base64", "audioBase64", default = "")
                ?: data.bestString("audioBase64", default = ""),
            audioMimeType = audio?.bestString("mimeType", default = "audio/wav")
                ?: data.bestString("audioMimeType", default = "audio/wav")
        )
    }

    private suspend fun createConversation(companion: CompanionProfile, replyStyle: ReplyStyle): String {
        val data = requestObject(
            method = "POST",
            path = "/conversations",
            body = JSONObject()
                .put("title", companion.name)
                .put("companionId", companion.id)
                .put("personalityMode", replyStyle.id)
                .put("companionContext", companionContextJson(companion, replyStyle))
        )
        return data.bestString("id", "_id", "conversationId", default = UUID.randomUUID().toString())
    }

    private fun companionContextJson(companion: CompanionProfile, replyStyle: ReplyStyle): JSONObject =
        JSONObject()
            .put("id", companion.id)
            .put("name", companion.name)
            .put("gender", "female")
            .put("bornIn", companion.bornIn)
            .put("age", companion.age)
            .put("personality", companion.personality)
            .put("voiceStyle", companion.voiceStyle)
            .put("interests", companion.interests)
            .put("about", companion.about)
            .put(
                "replyStyle",
                JSONObject()
                    .put("id", replyStyle.id)
                    .put("label", replyStyle.label)
                    .put("instruction", replyStyle.instruction)
            )

    private fun saveSession(data: JSONObject): AuthSession {
        val accessToken = data.bestString("accessToken", "token", default = "")
        if (accessToken.isBlank()) throw ApiException("Backend did not return an access token.")
        val refreshToken = data.bestString("refreshToken", default = "").ifBlank { null }
        val user = parseUser(data.optJSONObject("user") ?: data)

        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
        saveUser(user)

        return AuthSession(accessToken, refreshToken, user)
    }

    private fun saveUser(user: EvaUser) {
        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_name", user.name)
            .putString("user_email", user.email)
            .putString("user_avatar_url", user.avatarUrl.orEmpty())
            .apply()
    }

    private fun parseUser(json: JSONObject): EvaUser {
        val email = json.optString("email", "")
        val fallbackName = email.substringBefore("@").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }.ifBlank { "Vardhan" }
        return EvaUser(
            id = json.bestString("userId", "id", "_id", default = "user"),
            name = json.bestString("name", "displayName", "preferredName", default = fallbackName),
            email = email,
            avatarUrl = json.optString("avatarUrl", "").ifBlank { null }
        )
    }

    private fun parseSubscriptionState(json: JSONObject): SubscriptionState {
        val planJson = json.optJSONObject("plan") ?: JSONObject()
        val amount = planJson.optInt("amount", 29900)
        val currency = planJson.optString("currency", "INR").ifBlank { "INR" }
        val formattedAmount = planJson.bestString("formattedAmount", default = "")
            .ifBlank { "$currency ${amount / 100}" }
        val plan = SubscriptionPlan(
            planId = planJson.bestString("planId", "id", default = "plan_TRv3HKpujDyFoS"),
            name = planJson.bestString("name", default = "Eva Premium Monthly"),
            amount = amount,
            formattedAmount = formattedAmount,
            currency = currency,
            interval = planJson.bestString("interval", default = "monthly")
        )
        val usage = json.optJSONObject("usage")
        return SubscriptionState(
            active = json.optBoolean("active", false),
            status = json.optString("status", "none"),
            providerSubscriptionId = json.bestString("providerSubscriptionId", "subscriptionId", default = ""),
            checkoutUrl = json.bestString("checkoutUrl", default = ""),
            plan = plan,
            currentEnd = json.optString("currentEnd", "").ifBlank { null },
            freeUsed = usage?.optInt("freeUsed", 0) ?: 0,
            freeLimit = usage?.optInt("freeLimit", 10) ?: 10,
            freeRemaining = usage?.takeIf { !it.isNull("freeRemaining") }?.optInt("freeRemaining", 0)
        )
    }

    suspend fun reportAiContent(
        conversationId: String?,
        messageId: String?,
        companionId: String,
        reason: String = "offensive_ai_content",
        details: String = ""
    ) {
        requestJson(
            method = "POST",
            path = "/reports",
            body = JSONObject()
                .put("conversationId", conversationId.orEmpty())
                .put("messageId", messageId.orEmpty())
                .put("companionId", companionId)
                .put("reason", reason)
                .put("details", details.take(1000))
        )
    }

    suspend fun registerFcmDevice(): Boolean = runCatching {
        val token = EvaNotificationCenter.savedFcmToken(context) ?: return false
        requestJson(
            method = "POST",
            path = "/devices",
            body = JSONObject()
                .put("token", token)
                .put("platform", "android")
        )
        true
    }.getOrDefault(false)

    suspend fun unregisterFcmDevice(): Boolean = runCatching {
        val token = EvaNotificationCenter.savedFcmToken(context) ?: return false
        requestJson(
            method = "DELETE",
            path = "/devices/${token.urlPath()}"
        )
        true
    }.getOrDefault(false)

    private suspend fun requestObject(
        method: String,
        path: String,
        body: JSONObject? = null,
        authorized: Boolean = true,
        allowRefresh: Boolean = true
    ): JSONObject {
        val data = requestJson(method, path, body, authorized, allowRefresh)
        return data as? JSONObject ?: throw ApiException("Backend returned an unexpected response.")
    }

    private suspend fun requestJson(
        method: String,
        path: String,
        body: JSONObject? = null,
        authorized: Boolean = true,
        allowRefresh: Boolean = true
    ): Any = withContext(Dispatchers.IO) {
        val url = URI("$baseUrl$path").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 90_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (authorized) {
                savedAccessToken()?.let { token ->
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
            doInput = true
            if (body != null) doOutput = true
        }

        try {
            if (body != null) {
                connection.outputStream.use { stream ->
                    stream.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val code = connection.responseCode
            val responseStream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val responseText = responseStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            if (
                code == HttpURLConnection.HTTP_UNAUTHORIZED &&
                authorized &&
                allowRefresh &&
                refreshSessionIfPossible()
            ) {
                return@withContext requestJson(
                    method = method,
                    path = path,
                    body = body,
                    authorized = true,
                    allowRefresh = false
                )
            }

            if (responseText.isBlank()) {
                if (code in 200..299) return@withContext JSONObject()
                throw ApiException("Backend returned $code.", code)
            }

            val root = JSONObject(responseText)
            if (code !in 200..299 || root.optBoolean("success", true) == false) {
                val message = root.optJSONObject("error")?.optString("message")
                    ?: root.optString("message", "Backend returned $code.")
                throw ApiException(message, code)
            }
            val data = root.opt("data")
            if (data == null || data == JSONObject.NULL) root else data
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun refreshSessionIfPossible(): Boolean {
        val refreshToken = savedRefreshToken()?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            val data = requestObject(
                method = "POST",
                path = "/auth/refresh",
                body = JSONObject().put("refreshToken", refreshToken),
                authorized = false,
                allowRefresh = false
            )
            saveSession(data)
        }.isSuccess
    }
}

class ApiException(message: String, val statusCode: Int? = null) : Exception(message)
fun JSONObject.bestString(vararg keys: String, default: String = ""): String {
    for (key in keys) {
        val value = optString(key, "")
        if (value.isNotBlank()) return value
    }
    return default
}

fun JSONObject.bestTimestampMillis(vararg keys: String): Long? {
    for (key in keys) {
        val parsed = parseBackendTimestampMillis(optString(key, ""))
        if (parsed != null) return parsed
    }
    return null
}

private fun parseBackendTimestampMillis(value: String): Long? {
    val raw = value.trim()
    if (raw.isBlank()) return null
    raw.toLongOrNull()?.let { return it }

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(raw)?.time
        }.getOrNull()
    }
}

fun JSONArray.lastObjectWithRole(role: String): JSONObject? {
    for (index in length() - 1 downTo 0) {
        val item = optJSONObject(index) ?: continue
        if (item.optString("role", "").equals(role, ignoreCase = true)) return item
    }
    return null
}

fun String.urlPath(): String = URLEncoder.encode(this, Charsets.UTF_8.name())


