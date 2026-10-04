# Test Summary — Sprint 3 (2026-10-04)

Follow-up to `2026-09-29-cycle-1-summary.md`. Sprint 2 was bug fixing only (the app author fixing the
bugs from cycle 1). Sprint 3 retested those fixes and tested the spec changes of 2026-10-03/04
(`test/UPDATE_NOTES_PART2.md`: Parts 2 and 3). Written 2026-10-04. Manual items are listed in §5 and
are **not** part of the numbers below.

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

L4 not passing in the final run: `HIST-08` (`BUG-023`), `HIST-11`, `HIST-17`, `MON-10` (all `BUG-020`),
`PWA-11` (local Vite dev sends `no-cache`; spec says test a production build, production passes) and
`VOICE-06` (known, unchanged). Not automated, with reasons, are in `automation/README.md`.

## 3. Defects

**Fixed and verified (production):** `BUG-001`, `BUG-003`, `BUG-004`, `BUG-012`, `BUG-017`; `BUG-002` earlier.
**Fixed, verified on local dev only (production retest pending):** `BUG-005`, `BUG-006`, `BUG-010`, `BUG-015` (for "Semua Kategori"), `BUG-016`.
**Closed without a fix:** `BUG-007` (spec changed), `BUG-009` (deferred by the author), `BUG-014` (accepted).
**Not yet retested (need production, Google account or a device):** `BUG-008`, `BUG-011`, `BUG-013`, `BUG-018`, `BUG-019`.

**New this sprint (stubs, to file in Jira):**

| Bug | Summary | Severity (proposed) | Confidence |
|---|---|---|---|
| `BUG-020` | Ringkasan keeps `Rp 0` category rows and no empty state after delete/edit/undo | Medium | Reproduced twice on local dev |
| `BUG-021` | Offline create/edit/delete: Ringkasan can end up different from Riwayat (35.000 vs 45.000) | Medium | **Intermittent**: failed once, passed in the final run and most re-runs; verify by hand |
| `BUG-022` | Offline delete leaves the confirmation dialog stuck with a spinner | Low | One observation; spec does not define it |
| `BUG-023` | Category-filtered page indicator is exact, spec says `n+` | Low | Reproduced; may be an outdated spec |

Totals: 23 bugs filed to date (`BUG-001..023`). Open: `BUG-008`, `011`, `013`, `018`, `019`, `020..023`.

## 4. Test harness findings (not app bugs)
- With the emulator, the app writes under its own Firebase project id (`big-elysium-496003-j7`), not
  `demo-qicau-test`. The suite's between-scenario cleanup and its Firestore inspector were pointed at
  the wrong project. Fixed on 2026-10-04 (`emulator.project.id` in `automation/.../local.properties`).
  This may explain why the `LOWC-04` log-write check sometimes came back empty.
- Seeding back-dated data: the app stores `created_at` as epoch milliseconds (integer) and keeps
  `daily_summaries/{uid}_{yyyyMMdd}`; field types were observed, not read from source.

## 5. Not done in this sprint (manual / needs your account or device)
Guide: `test-cases/sprint3-manual-run-guide.md`.
- L5: `SYNC-16`, `SYNC-21..28` (real Google OAuth, Sheets, Drive).
- `PWA-05`, `PWA-12` (need a new deployment), `PWA-06` (native install dialog), `PWA-08` (real iOS devices).
- Android smoke pass of the new features; exploratory charter `CH-06` (written, not run).
- Production retests of the local-only fixes; manual check of `BUG-021`.
- Jira: file `BUG-020..023`, close the fixed and closed bugs.

## 6. Risks and open questions
- **Ringkasan correctness** is the main open risk: `BUG-020` is confirmed and `BUG-021` is unresolved.
  Every number on that tab depends on the daily summary staying in step with transactions.
- **Spec question for the author:** `HIST-08` with a category filter (`n+` or exact?) and what offline
  delete should do (`BUG-022`).
- **Production is not covered by the L4 results.** The UI suite runs on local dev; production
  confirmation of `BUG-005/006/010/015/016` is pending.
