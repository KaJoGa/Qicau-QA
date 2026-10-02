# Qicau — Test Plan

| | |
|---|---|
| **Version** | 0.4 (decisions locked; first test cycle executed and reported 2026-10-03) |
| **Date** | 2026-09-27 |
| **Application under test** | Qicau — voice/text expense-tracking PWA (Firebase Auth + Firestore, Gemini API, Google Sheets API). Live at https://qicau.kajoga.workers.dev/ (Cloudflare Worker, production Firebase). Local dev via Express (`server.ts`, `npm run dev`), which can target the Firebase Local Emulator Suite. |
| **Spec baseline** | [`test/Qicau.md`](test/Qicau.md) — every test traces to a spec ID (`AUTH`, `NAV`, `HOME`, `VOICE`, `SAVE`, `LOWC`, `MAN`, `HIST`, `MON`, `SYNC`, `API`, `PARSE`, `SEC`, `PWA`, `TOAST`) |

Everything below is a decision, not a placeholder; items that were open during planning are marked **[RESOLVED]** in §6.1.

---

## 1. Objectives

1. Verify that Qicau behaves as specified in `test/Qicau.md`, from a user's point of view (black-box).
2. Find and document defects with reproducible reports and clear severity.
3. Demonstrate, as a portfolio, the practices a QA intern is expected to show: test design, manual + automated execution, API testing, defect tracking, and (stretch) stress testing.

## 2. Scope

### 2.1 In scope

**Platforms / browsers** (Android phone + a Chromium-based desktop browser, one real Google account usable on both)

| Tier | Environment | Depth |
|---|---|---|
| Primary | Desktop Chromium-based browser (Chrome/Edge, Windows) | Full manual + automated (automation targets local dev + emulator, per §3.1) |
| Primary | Android Chrome (real device), browser tab **and** installed PWA | Full manual, on production, with the tester's own Google account; automated smoke via Appium is a stretch goal, not committed |
| Excluded | iOS Safari | **No device available.** Recorded as a coverage gap in the summary report, not silently dropped. |

"Android/iOS" in the job posting is interpreted as **mobile web + installed PWA**, since Qicau is a PWA, not a native app. iOS coverage would need a borrowed device or a cloud device farm — out of scope unless one becomes available later.

**Features** (all spec sections)

| Area | Spec IDs | Notes |
|---|---|---|
| Auth & session | AUTH | Google sign-in/out, cancelled popup, per-user isolation |
| Navigation & settings | NAV | Tabs, deep links `?tab=`, theme, layout |
| Home | HOME | Today total, recent-3 list, realtime updates |
| Voice input | VOICE | Record/stop, silence auto-stop (2 s), max 60 s, <0.8 s ignore, permission, offline |
| Post-save flow | SAVE, LOWC | Toast, undo, edit modal, low-confidence modal + log |
| Manual input | MAN | AI-text mode and direct form (offline-capable) |
| History | HIST | Filters, grouping, pagination (30/page), detail, delete |
| Monthly/weekly summary | MON | Totals, per-category, donut chart |
| Sheets export | SYNC | Auth token reuse, file/tab layout, idempotency, reset, error handling |
| AI service API | API | `/api/parse-audio`, `/api/parse-text` contract, fallback behaviour |
| AI parsing rules | PARSE | Slang money, brand correction, categories, confidence |
| Data security rules | SEC | Firestore rules for transactions, low-confidence logs, user profiles |
| Offline & PWA | PWA, TOAST | Offline banner, sync on reconnect, install, update prompt, manifest |

**Test types:** functional, boundary/negative, exploratory (session-based), cross-browser/device, API contract, authorization (rules), usability sanity, regression (automated), and **stress/load on the API layer** (stretch — see §3.5).

### 2.2 Out of scope

