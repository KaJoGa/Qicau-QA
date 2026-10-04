# BUG-022 — Deleting a transaction while offline leaves the "Hapus Transaksi?" dialog open with a spinner, blocking the app

| | |
|---|---|
| **Jira issue** | _to be filed in Jira_ (next numbers likely QAP-36...) |
| **Spec ID(s)** | `HIST-11`, `PWA-04` (related; the spec does not describe offline delete) |
| **Severity** | Low (proposed — recovers when back online) |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | Sprint 3 L4 UI automation run, 2026-10-04 |
| **Status** | Open - to be re-confirmed manually (and on production) before filing |

## Summary
Confirming a delete while offline keeps the confirmation dialog on screen with a spinner. The overlay blocks the whole app until the connection returns.

## Steps to reproduce
1. Open Riwayat with at least one transaction.
2. Go offline.
3. Click the delete icon and confirm "Hapus".

## Expected (per spec)
The spec does not define offline delete. `PWA-04` expects clear behaviour without a stuck UI (e.g. the dialog closes, or a clear offline message).

## Actual
Dialog stays open with a spinner and blocks the page until online.

## Evidence
`automation/dump/s3/13-offline-delete-overlay.html` (observation from the Sprint 3 L4 run, not a failed spec line - may need a spec decision).
