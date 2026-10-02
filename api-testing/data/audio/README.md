# api-testing/data/audio/

Real `.webm` recordings (the tester's own voice, recorded from Chrome's mic) used to properly
test `API-06` (missing `mimeType` → treated as webm). An earlier pass used placeholder garbage
bytes, which only proved Gemini rejects invalid content — it couldn't actually confirm the
mimeType-defaulting behavior. These fixtures can.

**The recordings are not committed** (they are a voice recording of a real person, so they are
git-ignored). To rerun `API-06`, record the two sentences below yourself in Chrome as `.webm`,
save them as `Test1.webm` / `Test2.webm` here, and base64-encode both into `webm-fixtures.json`
(same structure as the Newman data file the collection expects).

| File | Content (for reference) | Used for |
|---|---|---|
| `Test1.webm` | "Nasi padang Pak Asep, nasi doang 6.000 cash" | `API-06` |
| `Test2.webm` | "Mie ayam Pak Haji 3 porsi goban" | `API-06`; also incidentally exercises `PARSE-25` (goban = 50.000) |
| `webm-fixtures.json` | Both files base64-encoded, as Newman iteration data | Run with `-d api-testing/data/audio/webm-fixtures.json` against the "Contract — parse-audio — API-06" folder |

Both are also reusable later for `VOICE-*`/`MAN-*` UI automation (L4) if a similar real-audio
fixture is needed there — see `automation/README.md`.
