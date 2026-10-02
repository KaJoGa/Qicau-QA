# CH-05 — Cross-device consistency, time boundaries & UI polish

| | |
|---|---|
| **Risk areas** | R6 (time-boundary calculations), R7 (pagination × filters × realtime), R12 (cross-device consistency & UI) |
| **Spec IDs** | `HOME-02/07`, `HIST-01..13`, `MON-02/03/08`, `NAV-05..08` |
| **Environment** | Production, the tester's own account, two devices/sessions open at once where noted |
| **Timebox** | 45 minutes |

## Mission
Judge the things a script can assert but can't *evaluate* — does a layout genuinely look right
at an odd window size, does a realtime update feel instant or laggy, does a long platform name
actually wrap sensibly instead of just "not crashing." Also close two real gaps automation
explicitly couldn't reach: `HIST-03`/`HIST-05`/`MON-08` all need transactions on genuinely
different real days, which only a live account with real elapsed time can provide (the direct
form in `automation/` can only ever save "now").

## Test ideas
**Time boundaries — the ones automation structurally couldn't backdate**
- If this account already has data from a previous day (or wait until after midnight during a
  longer session), check `HIST-03` (transactions grouped by day with correct labels) and
  `HIST-05` (time filter excluding older data) directly against real multi-day data.
- Similarly for `MON-08` (previous month's transactions correctly excluded from the current
  month's total) — only checkable once real data exists in two different months.
- Save something right at 23:59 and just after 00:00 — correct "today" bucket on each side?
- Check `HOME-02`/`MON-02` totals right around a Monday-start week boundary.

**Realtime & pagination judgment calls**
- Two devices/tabs open side by side (same account) — how *fast* does a change on one appear on
  the other (`HOME-07`, `HIST-13`)? Automation confirms it happens within a wait window; this
  session is for judging whether the actual latency feels acceptable.
- With 60+ transactions, page through Riwayat while filtering by category — does the "n / total"
  indicator ever look wrong or lag behind a filter change?

**UI polish across real screen sizes**
- A very long platform name and a very long note — does either overflow, truncate awkwardly, or
  break the row layout on a small Android screen?
- Maximum price (`Rp 999.999.999`) — does it stay readable at every card/row size, or does it get
  clipped anywhere?
- Rotate the Android device to landscape mid-use (modal open, recording in progress, mid-scroll
  on Riwayat) — anything visually broken?
- Resize the desktop browser window slowly from narrow to very wide and watch the transition
  through the mobile → desktop layout breakpoint — anything that pops/flickers oddly (this is the
  same breakpoint `automation/`'s `DriverFactory` deliberately stays above — a real narrow-window
  pass through it hasn't been watched by a human this cycle)?

**Theme**
- Switch the OS-level dark/light preference while "Sistem" is selected (`NAV-07` — automation
  confirmed this via a forced CDP media-feature override; a real OS-level toggle is the more
  faithful version) and watch whether the transition itself looks intentional, not jarring.

## Session notes
Session run 2026-10-03, production, own account, two devices.

**Covered:**
- **Time boundaries** — all four checks passed with real data: `HIST-03` / `HIST-05` against
  genuinely multi-day data, `MON-08` across months, the 23:59 / after-midnight save, and the Monday
  week boundary. (The traceability matrix is updated for `HIST-03`, `HIST-05`, `MON-08`.)
- **Realtime** — a submit on one device shows up on the other device almost instantly (`HOME-07`,
  `HIST-13`).
- **Pagination indicator** — wrong, see `BUG-015`: it reads 1/2, 2/3, 3/4 ... 5/5 instead of a fixed
  total.
- **Long platform name / note** — readable; a very long platform name just takes more space.
- **Max price (`Rp 999.999.999`)** — readable, no clipping.
- **Rotation / landscape** — nothing breaks. The installed app does not rotate (portrait, matching
  the manifest in `PWA-09`); the browser version does, which is the browser's behaviour.
- **Desktop window resize** — no breakage through the mobile/desktop breakpoint.
- **Theme (`NAV-07`)** — fine (already known from earlier use).
- **Extreme input ("testing jahil")** — pasting a huge text into the direct form's Platform field
  made the browser lag, and saving gave a raw Firebase error; nothing was saved (`BUG-016`).

**Bugs found (file as `bug-reports/BUG-0NN-*.md`):** `BUG-015` (page indicator total), `BUG-016`
(no length limit on Platform in the direct form).

**Open questions / follow-up:** none.
