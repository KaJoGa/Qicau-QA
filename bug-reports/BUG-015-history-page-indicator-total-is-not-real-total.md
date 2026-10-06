# BUG-015 — Riwayat page indicator shows a growing "total", not the real total

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog?selectedIssue=QAP-31 |
| **Spec ID(s)** | `HIST-08` |
| **Severity** | Low (proposed — pagination itself works; only the indicator text is misleading) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/ |
| **Found during** | Exploratory session CH-05, 2026-10-03 |
| **Status** | **Fixed** on production (manual, 2026-10-05): fixed total with "Semua Kategori"; with a category filter the indicator reads `n+`. |

## Summary
The Riwayat page indicator should read `halaman / total`, but the "total" part only ever shows one
more than the current page until the last page is reached.

## Steps to reproduce
1. Have more than 60 transactions (3+ pages of 30).
2. Open Riwayat and watch the indicator while clicking the next-page button.

## Expected (per spec)
`HIST-08`: "tombol ◀ ▶ dan indikator `halaman / total`" — the number after the slash is the total
number of pages, constant while paging (for example 1/5, 2/5, ... 5/5).

## Actual
The indicator reads 1/2, then 2/3, then 3/4, and so on, finally ending at for example 5/5 on the last
page. The total keeps growing as pages are loaded, so the user never knows how many pages there
really are.

## Evidence
Observed manually during CH-05 (`test-cases/exploratory/CH-05-cross-device-and-ui-polish.md`),
2026-10-03. The L4 `HIST-08` scenario passes because it checks page size and prev/next behaviour,
not the indicator's total.
