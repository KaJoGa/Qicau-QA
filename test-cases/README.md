# test-cases/

Manual test cases, one CSV file per feature area, written **only from `test/Qicau.md`** (see the
blind-testing rule in the root `CLAUDE.md`). See `test-plan.md` §7.1 for the format.

## Status (2026-10-03) — written and executed
All 9 area files + `traceability.csv` are filled: every spec ID outside `API-*` (which lives in
`api-testing/` instead — see that folder's README) has at least one case. 166 cases across the 9
files; `traceability.csv` has one row per (spec ID, case) pair and is the place to look for real
results.

- The CSV case files document each case as written (`automated` column = how it was meant to be
  run). The actual **results** live in `traceability.csv`'s `notes` column: pass/fail, date, and
  the bug ID for anything that failed.
- `PARSE-*` and `SEC-*` ran via `api-testing/` and `security-rules/`; `AUTH`, `NAV`, `HOME`,
  `VOICE`, `SAVE`, `LOWC`, `MAN`, `HIST`, `MON`, `PWA`, `TOAST` ran through the Selenium/Cucumber
  suite in `automation/` (a few were run manually instead — noted per row); `SYNC` was run
  manually, see `sheets-sync-run-guide.md`.
- 177 of 180 spec IDs have an executed result; the 3 that don't are explained in the cycle report
  (`reports/2026-09-29-cycle-1-summary.md`).

## Files

| File | Feature area | Spec IDs |
|---|---|---|
| `auth-nav.csv` | Auth & session, navigation & settings | `AUTH`, `NAV` |
| `home-voice.csv` | Home tab, voice input, post-save flow | `HOME`, `VOICE`, `SAVE`, `LOWC` |
| `manual-input.csv` | Manual input (AI-text + direct form) | `MAN` |
| `history.csv` | History tab | `HIST` |
| `monthly.csv` | Monthly/weekly summary | `MON` |
| `sheets-sync.csv` | Google Sheets export | `SYNC` |
| `ai-parsing.csv` | AI parsing rules | `PARSE` |
| `security-rules.csv` | Firestore security rules | `SEC` |
| `pwa-offline.csv` | Offline behaviour & PWA | `PWA`, `TOAST` |
| `traceability.csv` | Cross-cutting: every spec ID → which case(s) cover it → automated? → result notes | all |
| `sheets-sync-run-guide.md` | Run order and recording format for the manual L5 (Sheets sync) pass, plus its result | `SYNC` |
| `exploratory/` | Session-based exploratory charters with session notes (not case-by-case; see risk areas in `test-plan.md` §4) | — |

## Column format (case files)

```
case_id, spec_id, title, preconditions, steps, expected_result, priority, type, environment, automated
```

- `case_id` — e.g. `TC-HIST-001`.
- `spec_id` — the `Qicau.md` ID(s) this case covers, e.g. `HIST-08`. One case can cover more than one ID; separate with `;`.
- `priority` — `P1` (smoke/critical path), `P2`, `P3` — see `test-plan.md` §5.2.
- `type` — `functional`, `negative`, `boundary`, `security`, `exploratory-charter`.
- `environment` — `local-emulator` or `production` — see `test-plan.md` §3.1 for which layer targets which.
- `automated` — `Y` / `N` / `planned`.
- Written in English (the spec itself is Indonesian).

## Column format (`traceability.csv`)

```
spec_id, case_id, layer, automated, notes
```

One row per (spec ID, case ID) pair. `layer` is one of the layers in `test-plan.md` §3.1 (`L1`..`L5`, `Mobile`, `Stress`, `Manual`). A spec ID with no row yet is uncovered — flagged at exit criteria time (§5.2).
