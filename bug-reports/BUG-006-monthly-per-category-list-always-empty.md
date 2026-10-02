# BUG-006 — Ringkasan/Bulanan's per-category list always shows the empty state, even with data

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-22 |
| **Spec ID(s)** | `MON-04`, `MON-05` |
| **Severity** | Medium |
| **Found in** | local dev + Firebase emulator (`http://localhost:3000/`) |
| **Found during** | L4 UI automation run, 2026-09-29 |

## Summary
On the Bulanan (Ringkasan) tab, the "Total Pengeluaran" card correctly sums and displays the
current month's transactions — but the "Kategori" section right below it always shows the empty
state ("Belum ada riwayat transaksi.") instead of a per-category breakdown, even when the total is
clearly non-zero and transactions exist.

## Steps to reproduce
1. Sign in with a brand-new account (no prior data).
2. Open "Input Manual" → "Formulir Langsung", leave Kategori at its default ("Makan"), enter a
   price (e.g. 18000), save.
3. Open the Bulanan/Ringkasan tab.

## Expected (per spec)
`MON-04`: "Hanya kategori dengan transaksi ditampilkan, diurutkan dari nominal terbesar ...
tiap baris menampilkan ikon, nama kategori, nominal, dan bar proporsi (nominal kategori dibagi
total)." With one category and one transaction, at minimum a single "Makan" row should appear
with its icon, amount ("Rp 18.000"), and a full-width proportion bar.

## Actual
The "Kategori" heading renders, but the section body shows "Belum ada riwayat transaksi." — the
same empty-state text used when there's genuinely no data — while the "Total Pengeluaran" card
directly above it correctly shows "Rp 18.000". Confirmed twice independently:
- With exactly one transaction (default category, no typing involved at all).
- With two transactions in two different categories (Rp 18.000 + Rp 50.000 = correctly totalled
  as "Rp 68.000"), the per-category list still shows the same empty state.
- Waited an additional 2 seconds and did a full page reload before re-checking in both cases — not
  a loading race, the empty state persists.

This means the total and the per-category breakdown clearly read from different logic/queries:
whatever computes the overall total is working, but whatever populates the per-category list is
not, regardless of how many categories or transactions exist.

Since the per-category section never renders with real data, `MON-05` (donut chart's fixed
per-category colors and tooltip) can't be meaningfully exercised either — if the chart is driven
by the same per-category aggregation, it's presumably also stuck on its own "no data" state.

## Evidence
`automation/dump/16-ringkasan-two-categories.html`, `automation/dump/16b-ringkasan-two-categories-after-wait.html`,
`automation/dump/19-ringkasan-single-default-category.html` (captured by
`automation/src/test/java/com/qicau/qa/runners/ExplorerTest.java`, `dumpMonthlySingleCategory`) —
all three show `<h3>...Rp&nbsp;18.000</h3>` (or 68.000) immediately followed by
`<p>...Belum ada riwayat transaksi.</p>` under the "Kategori" heading.
