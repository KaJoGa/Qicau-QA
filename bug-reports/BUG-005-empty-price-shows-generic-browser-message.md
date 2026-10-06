# BUG-005 — Empty-price validation shows the browser's generic message, not the app's specified text

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-21 |
| **Spec ID(s)** | `MAN-13` |
| **Severity** | Low |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | L4 UI automation run, 2026-09-29 |
| **Status** | **Fixed** (retest 2026-10-06): no native browser message; the app shows the inline error "Jumlah pengeluaran wajib diisi." (spec MAN-13 updated to match). Verified on local dev and a real phone; the tester also checked production. |

## Summary
Trying to save a transaction via the "Formulir Langsung" (direct form) with the price field left
empty does correctly block the save — but the message shown to the user is the browser's own
generic native validation text ("Please fill out this field."), not the Indonesian message the
spec calls for.

## Steps to reproduce
1. Sign in, open "Input Manual", switch to "Formulir Langsung".
2. Leave the "Harga (Rp)" field empty.
3. Click "Simpan Transaksi".

## Expected (per spec)
`MAN-13`: rejected with alert **"Harap masukkan jumlah pengeluaran."**

## Actual
The save is correctly blocked (no transaction created, form stays open) — but no app-level
message appears at all. The price `<input>` just has a plain `required` attribute with no custom
validity message set, so the browser's own default constraint-validation bubble appears instead,
reading **"Please fill out this field."** — confirmed by reading the input's own
`validationMessage` DOM property via a script run in the browser (`element.validationMessage`),
not by reading the app's source.

## Evidence
`automation/src/test/java/com/qicau/qa/steps/ManualInputSteps.java`
(`alertHarapMasukkanJumlah`), scenario `MAN-13` (`features/manual-input.feature`) — assertion
failed: `expected an alert/toast/native-validation message containing: Harap masukkan jumlah
pengeluaran - got: Please fill out this field.`

**Practical impact is low but real**: the save is still correctly rejected, so no bad data gets
in — but a user typing in Indonesian, on an Indonesian-only UI (`test/Qicau.md`'s own framing),
would see a stray English message here, which reads as unpolished/inconsistent with the rest of
the app's error handling (every other validation message in the spec is Indonesian).

**Likely fix**: set the input's custom validity message (e.g. via `setCustomValidity(...)` on
`invalid`/`input`, or handle the empty case in the submit handler directly instead of relying on
the bare `required` attribute) so a consistent Indonesian message shows in every browser, not
just whatever locale the visitor's browser happens to be in.
