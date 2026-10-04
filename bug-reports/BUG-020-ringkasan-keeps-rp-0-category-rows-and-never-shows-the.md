# BUG-020 — Ringkasan keeps `Rp 0` category rows and never shows the empty state after the last amount is removed

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog?selectedIssue=QAP-53 |
| **Spec ID(s)** | `MON-04`, `MON-06`, `MON-10`, `HIST-11`, `HIST-17` |
| **Severity** | Medium (proposed — wrong figures shown, no data loss) |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | Sprint 3 L4 UI automation run, 2026-10-04 |
| **Status** | Open - reproduced independently on 2026-10-04 (HIST-11, HIST-17, MON-10 re-run on local dev). Also happens after undo. Re-check manually/on production before filing. |

## Summary
After a category's last transaction is deleted, or an edit moves its only amount to another category, the Ringkasan tab still lists that category with `Rp 0`. When the month has no transactions left, the "Belum ada riwayat transaksi." empty state is not shown.

## Steps to reproduce
1. Sign in with a fresh account, save one transaction (category Makan).
2. Open Riwayat and delete that transaction (or edit it to category Transport).
3. Open Ringkasan.

## Expected (per spec)
`MON-04`: only categories that have transactions are listed. `MON-06`: with no transactions, total `Rp 0` and "Belum ada riwayat transaksi.". `HIST-11` / `HIST-17`: Ringkasan reflects the delete/edit.

## Actual
Delete case: total `Rp 0`, a `Makan Rp 0` row stays, no empty state (page text: `Total Pengeluaran|Rp 0|KATEGORI|Makan|Rp 0`). Edit case: `[Transport 35.000, Makan 0]`.

## Evidence
Selenium scenarios `HIST-11`, `HIST-17`, `MON-10` (`automation/`), 2026-10-04 run; see the Sprint 3 table in `automation/README.md`. Related: `BUG-006` (no longer reproduces).
