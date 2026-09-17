# Waseem Khan AI Guru Ji 🤖🫂

Android Studio project for a ChatGPT/Gemini-style multilingual AI chat app.

## Features
- Chat screen
- Gemini API integration point
- Hindi/English/other-language conversation (model-dependent)
- Voice input via Android Speech Recognizer
- Text-to-speech replies
- Android phone project

## Setup
1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Open `app/src/main/java/com/waseemkhan/aiguruji/MainActivity.kt`.
4. Replace `PASTE_YOUR_GEMINI_API_KEY_HERE` with your Gemini API key.
5. Build > Build APK(s).

**Security note:** For a production app, do not ship a permanent API key inside the APK. Put the API call behind your own backend/proxy and add authentication/rate limits.
