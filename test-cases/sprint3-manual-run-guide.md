# Sprint 3 — manual run guide (items automation cannot cover)

Sprint 3 automated what could be automated (L1 API, L3 rules, L4 UI — see `traceability.csv` and
`automation/README.md`). The cases below need your Google account, a real device, a deployed
update, or seeded data. Case details (steps/expected) are in the CSVs; this guide gives the run
order and what to record. Record each result in the `notes` column of `traceability.csv`
(`PASS/FAIL - date, where, what you saw`) and file bugs as in `bug-reports/`.

**Rules:** production + your own account only. Prefix all test data with `QATEST-`. Never test
anyone else's data.

## A. Sheets Sync and Reset (L5, production, desktop Chromium) — `sheets-sync.csv`
Order matters because "has synced" state is per browser.

| # | Case | Do | Record |
|---|---|---|---|
| 1 | `SYNC-20` (manual confirmation) | Clear site data for the app, sign in, click Sync | Educational dialog "Izinkan Akses Google Sheets & Drive"; Batal -> no Google popup |
| 2 | `SYNC-21` | Click Sync again, confirm the dialog | Normal flow continues (Google popup, export) |
| 3 | `SYNC-22` | After a successful sync, click Sync again | Short dialog "Sinkronisasi ke Sheets", no permission text; Batal -> nothing |
| 4 | `SYNC-23` | In a second browser/profile with the same account (never synced) click Sync. Back in the first browser let the token expire (~1 h) or revoke it and click Sync | 2nd browser: educational dialog. 1st: short dialog even if Google asks to log in |
| 5 | `SYNC-26` | Create transactions with Platform/Catatan starting with `=`, `+`, `-`, `@` and a tab (direct form), Sync, open the sheet | Plain text, no formula/hyperlink, no visible guard character (BUG-018 retest) |
| 6 | `SYNC-28` | Open two tabs, start Sync in tab 1, immediately Sync or Reset in tab 2 | Toast "Sync atau reset sedang berjalan di tab lain. Tunggu hingga selesai."; no duplicate rows (BUG-008 retest: also switch tabs mid-sync, buttons must stay disabled) |
| 7 | `SYNC-27` | Block popups for the site (browser address bar), click Sync | Toast "Jendela login Google diblokir browser..." (BUG-013 retest) |
| 8 | `SYNC-24` | Let the token expire/revoke it, click Reset Ekspor, confirm, close the Google popup | Nothing changes (rows still exported, files not trashed) |
| 9 | `SYNC-25` | Click Reset, confirm, then cut the network or revoke Drive access mid-way | Toast "Gagal reset sinkronisasi: ..."; token dropped; export status unchanged |
| 10 | `SYNC-16` (last, destructive) | Reset Ekspor, confirm | `Qicau_Export_*` files in Drive **Trash**; toast "...dipindahkan ke Trash Drive"; next Sync creates a new file with everything |

## B. PWA update and install (production + devices)
- `PWA-05` / `PWA-12`: needs a new deployment. When the author deploys one, with the app open: the
  "Pembaruan Tersedia" banner slides from the very top, has no close button; switch away and back
  -> page reloads by itself; repeat while a Sync is running -> no reload, banner stays.
- `PWA-06`: Chrome desktop and Android: install button -> browser dialog -> button disappears; an
  in-app toast is NOT required (BUG-014 closed as accepted).
- `PWA-08` (BUG-011/BUG-019 retest): install guide fits the screen, centered, opened from both the
  header button and Settings. iPhone: Safari wording "pada bilah navigasi Safari"; Chrome/Firefox/Edge
  iOS neutral wording ("di bilah alamat atau menu browser Anda").

## C. Data-dependent cases
- `HIST-05` 90-day boundary and `HIST-03` day labels: needs back-dated transactions. Use the
  Firebase emulator + Admin seeding (not production) to insert transactions dated 89/91 days ago.
- `MON-11`: seed (emulator) a `daily_summaries/{uid}_{YYYYMMDD}` doc whose `by_category` sums
  differently from `total`, open Ringkasan, wait: only that day is repaired, no toast, no reload;
  repeat offline -> not repaired.
- `MON-05`, `MON-08`: not automated; run manually with data in two categories / previous month.

## D. Android smoke (real device, production)
Edit from Riwayat (`HIST-14..19`), Ringkasan values after add/edit/delete (watch for the `Rp 0`
category row: BUG-020), offline banner layout (BUG-010 retest), Sync dialogs, install guide.

## E. Exploratory session (time-box 45 min) — charter "CH-06"
Ringkasan and edit-from-Riwayat: cross-day and cross-month edits and deletes, offline edits and
deletes (compare BUG-021/BUG-022), edit then delete, rapid edits, desktop + mobile on the same
account. Write notes in `test-cases/exploratory/CH-06-*.md` like the other charters.

## F. Retests still open after Sprint 3 automation
Not exercised yet: `BUG-004` (gocap, `PARSE-21`, run `api-testing` parse-quality on production),
`BUG-016` (`MAN-19`, passed at L4 locally), `BUG-018` (A5 above), `BUG-019` (B above).

## G. Manual checks of bugs found by automation (production)
- `BUG-020` (`HIST-11`/`HIST-17`/`MON-10`): delete or re-categorise a category's last transaction; check Ringkasan for a `Rp 0` row and the empty state.
- `BUG-021` (`MON-12`): offline create/edit/delete, reconnect, compare Ringkasan with Riwayat. Repeat 3 times (intermittent).
- `BUG-022` (`HIST-20`): offline delete closes the dialog at once.
- `BUG-024` (`HIST-08`): 31+ mixed-category transactions, category filter: `1 / n+` on page 1, next must load older rows.

## H. Real Google login on the phone (production, own Gmail account, not a work account)
`AUTH-01..06` (popup login, closing the popup, failure alert, sign out). The local emulator uses a fake account picker, so the real Google
popup flow is only covered here.

## I. Already done by automation (not manual)
Real Android phone, Appium, local dev: 14 scenarios pass, 3 fail with `BUG-020` (`test-cases/android-run-2026-10-05.md`).
Everything else automated is in `traceability.csv`.

## Done when
Every row above is PASS/FAIL recorded in `traceability.csv` (or noted "not testable" with a reason), the bug stubs/Jira are updated, and
`reports/2026-10-04-cycle-3-summary.md` has the final numbers. Sprint 3 can be completed in Jira after that.