- Modifying or fixing Qicau source code; unit/integration tests of the app's internals (white-box).
- Availability, latency, and correctness of Google's services themselves (Firebase, Gemini, Sheets/Drive) beyond how Qicau handles their failures.
- Benchmarking Gemini model quality in general. Only the specified PARSE examples and rules are checked (small sample, tolerant assertions).
- Load/stress testing against the **real** Gemini API or real Firebase project (cost and quota risk). Stress tests use a stubbed AI backend.
- Penetration testing / exploit development. Only basic authorization and input-handling checks appear here (SEC, input sanitisation, API auth observations).
- Native Android/iOS apps; browsers other than those in §2.1; OS/device matrix beyond the ones available.
- Localization (UI is Indonesian only) and formal accessibility audit (light checks may appear in exploratory sessions).
- CSV export or any feature not described in `test/Qicau.md`.
- CI/CD and deployment pipeline of Qicau.

## 3. Test approach

### 3.1 Layers

The app author's layering (`test/README.md`, L1–L5) is the authority for what each layer targets; this plan must stay consistent with it rather than invent a competing setup. Tooling is Java where the role requires it, with one deliberate exception (L3).

| Layer | Spec IDs | Target | Method | Tooling |
|---|---|---|---|---|
| L1 API contract | API-* | **Local Express** (`npm run dev`, isolated `PORT`), Gemini mocked at the `fetch` boundary in `shared/gemini.ts` | Direct HTTP | **Postman** collections (+ Newman for CLI runs) |
| L2 Parse quality | PARSE-* | **Live production** (https://qicau.kajoga.workers.dev/), real Gemini, ~20–30 sentences, repeated runs (e.g. 5×), pass on threshold, mandatory fields only | Real API calls, tolerant assertions | Postman data-driven run (CSV/JSON of sentences); run manually or on demand, **not** per commit |
| L3 Data rules | SEC-* | **Firebase Local Emulator Suite** (`npm run emulators`, project `demo-qicau-test`), real `firestore.rules` | Firestore rules exercised via the emulator | **`@firebase/rules-unit-testing` (Node/JS)** — kept as-is per `test/README.md`; not rewritten in Java (see §6.3) |
| L4 UI end-to-end | AUTH NAV HOME VOICE SAVE LOWC MAN HIST MON PWA TOAST | **Local dev + emulator** (`VITE_USE_FIREBASE_EMULATOR=true npm run dev` + `npm run emulators`), fake microphone, mocked AI | Browser automation, emulator's fake-account picker for sign-in | **Selenium 4 + Java 17 + Cucumber-JVM + JUnit 5**, Page Object pattern, Maven |
| L5 Sheets sync | SYNC-* | **Live production**, the tester's own Google account | Manual verification each release; automated request-shape checks only if the Sheets base URL can be redirected to a mock | Manual; optional stub |
| Manual/exploratory smoke | cross-cutting | **Live production**, Android Chrome + desktop Chromium, the tester's own account | Session-based exploratory + release smoke (never cross-user isolation tests — only one real account) | Manual |
| Mobile | mobile subset of L4 | Android Chrome (real device) | Same scenarios, manual | Manual; **Appium (Java)** stretch goal, not committed |
| Stress | API-*, VOICE-04, API-09 | **Local dev**, AI stubbed | Small-scale ramp against `/api/parse-*` — enough to demonstrate the practice, not a full load test | **k6** |

### 3.2 Manual testing
- Test cases written from the spec, one or more per spec ID, stored as **CSV** in `test-cases/` (columns in §8) — chosen over per-area markdown tables for compactness, and because a CSV can be bulk-imported into Jira as issues later without re-typing anything.
- Session-based exploratory charters per feature area, focused on the risk areas in §4.
- Executed on the environments in §2.1; results recorded per case (Pass/Fail/Blocked/Not run) with build/version and environment. Once Jira is in use, execution status can live there instead of a duplicate local column — the CSV stays the authored source of truth, Jira owns run history and assignment.

### 3.3 Automation (UI)
- Gherkin scenarios tagged with spec IDs (`@HIST-08 @regression`), so coverage can be reported per ID.
- Test tiers by tag: `@smoke` (minutes, gate), `@regression` (full), `@manual-only` (documented but not automatable).
- Browser: Chrome with `--use-fake-device-for-media-stream` and `--use-file-for-fake-audio-capture=<wav>` for deterministic voice input; permissions pre-granted.
- Explicit waits only; no fixed sleeps. Test data created/cleaned per scenario via the emulator, never through shared state.
- Not automated by design: real Google OAuth/Sheets flows (bot detection / 2FA / consent screens make them flaky and account-risky), real-microphone behaviour, iOS (no device).
- Sign-in in automation uses the Auth emulator's built-in fake-account picker, not real Google login — so `AUTH-01..07`/`NAV`/session tests *are* automatable, unlike originally assumed.

### 3.4 API testing (Postman)
- Confirmed: the client calls Qicau's own `/api/parse-audio` and `/api/parse-text` (no other backend), and the Sheets/Drive API is called client-side with the user's OAuth token. No Cloud Functions.
- Collections: contract (status codes, schema, required fields, enum values — API-01..06), negative/edge (empty, whitespace, huge, wrong types, missing mimeType), fallback behaviour via the stub (API-07/08), payload size (API-09), SPA/`no-store` header (API-10), and — separately — Firestore rules (SEC), run through `@firebase/rules-unit-testing` rather than Postman (see §3.1 L3).
- Postman environments: `local-dev` (primary target for L1, AI stubbed) and `production-smoke` (read-mostly checks against the live Worker — API-10, response shape — never anything that writes throwaway/garbage data at volume). Secrets live in environment variables that are not committed.

### 3.5 Non-determinism strategy (LLM)
- Assert the **contract**, not prose: JSON valid, required fields present, enums valid, `harga` integer ≥ 0.
- Assert **mandatory fields** exactly (harga, kategori, payment_method, confidence class); `platform`/`detail` compared loosely (case-insensitive, small variations).
- Repeat each PARSE case N times; pass if ≥ threshold (proposed 4/5) and record flakiness rate.
- Anything that decides whether data is saved (the `low` gate) is tested with **stubbed** AI responses for determinism, independently of AI quality.

### 3.6 Stress testing (small-scale, k6)
- Deliberately kept small: this demonstrates the practice for a portfolio, not a capacity-planning exercise.
- Target: local dev's `/api/parse-*` endpoints with the AI upstream stubbed (never the real Gemini endpoint or production). One or two short k6 scripts: a brief ramp (e.g. 1→20 VUs over a couple of minutes) and one edge case (large payload near the 50 MB body limit, or a burst that forces the fallback chain — API-07/08 — to run repeatedly).
- Metrics: p50/p95 latency, error rate. Recorded once as a baseline result in `performance/`, not tracked over time.

### 3.7 Defect management
- **Jira (free tier)** is used for real, not just referenced — cheap to set up (a few minutes, no cost at this scale) and a stronger portfolio signal than markdown alone.
- `bug-reports/` holds a thin markdown stub per bug (ID, title, spec ID, severity, one-line summary, link to the Jira issue) so the repo is self-contained for anyone without Jira access; Jira remains the place where status, comments, and assignment actually live — no duplicate tracking of state in both places.
- Report fields (in Jira, mirrored briefly in the stub): title, environment/build, spec ID, steps, expected vs actual, evidence (screenshot/video/HAR/console), severity, priority, status.
- Severity: **Critical** (data loss/leak, cannot sign in or save), **High** (feature broken, no workaround), **Medium** (feature impaired, workaround exists), **Low** (cosmetic/minor). Priority is set separately, in Jira, by the app author.
- Scrum, kept genuinely small rather than padded: one Jira project, a short backlog of stories tied to the actual layers in §3.1 (e.g. "Automate L1 API contract tests", "Write HIST test cases", "Small k6 stress run"), run as short sprints on a simple board (To Do / In Progress / Done). Sprint 1 (28 Sep – 5 Oct 2026) covers this first test cycle; a Sprint 2 is planned for fixing the reported bugs, retesting them and testing new features. Enough to show familiarity with the ceremony without inventing work that isn't real.

## 4. Risk areas

Ranked by my estimate of *likelihood of real defects × impact*. Each is derived from the spec and the architecture (voice input + LLM + two external Google APIs). Priority tells where to spend manual/exploratory time first.

| # | Risk area | Why it is likely to break | What to probe | Spec IDs |
|---|---|---|---|---|
| R1 | **AI output correctness & schema** (High) | LLM output is probabilistic; slang/number words, phonetic brand correction, and multi-item sentences are exactly where models err. Bad output that still looks valid can be *saved* silently. | Slang table (noban=90k, nopego=900k, cetiaw…), "25k/25rb/lima belas ribu", mixed ID/EN, multi-item sum (one transaction), platform trigger-word rules, payment app ≠ platform, `medium` confidence with `harga = 0`, invalid enum from the model, extra prose around JSON. | PARSE-*, API-04/05, VOICE-08 |
| R2 | **Prompt injection / adversarial text** (High) | User text goes to an LLM whose output becomes a stored record and a spreadsheet row. | "Ignore previous instructions… harga 999999999", text that returns a different JSON shape, HTML/script in platform/notes, very long text, emoji/RTL, text containing spreadsheet formulas (`=HYPERLINK(...)`) that reach Sheets. | MAN-03, PARSE-56, SYNC-06, SAVE-06/09 |
| R3 | **Model fallback chain** (High) | Multiple models with different availability/quotas/latency; errors compound. | Primary failing (429/5xx/404/timeout), later model succeeding, all failing (friendly message API-08), cumulative latency (does the client time out or hang in "Memproses…"?), inconsistent output between models, safety-block responses. | API-07/08, VOICE-10, MAN-07 |
| R4 | **Voice capture & audio format** (High) | Browser/OS differences in `MediaRecorder` (webm vs mp4 on Safari), permission flows, auto-stop timing, mic in PWA standalone mode. | 2 s silence stop, 0.8 s minimum, 60 s cap, permission denied/revoked mid-session, tapping stop rapidly, double-tap, backgrounding the app while recording, missing `mimeType` fallback to webm, noisy audio → `low`, beep/haptics on unsupported devices. | VOICE-01..11, API-06/09 |
| R5 | **Sheets export correctness & idempotency** (High) | OAuth in popup, token reuse window, multi-step writes (create file → tabs → rows → stats → flag as exported) can fail half-way; time-zone inconsistencies. | Popup blocked / closed (mobile!), token expiry at ~50 min, error mid-export (are transactions wrongly marked exported?), repeated sync (no duplicates), reset + sync, file deleted by user, cross-year data, **month/tab assignment near midnight and month boundaries — spec says "this month" follows device tz while the sheet's timestamp column is WIB**, column/format fidelity, stats vs row totals, many rows, special characters. | SYNC-01..18 |
| R6 | **Time-boundary calculations** (Medium-High) | "Today", week (Monday start), month all depend on device time zone; realtime queries. | Transactions at 23:59/00:00, first/last day of month, Sunday vs Monday, device tz ≠ WIB, clock change while app is open, "Bulan Ini" filter vs monthly total consistency. | HOME-02, HIST-05, MON-02/03/08 |
| R7 | **Pagination × filters × realtime** (Medium-High) | Cursor pagination combined with category/time filters and live updates is a classic source of duplicates/missing rows. | Exactly 30, 31, 60, 61 rows; filter change resets to page 1; category filter with sparse data; new transaction arriving while on page 2 (HIST-13); delete last item on a page; indicator `n / total` correctness; ▶/◀ enabled states. | HIST-01..13 |
| R8 | **Offline / PWA / service worker** (Medium-High) | Offline persistence, background sync, and cached shells are notoriously flaky; stale-version problems. | Offline entry then app close/reopen then reconnect; offline delete/edit; flapping connection; banner timing; update prompt after new release; stale HTML cache; install flow per browser; manifest shortcuts. | PWA-01..11, MAN-10, PWA-03 |
| R9 | **Authorization & data isolation** (Medium impact: High) | Rules are the only server-side protection if the client talks to Firestore directly; validation may live only in the client. | Cross-user read/write/delete/change of `user_id`, unauthenticated access, undefined collections, log update forbidden (append-only), and — beyond the spec — **whether the DB accepts values the UI would never allow** (price > 999,999,999, negative, invalid category/payment_method, huge strings) when written directly. | SEC-*, AUTH-07 |
| R10 | **AI endpoint abuse & cost** (Medium) | Confirmed by the app author: no app-level auth or rate limit on `/api/parse-*`; each call consumes paid Gemini quota and up to 50 MB is accepted. Test as a known, accepted gap — not a "will it happen" question. | Call without a session, repeated/parallel calls, whitespace-only text, extremely long text, huge audio payload, error messages leaking internals (API-03/08). | API-01..09 |
| R11 | **Edit/undo/delete flows** (Medium) | Multiple modals, optimistic UI, and toast timers interact. | Undo after toast expiry, edit while new toast appears, combo-box (SAVE-07) partial text, price cap (≥10 digits), delete confirmations, double-click on delete/save, failure handling when offline/rules reject. | SAVE-*, HIST-09..12 |
| R12 | **Cross-device consistency & UI** (Low-Medium) | Realtime sync between sessions; responsive layout; theming. | Two devices open simultaneously, small screens, landscape, large desktop, theme persistence, long platform names, very large amounts formatting (`Rp 999.999.999`). | HOME-07, NAV-05..08, HIST-04 |

Known blind spots to keep in mind: real-microphone quality, iOS Safari (device availability), and Google consent-screen behaviour cannot be fully covered by automation.

## 5. Entry and exit criteria

### 5.1 Entry criteria (to start a test cycle)
- Spec `test/Qicau.md` is baselined; any spec change is versioned.
- A **build/version identifier** for the app under test is known and stable for the cycle.
- The application is reachable (production URL or local instance) and passes a **smoke check** (sign-in, save a manual transaction, view history).
- Test environment ready per §6: emulators running for L3/L4, AI stub wired for L1/L4/stress, Gemini key present in local `.env` for L2 runs only, the tester's Google account available for production-facing manual/L2/L5 work.
- Test cases for the cycle are written and reviewed; test data set defined.
- Devices/browsers listed in §2.1 are available (or the gap is recorded).

### 5.2 Exit criteria (to close a test cycle)
- 100% of P1 (smoke/critical-path) test cases executed; ≥ 95% of all planned cases executed; the remainder documented as Blocked/Not run with reason.
- P1 pass rate 100%; overall pass rate ≥ 90% [ASSUMPTION – thresholds are proposals, to be confirmed by the app author].
- **No open Critical or High** defects; all Medium have a triage decision (fix / defer / won't fix) recorded.
- PARSE suite: mandatory-field accuracy ≥ 90% across repeated runs, with flakiness rate reported.
- Automated smoke suite green on 3 consecutive runs on the target environment.
- Every spec ID has at least one test case (traceability matrix complete); uncovered IDs listed with justification.
- Test summary report written: coverage, results, defect statistics, open risks.

### 5.3 Suspension / resumption
- Suspend when: app cannot be reached or signed into, build is unstable (smoke fails), emulator/stub environment down, or quota/cost limit reached.
- Resume when: smoke passes again on a stated build and the blocking cause is recorded.

## 6. Environment strategy

**Principle:** automated and bulk testing never touches the live production Firebase project or real user data — it runs on the Firebase Local Emulator Suite. The one real account available is used only for manual/exploratory work directly on production, deliberately, and never for anything that needs a second identity.

| Component | Target environment | Status |
|---|---|---|
| **Firebase Auth** (L3/L4) | Auth emulator, fake-account picker, project id `demo-qicau-test` | ✅ Resolved — wired via `VITE_USE_FIREBASE_EMULATOR=true`, fully offline, no billing/login |
| **Firestore** (L3/L4) | Firestore emulator, rules read live from the real `firestore.rules` | ✅ Resolved — never a stale copy (former constraint C6, closed) |
| **Gemini (AI)** — L1/L4/stress | Stub at the `fetch` boundary in `shared/gemini.ts` (both `server.ts` and `worker.ts` import it, and it already calls the REST endpoint via plain `fetch`, not an SDK) | ✅ Resolved — straightforward to intercept |
| **Gemini (AI)** — L2 | Real Gemini, on **production**, the app author's own key/quota | ✅ Accepted risk, kept small (§3.6-adjacent: ~20–30 sentences, on demand) |
| **Google Sheets/Drive** (L5) | Real Google API, production, the tester's own account | ✅ Manual only, once per release — no emulator exists for this, not attempted |
| **App under test** | Local dev (Express + Vite) for L1/L3/L4/stress; live Worker for L2/L5/manual/mobile | ✅ Resolved — see §3.1 |
| **Mobile devices** | Android real device (the tester's own) | ✅ Android covered manually; iOS excluded (no device) |

### 6.1 Remaining open items

- **[RESOLVED] L1 target vs the two deploy runtimes** (local Express chosen, as described below; the Worker's `no-store` header was checked and found wrong — `BUG-001`). `server.ts` (Express, local) and `worker.ts` (Cloudflare Worker, production) both import `shared/gemini.ts` but differ in payload limits and cache headers. This plan targets **local Express** for L1/L4 (deterministic, mockable, no production risk) and treats the Worker's behaviour as a thing to spot-check manually (API-09 payload limit, API-10 cache header) rather than fully re-run in automation against production. Flag if that spot-check should instead be a small dedicated Postman "production-smoke" pass — already sketched in §3.4 — versus something more.
- **[RESOLVED] Build tool for the Java stack (Maven vs Gradle): Maven was used.** Not specified anywhere; defaulting to **Maven** (more common with Selenium/Cucumber-JVM tutorials and CI examples, which matters for a portfolio a reviewer will skim). Say if Gradle is preferred instead.
- **Real microphone (C7, unresolved by nature).** Fake-audio-file input verifies the pipeline, not real microphone/codec behaviour. Only manual sessions on the real Android device cover that; accepted as a permanent gap, not something to solve.
- **Devices vs `localhost` (former C5, partially resolved).** The Android device is real, not an emulator, so it still can't reach a `localhost` dev server directly — use the machine's LAN IP or `adb reverse` when running L4/mobile scenarios against local dev from the phone. Manual production testing on the phone has no such issue (it's just hitting the public URL).

### 6.2 Test data & accounts
- Emulator (L3/L4): ≥ 2 synthetic users (A, B) for isolation tests (`SEC-03/05/09`, `AUTH-07`), plus a "large data" user (≥ 100 transactions) for pagination, seeded via emulator import/script. Reset between runs.
- Production (L2/L5/manual): the tester's own Google account only. Data written there during testing should be identifiable (e.g. a consistent note/platform prefix) and cleaned up after each session — it is real data in a real account, not disposable fixture data.
- Seed sets (emulator): empty user, boundary date set (midnight, month/week boundaries), cross-year data, max-price data.
- Audio fixtures: clean sentences, silence, < 0.8 s clip, long (> 60 s) clip, noisy clip.

### 6.3 Note on tooling and rules testing
Firestore rules are enforced only for client-SDK/REST requests carrying an Auth token; the Java **Admin** SDK bypasses rules entirely, so a Java-only approach can't test rules directly. `@firebase/rules-unit-testing` (Node/JS) is the standard tool for this, and the app author has already wired the emulator around it — this plan keeps that, rather than reinventing a REST-based Java approach, to stay consistent with `test/README.md`. It's the one deliberate non-Java corner of the stack; everything else (L1 Postman, L4 Selenium/Cucumber) stays Java per the role requirement.

## 7. Deliverables

| Deliverable | Location |
|---|---|
| Test plan, this file | `test-plan.md` |
| Manual test cases (CSV, one file per feature area) + traceability | `test-cases/` |
| Automation project (Selenium + Cucumber + Java/Maven) | `automation/` |
| Postman collections + environments | `api-testing/` |
| k6 stress script + one baseline result | `performance/` |
| Jira project (real) | external — link recorded in `README.md` |
| Bug report stubs (thin, link to Jira) | `bug-reports/` |
| Test summary report per cycle | `reports/` |

### 7.1 Test case CSV format
One row per case, one file per feature area (`test-cases/auth-nav.csv`, `home-voice.csv`, `manual-input.csv`, `history.csv`, `monthly.csv`, `sheets-sync.csv`, `ai-parsing.csv`, `security-rules.csv`, `pwa-offline.csv`). Columns:

`case_id, spec_id, title, preconditions, steps, expected_result, priority, type, environment, automated`

- `case_id`: e.g. `TC-HIST-001`. `spec_id`: the `Qicau.md` ID(s) it covers, e.g. `HIST-08`.
- `priority`: P1 (smoke/critical path) / P2 / P3, per §5.2.
- `type`: functional / negative / boundary / security / exploratory-charter.
- `environment`: local-emulator / production, per §3.1.
- `automated`: Y / N / planned — kept here even though Jira could track this, because the CSV needs to be readable and diffable without opening Jira.
- Written in English (spec itself is Indonesian; this keeps the artifact consistent with the automation code and the internship application, both English).

If/when this repo files these into Jira as issues, this CSV is the import file — written once, used twice, not retyped by hand into Jira.

## 8. Repository structure

```
QA-Qicau/
├── CLAUDE.md
├── test-plan.md
├── README.md                      # portfolio landing page: what was tested, how, results, Jira link
├── test/                          # EXISTING, untouched: Qicau.md (spec), README.md (OPEN_ISSUES.md is the app author's private notes, not published)
├── test-cases/
│   ├── README.md                  # CSV column reference (§7.1), naming, status legend
│   ├── traceability.csv           # spec ID -> case ID(s) -> automated? (Y/N)
│   ├── auth-nav.csv  home-voice.csv  manual-input.csv  history.csv
│   ├── monthly.csv   sheets-sync.csv ai-parsing.csv    security-rules.csv  pwa-offline.csv
│   └── exploratory/               # session charters + notes
├── automation/                    # Java 17, Maven
│   ├── pom.xml
│   └── src/test/
│       ├── java/.../{pages,steps,hooks,support,runners}
│       └── resources/{features,audio,testdata,config}
├── api-testing/
│   ├── collections/               # contract, negative, parse-quality (production), firestore-rules note
│   ├── environments/              # local-dev.example.json, production-smoke.example.json; real secrets git-ignored
│   └── data/                      # PARSE sentences CSV/JSON
├── security-rules/                # L3 — @firebase/rules-unit-testing (Node/JS, deliberate exception)
│   ├── package.json  rules.test.mjs
│   └── README.md                  # coverage table, how to run, collection/field names (provided by the app author)
├── performance/                   # k6 script(s) + one baseline result summary
├── environments/
│   ├── emulator/                  # start-script notes only (firebase.json/.firebaserc live in the app repo)
│   └── stubs/                     # AI fetch-mock used by L1/L4/stress
├── bug-reports/                   # BUG-###.md stub (title, spec ID, severity, one-line summary, Jira link) + index
├── reports/                       # test summary per cycle, coverage
└── .gitignore                     # .env, tokens, target/, secrets
```

Not included on purpose: any copy of Qicau application code. `firestore.rules` is referenced, never copied, so it can't go stale.
