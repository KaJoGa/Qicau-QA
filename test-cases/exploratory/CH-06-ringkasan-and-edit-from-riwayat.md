# CH-06 — Ringkasan consistency & editing old transactions from Riwayat

| | |
|---|---|
| **Risk areas** | R6 (time boundaries), R7 (pagination x filters x realtime), R8 (offline), plus the Part 2/3 features: daily summary behind Ringkasan, edit from Riwayat |
| **Spec IDs** | `HIST-14..19`, `MON-04/06/07`, `MON-09..12`, `HIST-11` |
| **Environment** | Production, the tester's own account (prefix test data with `QATEST-`), desktop + Android at the same time |
| **Timebox** | 45 minutes |

## Mission
Ringkasan is now built from per-day summaries that must stay in step with the transactions.
Automation (Sprint 3) already found `Rp 0` category rows that never disappear (`BUG-020`) and one
unconfirmed offline mismatch (`BUG-021`). Hunt for other ways the summary and the real transactions
drift apart, and judge the edit-from-Riwayat flow as a user would.

## Test ideas
**Summary vs history drift**
- Edit a transaction so its category changes, then delete it: does Ringkasan return to exactly the
  previous numbers? Does a `Rp 0` row stay (`BUG-020`) in monthly and weekly views?
- Edit the price of an old transaction (older day, still this month) and check monthly total, the
  weekly total (only if in this week) and the category bars.
- Undo from the save toast, then edit the next one quickly: any flicker or wrong total?
- Delete the last transaction of the month: empty state "Belum ada riwayat transaksi." (`MON-06`)?
- Two sessions (desktop + Android) editing/deleting different transactions of the same day at the
  same time: do both Ringkasan views end with the same numbers?

**Offline** (`BUG-021`, `BUG-022`)
- Offline: create, edit and delete a few transactions, go online, wait. Compare Ringkasan to the
  sum in Riwayat on both devices. Repeat a few times: is it consistent or intermittent?
- Offline: confirm a delete. What does the dialog do (stuck spinner = `BUG-022`)?

**Edit from Riwayat** (`HIST-14..19`)
- Open an old transaction, edit, save: does it stay under its original day? Can the date be changed
  in any way?
- Boundary values in the edit modal: price 0, negative, 10+ digits, note 200 chars, platform 50,
  paste a longer text; compare with the toast edit (`SAVE-06..09`).
- Edit while the same transaction is deleted on the other device: what message do you get?
- Edit to a weird category by typing in the combo box.

**Data-heavy**
- With 30/31/60/61 transactions, edit one on page 2 and check you stay on a sensible page.

## Session notes
_(to be filled in after the session: what was covered, what was not, bugs, open questions)_
