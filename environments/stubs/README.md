# environments/stubs/

## `gemini-fetch-mock.cjs`

A Node preload script that patches global `fetch` so any outbound call to
`generativelanguage.googleapis.com` returns a canned response instead of hitting real Gemini.
Everything else (Firestore, Sheets, anything not Gemini) passes through untouched. It does not
modify Qicau's source — it's an external harness loaded *before* the app's own code runs.

### How to use it — confirmed working (2026-09-27)

Qicau's `dev` script is `tsx server.ts`, ESM (`"type": "module"`) — but this mock file is
`.cjs`, and a `.cjs` extension forces CommonJS regardless of the package's `"type"`, so
`--require` still works unmodified.

**On Windows, set `NODE_OPTIONS` on its own line, then run `npm run dev` separately** — from
the **Qicau app repo** (not this one):

```powershell
$env:NODE_OPTIONS = "--require /absolute/path/to/QA-Qicau/environments/stubs/gemini-fetch-mock.cjs"
$env:MOCK_SCENARIO = "high"
npm run dev
```

Replace `/absolute/path/to/QA-Qicau/...` with wherever this QA repo actually sits on your
machine.

**On macOS/Linux** (or Git Bash on Windows *outside* of npm — see the gotcha below), the
inline Unix form also works:

```bash
NODE_OPTIONS="--require /absolute/path/to/QA-Qicau/environments/stubs/gemini-fetch-mock.cjs" \
MOCK_SCENARIO=high \
npm run dev
```

`NODE_OPTIONS` is read by the Node binary itself, before `npm run dev`'s script wrapper gets
control, so this works no matter what runs `server.ts` under the hood.

#### ⚠️ Windows gotcha: don't inline it through `npm.cmd`
The inline Unix-style form (`NODE_OPTIONS="..." npm run dev`) breaks on Windows when run through
Git Bash / `npm.cmd` — it throws `Could not determine Node.js install directory`. That's an
npm/Windows shell quirk (how the env var gets quoted on the way to `cmd.exe`), not a problem
with the mock. Use the two-line PowerShell form above instead.

#### Expect the log line 2–3 times, not once
`tsx` re-execs itself into a second Node process to register its ESM loader, and npm's own
wrapper spawns a node process too — `NODE_OPTIONS` is inherited by all of them, so
`[gemini-fetch-mock] active...` prints multiple times on a single `npm run dev`. That's
expected, not double-mocking — just double-logging on startup.

#### Unrelated noise you might see
`WebSocket server error: Port 24678 already in use` — Vite's HMR websocket colliding with
another already-running `npm run dev` instance. Harmless, unrelated to the mock.

**Confirmed working end-to-end** against the real app repo: the hook fires and the server still
starts normally on the requested port.

**2026-09-28 — used for real to close out `API-03`/`API-07`/`API-08`.** The app repo turned out
to live on the same machine as this QA repo (`D:\Project\Qicau`), so all three were run for real
rather than left as "needs the app author to do this manually": each on a short-lived instance on
`PORT=3001` (started, tested with an isolated single-request Newman collection, then killed),
never touching the `npm run dev` already running on port 3000. See
`api-testing/README.md` and `test-cases/traceability.csv`.

### Scenarios

| `MOCK_SCENARIO` | Behaviour | Used by |
|---|---|---|
| `high` (default) | Every call succeeds, high confidence | Baseline / most L1 contract tests |
| `medium-zero` | Succeeds, confidence `medium`, `harga: 0` | The confirmed edge case (see `CLAUDE.md`) |
| `low` | Succeeds, confidence `low` | VOICE-09 / MAN-05 — should not be saved |
| `primary-fail-fallback-success` | First `MOCK_FAIL_COUNT` (default 1) attempts fail, then succeeds | API-07 — fallback chain |
| `all-fail` | Every attempt fails | API-08 — all models exhausted |

