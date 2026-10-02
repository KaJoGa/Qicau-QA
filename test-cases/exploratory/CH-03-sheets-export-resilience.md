# CH-03 — Sheets export resilience

| | |
|---|---|
| **Risk area** | R5 (Sheets export correctness & idempotency) |
| **Spec IDs** | `SYNC-01..18` |
| **Environment** | Production, the tester's own Google account only (never a second identity — this is real OAuth against a real Drive/Sheets) |
| **Timebox** | 45 minutes |

## Mission
`test-cases/sheets-sync-run-guide.md` already has the scripted step-by-step L5 run guide — do that
first if it hasn't been run yet this cycle. This charter is the *unscripted* half: deliberately
try to break idempotency, interrupt the multi-step write sequence mid-way, and probe the
time-zone/boundary correctness the spec calls out explicitly.

## Why this needs a human, on production, right now
No emulator exists for Sheets/Drive (`test-plan.md` §6 — "Manual only, once per release"). Every
failure mode here involves real OAuth token lifecycle, real network interruption timing, or real
multi-minute waits (token expiry ~50 min) that aren't practical to script.

## Test ideas
**Idempotency & partial failure**
- Start a sync, then deliberately kill the tab/close the browser mid-write (between creating the
  sheet and finishing all rows) — does re-running sync produce duplicate rows, or does it
  correctly resume/detect the partial state?
- Sync once, then sync again immediately with no new data — zero duplicate rows expected.
- Delete the generated Sheets file from Drive, then sync again — does the app detect the
  missing file and recreate it, or does it think it already synced and skip silently (leaving
  data with no export at all)?
- Reset (per the app's own reset feature) then sync — confirm every previously-exported
  transaction reappears correctly, not just new ones.

**OAuth/popup edge cases**
- Block the OAuth popup in the browser's own popup-blocker setting, then trigger sync — is the
  failure message clear, or a silent no-op?
- On Android: close the OAuth popup/tab before completing consent — does the app recover to a
  clean "not synced" state, or get stuck?
- Let the token sit for ~50+ minutes (the spec-cited expiry window) mid-session, then sync — does
  it silently re-auth, or fail visibly?

**Time-boundary correctness (the spec explicitly flags this as tricky)**
- A transaction saved right around 23:59 local time / just after midnight — which month/day tab
  does it land in, and does that match "this month follows device tz while the sheet's timestamp
  column is WIB" as specified?
- Data spanning a month boundary and a year boundary — correct tab/column assignment in both
  cases?
- Very large amounts (`Rp 999.999.999`) and unusual characters in platform/notes — check column
  fidelity in the actual spreadsheet, not just what the app shows.

## Session notes
Session run 2026-10-03, production, own Google account.

**Covered (observed by running them):**
- **Interrupt mid-sync** — same finding as `BUG-009`: a new, empty spreadsheet is left in Drive. The
  sync is a sequence (create the file, create the tabs, fill the rows), so what the user is left with
  depends on which step the interruption hits.
- **Sync again with no new data** — cannot create duplicates; the "Semua data sudah tersinkronisasi"
  toast appears.
- **Delete the Sheets file, then sync** — the app recreates it (same as `SYNC-18`).
- **Reset, then sync** — every previously exported transaction is sent again.
- **Popup blocked in the browser** — nothing appears and the sync does not happen: no popup, no
  toast, no warning (new bug `BUG-013`).
- **Token left idle until it expired (~50 min)** — no notification; the next Sync click simply shows
  the Google OAuth popup again.

**Added 2026-10-03 (second pass):**
- **Android: close the OAuth popup before finishing consent** — same as the website: no popup and
  no message, and the sync does not happen. That matches `SYNC-03` (closing the popup deliberately
  gives no error toast), so it is not a bug.
- **Time boundaries** — fine. Tested once only, since it is hard to arrange.
- **Special characters** — text starting with a special character (for example `=`) is converted
  by Sheets into a real, working formula, like the hyperlink one. Confirms `BUG-018`.

**Not run:**
- **Access revoked from the Google account** — not tested: revoking it from Google's settings is
  too complicated for this test. A claim that "from the code it is safe" is not recorded as a
  result, because it comes from reading source code rather than from running it.
- Month/year boundary data and the `007` / `1/2`-style reinterpretation were not separately
  checked.

**Bugs found (file as `bug-reports/BUG-0NN-*.md`):** `BUG-013` (silent Sync when the OAuth popup is
blocked); formula conversion confirmed again, tracked in `BUG-018`. The empty-file finding is the
existing `BUG-009`.

**Open questions / follow-up:**
- Mapping note: the idle-token and blocked-popup answers were matched to the charter by their
  content, since the pasted answers did not line up one-to-one.
