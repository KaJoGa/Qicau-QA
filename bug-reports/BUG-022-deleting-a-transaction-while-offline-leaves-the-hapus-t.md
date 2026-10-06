# BUG-022 — Deleting a transaction while offline leaves the "Hapus Transaksi?" dialog open with a spinner, blocking the app

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog?selectedIssue=QAP-55 |
| **Spec ID(s)** | `HIST-20` (new 2026-10-04: offline delete closes the dialog at once), `HIST-11`, `PWA-04` |
| **Severity** | Low (proposed — recovers when back online) |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | Sprint 3 L4 UI automation run, 2026-10-04 |
| **Status** | **Fixed** - verified on production (manual, 2026-10-05): the offline delete dialog closes at once; also passes L4 and on a real phone. |

## Summary
Confirming a delete while offline keeps the confirmation dialog on screen with a spinner. The overlay blocks the whole app until the connection returns.

## Steps to reproduce
1. Open Riwayat with at least one transaction.
2. Go offline.
3. Click the delete icon and confirm "Hapus".

## Expected (per spec)
`HIST-20`: the dialog closes immediately and the row disappears, without a hanging spinner; the deletion and the Ringkasan correction sync together after reconnect.

## Actual
Dialog stays open with a spinner and blocks the page until online.

## Evidence
`automation/dump/s3/13-offline-delete-overlay.html` (observation from the Sprint 3 L4 run, not a failed spec line - may need a spec decision).
