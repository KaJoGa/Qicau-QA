// k6 stress test — brief ramp against local dev's /api/parse-text, Gemini stubbed.
// See test-plan.md §3.6, performance/README.md. Never point this at production or real Gemini.
//
// Usage (from this repo, after starting local dev with the fetch mock — see
// environments/stubs/README.md — on an isolated PORT so the dev server already running on 3000
// is untouched):
//
//   k6 run -e BASE_URL=http://localhost:3001 performance/parse-ramp.js
//
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '60s', target: 20 },
        { duration: '15s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3001';

const SENTENCES = [
  'Beli kopi di Starbucks 25 ribu pakai GoPay',
  'Bayar parkir gocap',
  'Makan siang di Solaria 45rb cash',
  'Top up OVO 100000',
  'Beli baju online 150rb transfer bank',
];

export default function () {
  const sentence = SENTENCES[Math.floor(Math.random() * SENTENCES.length)];
  const res = http.post(
    `${BASE_URL}/api/parse-text`,
    JSON.stringify({ textInput: sentence }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
    'body has result field': (r) => {
      try {
        return JSON.parse(r.body).result !== undefined;
      } catch {
        return false;
      }
    },
  });

  sleep(1);
}
