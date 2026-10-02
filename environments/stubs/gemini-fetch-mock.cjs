/**
 * Gemini fetch mock — intercepts outbound calls to Google's Generative Language API
 * (generativelanguage.googleapis.com) so Qicau's server can be tested deterministically,
 * without spending real Gemini quota or depending on real model availability.
 *
 * This file does NOT modify Qicau's source. It's a Node preload script that patches the
 * global `fetch` before the app's own code runs, so shared/gemini.ts's plain `fetch()` calls
 * get routed here instead of to Google — see test-plan.md §3.1 (L1) and §6.3.
 *
 * Usage (from the Qicau app repo, adjust the path to wherever this QA repo sits):
 *
 *   NODE_OPTIONS="--require /path/to/QA-Qicau/environments/stubs/gemini-fetch-mock.cjs" \
 *   MOCK_SCENARIO=high npm run dev
 *
 * NODE_OPTIONS is read by the Node binary itself before any dev-script wrapper (tsx,
 * ts-node-dev, nodemon, ...) gets control, and child processes inherit it — so this works
 * regardless of exactly how `npm run dev` starts server.ts under the hood.
 *
 * Scenarios (set via MOCK_SCENARIO env var, default "high"):
 *   high                         — every model call succeeds immediately, high confidence.
 *   medium-zero                  — succeeds with confidence "medium" and harga 0 (the
 *                                   confirmed-but-unhandled edge case — see CLAUDE.md).
 *   low                          — succeeds with confidence "low", harga 0 (should NOT be
 *                                   saved by the app — VOICE-09/MAN-05).
 *   primary-fail-fallback-success — the first MOCK_FAIL_COUNT model attempts return errors,
 *                                   later attempts succeed (tests the fallback chain, API-07).
 *   all-fail                     — every model attempt returns an error (tests API-08).
 *
 * MOCK_FAIL_COUNT (default 1) — how many leading attempts fail, for
 * "primary-fail-fallback-success".
 */

const REAL_FETCH = globalThis.fetch;
const GEMINI_HOST = 'generativelanguage.googleapis.com';

const SCENARIO = process.env.MOCK_SCENARIO || 'high';
const FAIL_COUNT = parseInt(process.env.MOCK_FAIL_COUNT || '1', 10);

let attemptCount = 0;

function structuredResult({ kategori, platform, harga, detail, payment_method, confidence, raw_transcript }) {
  // Shape matches API-04/05 in test/Qicau.md §10: a JSON *string* with these exact fields.
  return JSON.stringify({
    kategori,
    platform,
    harga,
    detail,
    payment_method,
    confidence,
    raw_transcript,
  });
}

function scenarioPayload() {
  switch (SCENARIO) {
    case 'medium-zero':
      return structuredResult({
        kategori: 'Lainnya',
        platform: '',
        harga: 0,
        detail: 'mock: medium confidence, harga 0',
        payment_method: 'QRIS',
        confidence: 'medium',
        raw_transcript: '(mocked input)',
      });
    case 'low':
      return structuredResult({
        kategori: 'Lainnya',
        platform: '',
        harga: 0,
        detail: '',
        payment_method: 'QRIS',
        confidence: 'low',
        raw_transcript: '(mocked input)',
      });
    case 'high':
    case 'primary-fail-fallback-success':
    default:
      return structuredResult({
        kategori: 'Jajan',
        platform: 'Starbucks',
        harga: 5000,
        detail: 'kopi',
        payment_method: 'GoPay',
        confidence: 'high',
        raw_transcript: '(mocked input)',
      });
  }
}

/** Mimics the real Gemini generateContent REST response shape. */
function successResponse() {
  const body = {
    candidates: [
      {
        content: {
          parts: [{ text: scenarioPayload() }],
          role: 'model',
        },
        finishReason: 'STOP',
      },
    ],
  };
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

/** Mimics a Gemini error response (quota/model-unavailable/etc.). */
function errorResponse() {
  const body = {
    error: { code: 429, message: 'Mocked: Resource exhausted (quota)', status: 'RESOURCE_EXHAUSTED' },
  };
  return new Response(JSON.stringify(body), {
    status: 429,
    headers: { 'Content-Type': 'application/json' },
  });
}

globalThis.fetch = async function mockedFetch(input, init) {
  const url = typeof input === 'string' ? input : input?.url || '';

  if (!url.includes(GEMINI_HOST)) {
    return REAL_FETCH(input, init);
  }

  attemptCount += 1;

  if (SCENARIO === 'all-fail') {
    return errorResponse();
  }

  if (SCENARIO === 'primary-fail-fallback-success' && attemptCount <= FAIL_COUNT) {
    return errorResponse();
  }

  return successResponse();
};

console.log(
  `[gemini-fetch-mock] active — scenario="${SCENARIO}"` +
    (SCENARIO === 'primary-fail-fallback-success' ? ` failCount=${FAIL_COUNT}` : '')
);
