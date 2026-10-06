# Qicau QA project — final summary

A one-page wrap-up of the whole project (27 Sep – 7 Oct 2026). Detailed numbers live in the two cycle
reports: [cycle 1](2026-09-29-cycle-1-summary.md) and [Sprint 3](2026-10-04-cycle-3-summary.md).

## What was tested
**Qicau**, a voice/text expense-tracking PWA (Firebase Auth + Firestore, Gemini API for parsing, Google Sheets
export). The app was tested as a black box: every test case was written **only from the functional spec**
(`test/Qicau.md`, stable IDs such as `HIST-08`), never from the source code. When the app and the spec disagreed
it was reported as a finding (a bug, or a spec that needed updating), never bent into a passing test.

## How it was tested
| Layer | What | Tooling |
|---|---|---|
| L1 API contract | the two AI endpoints, errors, fallback chain, cache headers | Postman / Newman (28 requests), Gemini mocked at the `fetch` boundary |
| L2 Parse quality | real Gemini on 28 Indonesian sentences, tolerant assertions, repeated runs | Postman data-driven |
| L3 Security rules | data isolation between users, append-only logs, no delete on summaries | `@firebase/rules-unit-testing` (Node), 34 tests |
| L4 UI end-to-end | 115 Cucumber scenarios on desktop Chrome | Selenium + Cucumber + Java 17 + Maven; Firebase emulators |
| L4 on a real phone | the same suite on a USB-connected Android phone | Appium (UiAutomator2) |
| L5 Sheets sync | real Google OAuth, real spreadsheets | manual, own account |
| Stress | `/api/parse-*` baseline | k6 |
| Exploratory | 6 session-based charters | manual, production |

192 written test cases, a traceability matrix (`test-cases/traceability.csv`) that maps every spec ID to its
test cases and result, and three sprints (Sprint 2 was the app author fixing bugs).

## Result
- **207 spec IDs, 205 executed (99%).** Not executed: `AUTH-02` (a sub-second loading state) and `MON-05`
  (a chart that cannot be asserted reliably).
- **23 real bugs** (`BUG-023` was a false alarm that was withdrawn): 18 fixed and verified, 3 closed without a fix
  (spec changed, deferred, accepted), 1 not reproducible on production, 1 open (`BUG-019`, needs an iPhone).
- Automated suites at the end: API 13/13, rules 34/34, desktop UI 97 of 99 run scenarios (the two failures are known
  and explained: the dev server's cache header and the denied-microphone case), and the retested scenarios pass on a
  real Android phone.

## Bugs worth reading first
| Bug | Why it matters |
|---|---|
| `BUG-002` (critical) | Production had no AI key configured, so every AI feature was down; found by the first API run, fixed the same day |
| `BUG-018` | Text starting with `=` became a live formula in the Google Sheets export (a spreadsheet-injection risk); found in an exploratory session on the Sheets export |
| `BUG-020` | The summary tab kept `Rp 0` category rows after a delete or edit; found by the automated suite, confirmed on production and on a real phone, then fixed |
| `BUG-001` | HTML was not served with `no-store`, so a new release could be hidden behind a cached page; found at the API layer |
| `BUG-017` | An amount above the stated maximum was accepted from AI input; found in an exploratory session |

## What went well, and what was learned
- **Blind testing paid off in both directions.** The suite found real bugs, and twice it exposed a spec that was
  out of date after the app improved (the empty-price message, and pagination under a category filter). In both cases the
  spec was updated first, then the tests.
- **A wrong test is a finding too.** `BUG-023` was reported from a badly seeded dataset and withdrawn after a retest with
  realistic data; the retest then found the real problem (an empty page under a category filter), which was later fixed.
- **Test harness defects are separate from app defects.** The harness once cleared the wrong emulator project; a real phone
  needs a different tap sequence than a desktop browser (the first tap closes the keyboard). Both were fixed in the tests and written down.
- **Real devices find different things.** Running the suite on a physical phone through Appium confirmed the layout and offline
  checks, and reproduced `BUG-020` outside the desktop.

## Limits (stated honestly)
- **iOS:** no device, so only one manual check; `BUG-019` stays open.
- **Real microphone quality** and production-scale load were not tested (voice used fixture audio; stress is a small baseline).
- **Production** is tested manually with the tester's own account only; automated runs target local dev + emulators by design.
- **Firestore is NoSQL**, so there was no SQL database layer to test.

## How this maps to a QA internship profile
| Skill | Evidence in this repo |
|---|---|
| Manual testing (mobile web, Android, API) | `test-cases/` (manual runs, Android run, Sheets run guide), exploratory charters, `api-testing/` |
| Java test automation | `automation/` (Selenium + Cucumber, page objects, step definitions, Maven) |
| Appium / mobile automation | Real-phone mode in `automation/` (`test-cases/android-run-2026-10-05.md`) |
| API testing | `api-testing/` (Postman collections, mock scenarios, Newman runs) |
| Defect tracking and reporting | `bug-reports/` (23 reports with steps, expected vs actual, status); a private Jira project was used alongside |
| Scrum | three short sprints mapped to the test layers; reports per cycle |
| Test design and traceability | spec-ID tagged cases, `traceability.csv`, risk-based plan in `test-plan.md` |
| Performance (a plus) | `performance/` (k6 baseline) |
| Security awareness | Firestore rules tests, the spreadsheet-injection and prompt-leak findings |

## Where to look
Start with the [README](../README.md), then [`test-plan.md`](../test-plan.md), the [traceability matrix](../test-cases/traceability.csv)
and the [bug index](../bug-reports/INDEX.md).
