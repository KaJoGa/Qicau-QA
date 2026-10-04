# BUG-016 — Direct form accepts an unlimited Platform length and fails with a raw Firebase error

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog?selectedIssue=QAP-32 |
| **Spec ID(s)** | `SAVE-06` (Platform max 50 in the edit modal; related to the direct form, `MAN-14..17`) |
| **Severity** | Low (proposed — needs deliberately huge input, nothing is saved or corrupted, but the failure is raw and unfriendly) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/ |
| **Found during** | Exploratory session CH-05 ("testing jahil"), 2026-10-03 |
| **Status** | **Fixed** (local dev) - MAN-19 retest passed at L4 (limits + counters, local dev), 2026-10-04. Production retest pending (see `test-cases/sprint3-manual-run-guide.md`). |

## Summary
The Platform field in the direct/manual form has no input length limit. A very large paste is
accepted (the browser lags while pasting), and on save it fails with a raw browser alert.

## Steps to reproduce
1. Open "Input Manual" → "Formulir Langsung".
2. Paste a huge amount of text (more than about 1 MB) into Platform and enter a price.
3. Click "Simpan Transaksi".

## Expected
The edit modal caps Platform at 50 characters with an `n/50` counter (`SAVE-06`), and the Catatan
field is capped at 200 (`SAVE-09`). By the same logic the direct form should cap Platform too, or
at least refuse oversized input gracefully instead of attempting the write.

## Actual
A browser alert appears: `Firebase write error: The value of property "platform" is longer than
1048487 bytes.` Nothing is saved, neither in Firebase nor in the app. The message is raw, in English,
and exposes the technical detail (same class as `BUG-005` and `BUG-012`).

## Evidence
Observed manually during CH-05, 2026-10-03. The spec does not state a Platform limit for the direct
form, so this is a missing-validation finding rather than a strict spec deviation.
