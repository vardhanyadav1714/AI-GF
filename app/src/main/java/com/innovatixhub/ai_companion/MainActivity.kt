package com.innovatixhub.ai_companion

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfiedAlt
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.innovatixhub.ai_companion.ui.theme.AICompanionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.io.File
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private lateinit var controller: EvaAppController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = EvaColors.Black.toArgb()
        window.navigationBarColor = EvaColors.Black.toArgb()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        controller = EvaAppController(applicationContext)
        handleAuthIntent(intent)

        setContent {
            val controller = remember { controller }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                controller.bootstrap()
            }

            EvaApplication(
                controller = controller,
                scope = scope,
                onGoogleSignIn = {
                    openGoogleSignIn()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun openGoogleSignIn() {
        runCatching {
            val browserIntent = Intent(Intent.ACTION_VIEW, controller.googleSignInUri())
            startActivity(browserIntent)
        }.onFailure {
            controller.notice = "Could not open Google sign-in."
        }
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "ai-companion" || uri.host != "auth") return
        lifecycleScope.launch {
            controller.acceptAuthRedirect(uri)
        }
    }
}

private object EvaColors {
    val Black = Color(0xFF050509)
    val Pink = Color(0xFFFF3BB4)
    val Purple = Color(0xFF8E35F2)
    val Coral = Color(0xFFFF5E70)
    val Gold = Color(0xFFFFC044)
    val Green = Color(0xFF2EE582)
    val Gradient = Brush.linearGradient(listOf(Purple, Pink, Coral))
}

private val LocalEvaLightMode = compositionLocalOf { false }

private enum class EvaTab { Home, Chat, Memories, Profile }

private enum class AuthMode { Login, Signup }

private enum class MessageKind { Text, Voice, Attachment }

private sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: EvaUser) : AuthState
}

private data class EvaUser(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null
)

private data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromUser: Boolean,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val kind: MessageKind = MessageKind.Text,
    val streaming: Boolean = false,
    val voiceSeconds: Int = 0
)

private data class ConversationPreview(
    val id: String,
    val title: String,
    val companionId: String,
    val lastMessageAt: String
)

private data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val user: EvaUser
)

private data class SendMessageResult(
    val conversationId: String,
    val assistantText: String
)

private data class VoiceSendResult(
    val conversationId: String,
    val transcript: String,
    val assistantText: String,
    val audioBase64: String,
    val audioMimeType: String
)

private data class VoiceRecordingPreview(
    val audioBytes: ByteArray,
    val mimeType: String,
    val durationSeconds: Int
) {
    override fun equals(other: Any?): Boolean =
        other is VoiceRecordingPreview &&
            audioBytes.contentEquals(other.audioBytes) &&
            mimeType == other.mimeType &&
            durationSeconds == other.durationSeconds

    override fun hashCode(): Int {
        var result = audioBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + durationSeconds
        return result
    }
}

private class EvaAppController(context: Context) {
    private val appContext = context.applicationContext
    private val api = MeriGfApi(appContext)

