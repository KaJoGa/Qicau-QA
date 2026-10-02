// k6 edge case — forces the model fallback chain (API-07) to run repeatedly under load,
// against local dev with the Gemini fetch mock in "primary-fail-fallback-success" scenario.
// See test-plan.md §3.6, performance/README.md. Local dev + stub only, never production.
//
// Start local dev with:
//   $env:NODE_OPTIONS = "--require <path>/environments/stubs/gemini-fetch-mock.cjs"
//   $env:MOCK_SCENARIO = "primary-fail-fallback-success"
//   $env:MOCK_FAIL_COUNT = "1"
//   $env:PORT = "3001"
//   npm run dev
//
// Then: k6 run -e BASE_URL=http://localhost:3001 performance/parse-edge-case.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    fallback_burst: {
      executor: 'constant-vus',
      vus: 15,
      duration: '30s',
    },
  },
  thresholds: {
    // Every request forces at least one failed model attempt before the fallback succeeds,
    // so latency is expected to be higher than the plain ramp test — a looser threshold.
    http_req_duration: ['p(95)<6000'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3001';

export default function () {
  const res = http.post(
    `${BASE_URL}/api/parse-text`,
    JSON.stringify({ textInput: 'Beli kopi di Starbucks 25 ribu pakai GoPay' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    // Even with every attempt forced through at least one failure, the fallback chain
    // should still resolve to a 200 under concurrent load (API-07 semantics under stress).
    'status is 200 despite forced primary failures': (r) => r.status === 200,
  });

  sleep(0.5);
}
