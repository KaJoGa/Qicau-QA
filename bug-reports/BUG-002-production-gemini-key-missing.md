# BUG-002 — Production has no Gemini API key configured; every AI-dependent feature is down

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-18 |
| **Spec ID(s)** | `API-04`, `API-05`, `API-06`, `API-07`, `API-08`, `API-09`, `VOICE-*`, `MAN-01..08` (anything going through the AI) |
| **Severity** | **Critical** — the app's core feature (record an expense by voice or text) cannot work at all for any user, right now, with no workaround. |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, 2026-09-27, ~right now |
| **Found during** | L1 API contract (Postman/Newman), automated — while running the AI-touching folders |
| **Status** | **Fixed** — confirmed resolved 2026-09-27, same day. Re-ran `/api/parse-text` with a clean input: `200`, well-formed result (`Jajan/Starbucks/5000/GoPay/high`), matches `PARSE-01`. |

## Summary
`POST /api/parse-text` and `POST /api/parse-audio` both fail on **every** call, including with
a perfectly clear, valid input, because the Cloudflare Worker has no `GEMINI_API_KEY` set. This
isn't a parsing bug or a fallback-chain issue — the request never reaches Gemini at all.

## Steps to reproduce
```
curl -X POST https://qicau.kajoga.workers.dev/api/parse-text \
  -H "Content-Type: application/json" \
  -d '{"textInput": "Beli kopi goceng di Starbucks pakai gopay"}'
```

## Expected (per spec)
- `API-04`: valid input → HTTP 200, `{ result }` with all required fields.

## Actual
Every AI-touching request — valid text, missing-mimeType audio, large-payload audio, even the
whitespace-only boundary check — returns the same thing:
```
HTTP 500
{"error":"GEMINI_API_KEY is missing. Set it in the Worker Variables and Secrets."}
```
Confirmed on both `/api/parse-text` and `/api/parse-audio`.

## One thing worth noting (not the main point, but relevant to `API-03`)
This response *does* technically satisfy `API-03`'s stated contract ("Kunci AI belum
dikonfigurasi → HTTP 500, `{ error }` yang menyebut kunci hilang") — the shape and wording are
right. But this is an accidental, live production outage, not the deliberately-configured
no-key deployment `API-03` was meant to describe. Treat `API-03`'s format as informally
confirmed by this incident, not as a substitute for a real, intentional test of it later.

## Evidence
```
$ curl -s -X POST https://qicau.kajoga.workers.dev/api/parse-text -H "Content-Type: application/json" \
    -d '{"textInput": "Beli kopi goceng di Starbucks pakai gopay"}' -w "\nHTTP_STATUS:%{http_code}\n"
{"error":"GEMINI_API_KEY is missing. Set it in the Worker Variables and Secrets."}
HTTP_STATUS:500

$ curl -s -X POST https://qicau.kajoga.workers.dev/api/parse-audio -H "Content-Type: application/json" \
    -d '{"audioBase64": "AAAA"}' -w "\nHTTP_STATUS:%{http_code}\n"
{"error":"GEMINI_API_KEY is missing. Set it in the Worker Variables and Secrets."}
HTTP_STATUS:500
```
Also visible in the earlier Newman run against `production-smoke` (4/4 AI-touching requests
returned 500 with this exact message).
