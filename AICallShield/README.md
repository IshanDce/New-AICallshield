# 🛡️ AICallShield – Intelligent AI Call Screening Assistant

An AI-powered Android application that screens unknown incoming calls, provides real-time transcription, generates AI responses, allows user takeover, and stores conversation history in chat format.

## 📁 Project Structure

```
AICallShield/
├── android/              # Android App (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/aicallshield/
│   │   │   │   ├── ui/           # Compose UI screens & components
│   │   │   │   ├── service/      # Call screening & audio services
│   │   │   │   ├── data/         # Models, repositories, DB, API
│   │   │   │   ├── viewmodel/    # ViewModels
│   │   │   │   └── util/         # Utilities & constants
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── backend/              # Python FastAPI Backend
│   ├── routers/          # API route handlers
│   ├── services/         # AI, STT, TTS, spam detection
│   ├── models/           # Pydantic schemas
│   ├── database/         # Local storage client
│   ├── main.py           # FastAPI entry point
│   ├── config.py         # Configuration
│   └── requirements.txt
│
└── README.md
```

## 🎯 Core Features

- ✅ Unknown call detection via `CallScreeningService`
- ✅ AI auto-screening with smart replies
- ✅ Real-time speech-to-text transcription
- ✅ Live WhatsApp-style chat interface
- ✅ User call takeover ("Join Call")
- ✅ Scam keyword detection & spam scoring
- ✅ Sentiment analysis
- ✅ AI post-call summary generation
- ✅ Call recording & local storage
- ✅ Chat history with search
- ✅ No-setup local protection mode (works without backend)

## 🏗️ Technology Stack

| Layer     | Technology                          |
|-----------|-------------------------------------|
| Frontend  | Kotlin, Jetpack Compose, Room DB    |
| Backend   | Python, FastAPI                     |
| AI/ML     | OpenAI Whisper, GPT/Gemini, TTS    |
| Database  | Local JSON + file storage           |
| Network   | Retrofit, OkHttp                    |

## 🚀 Getting Started

### Backend Setup

```bash
cd backend
pip install -r requirements.txt

# Set environment variables
# OPENAI_API_KEY=your_key
# LOCAL_STORAGE_DIR=local_storage

uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### Quick Start (No Backend Setup)

For everyday users, the app can run in local protection mode without configuring Python/FastAPI.
In this mode, call records are saved on-device and the app uses on-device scam heuristics when cloud AI is unavailable.

### Android Setup

1. Open `android/` folder in Android Studio
2. Sync Gradle
3. Update `BASE_URL` in `util/Constants.kt` to your backend URL
4. Build & run on device (API 29+)

## 📊 Data Flow

```
Incoming Call → Call Detection → Audio Capture → Speech-to-Text
→ AI Processing → Text-to-Speech → Live UI Update → Local Storage
```

## ⚠️ Important Note

> Due to Android system-level restrictions, this project implements a prototype
> AI call screening model using supported Android APIs and simulated routing
> where required. Full call audio interception requires the app to be set as
> the default dialer, which is subject to Google Play policy restrictions.

## 🔒 Security & Privacy

- User consent required before recording
- Caller notification: "This call is AI screened"
- Encrypted data storage
- No third-party data misuse

## 🔮 Future Scope

- Deepfake voice detection
- Fraud database integration
- Business call assistant
- Multi-language support
- AI voice customization

## 📦 Build Phases

| Phase | Focus                              | Complexity |
|-------|------------------------------------|------------|
| 1     | Call detection + Chat UI           | ⭐⭐        |
| 2     | STT + AI replies                   | ⭐⭐⭐      |
| 3     | Spam detection + sentiment         | ⭐⭐⭐⭐    |
| 4     | Polish + local storage             | ⭐⭐⭐⭐⭐  |
