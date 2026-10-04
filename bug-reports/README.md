# bug-reports/

Real defect tracking happens in **Jira** (project `QAP`). This folder holds a thin markdown
**stub** per bug so the repo stays self-contained for anyone without Jira access, without
duplicating Jira's own state (status, comments, assignment) — see `test-plan.md` §3.7.

## Files
- `INDEX.md` — the list of every bug, with severity, Jira link and status.
- `BUG-###-short-slug.md` — one stub per bug (`BUG-001` to `BUG-024` so far; `BUG-023` was withdrawn and is not in Jira).
- `TEMPLATE.md` — copy this for each new bug.

Each stub has a spec ID, a proposed severity, reproduction steps, expected vs. actual, evidence,
and the link to its Jira issue.

## Linking a stub to Jira
Stubs for bugs that are not filed yet say "not filed yet". After filing the issue in Jira, paste its link (`https://kalev.atlassian.net/browse/QAP-NN`) into the stub's **Jira issue** row and into the `INDEX.md` row, then change the status there. For an old bug that was fixed and retested, do not create a new Jira issue: comment on the existing one (reopen it only if the retest fails). Still not filed (2026-10-04): `BUG-020`, `BUG-021` (confirm by hand first), `BUG-022` (already fixed, comment only), `BUG-024`.
