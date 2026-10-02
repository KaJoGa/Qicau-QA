# Qicau — QA Portfolio

Manual + automated testing for **[Qicau](https://qicau.kajoga.workers.dev/)**, a voice/text
expense-tracking PWA (Firebase Auth + Firestore, Gemini API, Google Sheets API). Built as a QA
internship portfolio piece — see `test-plan.md` for the plan this repo executes and `CLAUDE.md`
for the project context and working rules.

This repo holds **QA artifacts only** — it never contains or modifies Qicau's application code.
Tests were written **blind**: only from the functional spec (`test/Qicau.md`), never from the app's
source code.

## Result of the first test cycle (2026-09-27 to 2026-10-03)

180 spec IDs, all with at least one test case; **176 executed (98%)**. **18 bugs** found and filed
in Jira (1 critical, fixed the same day; 17 open — 5 medium, 11 low, 1 trivial). Full report:
[`reports/2026-09-29-cycle-1-summary.md`](reports/2026-09-29-cycle-1-summary.md).

| Layer | What | Tooling | Result |
|---|---|---|---|
| L1 API contract | `API-*` | Postman / Newman | 13/13 executed |
| L2 Parse quality (real Gemini) | `PARSE-*` | Postman data-driven run | 26/28 sentences clean |
| L3 Firestore rules | `SEC-*` | `@firebase/rules-unit-testing` (Node) | 24/24 tests pass |
| L4 UI end-to-end | 98 spec IDs | Selenium + Cucumber + Java 17 + Maven | 40/44 `@smoke`, 43/55 `@regression` pass; every other result is an explained bug or a documented gap |
| L5 Sheets sync | `SYNC-*` | Manual (real Google OAuth) | 16/18 pass |
| Stress | `/api/parse-*` | k6 | small baseline, 0 failures |
| Exploratory | 5 charters | Session-based, manual | `test-cases/exploratory/` |

## Key documents

| Document | What's in it |
|---|---|
| [`test-plan.md`](test-plan.md) | Scope, approach, risk areas, environments, exit criteria |
| [`test/Qicau.md`](test/Qicau.md) | The functional spec — single source of truth for every test |
| [`test-cases/`](test-cases/) | Test cases (CSV), traceability matrix with results, L5 run guide, exploratory charters |
| [`bug-reports/`](bug-reports/) | One stub per bug, linking to Jira; start at `INDEX.md` |
| [`reports/`](reports/) | Test summary report |
| [`automation/`](automation/) | Java/Selenium/Cucumber UI automation (L4) |
| [`api-testing/`](api-testing/) | Postman collections (L1, L2) |
| [`security-rules/`](security-rules/) | Firestore rules tests (L3) |
| [`performance/`](performance/) | k6 stress scripts and baseline result |
| [`environments/`](environments/) | Gemini fetch-mock and emulator notes |
| [`CLAUDE.md`](CLAUDE.md) | Project context, conventions and working rules for the AI assistant used on this repo |

## Not covered (on purpose)
iOS (no device), real-microphone quality, and four UI scenarios that cannot be executed
(`AUTH-02`, `AUTH-05`, `MON-05`, `PWA-08`); all are listed with reasons in the cycle report.

## Tracking

- **Jira project:** [Qicau QA (QAP)](https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog) — backlog, sprints, and defect tracking (`QAP-17` to `QAP-34` are the bugs).
