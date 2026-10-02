# BUG-004 — "gocap" slang parsed as 50.000 instead of 50

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-20 |
| **Spec ID(s)** | `PARSE-21` |
| **Severity** | Low |
| **Found in** | production (https://qicau.kajoga.workers.dev/) |
| **Found during** | L2 parse-quality run, 2026-09-28 |

## Summary
The money-slang term "gocap" is parsed as `harga: 50000` instead of `50`. This is a genuine
deviation from spec (not just a spec-vs-app ambiguity): the spec lists `gocap` and `cepe` (100)
as a distinct, smaller-scale pair of slang terms from the `goceng`/`ceban`/`goban`/etc. family
(5.000/10.000/50.000/...), and this matches real Indonesian colloquial usage — "gocap" and "cepe"
are old small-change slang for 50 and 100 rupiah, separate from the "thousands" family that
happens to share some phonetic roots (e.g. `gocap` vs `goceng`, `cepe` vs `cepego`).

## Steps to reproduce
1. `POST /api/parse-text` with `{"textInput": "Bayar parkir gocap"}`

## Expected (per spec)
`PARSE-21`: `gocap` → `50`.

## Actual
```json
{
  "kategori": "Transport",
  "platform": "",
  "harga": 50000,
  "detail": "parkir",
  "payment_method": "QRIS",
  "confidence": "high",
  "raw_transcript": "Bayar parkir gocap"
}
```
`harga` is `50000`, not `50` — off by a factor of 1000, in the same direction/family as the
`goban` (50.000) term, suggesting the model conflates `gocap` with `goban`/`goceng`-style terms.

## Evidence
Newman run, `api-testing/collections/qicau-parse-quality.postman_collection.json`, row
`{"spec_ids": "PARSE-21", "sentence": "Bayar parkir gocap", "expect_harga": 50}` in
`api-testing/data/parse-quality-sentences.json` — assertion failed:
`expected 50000 to deeply equal 50`. All other fields for this case were reasonable
(`kategori: Transport` is a fair read of "parkir").

**Practical impact is low** — 50 rupiah is a trivial amount unlikely to be logged as a real
expense on its own — but the underlying slang-scale confusion could plausibly recur with other
inputs that mix the two families in a single sentence, which would be worth a follow-up if this
gets prioritized.

**Considered and rejected as "not a bug":** one read is that the model is deliberately
correcting an unrealistic literal amount (50 rupiah has no real purchasing power) up to a
plausible one — that would be reasonable, arguably even desirable, behavior. This doesn't hold
up though: in the same test batch, `PARSE-20` ("cepe" → expected `100`) came back as the literal
`100`, not auto-corrected to `100000`, even though "100 rupiah" for candy is equally unrealistic.
Since the model treats these two same-family, same-scale slang terms inconsistently, this looks
like an unreliable association with the `goceng`/`goban` family rather than an intentional
scale-correction feature — confirmed with the app author (2026-09-28), keeping as an open bug.
