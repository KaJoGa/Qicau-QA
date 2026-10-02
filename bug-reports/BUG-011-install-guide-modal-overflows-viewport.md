# BUG-011 — Manual install-guide modal overflows the viewport

| | |
|---|---|
| **Jira issue** | https://kalev.atlassian.net/jira/software/projects/QAP/list?jql=project%20%3D%20QAP%20ORDER%20BY%20cf%5B10019%5D%20ASC&selectedIssue=QAP-27 |
| **Spec ID(s)** | `PWA-08` |
| **Severity** | Low (proposed — cosmetic/layout, content is still readable and "Mengerti" is reachable, but the container itself visibly clips past the screen edge) |
| **Found in** | Production — https://qicau.kajoga.workers.dev/, both Android and desktop web |
| **Found during** | Manual exploration, 2026-09-30 |

## Summary
The manual install-instructions modal (`PWA-08` — shown when the browser has no native install
prompt, with the numbered "Buka menu browser Anda" / "Pilih Install Aplikasi" / "Ketuk Tambah"
steps and a "Mengerti" button) renders with its container extending past the visible screen
bounds, clipping part of the modal itself rather than staying fully within the viewport.

## Steps to reproduce
1. On a device/browser that shows the manual install guide instead of a native install prompt,
   tap the **header** "Pasang" install button (i.e. **not** the one inside Pengaturan/Settings).
2. Observe the modal's container edges relative to the screen edges.

Per `PWA-06`, the install button exists in two places — header and Settings. This bug is specific
to the **header** entry point: opening the same manual guide from inside **Settings** renders it
correctly, fully contained and centered on screen — I confirmed this. This strongly
suggests two separate modal instances/positioning code paths for the same content, one broken and
one fine, rather than one shared component with a general layout bug.

## Expected (per spec)
`PWA-08` only specifies the manual guide's content (steps to share → add to home screen); it
doesn't say anything about layout explicitly, but a modal container clipping past the screen edge
is not a reasonable rendering of any dialog, and isn't how the same modal behaves when opened from
Settings.

## Actual
Opened from the **header** install button, on Android, the modal's
container is visibly cut off / extends past the screen edge instead of staying fully contained
within it. Confirmed **not** mobile-only — the same overflow reproduces on desktop web too, also
via the header button. Opened from **Settings**, the identical guide renders normally, centered
in the viewport.

## Evidence
Screenshot I captured during manual exploration, 2026-09-30 — the modal's top edge is clipped by
the visible screen/browser frame, with the numbered steps and "Mengerti" button rendering as if
the container is taller/positioned higher than the viewport allows. Captured via the header
install button; I separately confirmed the Settings entry point does not reproduce it.

## Note
Since the same content renders correctly from Settings but not from the header, this is likely
two separate trigger/positioning implementations for what should be one shared modal — worth
checking whether the header button opens the guide in a different container (e.g. missing a
centering/`max-height` wrapper that the Settings-triggered version has) rather than reusing the
same modal component.
