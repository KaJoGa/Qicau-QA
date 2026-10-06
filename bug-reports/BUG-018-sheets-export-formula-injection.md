# BUG-018 — Text starting with "=" becomes a live formula in the Sheets export

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/boards/2/backlog?selectedIssue=QAP-34 |
| **Spec ID(s)** | `SYNC-06` (data rows should be one plain row per transaction; formula-injection risk flagged in `test-plan.md` R2/R5) |
| **Severity** | Medium (proposed — classic spreadsheet formula injection; limited because users export their own data, but formulas such as `=HYPERLINK` or `=IMPORTDATA` should never be created from free text) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, own Google account |
| **Found during** | Exploratory session CH-01, 2026-10-03 |
| **Status** | **Fixed** - `SYNC-26` passed on production (2026-10-05): text starting with = + - @ is plain text in the sheet. |

## Summary
When a transaction's platform or note contains text starting with `=`, the Sheets export writes it as
a live formula instead of plain text.

## Steps to reproduce
1. Add a transaction whose platform or note is `=HYPERLINK("http://evil.example")`
   (via the AI input or the manual form).
2. Open Riwayat and click "Sync ke Sheets".
3. Open the generated `Qicau_Export_<year>` spreadsheet and look at that row.

## Expected
Free text from the user or the AI is written as plain text. A value that starts with `=`, `+`, `-`
or `@` must not be evaluated as a formula.

## Actual
The cell in Google Sheets is a working hyperlink (the formula was evaluated), not the literal text.

## Evidence
Observed manually on production during CH-01, 2026-10-03: "Hyperlink" — the formula text became a
clickable hyperlink in the sheet. Not independently tested yet: other prefixes (`+`, `-`, `@`) and
values that Sheets reinterprets (`1/2`, `007`); worth checking together when running CH-03's
special-character test.
