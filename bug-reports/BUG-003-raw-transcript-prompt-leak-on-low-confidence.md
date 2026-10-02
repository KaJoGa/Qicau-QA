# BUG-003 — `raw_transcript` leaks part of the AI prompt when no transaction is detected

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-19 |
| **Spec ID(s)** | `PARSE-56` |
| **Severity** | Medium |
| **Found in** | production (https://qicau.kajoga.workers.dev/) |
| **Found during** | L2 parse-quality run, 2026-09-28 |

## Summary
For input with no transaction signal (a greeting), `raw_transcript` in the parsed result is not
the input text as-is — it has prompt instruction text appended directly onto it with no
separator.

## Steps to reproduce
1. `POST /api/parse-text` with `{"textInput": "Halo apa kabar"}`

## Expected (per spec)
`PARSE-56`: "`raw_transcript` | Selalu ada; untuk teks = input persis apa adanya." — for text
input, `raw_transcript` should always be the input exactly as given.

## Actual
```json
{
  "kategori": "Lainnya",
  "platform": "",
  "harga": 0,
  "detail": "",
  "payment_method": "QRIS",
  "confidence": "low",
  "raw_transcript": "Halo apa kabarExtract transaction data from this Indonesian, English, or mixed text input."
}
```
`raw_transcript` is the original input directly concatenated with what looks like the start of
the prompt template sent to Gemini, with no space/separator. Confirmed the input text itself
(`"Halo apa kabar"`) is a prefix of the returned value, not a coincidental resemblance.

## Evidence
Newman run, `api-testing/collections/qicau-parse-quality.postman_collection.json`, row
`{"spec_ids": "PARSE-06, PARSE-55", "sentence": "Halo apa kabar", ...}` in
`api-testing/data/parse-quality-sentences.json` — assertion `raw_transcript matches input exactly`
failed: `expected 'Halo apa kabarExtract transaction dat…' to deeply equal 'Halo apa kabar'`.
All other fields (`kategori`, `harga`, `confidence`) were correct for this case (`PARSE-55`
passes); only `raw_transcript` is affected.
