# L5 — Sheets sync: manual run guide

L5 is deliberately manual (`test-plan.md` §3.3): real Google OAuth against production, where bot
detection, 2FA and consent screens make scripting flaky and risky for the account. The 18 cases
(`SYNC-01`..`SYNC-18`) are fully written with steps and expected results in
`test-cases/sheets-sync.csv`. This guide puts them in a workable run order (the CSV's numeric order
is not the execution order) and says what to record. Results of the 2026-09-30 run are in the last
section.

## Before you start

- Target: **production** (https://qicau.kajoga.workers.dev/), the tester's own Google account,
  desktop Chromium (easiest for popup handling).
- **Use identifiable test data.** Prefix notes or platform names with `QATEST-` for every
  transaction created during the run, so test data can be told apart from real expenses afterwards
  (`test-plan.md` §6.2 — this is real data in a real account, not a disposable fixture).
- Keep Google Drive open in another tab to inspect the `Qicau_Export_*` spreadsheet directly
  instead of trusting the toast messages.
- If the account has zero transactions, do **Phase 0** first. If it already has real data, skip
  Phase 0 and note that `SYNC-12` was not independently testable without wiping real data.

## Phase 0 — empty-account case (skip if there are transactions)

- **`SYNC-12`** — with no transactions at all, click Sync. Expect: toast "Tidak ada data untuk
  disinkronkan."; no spreadsheet created; no request to Google.

## Phase 1 — before the first successful sync

- **`SYNC-01`** — go offline (devtools Network → Offline, or airplane mode), open Riwayat, click
  "Sync ke Sheets". Expect: error toast about needing internet; no request to `googleapis.com`
  (check the Network tab).
- **`SYNC-03`** — back online, click Sync to open the Google auth popup, then **close the popup**
  without finishing sign-in. Expect: no error toast, Sync button back to normal.
- **`SYNC-17`** — go offline again, click "Reset Ekspor". Expect: error toast about needing
  internet, no changes. (Doing this before the first real sync means there is nothing to reset yet;
  only the offline block is being checked.)

## Phase 2 — the first real sync

- **`SYNC-02`** — back online, click Sync and **complete** the Google sign-in. Expect: the popup
  asks for Sheets and Drive permission.
- **`SYNC-04`** — while the sync runs, note the button text ("Menyinkronkan..." + spinner) and that
  Sync and Reset are both disabled. Switch to another browser tab and back — they should stay
  disabled.
- **`SYNC-05`** — when it finishes, confirm in Drive that a new `Qicau_Export_<year>` spreadsheet
  exists, with a Summary tab and one tab per month that has data.
- **`SYNC-06`** — open a monthly tab. Check the frozen header row (Hari, Tanggal & Waktu (WIB),
  Platform/Toko, Kategori, Metode Pembayaran, Harga (Rp), Catatan), rows in ascending time order,
  and the day name only on the first row of each date.
- **`SYNC-07`** — on the same tab, compare the side "Statistik" and "Kategori" tables with the data
  rows: do totals and averages match?
- **`SYNC-08`** — open the Summary tab. Check the cross-month aggregates and the "Alokasi
  Pengeluaran (%)" pie chart, and confirm there is **no** frozen row (unlike the monthly tabs).
- **`SYNC-09`** — in the app, check that the success toast says "Berhasil menyinkronkan N transaksi
  baru!" with the right N (count the `QATEST-` rows).

## Phase 3 — re-sync behavior

- **`SYNC-10`** — click Sync again at once with nothing new. Expect: "Semua data sudah
  tersinkronisasi…" toast and **no duplicate rows** in the sheet.
- Add one more `QATEST-` transaction (Input Manual is fastest), then:
- **`SYNC-11`** — click Sync. Expect: only that new row is added; stats and Summary update to match.

## Phase 4 — reset flow

- **`SYNC-15`** — click "Reset Ekspor", then **Batal**. Expect: nothing changes.
- **`SYNC-16`** — click "Reset Ekspor" again and confirm. Expect: toast "Berhasil mereset status N
  transaksi!…". Then click Sync and confirm it resends everything (the toast count equals the total
  `QATEST-` transactions, not only the newest).

## Phase 5 — hard-to-force cases (best effort; "not reliably reproducible" is an acceptable result)

- **`SYNC-14`** (Google API error mid-sync) — hard to trigger as a normal user. A real disconnect
  during the sync is a reasonable stand-in.
- **`SYNC-13`** (transactions in two years → one spreadsheet per year) — needs transactions dated in
  two different years. The manual form cannot backdate; if no other way exists, note it as not
  independently verified.
- **`SYNC-18`** (Sheets file deleted by the user, then re-synced) — delete or trash the
  `Qicau_Export_<year>` file in Drive, then click Sync. Expect: a new file is created and **all** of
  that year's transactions are sent again, even those already marked exported.

## What to record per case

One line per `SYNC-ID` is enough:

```
SYNC-05: PASS — spreadsheet + tabs created as expected
SYNC-13: NOT RUN — couldn't backdate a transaction into a second year via the UI
SYNC-14: FAIL — error toast appeared but the transaction still got marked as exported (screenshot: sync14.png)
```

Take a screenshot of anything that fails or looks off, with a short note of expected vs. actual;
that is what a bug report is built from. Results go into `test-cases/traceability.csv`, and each
failure becomes a `bug-reports/BUG-0NN-*.md` stub.

## Cleanup afterwards

The `QATEST-` prefix makes test data easy to find. To leave the account clean, delete the
transactions in Riwayat (that alone does not touch the Sheets file) and delete or trash the
`Qicau_Export_<year>` file(s) in Drive.

## Result of the 2026-09-30 run

16 of 18 cases passed. `SYNC-04` failed (`BUG-008`: Sync/Reset buttons re-enable after switching
tabs mid-sync) and `SYNC-14` showed a deviation (`BUG-009`: a mid-sync disconnect leaves an empty or
partly filled spreadsheet, rated Trivial). Per-case results are in `test-cases/traceability.csv`;
the summary is in `reports/2026-09-29-cycle-1-summary.md`.
