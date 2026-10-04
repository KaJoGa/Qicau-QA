# BUG-012 — Raw Gemini API error shown to the user instead of a friendly message

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-28 |
| **Spec ID(s)** | `API-08`, `VOICE-10`, `MAN-07` |
| **Severity** | Low (proposed — same class as `BUG-005`: no data loss, save is correctly rejected, but a raw/technical message leaks through where the spec requires a friendly one) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/ |
| **Found during** | Manual exploration, 2026-09-30 |
| **Status** | **Fixed** - Fixed (retest 2026-10-04, API-08): friendly Indonesian message, raw Google text no longer reaches the client (text and audio). |

## Summary
When every Gemini model in the fallback chain fails, the app shows a raw, partly-English API
error string instead of the friendly, Indonesian message the spec requires.

## Steps to reproduce
Intermittent, not reliably forceable on demand — I hit it during normal AI (voice / Teks
AI) usage: input that worked moments before failed on the next attempt with the same input.

## Expected (per spec)
- `API-08`: "Semua model gagal → HTTP 500, `{ error }` **berisi pesan ramah** (mis. akses ditolak
  / model tak ditemukan / input diblokir kebijakan)."
- `VOICE-10` / `MAN-07`: "Alert 'Gagal memproses suara/teks. …pesan…'" — a clean, app-owned
  Indonesian alert, not the underlying API's own error text.

## Actual
The alert read **"Bad request, permintaan tidak valid. detail 400
user location is not supported for the api use"** — a mix of Indonesian ("permintaan tidak
valid") and an untranslated, technical Gemini API error string ("400 user location is not
supported for the api use") rather than a single clean, friendly message.

I separately confirmed (own research, not app-side) that the underlying cause is a
Gemini API geographic/location restriction, not an app defect — and that it's transient: retrying
after a short wait, or from a different browser, succeeds. That intermittency is expected for a
location/routing-based upstream restriction and isn't itself something to chase from this repo;
what's in scope is that this specific failure reaches the user as a raw API string instead of
being wrapped in the friendly message `API-08` requires, the same class of gap as `BUG-005`
(browser's generic validation text shown instead of the app's own Indonesian message).

## Evidence
Observed by me during manual use, 2026-09-30 — not independently reproduced
via automation (this failure mode isn't reliably forceable; `automation/README.md` already
documents a related, structurally-unforceable case for `VOICE-06`/`AUTH-05`). Exact string as
reported: `Bad request, permintaan tidak valid. detail 400 user location is not supported for the
api use`.

## Note
Likely fix: catch the "all models failed" case in `shared/gemini.ts` (or wherever the final
error is formatted before reaching `VOICE-10`/`MAN-07`'s alert) and always emit the app's own
"Gagal memproses suara/teks. …" wording, logging the raw Gemini error server-side for debugging
instead of forwarding it to the client as-is.