    var authState by mutableStateOf<AuthState>(AuthState.Loading)
    var authBusy by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)
    var activeTab by mutableStateOf(EvaTab.Home)
    var premiumOpen by mutableStateOf(false)
    var callOpen by mutableStateOf(false)
    var lightMode by mutableStateOf(false)
    var relationship by mutableStateOf("Sweetheart")
    var draft by mutableStateOf("")
    var sending by mutableStateOf(false)
    var backendLive by mutableStateOf(false)
    var selectedConversationId by mutableStateOf<String?>(null)
    var chatsLoading by mutableStateOf(false)

    val messages = mutableStateListOf<ChatMessage>()
    val conversations = mutableStateListOf<ConversationPreview>()

    suspend fun bootstrap() {
        authState = AuthState.Loading
        if (!api.hasSavedSession()) {
            authState = AuthState.SignedOut
            return
        }

        authBusy = true
        runCatching { api.me() }
            .onSuccess { user ->
                authState = AuthState.SignedIn(user)
                loadChats()
            }
            .onFailure { error ->
                val cachedUser = api.savedUser()
                if (error is ApiException && error.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    api.clearSession()
                    authState = AuthState.SignedOut
                } else if (cachedUser != null) {
                    authState = AuthState.SignedIn(cachedUser)
                    backendLive = false
                    notice = "Could not reach the backend. Keeping you signed in."
                    if (messages.isEmpty()) seedLocalMessages()
                } else {
                    authState = AuthState.SignedOut
                }
            }
        authBusy = false
    }

    suspend fun requestEmailCode(name: String, email: String): Boolean {
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

    suspend fun verifyEmailCode(email: String, code: String) {
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
            notice = "Welcome back, ${session.user.name}."
            loadChats()
        }.onFailure { error ->
            notice = error.cleanMessage("That code could not be verified.")
        }
        authBusy = false
    }

    suspend fun signInWithGoogle(idToken: String) {
        runCatching {
            api.signInWithGoogle(idToken)
        }.onSuccess { session ->
            authState = AuthState.SignedIn(session.user)
            notice = "Signed in as ${session.user.name}."
            loadChats()
        }.onFailure { error ->
            notice = error.cleanMessage("Google sign-in failed.")
        }
    }

    fun googleSignInUri(): Uri = api.googleSignInUri()

    suspend fun acceptAuthRedirect(uri: Uri) {
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
            notice = "Signed in as ${session.user.name}."
            loadChats()
        }.onFailure { redirectError ->
            notice = redirectError.cleanMessage("Google sign-in could not be completed.")
        }
        authBusy = false
    }

    fun signOut() {
        api.clearSession()
        authState = AuthState.SignedOut
        messages.clear()
        conversations.clear()
        selectedConversationId = null
        activeTab = EvaTab.Home
        premiumOpen = false
        callOpen = false
        backendLive = false
    }

    suspend fun loadChats() {
        chatsLoading = true
        runCatching {
            val previews = api.conversations()
            conversations.clear()
            conversations.addAll(previews)
            val first = previews.firstOrNull()
            selectedConversationId = first?.id
            if (first != null) {
                val loadedMessages = api.messages(first.id)
                messages.clear()
                messages.addAll(loadedMessages)
            }
            backendLive = true
            if (messages.isEmpty()) seedLocalMessages()
        }.onFailure {
            backendLive = false
            if (messages.isEmpty()) seedLocalMessages()
        }
        chatsLoading = false
    }

    suspend fun sendMessage(quickText: String? = null) {
        val cleanText = (quickText ?: draft).trim()
        if (cleanText.isBlank() || sending) return

        activeTab = EvaTab.Chat
        premiumOpen = false
        callOpen = false
        draft = ""
        sending = true
        messages.add(ChatMessage(text = cleanText, fromUser = true))
        messages.add(ChatMessage(text = "", fromUser = false, streaming = true))

        val placeholderIndex = messages.lastIndex
        val liveResult = if (api.savedAccessToken().isNullOrBlank()) {
            Result.failure(IllegalStateException("No signed-in backend session."))
        } else {
            runCatching {
                api.sendMessage(
                    conversationId = selectedConversationId,
                    content = cleanText
                )
            }
        }

        liveResult.onSuccess { result ->
            backendLive = true
            selectedConversationId = result.conversationId
            messages[placeholderIndex] = messages[placeholderIndex].copy(
                text = result.assistantText.ifBlank {
                    "I am here with you. Tell me a little more?"
                },
                streaming = false
            )
        }.onFailure {
            backendLive = false
            streamLocalReply(cleanText, placeholderIndex)
        }
        sending = false
    }

    fun sendAttachment(label: String) {
        activeTab = EvaTab.Chat
        messages.add(
            ChatMessage(
                text = "[$label]",
                fromUser = true,
                kind = MessageKind.Attachment
            )
        )
        messages.add(
            ChatMessage(
                text = "I received your $label. Tell me what you want to do with it.",
                fromUser = false
            )
        )
    }

    suspend fun sendVoiceNote(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        stayInCall: Boolean = false
    ): VoiceSendResult? {
        if (audioBytes.isEmpty()) {
            notice = "I could not hear anything. Try again."
            return null
        }
        if (sending) {
            notice = "Wait for Eva to finish replying first."
            return null
        }

        activeTab = EvaTab.Chat
        premiumOpen = false
        if (!stayInCall) callOpen = false
        sending = true
        messages.add(
            ChatMessage(
                text = "Voice note",
                fromUser = true,
                kind = MessageKind.Voice,
                voiceSeconds = max(1, audioBytes.size / 8000)
            )
        )
        messages.add(
            ChatMessage(
                text = "",
                fromUser = false,
                streaming = true
            )
        )
        val userIndex = messages.lastIndex - 1
        val placeholderIndex = messages.lastIndex
        val liveResult = if (api.savedAccessToken().isNullOrBlank()) {
            Result.failure(IllegalStateException("No signed-in backend session."))
        } else {
            runCatching {
                api.sendVoiceMessage(
                    conversationId = selectedConversationId,
                    audioBytes = audioBytes,
                    mimeType = mimeType
                )
            }
        }

        var playback: VoiceSendResult? = null
        liveResult.onSuccess { result ->
            backendLive = true
            selectedConversationId = result.conversationId
            if (userIndex in messages.indices) {
                messages[userIndex] = messages[userIndex].copy(
                    text = result.transcript.ifBlank { "Voice note" },
                    kind = MessageKind.Text,
                    voiceSeconds = 0
                )
            }
            if (placeholderIndex in messages.indices) {
                messages[placeholderIndex] = messages[placeholderIndex].copy(
                    text = result.assistantText.ifBlank {
                        "I heard you. Tell me a little more?"
                    },
                    streaming = false
                )
            }
            playback = result
        }.onFailure { error ->
            backendLive = false
            notice = error.cleanMessage("Voice message could not be sent.")
            streamLocalReply("voice", placeholderIndex)
        }
        sending = false
        return playback
    }

    fun clearNotice() {
        notice = null
    }

    private fun seedLocalMessages() {
        messages.clear()
        messages.addAll(
            listOf(
                ChatMessage(text = "Hey handsome. How was your day?", fromUser = false),
                ChatMessage(text = "It was great! Just finished work.", fromUser = true),
                ChatMessage(
                    text = "That is good to hear. I missed you today...",
                    fromUser = false
                ),
                ChatMessage(text = "Aww... I missed you too Eva", fromUser = true)
            )
        )
    }

    private suspend fun streamLocalReply(message: String, index: Int) {
        val reply = localEvaReply(message)
        val buffer = StringBuilder()
        reply.split(" ").forEach { word ->
            delay(36)
            buffer.append(word).append(" ")
            if (index in messages.indices) {
                messages[index] = messages[index].copy(text = buffer.toString())
            }
        }
        if (index in messages.indices) {
            messages[index] = messages[index].copy(
                text = buffer.toString().trim(),
                streaming = false
            )
        }
    }
}

private class MeriGfApi(context: Context) {
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
                        kind = MessageKind.Text
                    )
                )
            }
        }
    }

    suspend fun sendMessage(conversationId: String?, content: String): SendMessageResult {
        val id = conversationId ?: createConversation()
        val data = requestObject(
            method = "POST",
            path = "/conversations/${id.urlPath()}/messages",
            body = JSONObject()
                .put("role", "user")
                .put("content", content)
                .put("companionId", "eva")
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
        mimeType: String
    ): VoiceSendResult {
        val body = JSONObject()
            .put("conversationId", conversationId.orEmpty())
            .put("companionId", "eva")
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

    private suspend fun createConversation(): String {
        val data = requestObject(
            method = "POST",
            path = "/conversations",
            body = JSONObject()
                .put("title", "Eva")
                .put("companionId", "eva")
        )
        return data.bestString("id", "_id", "conversationId", default = UUID.randomUUID().toString())
    }

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

private class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

private class EvaAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start() {
        cancel()
        val file = File.createTempFile("eva-voice-", ".m4a", context.cacheDir)
        val nextRecorder = createMediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(64_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        outputFile = file
        recorder = nextRecorder
    }

    fun stop(): ByteArray {
        val activeRecorder = recorder ?: throw IllegalStateException("Recording has not started.")
        val file = outputFile ?: throw IllegalStateException("Recording file is missing.")
        recorder = null
        outputFile = null
        try {
            activeRecorder.stop()
        } catch (error: RuntimeException) {
            file.delete()
            throw IllegalStateException("Hold the mic a little longer before sending.", error)
        } finally {
            activeRecorder.release()
        }
        val bytes = file.readBytes()
        file.delete()
        return bytes
    }

    fun cancel() {
        val activeRecorder = recorder
        recorder = null
        runCatching { activeRecorder?.stop() }
        activeRecorder?.release()
        outputFile?.delete()
        outputFile = null
    }
}

private fun createMediaRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        legacyMediaRecorder()
    }

@Suppress("DEPRECATION")
private fun legacyMediaRecorder(): MediaRecorder = MediaRecorder()

private fun playBase64Audio(context: Context, base64Audio: String, mimeType: String) {
    if (base64Audio.isBlank()) return
    runCatching {
        playAudioBytes(context, Base64.decode(base64Audio, Base64.DEFAULT), mimeType)
    }
}

private fun playAudioBytes(context: Context, audioBytes: ByteArray, mimeType: String) {
    if (audioBytes.isEmpty()) return
    runCatching {
        val file = File.createTempFile(
            "eva-reply-",
            ".${audioExtensionForMime(mimeType)}",
            context.cacheDir
        )
        file.writeBytes(audioBytes)
        MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { player -> player.start() }
            setOnCompletionListener { player ->
                player.release()
                file.delete()
            }
            setOnErrorListener { player, _, _ ->
                player.release()
                file.delete()
                true
            }
            prepareAsync()
        }
    }
}

