# BUG-019 — iOS install guide describes Safari's button even when opened in Chrome for iOS

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog?selectedIssue=QAP-35 |
| **Spec ID(s)** | `PWA-08` (related; the spec does not say the guide must match the browser) |
| **Severity** | Low (proposed — the guide still appears and the install works, but the wording can confuse users) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, Chrome on iPhone (iOS) |
| **Found during** | Manual iOS check of `PWA-08`, 2026-10-03 |

## Summary
On an iPhone, in Chrome (not Safari), the install button shows the manual guide as the spec asks,
but the text tells the user to tap Safari's navigation button. In Chrome for iOS the Share button is
somewhere else (next to the address bar), so the instruction does not match what the user sees.

## Steps to reproduce
1. On an iPhone, open https://qicau.kajoga.workers.dev/ in Chrome (a normal tab, app not installed).
2. Tap the install button ("Pasang App").
3. Read the steps in the guide.

## Expected
`PWA-08`: a manual guide such as Share → Add to Home Screen is shown. That part works. Ideally the
wording also fits the browser in use, or stays generic enough to be correct in both Safari and Chrome
for iOS.

## Actual
The guide appears, but it refers to Safari's navigation button, although the user is in Chrome.

## Evidence
Observed manually on a friend's iPhone, 2026-10-03, Chrome (not incognito). The exact wording and a
screenshot should be attached to the Jira issue. Not reported: whether the guide's layout was fine
(compare `BUG-011`, the overflow seen on Android and desktop).
