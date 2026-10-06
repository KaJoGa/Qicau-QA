# CLAUDE.md — Qicau QA Portfolio Repository

## What this repo is
A **separate, QA-only repository** for the app **Qicau**. It holds test artifacts only: test plan, test cases, bug reports, automation, API tests. It is a portfolio piece for a **QA internship application**.

- **Never modify Qicau's application source code from here.** The app source lives in a different repo and is not present in this one.
- Findings about the app are reported (as bug reports / Jira issues), not fixed here.

## The app under test: Qicau
PWA (desktop + mobile browsers), built solo by the app author (who is also the tester of this repo) for a Google event (Juara Vibe Coding). UI is Indonesian, currency IDR (no decimals). Tagline: "Say it, Save it."
- **Live production:** https://qicau.kajoga.workers.dev/ — deployed as a **Cloudflare Worker** (`worker.ts`), real Firebase project (no emulator).
- **Local dev:** Express server (`server.ts`, `npm run dev`), can run against the **Firebase Local Emulator Suite**.
- Both runtimes import the same shared logic (`shared/gemini.ts`) — no separate Cloud Functions.

Core flow:
1. Sign in with Google (Firebase Auth)
2. Log an expense by **voice** (mic → audio) or **typing** (text)
3. Input is sent to the **Gemini API**, which parses/corrects it into a structured expense record (kategori, platform, harga, detail, payment_method, confidence, raw_transcript)
4. Several Gemini models are tried as **fallback** if the primary fails (`MODELS_TO_TRY` in `shared/gemini.ts`, called via plain `fetch`, no SDK)
5. Records saved to **Firestore**; `low` confidence results are NOT saved (a low-confidence log is written instead). `medium` confidence with `harga = 0` **is** currently saved — confirmed by the app author as an unhandled edge case, not something to "fix" from this repo; test it as current behaviour.
6. Expense history viewable in-app (Riwayat), plus Home ("Catat") and Monthly/Weekly summary tabs
7. Export to **Google Sheets** via the Sheets API (client-side OAuth token, confirmed no backend proxy) in a fixed format
8. Offline-capable PWA: manual form works offline; AI/voice/Sync do not

**Confirmed gaps (the app author acknowledges, not scope to fix here):**
- No auth or rate limit on `/api/parse-audio` / `/api/parse-text` — anyone with the URL can spend Gemini quota. Test as a known finding, not a surprise.

## Existing material in `/test` (do not overwrite or delete without asking)
| File | Role |
|---|---|
| `test/Qicau.md` | **The functional spec and the single source of truth for writing tests.** Stable IDs per behaviour (`AUTH-01`, `HIST-08`, `PARSE-24`, …). Written in Indonesian. |
| `test/README.md` | The app author's testing strategy (layers L1–L5), kept authoritative and updated as they wire up tooling (emulator flags, env vars, ports). Read it before changing layer targets here — this file must stay consistent with it, not invent a competing plan. |
| `test/OPEN_ISSUES.md` | The app author's private notes: spec-vs-implementation gaps, architecture decisions, suspected bugs. Git-ignored, **not published**. **Not used to pick what to test** — see "Blind-testing rule" — but was read once during planning to understand the real architecture (dual backend, `shared/gemini.ts`, emulator wiring) needed to design the test approach. That is a planning-level exception; test-case selection still comes only from `Qicau.md`. |

## Blind-testing rule
Tests and test cases are written **only from `test/Qicau.md`** (features + inputs/outputs), never from source code or `OPEN_ISSUES.md`. If app behaviour deviates from the spec, that is a finding (bug or outdated spec), not a reason to bend the test. If behaviour changes, update the spec first, then the tests.
Current phase: execution and wrap-up. Blackbox test-case writing was greenlit 2026-09-28; see Status below.

**The app repo happens to be reachable on disk** (`D:\Project\Qicau`, sibling folder to this repo), which makes running local-dev/emulator instances directly possible — but **do not read the app's source code content**, even though it's technically accessible. Filesystem access there is for *operational* actions only: running already-named npm scripts (`npm run dev`, `npm run emulators`), setting env vars, starting/stopping processes, reading a `package.json`'s script names. Never open `.ts`/`.tsx` files to read logic, and never grep source for how something is implemented — that defeats the point of blind testing now that the boundary is a request, not a missing repo. (`shared/gemini.ts`'s error-message wording, `firestore.rules`'s actual rules, etc. are learned by observing behavior, not by reading the file.)

