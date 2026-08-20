# Eva AI Companion Android App

Eva is the Android client for the Meri GF / AI Companion experience. It is built with Kotlin, Jetpack Compose, and a production backend at `https://api.merigf.com/api/v1`.

The app supports Google sign-in, email confirmation code sign-in through Resend, MongoDB-backed chat history, and backend-generated AI replies through an OpenAI-compatible provider such as DeepSeek.

## App Demo

Watch the app working here: [Eva AI Companion demo video](https://drive.google.com/file/d/1auxOkDGjLEPMesAlrH9TJuLLFtqGP_8v/view?usp=drivesdk)

## Current App

- Native Android app built with Jetpack Compose and Material 3.
- Dark Eva-style companion UI with home, chat, memories, premium, call, and profile screens.
- Google OAuth browser sign-in with Android deep link return.
- Email login/signup with one-time confirmation codes.
- Chat history loaded from MongoDB through the backend.
- Message send flow connected to backend conversations and assistant replies.
- Premium subscription flow connected to backend-created Razorpay checkout.
- Local fallback replies when backend AI is unavailable.
- Keyboard-safe chat composer and auth form fields.
- Eva model image used as the app launcher icon.

## Repository Layout

```text
.
|-- app/
|   |-- src/main/AndroidManifest.xml
|   |-- src/main/java/com/innovatixhub/ai_companion/MainActivity.kt
|   |-- src/main/res/drawable-nodpi/model_riya.png
|   |-- src/main/res/mipmap-*/ic_launcher.png
|   |-- src/main/res/values/strings.xml
|   `-- build.gradle.kts
|-- gradle/
|-- build.gradle.kts
|-- settings.gradle.kts
`-- README.md
```

## Architecture

The app currently uses a compact Compose state-controller architecture. It is close to MVVM in responsibility split, but kept in one Kotlin entry file for speed of iteration.

```text
Compose Screens
  AuthScreen, HomeScreen, ChatScreen, MemoriesScreen, ProfileScreen
        |
        v
EvaAppController
  Holds UI state, auth state, selected tab, conversations, messages, loading flags
        |
        v
MeriGfApi
  HTTP client for auth, profile, conversations, messages
        |
        v
Meri GF Backend
  Fastify + MongoDB + Resend + Google OAuth + DeepSeek/OpenAI-compatible chat
```

Recommended future split:

```text
ui/screens/*        Compose screens
ui/components/*     Shared UI widgets
viewmodel/*         AuthViewModel, ChatViewModel, ProfileViewModel
data/api/*          MeriGfApi
data/models/*       User, Message, Conversation DTOs
data/repository/*   AuthRepository, ChatRepository
```

## End-to-End Flow

```mermaid
flowchart TD
    A[Android App: Eva] --> B{Auth State}
    B -->|No session| C[Login / Signup]
    B -->|Valid token| D[Home + Chat]

    C --> E[Google Sign-In]
    C --> F[Email Code Sign-In]

    E --> G[Backend Google OAuth Start]
    G --> H[Google Consent Screen]
    H --> I[Backend Google Callback]
    I --> J[Android Deep Link: ai-companion://auth/google]
    J --> K[Save access token]

    F --> L[Backend Email Start]
    L --> M[Resend sends code from verified domain]
    M --> N[User enters code]
    N --> O[Backend verifies code]
    O --> K

    K --> P[Load /api/v1/auth/me]
    P --> Q[Load conversations]
    Q --> R[Load selected conversation messages]
    R --> D
```

## Chat Flow

```mermaid
sequenceDiagram
    participant App as Android App
    participant API as Meri GF Backend
    participant DB as MongoDB
    participant AI as DeepSeek / OpenAI-compatible API

    App->>API: GET /api/v1/conversations
    API->>DB: Find conversations by authenticated userId
    DB-->>API: Conversation list
    API-->>App: conversations[]

    App->>API: GET /api/v1/conversations/:id/messages
    API->>DB: Find messages by userId + conversationId
    DB-->>API: Message history oldest-first
    API-->>App: messages[]

    App->>API: POST /api/v1/conversations/:id/messages
    API->>DB: Save user message
    API->>DB: Load recent history
    API->>AI: system prompt + recent history + latest message
    AI-->>API: Eva reply
    API->>DB: Save assistant message
    API-->>App: reply + assistantMessage
```

## Email Confirmation With Resend

```mermaid
flowchart LR
    A[User enters name/email] --> B[POST /auth/email/start]
    B --> C[Backend creates one-time code]
    C --> D[Resend API]
    D --> E[Verified sending domain]
    E --> F[User inbox]
    F --> G[User enters code]
    G --> H[POST /auth/email/verify]
    H --> I[JWT access + refresh token]
```

Resend is handled fully by the backend. The Android app never stores the Resend API key.

## Google Sign-In Flow

```mermaid
flowchart TD
    A[Tap Continue with Google] --> B[Open browser]
    B --> C[GET /auth/google/start?redirectUri=ai-companion://auth/google]
    C --> D[Google OAuth consent]
    D --> E[Backend callback: /auth/google/callback]
    E --> F[Backend creates or finds user]
    F --> G[Redirect to app deep link]
    G --> H[Android MainActivity receives intent]
    H --> I[Save backend session tokens]
    I --> J[Load profile and chats]
```

Android deep link:

```text
ai-companion://auth/google
```

Backend callback configured in Google Cloud:

```text
https://api.merigf.com/api/v1/auth/google/callback
```

## Backend Integration

The Android app reads the backend base URL from:

```xml
app/src/main/res/values/strings.xml
```

Current value:

```text
https://api.merigf.com/api/v1
```

Main backend endpoints used by the app:

```text
POST /auth/email/start
POST /auth/email/verify
GET  /auth/google/start
POST /auth/google/mobile/exchange
GET  /auth/me

GET  /subscriptions/me
POST /subscriptions/checkout
POST /subscriptions/sync

GET  /conversations
POST /conversations
GET  /conversations/:conversationId/messages
POST /conversations/:conversationId/messages
```

## Subscription Flow

```mermaid
flowchart TD
    A[Premium screen] --> B[POST /subscriptions/checkout]
    B --> C[Backend creates Razorpay subscription]
    C --> D[Razorpay hosted checkout URL]
    D --> E[Android opens browser]
    E --> F[User authorises monthly payment]
    F --> G[App resumes]
    G --> H[POST /subscriptions/sync]
    H --> I[Premium status stored in MongoDB]
```

Current Razorpay plan:

```text
plan_TRv3HKpujDyFoS
Eva Premium Monthly
INR 299/month
```

## DeepSeek Configuration

DeepSeek is configured on the backend, not inside the Android app. The app only calls the Meri GF backend.

Recommended backend env values:

```env
AI_PROVIDER=deepseek
AI_API_BASE_URL=https://api.deepseek.com
AI_API_KEY=your_deepseek_key
AI_MODEL=deepseek-v4-flash
```

For stronger reasoning:

```env
AI_MODEL=deepseek-v4-pro
```

Do not put DeepSeek keys in the Android app.

## Build

Use the Android Studio JBR on Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

Generated debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Important Android Details

- Package name: `com.innovatixhub.ai_companion`
- App label: `Eva`
- Launcher icon: generated from `model_riya.png`
- Google auth return is handled by `singleTask` `MainActivity`.
- Keyboard behavior uses `adjustResize`, `imePadding()`, and focused-field bring-into-view behavior.
- Session tokens are stored in Android shared preferences under `meri_gf_session`.

## Security Notes

- No Google client secret, Resend key, MongoDB URI, or DeepSeek key should be stored in the Android app.
- The Android app stores only backend-issued access/refresh tokens.
- All third-party provider secrets belong in the backend deployment environment.
- The backend owns Google OAuth verification, Resend email delivery, MongoDB persistence, and DeepSeek requests.
