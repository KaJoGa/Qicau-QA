// L3 — Firestore security rules, tested blind from test/Qicau.md §10 (SEC-01..12, AUTH-07)
// against the REAL firestore.rules already loaded by the running emulator (npm run emulators
// in the app repo). This test client never reads the rules file itself — it only connects to
// whatever ruleset the emulator already has loaded, and observes allow/deny behavior.
import { initializeTestEnvironment, assertSucceeds, assertFails } from "@firebase/rules-unit-testing";
import test from "node:test";
import assert from "node:assert";

const ALICE = "alice-uid";
const BOB = "bob-uid";

let testEnv;

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-qicau-test",
    firestore: { host: "127.0.0.1", port: 8080 },
  });
});

test.after(async () => {
  await testEnv.cleanup();
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

function sampleTransaction(overrides = {}) {
  return {
    user_id: ALICE,
    kategori: "Makan",
    platform: "Lainnya",
    harga: 15000,
    detail: "test transaction",
    payment_method: "Cash",
    confidence: "high",
    raw_transcript: "test raw transcript",
    created_at: new Date().toISOString(),
    is_exported: false,
    ...overrides,
  };
}

function sampleLowConfidenceLog(overrides = {}) {
  return {
    user_id: ALICE,
    attempted_at: new Date().toISOString(),
    source: "text",
    input_text: "test input",
    raw_transcript: "test raw transcript",
    gemini_output: "{}",
    ...overrides,
  };
}

function sampleUserProfile(overrides = {}) {
  return {
    displayName: "Test User",
    email: "test@example.com",
    last_login: new Date().toISOString(),
    photoURL: "https://example.com/photo.jpg",
    ...overrides,
  };
}

async function seed(pathFn) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await pathFn(ctx.firestore());
  });
}

// SEC-01: unauthenticated cannot read or write anything
test("SEC-01 — unauthenticated write to transactions is denied", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("transactions").doc("x").set(sampleTransaction()));
});

test("SEC-01 — unauthenticated read of transactions is denied", async () => {
  await seed((db) => db.collection("transactions").doc("tx-1").set(sampleTransaction()));
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("transactions").doc("tx-1").get());
});

// SEC-02: create transaction with own user_id -> allowed
test("SEC-02 — authenticated user can create a transaction with their own user_id", async () => {
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("transactions").add(sampleTransaction({ user_id: ALICE })));
});

// SEC-03: create transaction with someone else's user_id -> denied
test("SEC-03 — cannot create a transaction with another user's user_id", async () => {
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("transactions").add(sampleTransaction({ user_id: BOB })));
});

