# CH-02 — Voice capture on real devices

| | |
|---|---|
| **Risk area** | R4 (voice capture & audio format) |
| **Spec IDs** | `VOICE-01..11`, `API-06/09` |
| **Environment** | Production, the tester's own account, **both** Android Chrome and desktop Chromium |
| **Timebox** | 45 minutes |

## Mission
Exercise real microphone/browser/OS behavior that `automation/`'s fake-audio-file approach
structurally cannot reach — `DriverFactory` always feeds a canned `.wav`, never a real mic, so
anything about actual hardware, codecs, or OS-level permission UI is unverified until now.

## Why automation can't do this
Every `VOICE-*` scenario in `automation/` uses `--use-file-for-fake-audio-capture`. That's the
right call for deterministic CI-style runs, but it means real `MediaRecorder` behavior
(`webm` vs `mp4`/`aac` container differences on Safari-family browsers, real permission prompts,
real background/interruption behavior) has never actually been exercised this cycle.

## Test ideas
**Permission flow (real, not `--use-fake-ui-for-media-stream`)**
- First-ever mic tap on a fresh browser profile — does the real permission prompt appear, and
  does declining it show the expected "Membutuhkan akses mikrofon." alert (`VOICE-06` — this is
  the one scenario automation left as a documented, known-unverifiable gap; this session is the
  actual way to close it)?
- Revoke mic permission mid-session (via the browser's own site settings) and try recording again
  without reloading.

**Real recording edge cases**
- Genuinely stay silent for ~2s mid-recording (not a scripted fixture) — does it auto-stop the
  same way `VOICE-03` verified?
- Tap-and-immediately-release faster than you can consciously time it (`VOICE-05`'s <0.8s case,
  but with real human reflexes instead of two scripted clicks).
- Record for the full 60s with a real voice, including natural pauses shorter than 2s — does the
  60s cap and the 2s-silence auto-stop interact correctly (e.g. a 1.5s pause mid-sentence
  shouldn't trigger an early stop)?
- Rapid double-tap on the mic button.
- Background the browser tab (switch apps on Android, minimize on desktop) while recording, then
  come back — does it recover cleanly or get stuck in a broken "listening" state?
- Put the phone in airplane mode *during* recording (not before) and see what happens when
  processing tries to start.

**Cross-browser/OS specifics**
- Same sentence, same volume, on both Android Chrome and desktop Chromium — does confidence or
  transcription quality differ noticeably?
- Any device-specific beep/haptic feedback behavior — does haptic feedback actually fire on
  Android, and does its absence on desktop look intentional or broken?

## Session notes
Session run 2026-10-02, production, own account, Android (installed PWA) + desktop.

**Covered:**
- **Permission denied (`VOICE-06`)** — the alert "Membutuhkan akses mikrofon." appears; no browser
  permission prompt appeared during the test. This closes the gap automation couldn't reach:
  the spec'd alert behaviour is confirmed on a real browser.
- **Permission revoked mid-recording** — the app can no longer hear after the revoke. When the
  recording ends (stop tap or 2s silence auto-stop), the audio captured before the revoke is still
  sent to Gemini and processed; only what was said before the revoke is parsed. No crash.
- **2s silence auto-stop (`VOICE-03`)** — works with a real voice.
- **Very fast taps (`VOICE-05`) / mic → stop → mic → stop spam** — no request is sent and nothing
  is saved; the icon just flips quickly. The button-press sound effect plays at first but stops
  after repeated spamming.
- **Full-length recording with natural pauses (`VOICE-04`)** — safe, no premature stop.
- **Rapid double-tap** — only the mic icon changes to stop, no request, no running app state.
- **Backgrounding (installed PWA on phone)** — recording does not capture audio while the app is
  in the background; on returning, the recording is auto-submitted with no chance to continue.
  A quick background trip (e.g. replying to a message) still keeps what was recorded; the exact
  time threshold was not measured.
- **Airplane mode during recording** — alert "Gagal memproses suara. Failed to fetch" (matches
  `VOICE-10`'s "Gagal memproses suara. …pesan…" shape, but the appended message is the raw English
  browser error).
- **Cross-browser (Android Chrome vs desktop)** — no noticeable difference in confidence or
  transcription quality; nothing looked broken regarding feedback/haptics.

**Bugs found (file as `bug-reports/BUG-0NN-*.md`):** none new. The raw "Failed to fetch" text is the
same class as `BUG-012` (raw technical English in a user-facing alert); consider adding it as a
second example there rather than a separate bug.

**Open questions / follow-up:**
- Improvement idea (not a bug): the app only shows an alert and never triggers the browser's own
  mic permission prompt in this case. If this happened on a fresh profile (not already blocked),
  requesting permission explicitly so the user can just click Allow would be friendlier. Not
  confirmed whether the permission was already blocked before the first tap.
- Background threshold: skipped on purpose, not measured. Roughly under 10 seconds, and it varies
  (a long utterance is sometimes only partly captured, depending on speaking speed and luck).
