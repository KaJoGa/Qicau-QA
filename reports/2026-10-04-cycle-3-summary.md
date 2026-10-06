# Test Summary — Sprint 3 (2026-10-04)

Follow-up to `2026-09-29-cycle-1-summary.md`. Sprint 2 was bug fixing only (the app author fixing the
bugs from cycle 1). Sprint 3 retested those fixes and tested the spec changes of 2026-10-03/04
(`test/UPDATE_NOTES_PART2.md`: Parts 2 and 3). Written 2026-10-04, updated 2026-10-06 with the manual production results (§3, §5) and the retest after the author fixed `BUG-005`, `013` and `020` (§7). The L5 and
manual rows in §2 reflect that update.

## 1. What changed in scope
- **New/changed spec IDs:** edit from Riwayat (`HIST-14..19`), Ringkasan consistency and repair
  (`MON-09..12`), Sync/Reset dialogs and hardening (`SYNC-19..28`), `MAN-19`, `API-11..13`, daily
  summary rules (`SEC-12..14`, old `SEC-12` is now `SEC-15`), `PWA-12`; renames (Bulanan to Ringkasan)
  and reworded `HIST-02/05/08`, `PWA-01/05/06/08/11`.
- **Removed feature:** Rebuild Ringkasan (its cases were dropped).
- **Test cases:** CSVs updated (`test-cases/`), traceability marked per row.

## 2. Results by layer (local dev + emulator, plus read-only production checks)

| Layer | Result |
|---|---|
| L1 API (`API-01..13`) | All pass in their proper environment. 15 new Postman requests, 8 new mock scenarios. Production: `no-store` on HTML, `immutable` on `/assets/*`, no raw Google errors, `raw_transcript` equals input. |
| L2 parse | `PARSE-21` retest on production: "gocap" returns 50 (2 of 2 runs). |
| L3 rules (`SEC-01..15`) | 34 of 34 pass, including the new `daily_summaries` tests. |
| L4 UI | Final full run (mock-free group): 114 scenarios in the suite, 16 excluded by tag, 98 run: **92 pass, 6 do not**. Mock groups (high/low/all-fail): 7 of 7 pass, re-run after the harness fix. Seeded-data cases `HIST-03`, `HIST-05`, `MON-08`, `MON-11` pass (back-dated data written to the emulator). |
| L5 Sheets | **Not run** (manual, see §5). |

L4 not passing in the final run: `HIST-08` (`BUG-024`), `HIST-11`, `HIST-17`, `MON-10` (all `BUG-020`),
`PWA-11` (local Vite dev sends `no-cache`; spec says test a production build, production passes) and
`VOICE-06` (known, unchanged). Not automated, with reasons, are in `automation/README.md`.

## 3. Defects

Retests on production (manual, 2026-10-05/06) are included below.

**Fixed and verified on production:** `BUG-001`, `003`, `004`, `006` (the list itself), `008`, `010`, `011` (Android), `012`, `015`, `016`, `017`, `018`, `022`; `BUG-002` earlier.
**Still open after retest:**
- `BUG-005` still shows the browser's native message on production (local dev passes, so the fix may not be deployed). Reopen `QAP-21`.
- `BUG-013` no toast when the Google popup is blocked (`SYNC-27`). Reopen `QAP-29`.
- `BUG-019` not retested (no iPhone).
**Closed without a fix:** `BUG-007` (spec changed), `BUG-009` (deferred), `BUG-014` (accepted).

**New this sprint:**

| Bug | Summary | Severity (proposed) | Status |
|---|---|---|---|
| `BUG-020` (`QAP-53`) | Ringkasan keeps `Rp 0` category rows after delete/edit; after deleting a whole month every category stays at `Rp 0` | Medium | **Confirmed on production**, local dev and a real phone |
| `BUG-021` (`QAP-54`) | Offline create/edit/delete: Ringkasan can differ from Riwayat | Medium | Not reproduced on production; failed once on local dev. Suggest closing |
| `BUG-022` (`QAP-55`) | Offline delete left the dialog stuck | Low | Fixed, verified on production |
| `BUG-023` | Withdrawn (invalid test data) | - | - |
| `BUG-024` (`QAP-56`) | Category filter: empty page after `1 / 1+` | Medium | Not reproduced on production; local dev with seeded data only. Suggest closing |