// SEC-04: read/update/delete own transaction -> allowed
test("SEC-04 — owner can read their own transaction", async () => {
  await seed((db) => db.collection("transactions").doc("tx-alice").set(sampleTransaction({ user_id: ALICE })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("transactions").doc("tx-alice").get());
});

test("SEC-04 — owner can update their own transaction", async () => {
  await seed((db) => db.collection("transactions").doc("tx-alice").set(sampleTransaction({ user_id: ALICE })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("transactions").doc("tx-alice").update({ harga: 25000 }));
});

test("SEC-04 — owner can delete their own transaction", async () => {
  await seed((db) => db.collection("transactions").doc("tx-alice").set(sampleTransaction({ user_id: ALICE })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("transactions").doc("tx-alice").delete());
});

// SEC-05 / AUTH-07: read/update/delete someone else's transaction -> denied
test("SEC-05 / AUTH-07 — user cannot read another user's transaction", async () => {
  await seed((db) => db.collection("transactions").doc("tx-bob").set(sampleTransaction({ user_id: BOB })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("transactions").doc("tx-bob").get());
});

test("SEC-05 / AUTH-07 — user cannot update another user's transaction", async () => {
  await seed((db) => db.collection("transactions").doc("tx-bob").set(sampleTransaction({ user_id: BOB })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("transactions").doc("tx-bob").update({ harga: 1 }));
});

test("SEC-05 / AUTH-07 — user cannot delete another user's transaction", async () => {
  await seed((db) => db.collection("transactions").doc("tx-bob").set(sampleTransaction({ user_id: BOB })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("transactions").doc("tx-bob").delete());
});

// SEC-06: change a transaction's user_id to someone else -> denied
test("SEC-06 — owner cannot reassign their transaction's user_id to another user", async () => {
  await seed((db) => db.collection("transactions").doc("tx-alice").set(sampleTransaction({ user_id: ALICE })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("transactions").doc("tx-alice").update({ user_id: BOB }));
});

// SEC-07: low_confidence_logs create/read/delete own -> allowed
test("SEC-07 — user can create their own low-confidence log", async () => {
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("low_confidence_logs").add(sampleLowConfidenceLog({ user_id: ALICE })));
});

test("SEC-07 — user can read their own low-confidence log", async () => {
  await seed((db) => db.collection("low_confidence_logs").doc("log-alice").set(sampleLowConfidenceLog({ user_id: ALICE })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("low_confidence_logs").doc("log-alice").get());
});

test("SEC-07 — user can delete their own low-confidence log", async () => {
  await seed((db) => db.collection("low_confidence_logs").doc("log-alice").set(sampleLowConfidenceLog({ user_id: ALICE })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("low_confidence_logs").doc("log-alice").delete());
});

// SEC-08: low_confidence_logs update (even own) -> denied, append-only
test("SEC-08 — user cannot update their own low-confidence log (append-only)", async () => {
  await seed((db) => db.collection("low_confidence_logs").doc("log-alice").set(sampleLowConfidenceLog({ user_id: ALICE })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("low_confidence_logs").doc("log-alice").update({ source: "voice" }));
});

// SEC-09: low_confidence_logs belonging to someone else -> denied (create on their behalf, read, delete)
test("SEC-09 — user cannot create a low-confidence log on another user's behalf", async () => {
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("low_confidence_logs").add(sampleLowConfidenceLog({ user_id: BOB })));
});

test("SEC-09 — user cannot read another user's low-confidence log", async () => {
  await seed((db) => db.collection("low_confidence_logs").doc("log-bob").set(sampleLowConfidenceLog({ user_id: BOB })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("low_confidence_logs").doc("log-bob").get());
});

test("SEC-09 — user cannot delete another user's low-confidence log", async () => {
  await seed((db) => db.collection("low_confidence_logs").doc("log-bob").set(sampleLowConfidenceLog({ user_id: BOB })));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("low_confidence_logs").doc("log-bob").delete());
});

// SEC-10 / SEC-11: users/{uid} profile - only the owner uid may read/write; covers both first-login
// (create) and subsequent-login (update) since both are just "write to own profile".
test("SEC-10/11 — user can create (first login) their own profile", async () => {
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("users").doc(ALICE).set(sampleUserProfile()));
});

test("SEC-10/11 — user can update (subsequent login) their own profile", async () => {
  await seed((db) => db.collection("users").doc(ALICE).set(sampleUserProfile()));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("users").doc(ALICE).update({ last_login: new Date().toISOString() }));
});

test("SEC-10 — user can read their own profile", async () => {
  await seed((db) => db.collection("users").doc(ALICE).set(sampleUserProfile()));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertSucceeds(db.collection("users").doc(ALICE).get());
});

test("SEC-10 / AUTH-07 — user cannot read another user's profile", async () => {
  await seed((db) => db.collection("users").doc(BOB).set(sampleUserProfile()));
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("users").doc(BOB).get());
});

test("SEC-10 / AUTH-07 — user cannot write another user's profile", async () => {
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("users").doc(BOB).set(sampleUserProfile()));
});

// SEC-12: any collection not defined in the rules -> denied entirely
test("SEC-12 — an undefined collection is denied even for an authenticated user", async () => {
  const db = testEnv.authenticatedContext(ALICE).firestore();
  await assertFails(db.collection("some_undefined_collection").doc("x").set({ foo: "bar" }));
});
