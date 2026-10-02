# performance/

Small-scale k6 stress test, kept deliberately small — it demonstrates the practice for a
portfolio, not a capacity-planning exercise (`test-plan.md` §3.6).

## Status (2026-09-28) — complete
Both scripts written and run for real against local dev (Express, `PORT=3001`, isolated from
the `npm run dev` already running on port 3000), Gemini stubbed via
`environments/stubs/gemini-fetch-mock.cjs` — see `results/baseline-2026-09-28.md` for the full
numbers. **0 failures across both runs; nothing to file as a bug.**

- `parse-ramp.js` — a brief ramp, 1→20 VUs over ~105s, `MOCK_SCENARIO=high`. 1,663 requests,
  0 failed, p95 latency 2.37ms.
- `parse-edge-case.js` — the model fallback chain (API-07) forced on every request under 15
  constant VUs for 30s, `MOCK_SCENARIO=primary-fail-fallback-success`. 900 requests, 0 failed,
  p95 latency 2.26ms — fallback holds up under concurrent load.
- `results/baseline-2026-09-28.md` — the baseline summary (not tracked over time, run once for
  the portfolio per §3.6).

k6 was installed via `winget install GrafanaLabs.k6` to run these for real rather than leaving
them unexecuted.

## Rules
- **Never** against the real Gemini endpoint or production Firebase — local dev only, AI stubbed.
- Requires k6 installed locally (`k6.io` — a single binary, no separate runtime needed).
