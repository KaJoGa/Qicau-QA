# api-testing/

Postman collections for **L1 API contract** (`API-*`, local dev, Gemini stubbed) and **L2 parse
quality** (`PARSE-*`, real Gemini, production). See `test-plan.md` §3.1, §3.4.

## Status
`collections/qicau-api-contract.postman_collection.json` exists. Every request in the collection
has now been run for real — production via Newman for the AI-touching/contract folders, and
local-dev (Express + the Gemini fetch mock, three separate scenario runs) for "Fallback &
Errors" — see `test-cases/traceability.csv` for the per-spec-ID results. **L1 is complete.**

Two findings so far:
- `bug-reports/BUG-001-*` (Medium, open) — `Cache-Control` on the SPA fallback isn't `no-store`.
- `bug-reports/BUG-002-*` (Critical, **fixed same day**) — production briefly had no Gemini key
  configured; confirmed resolved by re-running the same checks.

`API-06` (missing `mimeType` defaults to webm) is now **confirmed** — two real `.webm`
recordings (`api-testing/data/audio/`, not committed — see that folder's README) both processed
correctly on production with no `mimeType` field sent. Run it with:
```
newman run collections/qicau-api-contract.postman_collection.json \
  -e environments/production-smoke.example.json \
  --folder "Contract — parse-audio — API-06 (data-driven, real webm fixtures — see api-testing/data/audio)" \
  -d data/audio/webm-fixtures.json
```

`API-03` (no AI key configured) and the fallback chain (`API-07`/`API-08`) are now confirmed too
— run for real against local-dev (the app repo happens to live on this machine, at
`D:\Project\Qicau`), each on its own short-lived server instance on port 3001 so the already-running
`npm run dev` on port 3000 was never touched:
- `API-03`: server started with `GEMINI_API_KEY` unset → 500 + error mentions the missing key
- `API-07`: server started with the fetch mock, `MOCK_SCENARIO=primary-fail-fallback-success` →
  client still gets a 200 result
- `API-08`: server started with the fetch mock, `MOCK_SCENARIO=all-fail` → 500 with a non-empty
  friendly error

`test-cases/traceability.csv` has the full per-spec-ID status. **L1 (API contract testing) is
now fully automated and fully executed — no items pending.**

## Sprint 3 status (2026-10-04)
Contract collection extended with `API-08` (strengthened), `API-10`/`PWA-11` (cache headers),
`API-11`, `API-12`, `API-13`. New Gemini-mock scenarios (`over-limit-amount`, `max-amount`,
`out-of-list-enums`, `long-fields`, `negative-harga`, `non-number-harga`, `invalid-json`,
`google-raw-error`) are documented in `environments/stubs/README.md`. Each new folder is named
"Mock scenario: <name> ..." and must be run against a server started with that `MOCK_SCENARIO`
(port 3001, `--folder "<name>"`).

Results (Newman, local dev on :3001 + mock, one server restart per scenario; production for headers):
- `API-01..09` regression: pass (each fallback/error request passes in its own scenario).
- `API-08`: pass - 500 with Indonesian message, raw Google text ("User location is not supported...")
  only appears in the server log, not in the response (BUG-012 retest: fixed).
- `API-11`: pass for parse-text and parse-audio; limit value itself (999999999) still accepted (BUG-017 retest: fixed).
- `API-12`: pass for all six normalisation cases + invalid JSON -> friendly 500.
- `API-13`: pass locally (mock returns a different transcript, server still returns the exact input,
  also for MOCK_SCENARIO=low) and on production (2 calls: coffee text, and "halo apa kabar hari ini cerah ya"
  -> confidence low, raw_transcript equal to input) (BUG-003 retest: fixed).
- `API-10`/`PWA-11`: production passes (`/`, nested unknown route, unknown route: `no-store`;
  `/assets/*.js`: `public, max-age=31536000, immutable`) (BUG-001 retest: fixed on production).
  Local dev (Vite middleware) still sends `Cache-Control: no-cache` for HTML and the `/assets`
  check is not applicable there: these 3-4 assertions fail on local dev by design, only the
  request marked "LOCAL VITE DEV ONLY" records that behaviour. Meaningful only on production/build.

## L2 status (2026-09-28)
`collections/qicau-parse-quality.postman_collection.json` + `data/parse-quality-sentences.json`
(28 sentences covering `PARSE-01..10, 20-32, 40-45, 50-57`, written blind from `Qicau.md` §11).
Run against production with a 4s delay between requests to stay under the Gemini free-tier rate
limit:
```
newman run collections/qicau-parse-quality.postman_collection.json \
  -e environments/production-smoke.example.json \
  -d data/parse-quality-sentences.json \
  --delay-request 4000
```
**26/28 pass cleanly. 2 real findings:**
- `bug-reports/BUG-003-*` (Medium, open) — `raw_transcript` leaks prompt text onto the input when
  no transaction signal is detected (`PARSE-56`).
- `bug-reports/BUG-004-*` (Low, open) — "gocap" slang parsed as `50000` instead of `50`
  (`PARSE-21`), likely confused with the `goceng`/`goban`-family terms.

`PARSE-25` (goban=50000) was already confirmed earlier via `API-06`'s real `.webm` fixture
(`Test2.webm`) rather than re-spending quota on a duplicate text call. `test-cases/traceability.csv`
has the full per-spec-ID breakdown. **L2 is complete** — 4 real bugs found across L1/L2 so far
(`BUG-001` through `BUG-004`), all documented with reproduction steps.

## Folders

| Folder | Contents |
|---|---|
| `collections/` | One Postman collection per concern: contract (`API-01..10`), negative/edge cases, parse-quality (`PARSE-*`, data-driven), and a short note pointing at `automation/` for why Firestore rules (`SEC-*`) are tested there instead (see `test-plan.md` §3.1 L3, §6.3). |
| `environments/` | `local-dev.example.json` and `production-smoke.example.json` — placeholders only. Real environment files (with the Gemini key, base URLs) are **not committed**; copy the `.example.json` and fill in locally. |
| `data/` | The ~20–30 PARSE test sentences (§11 of `Qicau.md`) as CSV/JSON, for the data-driven parse-quality run. |

## Environments (once created)
- `local-dev` — targets local Express with the AI `fetch` mock. Primary target for L1.
- `production-smoke` — targets https://qicau.kajoga.workers.dev/, read-mostly checks only (response shape, `no-store` header) — never anything that writes data at volume. Also used for L2 parse-quality runs.
