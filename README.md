# MomentLoop

MomentLoop is an Android personal growth tracker built around a simple daily loop:
create a journey, capture a moment, add a note, get Gemini-powered feedback, and
keep the habit visible through home-screen widgets.

## Highlights

- Journey tracking with progress, streaks, and saved moments.
- Camera/image support for richer daily check-ins.
- Gemini feedback for progress scoring and coaching-style insights.
- Home-screen widgets powered by Glance.
- Local Room persistence and WorkManager refresh jobs.

## Requirements

- Android Studio with JDK 17.
- Android SDK with the compile SDK required by the project.
- A Gemini API key for AI features.

## Run Locally

1. Open the project in Android Studio.
2. Copy `.env.example` to `.env`.
3. Set `GEMINI_API_KEY` in `.env`.
4. Sync Gradle and run the `app` configuration on an emulator or device.

You can also build from the command line:

```bash
./gradlew :app:assembleDebug
```

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## Secrets

Do not commit `.env`, keystores, or generated APKs. The debug build uses the
default Android debug signing config, so no checked-in debug keystore is needed.
