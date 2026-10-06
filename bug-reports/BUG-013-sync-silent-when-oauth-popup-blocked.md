# BUG-013 — Sync gives no feedback at all when the Google sign-in popup is blocked

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog?selectedIssue=QAP-29 |
| **Spec ID(s)** | `SYNC-02`, `SYNC-03` (related; the spec does not define the blocked-popup case) |
| **Severity** | Low (proposed — no data loss, but the user gets no hint why Sync did nothing) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/ |
| **Found during** | Exploratory session CH-03, 2026-10-03 |
| **Status** | **Fixed** per the tester (manual `SYNC-27` on production, 2026-10-06). Not retested by automation (real Google login). |

## Summary
When the browser blocks the Google OAuth popup that Sync opens, clicking "Sync ke Sheets" does
nothing visible: no popup, no toast, no alert. The sync simply doesn't happen and the user is not
told why.

## Steps to reproduce
1. In the browser's site settings, block popups for the Qicau site.
2. Sign in, have at least one unsynced transaction (or a Google token that needs to be re-granted).
3. Open Riwayat and click "Sync ke Sheets".

## Expected
`SYNC-02` says a Google popup asks for Sheets & Drive permission; `SYNC-03` says closing it
deliberately gives no error toast. Neither covers a popup that never opens. A sensible expectation
is a short message (for example asking the user to allow popups), since the sync cannot proceed.

## Actual
Nothing appears — no popup, no warning, no toast. The sync fails silently.

## Evidence
Observed manually during CH-03 (`test-cases/exploratory/CH-03-sheets-export-resilience.md`),
2026-10-03. Not automatable (L5 is manual by design). Because the spec is silent on this case,
treat it as a usability gap to triage, not a spec violation.
