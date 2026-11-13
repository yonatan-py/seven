# Seven

A tiny standalone Wear OS meditation app.

Tap **Start**. The same gong and a short vibration occur at elapsed **0:00**, **0:17**, and **7:00**. The session then ends after the final gong rings out. No accounts, network, settings, history, or phone companion.

## Open and build

- Open this directory in Android Studio. Use JDK 17.
- Install Android SDK Platform 36 and Build Tools 35.0.0 if prompted.
- Run `./gradlew assembleDebug testDebugUnitTest lintDebug` (Windows: `gradlew.bat`). The first build downloads Gradle and dependencies.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Install/run on a Wear OS 3 or newer watch (Android API 30+). A watch speaker/audio output and vibrator are needed for both cues.
- Allow notifications when asked to keep the ongoing session notification and its Stop action accessible. Denial does not prohibit a foreground service, but can hide its notification from the normal notification list.

## Small implementation

The UI uses native Android views and Java, without runtime third-party dependencies. A service owns the session, so leaving the screen does not tie timing to Activity callbacks. It preloads a three-second original synthetic gong, then schedules all cues from one start instant. A partial wake lock is held only during the session, with a 435-second safety timeout. The display is not forced to stay bright. The foreground notification provides a Stop action. All pending callbacks, vibration and the wake lock are cleaned up on service destruction.

The app uses alarm-category audio and haptic attributes. Watch volume, Do Not Disturb, hardware and vendor behavior can affect the cues. No volume or DND setting is overridden. Ordinary task/process termination can stop a session; it deliberately does not restart or replay missed gongs after process death, reboot or force-stop.

The manifest uses the `specialUse` foreground service type with an explanation for Android 14+. This is a local prototype, not a Play-approved app. Any future Play submission requires review of that use case and current store policies.

## Verification

- Debug APK compilation: passed.
- Four Android JVM/Robolectric tests: passed, including the 0/17/420-second schedule under a simulated clock, duplicate-start prevention, final-sample tail, cleanup, cancellation and an Android-rendered 384px UI preview.
- Android lint: no errors; informational warnings may remain.
- These tests do **not** verify execution on a Wear OS device, physical sound/vibration, battery use, or real seven-minute screen-off timing. Those are still required before treating this as ready for daily use.

Render preview after tests: `app/build/previews/seven.png`. Test results: `app/build/reports/tests/testDebugUnitTest/`.

## Watch acceptance check

1. Start once. Hear and feel the first cue. Tap Start repeatedly: no overlapping session should begin.
2. Let the screen dim or turn off. Hear/feel cues at about 17 and 420 seconds from the first cue, checked against an independent stopwatch.
3. Confirm the last gong rings out and the ongoing notification disappears.
4. Run again; use notification Stop, then wait past a cue. No further cue should occur.
5. Repeat on the intended watch with its usual volume/DND settings and check battery use. Reopen the UI mid-session and confirm it still says Meditating.

## Audio

`app/src/main/res/raw/gong.wav` is an original generated, mono 22.05 kHz, 16-bit, three-second synthetic gong (about 132 KB decoded). No external audio file is included.

## API references

- https://developer.android.com/reference/android/os/SystemClock
- https://developer.android.com/reference/android/media/SoundPool
- https://developer.android.com/develop/ui/views/haptics/haptic-feedback
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/develop/background-work/background-tasks/awake
- https://developer.android.com/build/releases/agp-8-13-0-release-notes