## Environments & how each layer targets them
- **L1 API contract, L4 UI automation:** local dev only — Express (`npm run dev`) + Firebase Local Emulator Suite (`npm run emulators`, project id `demo-qicau-test`, fully offline, no billing/login). App opts in via `VITE_USE_FIREBASE_EMULATOR=true`. Gemini is mocked at the `fetch` boundary in `shared/gemini.ts`, not a client SDK. `signInWithPopup` works against the emulator's built-in fake account picker — no real Google login needed for automation.
- **L3 Firestore rules:** Firebase emulator + `@firebase/rules-unit-testing` (Node/JS) — deliberately **not** rewritten in Java; it's the canonical tool for this narrow layer and the app author already wired it up. Rules are read live from the real `firestore.rules`, never a stale copy.
- **L2 parse quality, manual/exploratory smoke, Sheets (L5):** the **live production URL** (https://qicau.kajoga.workers.dev/), using the tester's own Gmail account, on Android Chrome and desktop Chromium (same account, both devices). This writes real data to the real Firebase project under that one real account — never used for cross-user isolation tests (only one account available there); those (`SEC-03/05/09`, `AUTH-07`) run against the emulator with two synthetic UIDs instead.
- **Stress testing:** small-scale k6 script against local dev with Gemini stubbed — enough to demonstrate the practice, not a full load-testing exercise.

## Conventions
- Every test case / scenario references the spec ID(s) it covers (e.g. Gherkin tag `@HIST-08`, test-case column "Spec ID").
- **Automation language is Java** (job requirement) for UI (Selenium + Cucumber) and API (Postman/Newman). The one exception is L3 Firestore-rules testing (see above), which stays in its native JS tool.
- Test cases are authored as **CSV** (compact, and double as a Jira CSV-import source if/when bulk-imported) — see `test-plan.md` §8 for the format.
- Never run automated/bulk tests against real user accounts or data. Manual/exploratory runs on production use the tester's own account only, never anyone else's.
- Real Gemini key only in local `.env` (never committed). Never commit credentials, tokens, or Google account details.
- Keep AI-parsing checks tolerant: assert mandatory fields (harga, kategori, payment_method, confidence), loosely compare `platform`/`detail`, repeat runs because output is non-deterministic.

## Defect tracking & Scrum
- Bugs are filed in **Jira** (free tier). `bug-reports/` holds thin markdown stubs (ID, title, spec ID, severity, one-line summary, link to the Jira issue) for portfolio visibility without duplicating Jira's content.
- A small Jira backlog/board (a handful of stories + a couple of short sprints mapped to the test layers) demonstrates Scrum familiarity — kept genuinely tied to this project's actual work, not padded.

## Target role requirements driving scope
Manual testing across mobile web / Android / iOS / API; Java; DBeaver + MySQL/SQL Server; Postman; Selenium / Appium / Cucumber; JIRA (or similar) defect tracking; Scrum; stress testing as a plus.
- iOS: no device available — excluded from execution, noted as a coverage gap.
- DBeaver/MySQL/SQL Server: Firestore is NoSQL, no direct target — out of scope, noted as such rather than forced in.

## Status
See `test-plan.md`. L1 (API contract), L2 (parse quality), L3 (Firestore rules), and a small k6
stress baseline are automated and executed. L4 (Selenium/Cucumber, `automation/`) is
build-verified and run for real: 40 of 44 `@smoke` and 43 of 55 `@regression` scenarios pass
against local dev + emulator (+ the Gemini mock where needed) — see `automation/README.md` for
the full breakdown, including several genuinely not automatable from the UI (documented, not
skipped silently). L5 (Sheets sync, `test-cases/sheets-sync.csv`, run guide
`test-cases/sheets-sync-run-guide.md`) was executed manually on production, 2026-09-30: 16 of 18
`SYNC-*` cases pass, 2 found real bugs (`SYNC-04` → `BUG-008`, `SYNC-14` → `BUG-009`). Five
exploratory sessions (`test-cases/exploratory/`) were run manually on production on 2026-10-02/03.
Blackbox test cases exist for every spec ID (`test-cases/`, greenlit 2026-09-28).

**First test cycle complete: 177 of 180 spec IDs executed (98%).** The summary report is
`reports/2026-09-29-cycle-1-summary.md`.

**19 bugs filed in cycle 1** (`bug-reports/`), all in Jira (`BUG-001`..`BUG-019` → `QAP-17`..`QAP-35`) with
priorities set (`BUG-002` is fixed and closed). Real app bugs found: `BUG-001`/`BUG-002` (L1),
`BUG-003`/`BUG-004` (L2), `BUG-005`/`BUG-006`/`BUG-007` (L4), `BUG-008`/`BUG-009` (L5, manual),
`BUG-010`..`BUG-019` (manual use and exploratory sessions — offline-banner overlap,
install-guide-modal overflow, raw Gemini error surfaced to the user, silent Sync when the OAuth
popup is blocked, missing install-success toast, wrong page-indicator total, unlimited Platform
length in the direct form, AI input accepting a price above the maximum, formula injection in the
Sheets export, iOS install guide wording). `BUG-001` was re-confirmed at L4 on local dev too.

Jira Sprint 2 was bug fixing only (done by the app author). **Sprint 3 (2026-10-04..06)** retested the fixes and covered the Part 2/3 spec changes (`test/UPDATE_NOTES_PART2.md`): L1 `API-01..13` pass, L3 34/34 rules tests pass (`SEC-12..15`), L4 final run 92 of 98 pass (+ mock groups 7 of 7), a real Android phone via Appium (14 pass, 3 fail with `BUG-020`), and the manual production pass (L5 Sheets, real login, PWA, exploratory `CH-06`). 24 bug stubs in total. New this sprint: `BUG-020` (confirmed on production), `021` and `024` (not reproduced on production), `022` (fixed), `023` withdrawn; filed as `QAP-53..56`. After the author fixed `BUG-005`, `013` and `020` (2026-10-06): `BUG-020` and `BUG-013` fixed, `BUG-005` fixed in substance (spec `MAN-13` wording to update). Still open: `BUG-019` (no iPhone). Not run: `PWA-05`/`PWA-12` (need a deploy). Spec deviation to decide: `SYNC-25`. Summary: `reports/2026-10-04-cycle-3-summary.md`.
Not done in cycle 1: formal P1 scoring, full iOS coverage (one manual check on a borrowed iPhone
only), and three spec IDs that cannot be executed (see the report).
