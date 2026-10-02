# bug-reports/

Real defect tracking happens in **Jira** (project `QAP`). This folder holds a thin markdown
**stub** per bug so the repo stays self-contained for anyone without Jira access, without
duplicating Jira's own state (status, comments, assignment) — see `test-plan.md` §3.7.

## Files
- `INDEX.md` — the list of every bug, with severity, Jira link and status.
- `BUG-###-short-slug.md` — one stub per bug (`BUG-001` to `BUG-018` so far).
- `TEMPLATE.md` — copy this for each new bug.

Each stub has a spec ID, a proposed severity, reproduction steps, expected vs. actual, evidence,
and the link to its Jira issue.
