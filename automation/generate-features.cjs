// One-off generator: converts the already-authored blind test-case CSVs (test-cases/*.csv) into
// Cucumber .feature files for the L4 automation project. Mechanical reformatting only — no new
// test-design decisions are made here; the content is exactly what's in the CSVs (see
// test-cases/README.md), which were themselves written blind from test/Qicau.md.
//
// Run once with: node automation/generate-features.cjs
const fs = require('fs');
const path = require('path');

function parseCSV(text) {
  const rows = [];
  let row = [], field = '', inQuotes = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (inQuotes) {
      if (c === '"') { if (text[i + 1] === '"') { field += '"'; i++; } else inQuotes = false; }
      else field += c;
    } else {
      if (c === '"') inQuotes = true;
      else if (c === ',') { row.push(field); field = ''; }
      else if (c === '\n') { row.push(field); field = ''; rows.push(row); row = []; }
      else if (c === '\r') { /* skip */ }
      else field += c;
    }
  }
  if (field.length || row.length) { row.push(field); rows.push(row); }
  return rows.filter((r) => !(r.length === 1 && r[0] === ''));
}

function readCases(csvPath) {
  const text = fs.readFileSync(csvPath, 'utf8');
  const rows = parseCSV(text);
  const header = rows[0];
  return rows.slice(1).map((r) => {
    const obj = {};
    header.forEach((h, i) => (obj[h] = r[i] || ''));
    return obj;
  });
}

function splitSentences(s) {
  // Split on "; " which is how the CSVs separate independent expected-result clauses.
  return s.split(/;\s+/).map((x) => x.trim()).filter(Boolean);
}

function splitSteps(s) {
  // Steps are numbered "1. ... \n2. ..." in the CSVs.
  return s
    .split(/\n/)
    .map((x) => x.replace(/^\d+\.\s*/, '').trim())
    .filter(Boolean);
}

function priorityTag(p) {
  return p === 'P1' ? '@smoke' : '@regression';
}

function toFeatureLines(cases, manualOnlyIds = new Set()) {
  const lines = [];
  for (const c of cases) {
    const specIds = c.spec_id.split(';').map((x) => x.trim());
    const tags = ['@' + specIds.join(' @'), priorityTag(c.priority)];
    if (specIds.some((id) => manualOnlyIds.has(id))) tags.push('@manual-only');
    lines.push('  ' + tags.join(' '));
    lines.push(`  Scenario: ${c.title}`);
    const pre = c.preconditions && c.preconditions !== 'None' ? c.preconditions : 'the app is in its default state';
    lines.push(`    Given ${pre}`);
    const steps = splitSteps(c.steps);
    steps.forEach((s, i) => lines.push(`    ${i === 0 ? 'When' : 'And'} ${s}`));
    const expects = splitSentences(c.expected_result);
    expects.forEach((e, i) => lines.push(`    ${i === 0 ? 'Then' : 'And'} ${e}`));
    lines.push('');
  }
  return lines;
}

function writeFeature(outPath, featureTitle, casesBySpecPrefix, manualOnlyIds) {
  const lines = [`Feature: ${featureTitle}`, ''];
  for (const cases of casesBySpecPrefix) lines.push(...toFeatureLines(cases, manualOnlyIds));
  fs.writeFileSync(outPath, lines.join('\n').replace(/\n{3,}/g, '\n\n'));
  console.log('wrote', outPath, `(${casesBySpecPrefix.flat().length} scenarios)`);
}

const TC = path.join(__dirname, '..', 'test-cases');
const OUT = path.join(__dirname, 'src', 'test', 'resources', 'features');

const authNav = readCases(path.join(TC, 'auth-nav.csv'));
const homeVoice = readCases(path.join(TC, 'home-voice.csv'));
const manualInput = readCases(path.join(TC, 'manual-input.csv'));
const history = readCases(path.join(TC, 'history.csv'));
const monthly = readCases(path.join(TC, 'monthly.csv'));
const pwaOffline = readCases(path.join(TC, 'pwa-offline.csv'));

const byPrefix = (cases, prefix) => cases.filter((c) => c.spec_id.split(';')[0].trim().startsWith(prefix));

const MANUAL_ONLY = new Set(['PWA-08']); // no iOS device available (test-plan.md §2.1)

writeFeature(path.join(OUT, 'auth.feature'), 'Authentication & Session (AUTH)', [byPrefix(authNav, 'AUTH')], MANUAL_ONLY);
writeFeature(path.join(OUT, 'navigation.feature'), 'Navigation & Settings (NAV)', [byPrefix(authNav, 'NAV')], MANUAL_ONLY);
writeFeature(path.join(OUT, 'home.feature'), 'Home / Catat Tab (HOME)', [byPrefix(homeVoice, 'HOME')], MANUAL_ONLY);
writeFeature(path.join(OUT, 'voice.feature'), 'Voice Input (VOICE)', [byPrefix(homeVoice, 'VOICE')], MANUAL_ONLY);
writeFeature(path.join(OUT, 'save.feature'), 'Post-Save: Toast, Edit, Undo (SAVE)', [byPrefix(homeVoice, 'SAVE')], MANUAL_ONLY);
writeFeature(path.join(OUT, 'low-confidence.feature'), 'Low-Confidence Modal (LOWC)', [byPrefix(homeVoice, 'LOWC')], MANUAL_ONLY);
writeFeature(path.join(OUT, 'manual-input.feature'), 'Manual Input (MAN)', [manualInput], MANUAL_ONLY);
writeFeature(path.join(OUT, 'history.feature'), 'History / Riwayat (HIST)', [history], MANUAL_ONLY);
writeFeature(path.join(OUT, 'monthly.feature'), 'Monthly / Weekly Summary (MON)', [monthly], MANUAL_ONLY);
writeFeature(path.join(OUT, 'pwa.feature'), 'Offline & PWA (PWA)', [byPrefix(pwaOffline, 'PWA')], MANUAL_ONLY);
writeFeature(path.join(OUT, 'toast.feature'), 'Global Notifications (TOAST)', [byPrefix(pwaOffline, 'TOAST')], MANUAL_ONLY);

// Sprint 3: only the in-app custom-dialog slices of SYNC (L5 otherwise stays manual on production).
// These dialogs appear BEFORE any Google OAuth call, so they can be driven on local dev + emulator:
// SYNC-15 (Reset dialog + Batal), SYNC-19 (Ya, Reset disabled ~1s), SYNC-20 (first-time Sync dialog + Batal).
// SYNC-22 is not generated: it needs the per-browser "has synced" flag, whose storage key is an app internal.
const syncAll = readCases(path.join(TC, 'sheets-sync.csv'));
const SYNC_DIALOG_IDS = new Set(['SYNC-15', 'SYNC-19', 'SYNC-20']);
writeFeature(
  path.join(OUT, 'sync-dialogs.feature'),
  'Sync / Reset confirmation dialogs - in-app part only (SYNC)',
  [syncAll.filter((c) => SYNC_DIALOG_IDS.has(c.spec_id.split(';')[0].trim()))],
  MANUAL_ONLY
);
