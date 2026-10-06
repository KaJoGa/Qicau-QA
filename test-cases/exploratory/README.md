# Exploratory session charters

Session-based exploratory charters (`test-plan.md` §3.2, §4) — each targets a risk area from the
test plan's risk table that automation genuinely can't reach well: subjective UX judgment,
adversarial/creative input, real devices, real network conditions, or a real Google account.
Execution is manual, on production (`https://qicau.kajoga.workers.dev/`), using the tester's own
Google account, per `test-plan.md` §3.1's environment rule for manual/exploratory work.

Each charter is a **mission + timebox + test ideas**, not a scripted step list — the tester follows
their nose within the mission. The list of test ideas is deliberately longer than the timebox, so
not every idea gets run; whatever was and wasn't covered is recorded in the **Session notes**
section at the bottom of each file (coverage, bugs, open questions).

| Charter | Risk area(s) | Mission | Timebox | Bugs found |
|---|---|---|---|---|
| [CH-01](CH-01-ai-parsing-and-adversarial-input.md) | R1, R2 | Break the AI parser with real slang, ambiguity, and adversarial text | 60 min | `BUG-017`, `BUG-018` (and `BUG-012` earlier) |
| [CH-02](CH-02-voice-capture-real-devices.md) | R4 | Stress voice capture across real mic/device/browser conditions automation can't reach | 45 min | none |
| [CH-03](CH-03-sheets-export-resilience.md) | R5 | Try to break Sheets export idempotency and time-boundary correctness | 45 min | `BUG-013` (and `BUG-009`) |
| [CH-04](CH-04-pwa-offline-real-devices.md) | R8 | Explore PWA/offline resilience under real flaky-network conditions | 45 min | `BUG-014` (and `BUG-010`, `BUG-011` earlier) |
| [CH-05](CH-05-cross-device-and-ui-polish.md) | R6, R7, R12 | Judge cross-device consistency, time-boundary edge cases, and UI polish that automation can't judge | 45 min | `BUG-015`, `BUG-016` |
| [CH-06](CH-06-ringkasan-and-edit-from-riwayat.md) | R6, R7, R8 | Hunt drift between Ringkasan and real transactions; judge edit-from-Riwayat | 45 min | `BUG-020` confirmed on production (`BUG-021`, `022` not reproduced) |

Each spec ID these charters touch already has automated and/or scripted manual coverage
elsewhere (`automation/`, `test-cases/*.csv`) — these sessions are deliberately about what's
*around* the scripted cases: creative variations, real-world conditions, and "does this actually
feel right" judgment calls a script can't make.

## How to use one
1. Pick a charter, set a timer for its timebox.
2. Read the mission and test ideas as a starting point, not a checklist — deviate wherever
   something looks interesting.
3. Log bugs found as new `bug-reports/BUG-0NN-*.md` stubs (same format as the existing ones) and
   file them in Jira per `CLAUDE.md`'s defect-tracking convention.
4. Fill in that charter's own "Session notes" section: what was covered, what wasn't, open
   questions, follow-up charters worth writing.
