# BUG-023 — Riwayat page indicator with a category filter shows an exact total instead of the `n+` lower bound

| | |
|---|---|
| **Jira issue** | _to be filed in Jira_ (next numbers likely QAP-36...) |
| **Spec ID(s)** | `HIST-08` |
| **Severity** | Low (proposed — may be an outdated spec rather than an app defect) |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | Sprint 3 L4 UI automation run, 2026-10-04 |
| **Status** | **Withdrawn - invalid test data (2026-10-04).** With mixed data the `n+` lower bound does appear (`1 / 1+`), as the spec says. My first seed (61 rows, all Makan) had everything loaded, where an exact total is correct. The real problem found in the retest is `BUG-024`. |

## Summary
With a specific category filter the spec says the total is a lower bound (`n+`) until the last page. The app shows an exact total.

## Steps to reproduce
1. Seed 61 transactions in one category (Makan).
2. Open Riwayat, filter by Makan, step through the pages.

## Expected (per spec)
`HIST-08`: with "Semua Kategori" the total is fixed (e.g. 1/3 ... 3/3); with a specific category filter it shows `n+` until the last page.

## Actual
Makan filter shows `1 / 3`, `2 / 3`, `3 / 3` (exact). "Semua Kategori" is correct and fixed.

## Evidence
Selenium scenario `HIST-08`, 2026-10-04. Ask the app author whether the spec or the app is the reference.
