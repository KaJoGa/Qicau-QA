# BUG-021 — After offline create/edit/delete and reconnect, Ringkasan no longer matches Riwayat

| | |
|---|---|
| **Jira issue** | _not filed yet - after filing, paste the QAP link here and in `INDEX.md`_ |
| **Spec ID(s)** | `MON-12`, `MON-07` |
| **Severity** | Medium (proposed — wrong totals after a normal offline use case) |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | Sprint 3 L4 UI automation run, 2026-10-04 |
| **Status** | Open - INTERMITTENT. Failed once in the Sprint 3 L4 run, but 4 of 6 later local re-runs of MON-12 passed. Verify by hand (offline create/edit/delete, reconnect, compare Ringkasan with Riwayat) before filing. |

## Summary
Transactions created, edited and deleted while offline sync back correctly in Riwayat, but the daily summary behind Ringkasan ends up with a different total.

## Steps to reproduce
1. Sign in, note totals. Go offline.
2. Create, edit and delete transactions (direct form / Riwayat).
3. Go online and wait for sync.
4. Compare the Ringkasan total with the sum of transactions in Riwayat; check a second session too.

## Expected (per spec)
`MON-12`: transaction and daily summary are saved together; the final Ringkasan total equals the Riwayat sum.

## Actual
First session: Riwayat sum 35.000, Ringkasan total 45.000 (10.000 off). A second session sees an empty Riwayat while Ringkasan says 45.000.

## Evidence
Selenium scenario `MON-12` (NetworkSimulator offline), 2026-10-04; a re-run of MON-12 alone gave the same 10.000 gap.
