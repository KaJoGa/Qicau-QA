# BUG-024 — Riwayat with a category filter: Next after `1 / 1+` shows an empty page although older matching rows exist

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog?selectedIssue=QAP-56 |
| **Spec ID(s)** | `HIST-08` |
| **Severity** | Medium (proposed — older transactions of that category cannot be reached by paging; data is intact) |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | Sprint 3 L4 retest of `HIST-08` with mixed-category data, 2026-10-04 |
| **Status** | Not reproduced on production (manual, 2026-10-05): with a category filter page 1 reads `1 / n+`, next loads older rows, the last page has no `+`. The empty page was seen only on local dev with seeded data. Suggest closing as cannot-reproduce. |

## Summary
With a category filter active, the first page shows `1 / 1+` and only the few matching rows of the first 30 loaded transactions (5 rows). Clicking next shows an empty list with "Belum ada riwayat transaksi." and no page indicator, although 35 older rows of that category exist. Spec `HIST-08` says next loads the next page of older data without missing rows.

## Steps to reproduce
1. Seed 100 transactions on 100 different days in 4 categories; 40 are Makan, only 5 of them within the newest 30 (the other 35 are older).
2. Open Riwayat, time filter "Semua Waktu", **without** paging through "Semua Kategori" first (fresh load).
3. Select the category filter "Makan". Note page 1.
4. Click next (▶) and wait 8 seconds.

## Expected (per spec)
`HIST-08`: with a category filter the total is shown as a lower bound `n+` until the last page; clicking next loads the next page of older data with no missing rows (the 35 older Makan rows become reachable).

## Actual
Page 1 shows `1 / 1+` and 5 rows (the lower bound itself matches the spec). After next: no rows, no indicator, text "Belum ada riwayat transaksi." for at least 8 seconds. The control case (paging through "Semua Kategori" first so all data is loaded, then Makan) works: `1 / 2` and `2 / 2` with 30 + 10 rows.

## Evidence
Local files (git-ignored `automation/dump/hist08/`): `makan-fresh-p1.png`/`.txt`, `makan-fresh-p2.png` (empty page), control `makan-all-loaded-p1..p2`, and `semua-p1..p4`. Scenario `HIST-08` in `automation/` fails with `indicators=[1 / 1+, ] rows=[5, 0]`.
