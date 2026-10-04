# BUG-010 — "Mode Offline" banner overlaps the Riwayat action buttons instead of pushing them down

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-26 |
| **Spec ID(s)** | `PWA-01` |
| **Severity** | Low (proposed — cosmetic/layout, no data or functional loss, but genuinely obscures controls) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, desktop and narrow/mobile widths |
| **Found during** | Manual exploration, 2026-09-30 |
| **Status** | **Fixed** (local dev) - PWA-01 retest passed at L4 (bounding boxes at 800/1280/390 px, local dev), 2026-10-04. Production retest pending (see `test-cases/sprint3-manual-run-guide.md`). |

## Summary
When the connection drops, the "Mode Offline: Data tersimpan lokal & siap sync." banner
(`PWA-01`) renders as an overlay on top of Riwayat's action-button row ("Rebuild Ringkasan",
"Reset Ekspor", "Sync ke Sheets") instead of pushing that row down. At desktop width the banner
sits directly across the buttons, and at narrow/mobile width it covers "Sync ke Sheets" almost
entirely while pushing part of the page title behind it too.

## Steps to reproduce
1. Open Riwayat with at least one transaction.
2. Go offline (devtools Network → Offline, or airplane mode).
3. Observe the banner's position relative to the "Rebuild Ringkasan" / "Reset Ekspor" /
   "Sync ke Sheets" button row, at both desktop and narrow/mobile viewport widths.

## Expected (per spec)
`PWA-01` only specifies the banner's text and that it "muncul di atas" (appears at/on top) — it
doesn't explicitly forbid overlapping content, but a banner overlapping and obscuring clickable
controls is not a reasonable reading of "muncul di atas" as a layout instruction, and isn't
consistent with how other transient banners in the app behave (e.g. the online-recovery banner
per `PWA-02`, which is not reported as overlapping anything).

## Actual
The banner is positioned as an overlay (likely `fixed`/`absolute`) rather than reserving its own
space in the document flow, so it renders on top of the Sync/Reset/Rebuild buttons rather than
above them. Confirmed at both a desktop-width viewport and a narrow/mobile-width viewport
(screenshots below).

## Evidence
Screenshots I captured during manual exploration, 2026-09-30:
- Desktop width: banner spans across and behind the "Reset Ekspor" text, all three buttons
  partly obscured.
- Narrow/mobile width: banner covers "Sync ke Sheets" almost completely and overlaps the
  "Riwayat Transaksi" heading.

## Note
The three buttons are functionally inert while offline anyway (`PWA-04` — Sync/Reset need
internet, and Rebuild Ringkasan is presumably local-recompute-only but untested here), so this
isn't blocking any working feature. It matters because: (1) it's the kind of visible layout glitch
that reads as unpolished, and (2) the buttons become relevant again the moment connection returns,
which is also roughly when the offline banner is still visible/transitioning out — worth a look
at whether the banner reserves layout space (push content down) instead of overlaying it.
