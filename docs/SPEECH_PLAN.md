# Speech recognition — plan

## Where it stands

`VoiceScreen` is a **visual stub**. The five states (Idle / Listening /
Thinking / Speaking / Error) exist and drive the orb's motion exactly as the
design specifies, but tapping the mic only advances the state machine by hand.
There is no microphone permission, no audio capture, no recognizer, no
synthesis. Nothing records, nothing leaves the device — because nothing runs.

## Two engines, one interface

Recognition ships in **two interchangeable modes** behind a single
`SpeechEngine` interface, chosen in Settings:

| | **Local** (default) | **Remote** |
|---|---|---|
| API | `createOnDeviceSpeechRecognizer` (API 33+) | cloud recognizer / STT service |
| Audio | never leaves the phone | uploaded to the provider |
| Quality | weaker, varies by OEM and language | consistently better, esp. Russian |
| Availability | needs a downloaded language pack | needs network |
| Config | none | endpoint + key via `AppConfig` (`samind.*` gradle properties), never committed |

```kotlin
interface SpeechEngine {
    fun start(onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (Int) -> Unit)
    fun stop()
    val runsOnDevice: Boolean
}
```

Rules for the remote mode, since it changes what the app promises:

- **Off by default.** Local is the default engine; remote is opt-in.
- **Explicit consent** the first time it is enabled, naming what is uploaded
  and to whom — not buried in a settings label.
- The Home/privacy copy must switch to match the active mode; the app must not
  claim "nothing leaves the device" while remote is on.
- A persistent indicator on the voice screen showing which engine is live.
- Local stays fully functional if remote is never configured; if the local
  language pack is missing, the app offers remote rather than failing silently.

## Phases

**1. Permission and lifecycle**
- `RECORD_AUDIO` requested in context, on first mic tap, with a plain
  explanation; denial keeps the screen usable in text mode.
- Recognition is bound to the composable lifecycle: stopped on background,
  released on dispose. No listener survives the screen.

**2. Recognition** (both engines implement the same interface)
- Local: `createOnDeviceSpeechRecognizer` + `RecognizerIntent` with partial results.
- Remote: streaming client behind the same callbacks; failures fall back to the
  local engine rather than dropping the session.
- `EXTRA_LANGUAGE` follows the app locale (RU/EN); the model pack may need a
  one-time system download — surface that as the Error state with a link, not
  a crash.
- Map callbacks to states: `onReadyForSpeech` → Listening, `onEndOfSpeech` →
  Thinking, reply playback → Speaking, `onError` → Error.
- Amplitude from `onRmsChanged` drives the orb's amplitude, so the motion
  reflects the actual voice — which is what the design asks for.

**3. Reply path**
- Recognised text goes through the existing `ChatEngine` (same rules, same
  crisis handling) — voice must never be a second, less safe brain.
- Reply spoken with `TextToSpeech`, on-device voice, interruptible by tapping
  the orb.

**4. Safety**
- Transcripts are held in memory only, cleared on leaving the screen; never
  written to Room, never logged — in **both** modes.
- The crisis rule fires identically in voice; the helpline message is both
  spoken and shown as text.
- A visible recording indicator whenever the mic is live (system-level too on
  Android 12+).

**5. Tests**
- Fake recognizer in unit tests to assert the state machine, error paths and
  the crisis route.
- The evidence harness captures each of the five states via a debug intent
  extra (`--es voice_state listening`) so parity screenshots stay possible
  without speaking.

## Cost and open decisions

- On-device quality in Russian is noticeably weaker than English and varies by
  OEM — the main reason the remote option exists. Needs a real-device test.
- Which remote provider (and therefore which data-processing terms the project
  signs up to).
- Whether the pilot defaults to local-only, or offers the choice from day one.
