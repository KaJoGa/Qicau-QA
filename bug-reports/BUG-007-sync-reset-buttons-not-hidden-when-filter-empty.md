# BUG-007 — Sync/Reset buttons stay visible even when the active filter matches nothing

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-23 |
| **Spec ID(s)** | `HIST-02` |
| **Severity** | Low |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | L4 UI automation run, 2026-09-29 |

## Summary
When a Riwayat filter narrows the list down to zero matching transactions, the empty-state
message correctly appears, but the "Sync ke Sheets" / "Reset Ekspor" buttons remain visible
instead of being hidden.

## Steps to reproduce
1. Sign in with a fresh account, save one transaction (default category "Makan").
2. Open Riwayat.
3. Open the category filter and select a category with no transactions (e.g. "Belanja").

## Expected (per spec)
`HIST-02`: "Empty state for the active filter hides Sync/Reset" — once the empty-state message
("Belum ada riwayat transaksi.") shows for the active filter, the Sync/Reset buttons should be
hidden along with it.

## Actual
The empty-state message shows correctly, but "Sync ke Sheets" and "Reset Ekspor" both remain
rendered and clickable.

## Evidence
`automation/src/test/java/com/qicau/qa/steps/HistorySteps.java` (`syncResetButtonsHiddenWhileListEmpty`),
scenario `HIST-02` (`features/history.feature`) — assertion failed:
```
Expected Sync/Reset to be hidden once the active filter matches no transactions ==> expected: <true> but was: <false>
```
