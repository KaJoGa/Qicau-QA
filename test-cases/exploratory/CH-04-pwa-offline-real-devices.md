# CH-04 — PWA & offline resilience on real devices

| | |
|---|---|
| **Risk area** | R8 (offline / PWA / service worker) |
| **Spec IDs** | `PWA-01..11`, `MAN-10`, `PWA-03` |
| **Environment** | Production, the tester's own account, Android Chrome (installed as a PWA) + desktop Chromium |
| **Timebox** | 45 minutes |

## Mission
`automation/`'s `NetworkSimulator` forces "offline" via a clean CDP network-conditions toggle —
a real network never behaves that cleanly. Explore what happens under a genuinely flaky
connection, across an actual app close/reopen cycle, and through a real install flow, none of
which the CDP toggle can represent.

## Test ideas
**Real flaky network, not a clean toggle**
- Walk into an elevator / a known dead spot mid-session instead of toggling airplane mode — does
  a slow, degrading connection behave differently than an instant on/off?
- Save a transaction offline, then close the app entirely (not just the tab — swipe it away on
  Android) before reconnecting, then reopen later — does the offline entry survive an app
  restart, not just a network toggle?
- Go offline, save 2-3 transactions, flap the connection on and off a few times before settling
  online — any duplicate syncs or lost entries?

**Install flow (real, not simulated)**
- Actually install the PWA on Android ("Add to Home Screen") and desktop (browser's install
  button) — does the install-success toast (`PWA-06`) actually appear, and does the header
  install button correctly disappear afterward?
- Launch the installed app via each of the 3 manifest shortcuts (long-press the icon on Android)
  — do they land on the right tab (`PWA-10`, already automation-confirmed via direct URL
  navigation, but the *real* OS-level shortcut launch path is untested)?
- Open the installed app (standalone mode) vs. the same URL in a regular browser tab — does the
  install button correctly stay hidden in standalone mode (`PWA-07`)?

**Update/versioning**
- If a new version is deployed during the session, revisit the app and see whether the update
  prompt (`PWA-05`) actually appears and whether accepting it cleanly loads the new version
  without losing any offline-pending data.
- Check whether a stale service-worker cache ever causes an old version to load after a
  deployment — refresh a few times across the session and watch for it.

**Manifest/shortcut fidelity on the real OS**
- On Android, check the installed icon's actual appearance (maskable icon rendering on
  different launcher shapes) — something `PWA-09`'s HTTP-fetch-the-manifest check can confirm the
  *data* for but not how it actually renders.

## Session notes
Session run 2026-10-03, production, own account, Android + desktop Chromium (Windows).

**Covered:**
- **Slow/degraded network** (browser throttling, not an on/off toggle) — just slow loading and
  slow uploading; nothing broke.
- **Offline save, then full restart** — saved a transaction offline and restarted the phone; the
  data was still there, and after going online it uploaded to Firebase and was ready to sync.
- **Connection flapping** — no duplicate entries.
- **Install flow** — no success toast after installing (only the browser's own notification), see
  `BUG-014`. The in-app install button does disappear, but it appears again after a refresh/reopen.
- **Manifest shortcuts (`PWA-10`)** — all 3 work as intended from the real installed app.
- **Standalone mode (`PWA-07`)** — the install button is hidden in the installed app, but still
  shown in the plain browser version.
- **Update prompt (`PWA-05`)** — appears, and accepting it loses no offline-pending data.
- **Stale service-worker cache after a deploy** — did not occur.
- **Icon fidelity** — fine in the installed app, but looks broken on the website-shortcut version
  (browser "create shortcut").

**Bugs found (file as `bug-reports/BUG-0NN-*.md`):** `BUG-014` (no install-success toast).
Found earlier in this area during manual exploration: `BUG-010` (the "Mode Offline" banner overlaps
the Riwayat action buttons, `PWA-01`) and `BUG-011` (the manual install guide opened from the header
button overflows the screen, `PWA-08`).

**Open questions / follow-up:**
- Resolved: the install button reappearing after refresh/reopen happens in a normal browser tab
  only; the installed app is fine. Not a bug (`PWA-07` only concerns the installed app). A
  browser-made shortcut just reopens the same browser, so it isn't the installed app either.
- Resolved: the slightly broken icon is only on the browser's "create shortcut" version, not on the
  installed app. That kind of shortcut is made by the browser from the page's own icon rather than
  the installed-app manifest icons, so it is not recorded as an app bug. Worth a quick look only if
  the page's favicon looks low-quality.