Totals: 24 bug stubs (`BUG-001..024`). Open: `BUG-005`, `011` (iOS only), `013`, `019`, `020`. Suggested close as cannot-reproduce: `BUG-021`, `BUG-024`.

**Spec deviation needing an author decision:** `SYNC-25`. With the network cut during Reset the app shows no error toast; it keeps retrying and finishes the Reset after reconnect (success toast, files in Trash). The spec expects "Gagal reset sinkronisasi". Either the spec or the app should change.

## 4. Test harness findings (not app bugs)
- With the emulator, the app writes under its own Firebase project id (`big-elysium-496003-j7`), not
  `demo-qicau-test`. The suite's between-scenario cleanup and its Firestore inspector were pointed at
  the wrong project. Fixed on 2026-10-04 (`emulator.project.id` in `automation/.../local.properties`).
  This may explain why the `LOWC-04` log-write check sometimes came back empty.
- Seeding back-dated data: the app stores `created_at` as epoch milliseconds (integer) and keeps
  `daily_summaries/{uid}_{yyyyMMdd}`; field types were observed, not read from source.
- Real Android phone through Appium works (`test-cases/android-run-2026-10-05.md`): 14 scenarios pass, 3 fail with `BUG-020`.
  On a phone the first tap after typing only closes the keyboard, so the tests blur the input before tapping Save.

## 5. Manual results (production and devices, 2026-10-05/06)
- **L5 Sheets:** `SYNC-16`, `20`, `21`, `22`, `23`, `24`, `26`, `28` pass; `SYNC-27` fails (`BUG-013`); `SYNC-25` deviates from the spec (see §3).
- **Real Google login on the phone:** `AUTH-01`, `03`, `04`, `05`, `06` pass.
- **PWA:** `PWA-06` pass (best effort), `PWA-08` pass on Android. `PWA-05` and `PWA-12` not run: they need a new deployment.
- **Exploratory `CH-06`:** done; confirmed `BUG-020`; `BUG-021` and `BUG-022` not reproduced. One observation: editing a transaction deleted on the other device shows a raw Firebase message (not filed).
- **Not done:** iOS (no device), `PWA-05`/`PWA-12` (deploy).

## 6. Risks and open questions
- **Ringkasan correctness** remains the main risk: `BUG-020` is confirmed on production in every environment tested. The totals are right, but stale `Rp 0` rows show.
- **Production may be behind local dev:** `BUG-005` passes on local dev but still fails on production. Check whether the latest build is deployed.
- **Author decisions:** `SYNC-25` (error toast or keep retrying), and whether the raw Firebase message on a conflicting edit is acceptable.
- **Coverage gaps:** iOS, and the update flow (`PWA-05`, `PWA-12`).

## 7. Retest after the author's fixes (2026-10-06)
The author fixed `BUG-005`, `BUG-013` and `BUG-020`, deployed to production and checked them there.
- **`BUG-020` fixed.** L4 `HIST-11`, `HIST-17`, `MON-10` pass on local dev and on a real phone (no `Rp 0` rows).
- **`BUG-005` fixed.** The browser's native message is gone; the app shows an inline error "Jumlah pengeluaran wajib diisi.".
  The author updated spec `MAN-13` (alert -> inline error); the case now passes on local dev and a real phone.
- **`BUG-013` fixed per the tester** (manual `SYNC-27` on production). Not retested by automation (real Google login).
- **Regression run (L4, local dev, 99 scenarios run):** 95 pass, 4 not passing (after `MAN-13` was re-run against the updated spec): `HIST-08` (category-filter
  paging with seeded data on local dev, `BUG-024`, passes on production), `PWA-11` (dev server `no-cache`, known), `VOICE-06` (known),
  and `AUTH-03` (flaky once, passed 2 of 2 on rerun). On the real phone: 6 of 7 pass (`MAN-13` the same way).
- **Still open:** `BUG-019` (no iPhone), `SYNC-25` decision, `PWA-05`/`PWA-12` (need a deploy).
