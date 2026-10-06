# Qicau — QA Portfolio

Manual + automated testing for **[Qicau](https://qicau.kajoga.workers.dev/)**, a voice/text
expense-tracking PWA (Firebase Auth + Firestore, Gemini API, Google Sheets API). Built as a QA
internship portfolio piece — see `test-plan.md` for the plan this repo executes and `CLAUDE.md`
for the project context and working rules.

This repo holds **QA artifacts only** — it never contains or modifies Qicau's application code.
Tests were written **blind**: only from the functional spec (`test/Qicau.md`), never from the app's
source code.

## Results

Two test cycles, both written up in [`reports/`](reports/):

- **Cycle 1 (2026-09-27 to 2026-10-03):** 180 spec IDs, **177 executed (98%)**, **19 bugs** found.
  Report: [`reports/2026-09-29-cycle-1-summary.md`](reports/2026-09-29-cycle-1-summary.md).
- **Cycle 3 / Sprint 3 (2026-10-04 to 2026-10-06):** the spec grew to **207 spec IDs**; **203 executed (98%)**. Retested the
  author's fixes, covered the new features (edit from Riwayat, Ringkasan consistency, Sheets dialogs, `API-11..13`, daily-summary rules),
  and added a real Android phone run (Appium) and a manual production pass. Report:
  [`reports/2026-10-04-cycle-3-summary.md`](reports/2026-10-04-cycle-3-summary.md).
- **Bugs overall:** 23 real bugs (`bug-reports/`; `BUG-023` was withdrawn as a false alarm). 17 fixed and verified, 3 closed without a
  fix (spec changed, deferred, accepted), 2 not reproducible on production, 1 open (`BUG-019`, needs an iPhone).

| Layer | What | Tooling | Result |
|---|---|---|---|
| L1 API contract | `API-*` | Postman / Newman | 13/13 pass (Sprint 3) |
| L2 Parse quality (real Gemini) | `PARSE-*` | Postman data-driven run | 26/28 sentences clean in cycle 1; `PARSE-21` fixed |
| L3 Firestore rules | `SEC-*` | `@firebase/rules-unit-testing` (Node) | 34/34 tests pass |
| L4 UI end-to-end | `AUTH NAV HOME VOICE SAVE LOWC MAN HIST MON PWA TOAST` | Selenium + Cucumber + Java 17 + Maven | 96 of 99 run scenarios pass (desktop); the rest are known or documented gaps |
| L4 on a real Android phone | same suite | Appium (UiAutomator2) over USB | the layout, offline and Ringkasan checks pass |
| L5 Sheets sync | `SYNC-*` | Manual (real Google OAuth, own account) | all pass except one spec deviation (`SYNC-25`) |
| Stress | `/api/parse-*` | k6 | small baseline, 0 failures |
| Exploratory | 6 charters | Session-based, manual | `test-cases/exploratory/` |

## Key documents

| Document | What's in it |
|---|---|
| [`test-plan.md`](test-plan.md) | Scope, approach, risk areas, environments, exit criteria |
| [`test/Qicau.md`](test/Qicau.md) | The functional spec — single source of truth for every test |
| [`test-cases/`](test-cases/) | Test cases (CSV), traceability matrix with results, manual run guides, Android run, exploratory charters |
| [`bug-reports/`](bug-reports/) | One report per bug with steps, expected vs actual and status; start at `INDEX.md` |
| [`reports/`](reports/) | Test summary reports (cycle 1 and Sprint 3) |
| [`automation/`](automation/) | Java/Selenium/Cucumber UI automation (L4) |
| [`api-testing/`](api-testing/) | Postman collections (L1, L2) |
| [`security-rules/`](security-rules/) | Firestore rules tests (L3) |
| [`performance/`](performance/) | k6 stress scripts and baseline result |
| [`environments/`](environments/) | Gemini fetch-mock and emulator notes |
| [`CLAUDE.md`](CLAUDE.md) | Project context, conventions and working rules for the AI assistant used on this repo |

## Not covered (on purpose)
Full iOS coverage (no device, so `BUG-019` stays open), real-microphone quality, the app-update banner (`PWA-05`, `PWA-12`: they need a
new deployment while the app is open), and two UI scenarios that cannot be executed reliably (`AUTH-02`, `MON-05`). All are listed with
reasons in the reports.

## Tracking

Defects were also tracked in Jira (backlog, sprints and a board; Sprint 1 and 3 covered the test cycles and Sprint 2 was the author's bug
fixing). The project is private, so every bug is written out in full in [`bug-reports/`](bug-reports/).
