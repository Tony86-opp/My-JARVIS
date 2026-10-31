# JARVIS Android Starter (safe prototype)

A small Android Studio project starter for a JARVIS-inspired assistant interface.

## What this prototype does
- Shows a futuristic dark interface.
- Has a tap-to-talk button using Android speech recognition when available.
- Speaks a simple spoken response using Android TextToSpeech.
- Does **not** include banking integrations, payment APIs, Accessibility Service, SMS sending, call control, or background microphone/wake-word detection.

## Important limitations
This is a starter project, not a finished always-listening JARVIS. "Hey JARVIS" detection while the screen is off requires a dedicated wake-word engine and an Android foreground service, with persistent notification and device-specific testing. It is intentionally not enabled here.
Android's built-in speech recognition may use a provider installed on the phone and may require internet. This prototype does not promise a fully offline AI brain.

## Build
1. Open this project in Android Studio on a computer.
2. Use a recent Android Gradle Plugin and Gradle version compatible with your installed Android Studio.
3. Sync Gradle and run on an Android device.
4. Grant microphone permission when asked.

This chat can generate source files, but cannot install the APK directly on your phone. A phone-only build requires a compatible Android IDE/build environment and is not guaranteed.

## Security
This prototype contains no banking code or money-transfer capability. If you later add integrations, keep them allow-listed and do not grant broad Accessibility access. No app can guarantee security merely by telling its AI not to do something.
