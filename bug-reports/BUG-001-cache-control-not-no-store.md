# BUG-001 — HTML response on production is not served with `Cache-Control: no-store`

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-17 |
| **Spec ID(s)** | `API-10`, `PWA-11` |
| **Severity** | Medium (proposed — feature impaired, workaround exists: hard refresh; but it undermines the documented reason for `no-store`, which is making new releases take effect immediately) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, 2026-09-27 |
| **Found during** | L1 API contract (Postman/Newman), automated |
| **Status** | **Fixed** - Fixed on production (retest 2026-10-04): HTML no-store on all routes, /assets immutable. Local Vite dev still no-cache - expected, not a defect per the updated PWA-11. |

## Summary
The SPA fallback route returns `Cache-Control: public, max-age=0, must-revalidate` instead of
the spec-required `no-store`.

## Steps to reproduce
```
GET https://qicau.kajoga.workers.dev/this-route-does-not-exist
```
(any non-API, non-existent path works the same way)

## Expected (per spec)
- `API-10`: "Route non-API di produksi → Menyajikan halaman app (SPA) dengan
  `Cache-Control: no-store` untuk HTML."
- `PWA-11`: "Halaman HTML tidak di-cache oleh server (`no-store`) agar rilis baru langsung
  terpakai."

## Actual
`Cache-Control: public, max-age=0, must-revalidate` — HTML *can* be cached by intermediate
caches/browsers (subject to revalidation), which is not what `no-store` guarantees. This may
mean a newly deployed release doesn't reliably reach every client immediately, which is exactly
what `no-store` was meant to prevent.

## Evidence
Newman run, 2026-09-27, against `production-smoke` environment:
```
GET https://qicau.kajoga.workers.dev/this-route-does-not-exist [200 OK, 1.87kB, 67ms]
✓  status is 200 (SPA fallback)
✓  content-type is HTML
✗  Cache-Control is no-store
   AssertionError: expected 'public, max-age=0, must-revalidate' to include 'no-store'
```
Reproducible via `api-testing/collections/qicau-api-contract.postman_collection.json`,
folder "Non-API routes".

## Addendum, 2026-09-29 — also reproduces on local dev (different wrong value)
Re-checked `PWA-11` against local dev (`http://localhost:3000/`, Express `server.ts`, not the
production Cloudflare Worker) as part of L4 automation. Local dev returns
`Cache-Control: no-cache` on `GET /` — a *different* wrong value than production's
`public, max-age=0, must-revalidate`, but the same underlying defect: neither backend actually
sends `no-store`. Confirms this isn't a one-off Worker/Cloudflare-layer quirk — both of the app's
two server implementations (`server.ts` and `worker.ts`) diverge from the spec here, just in
different ways.