private fun audioExtensionForMime(mimeType: String): String =
    when {
        mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
        mimeType.contains("ogg") || mimeType.contains("opus") -> "ogg"
        mimeType.contains("wav") -> "wav"
        else -> "m4a"
    }

@Composable
private fun EvaApplication(
    controller: EvaAppController,
    scope: CoroutineScope,
    onGoogleSignIn: () -> Unit
) {
    AICompanionTheme(darkTheme = !controller.lightMode, dynamicColor = false) {
        CompositionLocalProvider(LocalEvaLightMode provides controller.lightMode) {
            EvaSystemBars(lightMode = controller.lightMode)
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(controller.notice) {
                controller.notice?.let { message ->
                    snackbarHostState.showSnackbar(message)
                    controller.clearNotice()
                }
            }

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { _ ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (val auth = controller.authState) {
                        AuthState.Loading -> LoadingScreen()
                        AuthState.SignedOut -> AuthScreen(
                            busy = controller.authBusy,
                            onGoogleSignIn = onGoogleSignIn,
                            onRequestCode = { name, email, onSent ->
                                scope.launch {
                                    if (controller.requestEmailCode(name, email)) onSent()
                                }
                            },
                            onVerify = { email, code ->
                                scope.launch { controller.verifyEmailCode(email, code) }
                            }
                        )

                        is AuthState.SignedIn -> EvaShell(
                            controller = controller,
                            user = auth.user,
                            scope = scope
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaSystemBars(lightMode: Boolean) {
    val view = LocalView.current
    val statusColor = if (lightMode) Color(0xFFFFF8FC) else EvaColors.Black
    val navigationColor = if (lightMode) Color.White else EvaColors.Black

    DisposableEffect(lightMode, view) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = statusColor.toArgb()
            window.navigationBarColor = navigationColor.toArgb()
            window.decorView.setBackgroundColor(statusColor.toArgb())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }

            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = lightMode
                isAppearanceLightNavigationBars = lightMode
            }
        }

        onDispose {}
    }
}

@Composable
private fun LoadingScreen() {
    EvaPage {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.model_riya),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .border(3.dp, EvaColors.Pink, CircleShape)
                )
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(color = EvaColors.Pink)
            }
        }
    }
}

