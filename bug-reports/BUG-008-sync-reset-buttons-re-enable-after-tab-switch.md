# BUG-008 — Sync/Reset buttons re-enable after switching tabs during an active sync

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-24 |
| **Spec ID(s)** | `SYNC-04` |
| **Severity** | Medium (proposed — could let a user trigger a second, overlapping sync/reset while one is already in flight) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, my own account |
| **Found during** | L5 manual Sheets-sync testing, 2026-09-30 |

## Summary
While a sync is running, the Sync and Reset buttons correctly switch to "Menyinkronkan..." with a
spinner and go disabled. Switching to another browser tab and back, though, brings them back to a
clickable state even though the sync is still in progress.

## Steps to reproduce
1. Sign in, have at least one unsynced transaction.
2. Click "Sync ke Sheets" on Riwayat.
3. While it's still running ("Menyinkronkan..." showing), switch to a different browser tab, then
   switch back to the Qicau tab.

## Expected (per spec)
`SYNC-04`: "Tombol berubah 'Menyinkronkan...' + spinner; Sync **dan** Reset nonaktif — tetap
begitu walau pindah tab." (Sync and Reset stay disabled for the duration of the sync, including
across a tab switch.)

## Actual
After switching tabs and back, the buttons became clickable again while the original sync was
presumably still running.

## Evidence
Observed during manual L5 execution on production (`test-cases/sheets-sync-run-guide.md`), not
captured via automation (L5 is deliberately manual-only).

**Practical impact**: if the re-enabled buttons let a user fire a second sync/reset while
the first is still mid-flight, that's a real risk for duplicate rows or a race between the two
operations' Firestore writes.

## Follow-up check, 2026-10-03: a second concurrent sync does start
Checked with two browser tabs of the same account. Only the first attempt succeeded. The second
one started anyway and failed with a toast (shown in the other tab too) containing a raw Google API
error: `Gagal sinkronisasi: Failed to add sheet September 2026: Invalid requests[0].addSheet: A
sheet with the name "September 2026" already exists. Please enter another name.` So the buttons
being re-enabled does let a second overlapping operation begin, and it collides with the first.
No duplicate rows resulted in this run, so the impact stays an error and confusion rather than data
corruption, but the overlap itself is real.

Same check for Reset + Sync: the first operation reported N transactions reset, the second reported
0 reset. Also, the second tab showed the Google OAuth popup again when Reset/Sync was tried there,
so two tabs apparently do not share the Sheets access token.
