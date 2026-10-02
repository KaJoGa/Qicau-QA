# environments/

Notes and non-secret helper files for the test environments described in `test-plan.md` §6.
This folder does **not** hold a copy of `firebase.json`/`.firebaserc`/`firestore.rules` —
those live in the Qicau app repo and are referenced live so they can never go stale (see
`test-plan.md` §8 closing note).

## Folders

| Folder | Contents |
|---|---|
| `emulator/` | Notes on how this repo's tests expect the Firebase Local Emulator Suite to be running (ports, project id `demo-qicau-test`, how to point automation/Postman at it) — a usage note, not a copy of the app's config. |
| `stubs/` | The `fetch`-level mock for `shared/gemini.ts`, shared between L1 (Postman/mock server) and L4 (Selenium) so both layers fake the AI the same way. Not built yet — needed starting Phase 1. |
