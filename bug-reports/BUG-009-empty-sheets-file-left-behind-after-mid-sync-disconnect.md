# BUG-009 — Disconnecting mid-sync leaves an empty Sheets file behind

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-25 |
| **Spec ID(s)** | `SYNC-14` |
| **Severity** | Trivial / cosmetic (proposed — the empty file is the same `Qicau_Export_<year>` file the next sync reuses and populates, so no duplicates and no data loss; could rise if the export-marking check below shows transactions wrongly marked as exported) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, my own account |
| **Found during** | L5 manual Sheets-sync testing, 2026-09-30 |

## Summary
Turning off internet partway through a sync does correctly show an error toast and cancel the
sync, matching the spirit of `SYNC-14` — but a `Qicau_Export_<year>` spreadsheet file is still
created in Drive, and it's left empty rather than not being created at all (or being cleaned up).

**Impact is minor:** on the next sync the app reuses that same-named file (no second file is
created) and writes the rows into it, so the only effect is a temporarily empty file in Drive —
visible only if the user doesn't retry.

**Scope of what was tested:** network disconnect only. Behaviour on a server-side Sheets/Drive API
error (e.g. 403/500) was not tested and may differ.

## Steps to reproduce
1. Sign in, have at least one unsynced transaction (no prior `Qicau_Export_<year>` file for the
   current year).
2. Click "Sync ke Sheets".
3. Turn off internet while the sync is in progress.

## Expected (per spec)
`SYNC-14`: "Toast error 'Gagal sinkronisasi: …'; token tersimpan dibuang (sync berikutnya minta
login lagi); transaksi **tidak** ditandai terekspor." The spec doesn't explicitly say whether a
spreadsheet file should exist afterward, but an empty file being left behind after a declared
failure is not a documented, intended outcome.

## Actual
The error toast appeared and the sync was canceled as expected, but the `Qicau_Export_<year>`
spreadsheet had already been created in Drive, empty. The disconnect may have happened very early
(before rows were written) rather than truly mid-write, and I did not independently verify whether the affected transactions were correctly left
**un**marked as exported (per spec) or incorrectly marked despite the failure. Re-running the sync
once reconnected did resolve it (the retry populated the file normally) — this is not a
data-loss bug and produced no duplicate file (the retry reused the existing same-named file),
but it is a small deviation worth noting: either don't create the file until there's data to
write, or clean it up on a failed sync.

## Evidence
Observed by me during manual L5 execution
(`test-cases/sheets-sync.csv`, `test-cases/sheets-sync-run-guide.md`), 2026-09-30 — not captured via
automation (L5 is deliberately manual-only, `test-plan.md` §3.3). This was Phase 5's "hard to
force" case (`SYNC-14`); I forced it via a real mid-sync disconnect rather than a genuine
Google API error, which is a reasonable stand-in but may not be byte-for-byte the same failure
mode the spec had in mind.

## Follow-up check, 2026-10-03: export-marking is correct, severity stays Trivial
Repeated the disconnect/reconnect several times against a larger set (about 130 transactions). Data
stayed safe: no duplicate rows and no errors. A sync cut off midway leaves the rows it had already
written in the sheet, so the sheet fills up bit by bit across the interrupted attempts. A failed
attempt shows no "N transaksi baru" toast, and the transactions are not marked as exported: the final
sync that completes without a disconnect reports the full count (for example 130), not just the
remainder. So the only effect is the temporary empty or partly filled file, as originally rated.
