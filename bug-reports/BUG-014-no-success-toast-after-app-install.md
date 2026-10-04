# BUG-014 — No "Aplikasi berhasil dipasang…" toast after installing the app

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/browse/QAP-30 |
| **Spec ID(s)** | `PWA-06` (also `TOAST-01`) |
| **Severity** | Low (proposed — install works; only the in-app confirmation is missing) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, desktop Chromium (Windows) |
| **Found during** | Exploratory session CH-04, 2026-10-03 |
| **Status** | Closed - accepted by the app author, 2026-10-04. `PWA-06` now says the install confirmation comes from the browser notification and an in-app toast is not guaranteed. |

## Summary
After installing the app from the in-app install button, the success toast the spec calls for does
not appear. Only the browser's own notification shows.

## Steps to reproduce
1. Open the site in a browser that supports installing, not yet installed.
2. Click the install button ("Pasang") in the header or Settings and accept the browser's install
   dialog.

## Expected (per spec)
`PWA-06`: "berhasil → toast 'Aplikasi berhasil dipasang…' dan tombol hilang."

## Actual
No toast from the app. The browser shows its own installation notification. The install button
does disappear.

## Evidence
Observed manually during CH-04 (`test-cases/exploratory/CH-04-pwa-offline-real-devices.md`),
2026-10-03. Not automatable: the real install flow cannot be driven from Selenium
(`automation/README.md`, `PWA-06`).
