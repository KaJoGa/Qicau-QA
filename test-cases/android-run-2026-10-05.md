# Android run on a real phone — 2026-10-05

Phone: Xiaomi/Redmi M2103K19G, Android 13, Chrome 154. App: local dev + Firebase emulator reached over
`adb reverse` (3000/8080/9099), automation through Appium (UiAutomator2) with the same Cucumber suite
(`-Dbrowser.target=android -Dandroid.udid=<serial>`). How to set it up: `automation/README.md` (Android section).
This is a real device, not an emulator. Voice (`VOICE-*`, no fake microphone), second-session scenarios
(`HIST-19`, `MON-12`, `HOME-07`: one phone = one browser) and Google OAuth were not run on the phone.

| Spec ID | Result on the phone | Note |
|---|---|---|
| AUTH-03 | PASS | login popup works, lands on Catat |
| NAV-04 | PASS | |
| MON-01, MON-09 | PASS | title shows month and year; no permission-denied in the console |
| HIST-02, HIST-09 | PASS | Sync/Reset visible with an empty filter; detail modal has Edit + Hapus |
| HIST-14, HIST-15, HIST-16, HIST-18 | PASS | edit from Riwayat works on the small screen |
| HIST-20 | PASS | offline delete: dialog closes at once, row disappears (CDP offline through Appium) |
| PWA-01 | PASS | offline banner does not cover the Riwayat buttons or title (measured on the phone's own viewport 392x732) |
| MAN-13, MAN-19 | PASS | no native message; 50/200 limits and counters |
| HIST-11 | FAIL | `BUG-020` reproduced on the phone: after deleting the last transaction Ringkasan keeps a `Makan Rp 0` row and no empty state |
| HIST-17 | FAIL | `BUG-020`: after the edit `[Transport 35.000, Makan 0]` |
| MON-10 | FAIL | `BUG-020`: `Rp 0` category rows still listed after edit and undo |

No new defect appeared that is specific to the phone. Two harness findings (not app bugs): on a real phone the first
tap after typing only closes the soft keyboard, so tests must blur the focused input before tapping Save (added to
`ManualInputModal.saveDirectForm` and `EditTransactionModal.save`); and Appium does not proxy
`chromium/network_conditions`, offline is done through `goog/cdp/execute` instead.