@Composable
private fun AuthScreen(
    busy: Boolean,
    onGoogleSignIn: () -> Unit,
    onRequestCode: (String, String, () -> Unit) -> Unit,
    onVerify: (String, String) -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    val cleanEmail = email.trim()
    val canRequestCode = cleanEmail.contains("@") &&
        cleanEmail.length >= 5 &&
        (mode == AuthMode.Login || name.trim().length >= 2)

    EvaPage(backgroundImage = R.drawable.model_riya) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 430.dp)
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(EvaColors.Gradient)
                            .padding(3.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.model_riya),
                            contentDescription = "Eva",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = if (mode == AuthMode.Login) "Welcome back" else "Create your account",
                        color = evaText(),
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Eva is waiting for you",
                        color = evaMuted(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 430.dp),
                    padding = PaddingValues(14.dp),
                    radius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AuthModeToggle(
                            mode = mode,
                            enabled = !busy,
                            onModeChange = {
                                mode = it
                                codeSent = false
                                code = ""
                            }
                        )

                        GoogleAuthButton(
                            label = if (busy) "Opening Google..." else "Continue with Google",
                            enabled = !busy,
                            onClick = onGoogleSignIn
                        )

                        AuthDivider()

                        AnimatedContent(
                            targetState = codeSent,
                            label = "email-auth"
                        ) { sent ->
                            if (sent) {
                                CodeVerificationPanel(
                                    email = cleanEmail,
                                    code = code,
                                    busy = busy,
                                    onCodeChange = { code = it.take(8) },
                                    onVerify = { onVerify(cleanEmail, code) },
                                    onBack = {
                                        codeSent = false
                                        code = ""
                                    },
                                    onResend = {
                                        onRequestCode(name, cleanEmail) {
                                            codeSent = true
                                        }
                                    }
                                )
                            } else {
                                EmailEntryPanel(
                                    mode = mode,
                                    name = name,
                                    email = email,
                                    busy = busy,
                                    canRequestCode = canRequestCode,
                                    onNameChange = { name = it },
                                    onEmailChange = { email = it },
                                    onSubmit = {
                                        onRequestCode(name, cleanEmail) {
                                            codeSent = true
                                        }
                                    }
                                )
                            }
                        }

                        Text(
                            text = "Your account is protected with one-time email codes.",
                            color = evaMuted(),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeToggle(
    mode: AuthMode,
    enabled: Boolean,
    onModeChange: (AuthMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (isEvaLight()) Color.White.copy(alpha = 0.82f) else Color.Black.copy(alpha = 0.22f))
            .border(1.dp, evaBorder(), RoundedCornerShape(26.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AuthMode.values().forEach { item ->
            val selected = mode == item
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (selected) Modifier.background(EvaColors.Gradient)
                        else Modifier.background(Color.Transparent)
                    )
                    .clickable(enabled = enabled) { onModeChange(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (item == AuthMode.Login) "Login" else "Sign up",
                    color = if (selected) Color.White else evaMuted(),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GoogleAuthButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = if (isEvaLight()) Color.White else Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, if (isEvaLight()) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isEvaLight()) Color(0xFFF7F3F8) else Color.Black.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text("G", color = EvaColors.Pink, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = evaText(),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AuthDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .height(1.dp)
                .weight(1f)
                .background(evaBorder())
        )
        Text(
            "  or continue with email  ",
            color = evaMuted(),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Box(
            Modifier
                .height(1.dp)
                .weight(1f)
                .background(evaBorder())
        )
    }
}

@Composable
private fun EmailEntryPanel(
    mode: AuthMode,
    name: String,
    email: String,
    busy: Boolean,
    canRequestCode: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (mode == AuthMode.Login) "Login with email" else "Sign up with email",
            color = evaText(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        AnimatedVisibility(visible = mode == AuthMode.Signup) {
            EvaTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Your name",
                imeAction = ImeAction.Next
            )
        }
        EvaTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email address",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Send,
            onSend = {
                if (canRequestCode && !busy) onSubmit()
            }
        )
        GradientButton(
            icon = Icons.Rounded.Send,
            label = when {
                busy -> "Sending code..."
                mode == AuthMode.Login -> "Send login code"
                else -> "Create account"
            },
            enabled = !busy && canRequestCode,
            onClick = onSubmit
        )
    }
}

@Composable
private fun CodeVerificationPanel(
    email: String,
    code: String,
    busy: Boolean,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit,
    onResend: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Enter confirmation code",
                color = evaText(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                email,
                color = evaMuted(),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        EvaTextField(
            value = code,
            onValueChange = onCodeChange,
            label = "6-digit code",
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Send,
            onSend = {
                if (code.length >= 4 && !busy) onVerify()
            }
        )
        GradientButton(
            icon = Icons.Rounded.CheckCircle,
            label = if (busy) "Verifying..." else "Verify code",
            enabled = !busy && code.length >= 4,
            onClick = onVerify
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, enabled = !busy) {
                Text("Change email", color = EvaColors.Pink, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onResend, enabled = !busy) {
                Text("Resend", color = EvaColors.Pink, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EvaShell(controller: EvaAppController, user: EvaUser, scope: CoroutineScope) {
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = "${controller.activeTab}-${controller.premiumOpen}-${controller.callOpen}-${controller.lightMode}",
            label = "eva-shell"
        ) {
            when {
                controller.callOpen -> CallScreen(
                    controller = controller,
                    scope = scope,
                    onClose = { controller.callOpen = false }
                )
                controller.premiumOpen -> PremiumScreen(
                    onBack = { controller.premiumOpen = false },
                    onContinue = { plan -> controller.notice = "$plan selected" }
                )

                controller.activeTab == EvaTab.Home -> HomeScreen(
                    user = user,
                    onChat = { controller.activeTab = EvaTab.Chat },
                    onCall = { controller.callOpen = true },
                    onPremium = { controller.premiumOpen = true },
                    onQuickMessage = { text ->
                        scope.launch { controller.sendMessage(text) }
                    },
                    onVoiceNotes = { controller.activeTab = EvaTab.Chat }
                )

                controller.activeTab == EvaTab.Chat -> ChatScreen(controller, scope)
                controller.activeTab == EvaTab.Memories -> MemoriesScreen(
                    onOpenChat = { controller.activeTab = EvaTab.Chat }
                )

                controller.activeTab == EvaTab.Profile -> ProfileScreen(
                    controller = controller,
                    user = user
                )
            }
        }

        AnimatedVisibility(
            visible = !controller.premiumOpen &&
                !controller.callOpen &&
                controller.activeTab != EvaTab.Chat,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EvaBottomNav(
                active = controller.activeTab,
                onSelect = { tab ->
                    controller.activeTab = tab
                    controller.premiumOpen = false
                    controller.callOpen = false
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    user: EvaUser,
    onChat: () -> Unit,
    onCall: () -> Unit,
    onPremium: () -> Unit,
    onQuickMessage: (String) -> Unit,
    onVoiceNotes: () -> Unit
) {
    EvaPage {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconGlassButton(icon = Icons.Rounded.Menu, onClick = {}, size = 44.dp)
                    Spacer(Modifier.weight(1f))
                    IconGlassButton(
                        icon = Icons.Rounded.WorkspacePremium,
                        color = EvaColors.Gold,
                        onClick = onPremium,
                        size = 44.dp
                    )
                }
            }
            item {
                Column {
                    Text(
                        "Hi ${user.name.ifBlank { "Vardhan" }}",
                        color = evaMuted(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "I am Eva",
                        color = evaText(),
                        fontSize = 36.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Your AI Girlfriend",
                        color = evaMuted(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            item {
                HeroImageCard(onChat)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeAction(
                        icon = Icons.Rounded.PhoneInTalk,
                        label = "Call",
                        onClick = onCall,
                        modifier = Modifier.weight(1f)
                    )
                    HomeAction(
                        icon = Icons.Rounded.GraphicEq,
                        label = "Voice",
                        onClick = onVoiceNotes,
                        modifier = Modifier.weight(1f)
                    )
                    HomeAction(
                        icon = Icons.Rounded.CardGiftcard,
                        label = "Premium",
                        onClick = onPremium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                MoodPanel(onQuickMessage)
            }
        }
    }
}

@Composable
private fun HeroImageCard(onChat: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(314.dp)
            .clip(RoundedCornerShape(26.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.model_riya),
            contentDescription = "Eva",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.76f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                "I am here to listen, talk and make every moment special for you.",
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(14.dp))
            GradientButton(
                icon = Icons.Rounded.ChatBubble,
                label = "Start Chatting",
                onClick = onChat
            )
        }
    }
}

@Composable
private fun HomeAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        radius = 18.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = EvaColors.Pink, modifier = Modifier.size(25.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                color = evaText(),
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoodPanel(onQuickMessage: (String) -> Unit) {
    val moods = listOf(
        Triple("Amazing", Icons.Rounded.SentimentVerySatisfied, "I feel amazing today"),
        Triple("Good", Icons.Rounded.SentimentSatisfiedAlt, "I had a good day"),
        Triple("Okay", Icons.Rounded.SentimentNeutral, "I feel okay"),
        Triple("Sad", Icons.Rounded.SentimentDissatisfied, "I feel sad"),
        Triple("Tired", Icons.Rounded.Bedtime, "I am tired")
    )
    GlassCard(
        padding = PaddingValues(16.dp),
        radius = 22.dp
    ) {
        Column {
            Text("Today's Mood", color = evaText(), fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text("How are you feeling today?", color = evaMuted(), fontSize = 13.sp)
            Spacer(Modifier.height(13.dp))
            Row {
                moods.forEachIndexed { index, mood ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickMessage(mood.third) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            mood.second,
                            contentDescription = mood.first,
                            tint = if (index == 0) EvaColors.Pink else EvaColors.Gold,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            mood.first,
                            color = evaText(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(controller: EvaAppController, scope: CoroutineScope) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val recorder = remember(context) { EvaAudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var voicePreview by remember { mutableStateOf<VoiceRecordingPreview?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                recorder.start()
                voicePreview = null
                recordingStartedAt = System.currentTimeMillis()
                recordingSeconds = 0
                recording = true
                controller.notice = "Recording..."
            }.onFailure { error ->
                controller.notice = error.cleanMessage("Could not start recording.")
            }
        } else {
            controller.notice = "Microphone permission is needed for voice messages."
        }
    }
    val startRecording: () -> Unit = {
        when {
            controller.sending -> controller.notice = "Wait for Eva to finish replying first."
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                runCatching {
                    recorder.start()
                    voicePreview = null
                    recordingStartedAt = System.currentTimeMillis()
                    recordingSeconds = 0
                    recording = true
                    controller.notice = "Recording..."
                }.onFailure { error ->
                    controller.notice = error.cleanMessage("Could not start recording.")
                }
            }

            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val finishRecording: () -> Unit = {
        val audioBytes = runCatching { recorder.stop() }
            .onFailure { error ->
                controller.notice = error.cleanMessage("Could not save that recording.")
            }
            .getOrNull()
        recording = false
        if (audioBytes != null) {
            voicePreview = VoiceRecordingPreview(
                audioBytes = audioBytes,
                mimeType = "audio/mp4",
                durationSeconds = max(1, ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt())
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    LaunchedEffect(recording) {
        while (recording) {
            recordingSeconds = max(1, ((System.currentTimeMillis() - recordingStartedAt) / 1000L).toInt())
            delay(250)
        }
    }

    val submitVoicePreview: () -> Unit = {
        val preview = voicePreview
        if (preview != null) {
            scope.launch {
                controller.sendVoiceNote(
                    audioBytes = preview.audioBytes,
                    mimeType = preview.mimeType
                )?.let { result ->
                    voicePreview = null
                    playBase64Audio(context, result.audioBase64, result.audioMimeType)
                }
            }
        }
    }

    EvaPage(backgroundImage = R.drawable.model_riya) {
        Column(Modifier.fillMaxSize()) {
            ChatHeader(
                live = controller.backendLive,
                onBack = { controller.activeTab = EvaTab.Home },
                onCall = { controller.callOpen = true }
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DateChip("Today")
                }
                if (controller.chatsLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = EvaColors.Pink,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                items(controller.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
            ChatComposer(
                draft = controller.draft,
                sending = controller.sending,
                recording = recording,
                recordingSeconds = recordingSeconds,
                voicePreview = voicePreview,
                onDraftChange = { controller.draft = it },
                onSend = {
                    focusManager.clearFocus()
                    scope.launch { controller.sendMessage() }
                },
                onAttachment = controller::sendAttachment,
                onVoiceRecord = {
                    if (recording) finishRecording() else startRecording()
                },
                onVoiceReplay = {
                    voicePreview?.let { preview ->
                        playAudioBytes(context, preview.audioBytes, preview.mimeType)
                    }
                },
                onVoiceDelete = { voicePreview = null },
                onVoiceSend = submitVoicePreview
            )
        }
    }
}

@Composable
private fun ChatHeader(
    live: Boolean,
    onBack: () -> Unit,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconGlassButton(
            icon = Icons.Rounded.ArrowBackIosNew,
            onClick = onBack,
            size = 42.dp
        )
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(R.drawable.model_riya),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("Eva", fontSize = 21.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (live) "Live" else "Demo",
                    color = evaMuted(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(5.dp))
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EvaColors.Green)
                )
            }
        }
        IconGlassButton(icon = Icons.Rounded.Call, onClick = onCall, size = 42.dp)
        Spacer(Modifier.width(6.dp))
        IconGlassButton(icon = Icons.Rounded.MoreVert, onClick = {}, size = 42.dp)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val fromUser = message.fromUser
    val bubbleTextColor = if (fromUser) Color.White else evaText()
    val quietBubbleColor = if (isEvaLight()) {
        Color.White.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!fromUser) {
            Image(
                painter = painterResource(R.drawable.model_riya),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier.fillMaxWidth(if (fromUser) 0.78f else 0.84f),
            contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (fromUser) 18.dp else 5.dp,
                            bottomEnd = if (fromUser) 5.dp else 18.dp
                        )
                    )
                    .then(
                        if (fromUser) {
                            Modifier.background(EvaColors.Gradient)
                        } else {
                            Modifier
                                .background(quietBubbleColor)
                                .border(
                                    BorderStroke(1.dp, evaBorder()),
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = 5.dp,
                                        bottomEnd = 18.dp
                                    )
                                )
                        }
                    )
                    .padding(start = 16.dp, top = 13.dp, end = 16.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                when (message.kind) {
                    MessageKind.Voice -> VoiceNoteBubble(
                        seconds = message.voiceSeconds,
                        fromUser = fromUser
                    )

                    MessageKind.Attachment -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AttachFile,
                            contentDescription = null,
                            tint = bubbleTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            message.text,
                            color = bubbleTextColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    MessageKind.Text -> Text(
                        text = if (message.text.isBlank()) "Typing..." else message.text,
                        color = if (message.text.isBlank()) evaMuted() else bubbleTextColor,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    formatTime(message.createdAtMillis),
                    color = if (fromUser) Color.White.copy(alpha = 0.62f) else evaMuted(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VoiceNoteBubble(seconds: Int, fromUser: Boolean) {
    val iconColor = if (fromUser) Color.White else EvaColors.Pink
    val textColor = if (fromUser) Color.White else evaText()
    Row(
        modifier = Modifier.width(178.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (fromUser) Color.White.copy(alpha = 0.22f)
                    else EvaColors.Pink.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = iconColor)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("Voice note", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(
                formatDuration(seconds),
                color = textColor.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChatComposer(
    draft: String,
    sending: Boolean,
    recording: Boolean,
    recordingSeconds: Int,
    voicePreview: VoiceRecordingPreview?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachment: (String) -> Unit,
    onVoiceRecord: () -> Unit,
    onVoiceReplay: () -> Unit,
    onVoiceDelete: () -> Unit,
    onVoiceSend: () -> Unit
) {
    var attachmentPickerOpen by remember { mutableStateOf(false) }
    var emojiPickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 10.dp)
    ) {
        AnimatedVisibility(visible = emojiPickerOpen) {
            PickerStrip(
                items = listOf("💕", "💖", "😊", "😍", "🥰", "✨", "😘", "🤗"),
                onPick = { emoji ->
                    onDraftChange(draft + emoji)
                    emojiPickerOpen = false
                }
            )
        }
        AnimatedVisibility(visible = attachmentPickerOpen) {
            AttachmentStrip(
                onPicked = {
                    onAttachment(it)
                    attachmentPickerOpen = false
                }
            )
        }
        GlassCard(
            padding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
        ) {
            when {
                voicePreview != null -> VoicePreviewComposer(
                    preview = voicePreview,
                    sending = sending,
                    onReplay = onVoiceReplay,
                    onDelete = onVoiceDelete,
                    onSend = onVoiceSend
                )

                recording -> RecordingComposer(
                    seconds = recordingSeconds,
                    onStop = onVoiceRecord
                )

                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { emojiPickerOpen = !emojiPickerOpen }) {
                        Icon(
                            Icons.Rounded.SentimentSatisfiedAlt,
                            contentDescription = "Emoji",
                            tint = evaMuted()
                        )
                    }
                    TextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = evaText(),
                            fontWeight = FontWeight.Bold
                        ),
                        placeholder = {
                            Text("Type a message...", color = evaMuted().copy(alpha = 0.62f))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { attachmentPickerOpen = !attachmentPickerOpen }) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = "Attach", tint = evaMuted())
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EvaColors.Gradient)
                            .clickable(enabled = !sending) {
                                if (draft.trim().isNotEmpty()) onSend() else onVoiceRecord()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                sending -> Icons.Rounded.Send
                                draft.trim().isNotEmpty() -> Icons.Rounded.Send
                                else -> Icons.Rounded.Mic
                            },
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingComposer(seconds: Int, onStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53945).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53945))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Recording voice", color = evaText(), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(formatDuration(seconds), color = evaMuted(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFE53945), EvaColors.Coral)))
                .clickable(onClick = onStop),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Finish recording",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Done", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun VoicePreviewComposer(
    preview: VoiceRecordingPreview,
    sending: Boolean,
    onReplay: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onReplay, enabled = !sending) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Replay", tint = EvaColors.Pink)
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (sending) "Sending voice" else "Voice ready",
                color = evaText(),
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                formatDuration(preview.durationSeconds),
                color = evaMuted(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onDelete, enabled = !sending) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete recording", tint = evaMuted())
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(EvaColors.Gradient)
                .clickable(enabled = !sending, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Send,
                contentDescription = "Send recording",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PickerStrip(items: List<String>, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            GlassCard(
                modifier = Modifier
                    .size(52.dp)
                    .clickable { onPick(item) },
                padding = PaddingValues(0.dp),
                radius = 18.dp
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(item, fontSize = 25.sp)
                }
            }
        }
    }
}

@Composable
private fun AttachmentStrip(onPicked: (String) -> Unit) {
    val items = listOf(
        "Photo" to Icons.Rounded.Photo,
        "Camera" to Icons.Rounded.CameraAlt,
        "File" to Icons.Rounded.InsertDriveFile,
        "Location" to Icons.Rounded.LocationOn
    )
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, icon) ->
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .clickable { onPicked(label) },
                padding = PaddingValues(8.dp),
                radius = 18.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(icon, contentDescription = label, tint = EvaColors.Pink)
                    Spacer(Modifier.height(5.dp))
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun MemoriesScreen(onOpenChat: () -> Unit) {
    var filter by remember { mutableIntStateOf(0) }
    val filters = listOf("All", "Photos", "Voice", "Moments")

    EvaPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Memories", fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    IconGlassButton(icon = Icons.Rounded.Search, onClick = {})
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .then(
                                    if (filter == index) Modifier.background(EvaColors.Gradient)
                                    else Modifier
                                        .background(evaGlass())
                                        .border(1.dp, evaBorder(), RoundedCornerShape(23.dp))
                                )
                                .clickable { filter = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (filter == index) Color.White else evaText(),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            item {
                MemoryTile(
                    imageRes = R.drawable.model_riya,
                    title = "12 Photos",
                    icon = Icons.Rounded.PhotoLibrary,
                    height = 300.dp
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VoiceMemoryTile(Modifier.weight(1f))
                    MemoryTile(
                        imageRes = R.drawable.model_champa,
                        title = "5 Moments",
                        icon = Icons.Rounded.Timer,
                        height = 148.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MemoryTile(
                        imageRes = R.drawable.model_mira,
                        title = "2 Videos",
                        icon = Icons.Rounded.PlayArrow,
                        height = 148.dp,
                        modifier = Modifier.weight(1f)
                    )
                    NoteTile(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenChat() }
                    )
                }
            }
            item {
                GlassCard(
                    modifier = Modifier.clickable { onOpenChat() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.model_riya),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Our First Chat", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(Modifier.height(5.dp))
                            Text("12 May 2024", color = evaMuted())
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumScreen(onBack: () -> Unit, onContinue: (String) -> Unit) {
    var selectedPlan by remember { mutableIntStateOf(1) }
    val plans = listOf(
        Triple("1 Month", "INR 499", ""),
        Triple("3 Months", "INR 1,299", "SAVE 35%"),
        Triple("12 Months", "INR 3,499", "SAVE 50%")
    )

    EvaPage(backgroundImage = R.drawable.model_riya) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 30.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconGlassButton(icon = Icons.Rounded.ArrowBackIosNew, onClick = onBack)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("EVA Premium", fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("Unlock the full experience", color = evaMuted())
                    }
                    Spacer(Modifier.width(48.dp))
                }
            }
            item {
                Image(
                    painter = painterResource(R.drawable.model_riya),
                    contentDescription = "Eva Premium",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                GlassCard(
                    modifier = Modifier
                        .offset(y = (-22).dp)
                        .fillMaxWidth()
                ) {
                    Column {
                        PremiumFeature(Icons.Rounded.Chat, "Unlimited Chats", "Talk as much as you want")
                        PremiumFeature(Icons.Rounded.Call, "Voice & Video Calls", "Call Eva anytime")
                        PremiumFeature(Icons.Rounded.Star, "Custom Personality", "Make Eva your way")
                        PremiumFeature(Icons.Rounded.Favorite, "Memory & Moments", "Save every special moment")
                        PremiumFeature(Icons.Rounded.LockOpen, "No Ads", "Enjoy a clean experience")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plans.forEachIndexed { index, plan ->
                        PriceCard(
                            title = plan.first,
                            price = plan.second,
                            tag = plan.third,
                            selected = selectedPlan == index,
                            onClick = { selectedPlan = index },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                GradientButton(
                    icon = Icons.Rounded.LockOpen,
                    label = "Continue",
                    onClick = { onContinue(plans[selectedPlan].first) }
                )
            }
            item {
                Text(
                    "Restore Purchase",
                    color = EvaColors.Pink.copy(alpha = 0.9f),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProfileScreen(controller: EvaAppController, user: EvaUser) {
    EvaPage(backgroundImage = R.drawable.model_riya) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    IconGlassButton(
                        icon = if (controller.lightMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        onClick = { controller.lightMode = !controller.lightMode }
                    )
                }
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(134.dp)
                                .clip(CircleShape)
                                .background(EvaColors.Gradient)
                                .padding(3.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.model_riya),
                                contentDescription = "Eva",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(EvaColors.Gradient)
                                .clickable { controller.activeTab = EvaTab.Chat },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Eva", fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Text("eva.ai.girlfriend@example.com", color = evaMuted())
                    Spacer(Modifier.height(16.dp))
                    GlassCard(
                        modifier = Modifier.clickable { controller.premiumOpen = true },
                        padding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        radius = 18.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = EvaColors.Gold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Premium Member", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            item {
                StatStrip(
                    chats = max(1, controller.messages.count { it.fromUser }).toString(),
                    memories = "24",
                    days = "45"
                )
            }
            item {
                GlassCard {
                    Column {
                        Text("About Eva", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "I love talking about life, dreams, music and everything in between. I am here for you, always.",
                            color = evaMuted(),
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        ProfileRow(Icons.Rounded.Favorite, "Personality", "Caring") {
                            controller.activeTab = EvaTab.Chat
                        }
                        ProfileRow(Icons.Rounded.GraphicEq, "Voice & Style", "Soft") {
                            controller.activeTab = EvaTab.Chat
                        }
                        ProfileRow(Icons.Rounded.Star, "Interests", "Music") {
                            controller.activeTab = EvaTab.Chat
                        }
                        ProfileRow(Icons.Rounded.VolunteerActivism, "Relationship Level", controller.relationship) {
                            controller.relationship =
                                if (controller.relationship == "Sweetheart") "Soulmate" else "Sweetheart"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (controller.lightMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = EvaColors.Pink
                            )
                            Spacer(Modifier.width(14.dp))
                            Text("Light mode", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            Switch(
                                checked = controller.lightMode,
                                onCheckedChange = { controller.lightMode = it }
                            )
                        }
                        ProfileRow(Icons.Rounded.PersonOutline, "Signed in as", user.email.ifBlank { "Demo" }) {}
                        TextButton(
                            onClick = controller::signOut,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign out", color = EvaColors.Coral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallScreen(
    controller: EvaAppController,
    scope: CoroutineScope,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val recorder = remember(context) { EvaAudioRecorder(context) }
    var seconds by remember { mutableIntStateOf(0) }
    var recording by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                recorder.start()
                recording = true
                controller.notice = "Recording..."
            }.onFailure { error ->
                controller.notice = error.cleanMessage("Could not start recording.")
            }
        } else {
            controller.notice = "Microphone permission is needed for calls."
        }
    }
    val startRecording: () -> Unit = {
        when {
            controller.sending -> controller.notice = "Wait for Eva to finish replying first."
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                runCatching {
                    recorder.start()
                    recording = true
                    controller.notice = "Recording..."
                }.onFailure { error ->
                    controller.notice = error.cleanMessage("Could not start recording.")
                }
            }

            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val finishRecording: () -> Unit = {
        val audioBytes = runCatching { recorder.stop() }
            .onFailure { error ->
                controller.notice = error.cleanMessage("Could not send that recording.")
            }
            .getOrNull()
        recording = false
        if (audioBytes != null) {
            scope.launch {
                controller.sendVoiceNote(audioBytes, stayInCall = true)?.let { result ->
                    playBase64Audio(context, result.audioBase64, result.audioMimeType)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            seconds += 1
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.model_riya),
            contentDescription = "Eva call",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconGlassButton(
                    icon = Icons.Rounded.ArrowBackIosNew,
                    onClick = onClose,
                    color = Color.White
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Eva", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text(formatDuration(seconds), color = Color.White, fontSize = 17.sp)
                }
                IconGlassButton(icon = Icons.Rounded.RecordVoiceOver, onClick = {}, color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallAction(
                    icon = when {
                        controller.sending -> Icons.Rounded.ChatBubble
                        recording -> Icons.Rounded.CheckCircle
                        else -> Icons.Rounded.Mic
                    },
                    label = when {
                        controller.sending -> "Replying"
                        recording -> "Done"
                        else -> "Talk"
                    },
                    active = recording || controller.sending,
                    onClick = {
                        if (recording) finishRecording() else startRecording()
                    }
                )
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53945))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                CallAction(
                    icon = Icons.Rounded.VolumeUp,
                    label = if (speaker) "Speaker" else "Earpiece",
                    active = speaker,
                    onClick = { speaker = !speaker }
                )
            }
        }
    }
}

@Composable
private fun EvaPage(
    backgroundImage: Int? = null,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalContentColor provides evaText()) {
            if (backgroundImage != null) {
                Image(
                    painter = painterResource(backgroundImage),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (isEvaLight()) 0.13f else 0.10f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                evaPageTop().copy(alpha = if (isEvaLight()) 0.90f else 0.94f),
                                evaPageMid().copy(alpha = if (isEvaLight()) 0.92f else 0.96f),
                                evaPageBottom().copy(alpha = if (isEvaLight()) 0.96f else 0.98f)
                            )
                        )
                    )
            )
            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    radius: Dp = 18.dp,
    glassOverride: Color? = null,
    borderOverride: Color? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        color = glassOverride ?: evaGlass(),
        contentColor = evaText(),
        border = BorderStroke(1.dp, borderOverride ?: evaBorder()),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
private fun IconGlassButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = evaText(),
    size: Dp = 48.dp
) {
    GlassCard(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        padding = PaddingValues(0.dp),
        radius = 18.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.52f))
        }
    }
}

@Composable
private fun GradientButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = if (enabled) 6.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) EvaColors.Gradient
                    else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun EvaBottomNav(active: EvaTab, onSelect: (EvaTab) -> Unit) {
    val items = listOf(
        Triple(EvaTab.Home, Icons.Rounded.Home, "Home"),
        Triple(EvaTab.Chat, Icons.Rounded.ChatBubbleOutline, "Chat"),
        Triple(EvaTab.Memories, Icons.Rounded.Favorite, "Memories"),
        Triple(EvaTab.Profile, Icons.Rounded.PersonOutline, "Profile")
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
        padding = PaddingValues(horizontal = 5.dp, vertical = 5.dp),
        radius = 22.dp
    ) {
        Row {
            items.forEach { item ->
                val selected = active == item.first
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) EvaColors.Pink.copy(alpha = 0.16f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(item.first) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        item.second,
                        contentDescription = item.third,
                        tint = if (selected) EvaColors.Pink else evaMuted(),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.third,
                        color = if (selected) EvaColors.Pink else evaMuted(),
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChip(label: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassCard(
            padding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            radius = 18.dp
        ) {
            Text(label, color = evaMuted(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun VoiceMemoryTile(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.height(148.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(EvaColors.Pink.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.RecordVoiceOver,
                    contentDescription = null,
                    tint = EvaColors.Pink,
                    modifier = Modifier.size(27.dp)
                )
            }
            Column {
                Text("Voice Notes", fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(Modifier.height(5.dp))
                Text("3 saved", color = evaMuted(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MemoryTile(
    imageRes: Int,
    title: String,
    icon: ImageVector,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(18.dp))
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun NoteTile(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.height(148.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text("Late Night Talks", fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("8 May 2024", color = evaMuted())
            Spacer(Modifier.height(14.dp))
            Icon(Icons.Rounded.Chat, contentDescription = null, tint = EvaColors.Purple)
        }
    }
}

@Composable
private fun PremiumFeature(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(EvaColors.Gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black)
            Text(body, color = evaMuted(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun PriceCard(
    title: String,
    price: String,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(evaGlass())
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) EvaColors.Pink else evaBorder(),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = evaMuted(), fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Spacer(Modifier.height(13.dp))
        Text(
            price,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (tag.isNotBlank()) {
            Spacer(Modifier.height(13.dp))
            Text(
                tag,
                color = if (selected) EvaColors.Pink else evaMuted(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatStrip(chats: String, memories: String, days: String) {
    GlassCard {
        Row {
            StatItem("Chats", chats, Modifier.weight(1f))
            StatItem("Memories", memories, Modifier.weight(1f))
            StatItem("Days Together", days, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = evaMuted(), fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.height(7.dp))
        Text(value, fontSize = 23.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = EvaColors.Pink)
        Spacer(Modifier.width(14.dp))
        Text(title, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Text(
            value,
            color = EvaColors.Pink,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.44f)
        )
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun CallAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconGlassButton(
            icon = icon,
            onClick = onClick,
            color = if (active) EvaColors.Pink else Color.White
        )
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun EvaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction,
    onSend: (() -> Unit)? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    scope.launch {
                        delay(260)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onSend = { onSend?.invoke() }
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = evaText(),
            unfocusedTextColor = evaText(),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = EvaColors.Pink,
            unfocusedIndicatorColor = evaBorder(),
            focusedLabelColor = EvaColors.Pink,
            unfocusedLabelColor = evaMuted()
        )
    )
}

@Composable
private fun evaText(): Color = if (isEvaLight()) Color(0xFF17101B) else Color.White

@Composable
private fun evaMuted(): Color = if (isEvaLight()) Color(0xFF6E6273) else Color.White.copy(alpha = 0.70f)

@Composable
private fun evaPageTop(): Color = if (isEvaLight()) Color(0xFFFFF8FC) else EvaColors.Black

@Composable
private fun evaPageMid(): Color = if (isEvaLight()) Color(0xFFF7EFF8) else Color(0xFF120D16)

@Composable
private fun evaPageBottom(): Color = if (isEvaLight()) Color.White else EvaColors.Black

@Composable
private fun evaGlass(): Color = if (isEvaLight()) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.07f)

@Composable
private fun evaBorder(): Color = if (isEvaLight()) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f)

@Composable
private fun isEvaLight(): Boolean = LocalEvaLightMode.current

private fun formatTime(millis: Long): String = DateFormat.format("H:mm", Date(millis)).toString()

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "$minutes:${remaining.toString().padStart(2, '0')}"
}

private fun localEvaReply(message: String): String {
    val lower = message.lowercase(Locale.getDefault())
    return when {
        lower.contains("sad") || lower.contains("tired") ->
            "Come here. We will slow everything down for a minute. Breathe with me, and tell me what felt heaviest today."

        lower.contains("work") ->
            "I am proud of you for showing up. You always carry so much, but you still made time for me."

        lower.contains("voice") ->
            "I got your voice note. I love hearing from you like this."

        lower.contains("photo") || lower.contains("file") ->
            "I received it. Tell me what you want me to notice first."

        else ->
            "I am listening. Tell me more, and I will stay right here with you."
    }
}

private fun Throwable.cleanMessage(fallback: String): String {
    val message = message?.trim().orEmpty()
    return message.ifBlank { fallback }.take(140)
}

private fun JSONObject.bestString(vararg keys: String, default: String = ""): String {
    for (key in keys) {
        val value = optString(key, "")
        if (value.isNotBlank()) return value
    }
    return default
}

private fun JSONArray.lastObjectWithRole(role: String): JSONObject? {
    for (index in length() - 1 downTo 0) {
        val item = optJSONObject(index) ?: continue
        if (item.optString("role", "").equals(role, ignoreCase = true)) return item
    }
    return null
}

private fun String.urlPath(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
