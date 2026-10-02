# security-rules/

**L3 — Firestore security rules**, the one deliberate non-Java layer (see `test-plan.md` §6.3):
`@firebase/rules-unit-testing` (Node/JS) is the canonical tool for this, since rules are only
enforced for client-SDK/REST traffic — the Java Admin SDK bypasses them entirely, so a Java
approach can't actually test them.

Written **blind from `test/Qicau.md` §10** (`SEC-01`..`SEC-12`, cross-linked to `AUTH-07`) — this
test file never reads `firestore.rules` itself. It just connects to whatever ruleset the running
emulator already has loaded (from the app repo's own `firebase.json` config), the same way a real
client app would, and observes allow/deny behavior.

Collection and field names (not given in `Qicau.md`'s conceptual description of §10) were
provided directly by the app author rather than read from source:
- `transactions`: `confidence, created_at, detail, harga, is_exported, kategori, payment_method, platform, raw_transcript, user_id`
- `users`: `displayName, email, last_login, photoURL`
- `low_confidence_logs`: `user_id, attempted_at, source, input_text, raw_transcript, gemini_output`
  (this collection has never actually been triggered in production — the app author confirmed it's fine
  to seed dummy documents directly via the emulator for rules validation, since that only
  exercises the rules themselves, not production behavior)

## How to run
1. Start the emulator from the **app repo** (not this one): `npm run emulators` — starts
   Firestore on `127.0.0.1:8080`, Auth on `127.0.0.1:9099`, project `demo-qicau-test`.
2. From this folder: `npm install` (first time only), then `npm test`.

## Result (2026-09-28)
**24/24 tests pass** on the first run — every `SEC-*`/`AUTH-07` case matched the spec exactly.
See `test-cases/traceability.csv` for the per-spec-ID breakdown. **L3 is complete.**

## Coverage
| Spec ID(s) | Covered by |
|---|---|
| SEC-01 | Unauthenticated write + read denied |
| SEC-02 | Create own transaction — allowed |
| SEC-03 | Create transaction with another user's `user_id` — denied |
| SEC-04 | Read/update/delete own transaction — allowed |
| SEC-05, AUTH-07 | Read/update/delete another user's transaction — denied |
| SEC-06 | Reassign `user_id` on own transaction — denied |
| SEC-07 | Create/read/delete own low-confidence log — allowed |
| SEC-08 | Update own low-confidence log — denied (append-only) |
| SEC-09 | Create-on-behalf-of/read/delete another user's low-confidence log — denied |
| SEC-10, SEC-11, AUTH-07 | Own profile create/update/read allowed; another user's profile read/write denied |
| SEC-12 | Undefined collection — denied |
