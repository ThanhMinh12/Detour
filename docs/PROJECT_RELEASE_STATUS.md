# Project release status — October 17, 2026

The three résumé projects have working, locally verified implementations. They
are **not all publicly deployed**, and local tests are not a certification for
unrestricted public production traffic.

| Project | Implemented and verified | Remaining release gate |
| --- | --- | --- |
| Detour | Real React/strict TypeScript client; Spring Boot APIs; all four expense split modes; reservations, voting, settlements and repayment; PostgreSQL migrations and restart persistence. | Approval to reset/redeploy the disposable AWS preview; durable PostgreSQL before relying on hosted data. |
| Fretwork | Real two-model audio inference, consensus, chord labels, MusicXML/TAB exports, private asynchronous jobs, and engraved sheet-music previews in React. | Repository visibility approval and suitable existing AWS compute/persistent storage. |
| SceneSeek | Real CLIP text/image retrieval, native C++ similarity search, variable-speed clip matching, private libraries, and browser-compatible video playback. | Repository visibility approval and suitable existing AWS compute/persistent storage. |

## Verification evidence

- **Detour:** 16 backend tests pass with H2 and with PostgreSQL 16.13; four
  frontend tests pass. The backend suite includes concurrent repayment
  serialization. Real Chromium workflows cover all four split modes,
  reservation creation/editing/voting, repayment, logout/login restoration and
  navigation/dialog access at six widths from 320–1440 px. A conflicting legacy
  mobile-sidebar transform was removed. A separate process-restart check preserves both the
  PostgreSQL trip and the authenticated HTTP session. Maven release packaging
  succeeds. The old imperative client was replaced; its source remains
  recoverable from Git history.
- **Fretwork:** 41 core tests and five API tests pass without skips. Installation
  from the frozen runtime succeeds in a fresh Python 3.11 environment; the
  supported checker documents two Basic Pitch upstream metadata exceptions
  rather than claiming a clean generic `pip check`. Real guitar audio produces
  nonempty notation, TAB, chord and report artifacts using both actual models;
  Flat/slash chords use the notation library's spelling; unsupported extensions
  remain visible text annotations instead of being silently dropped. Chromium
  renders the resulting MusicXML score, with all 29 predicted chord labels
  preserved in the final real-audio replay. Another browser session
  cannot read the recording or job. Model Git revisions and the guitar
  checkpoint SHA-256 are pinned and checked.
- **SceneSeek:** 26 Python tests pass across native and NumPy paths; native
  Release assertions/tests pass. Actual CLIP inference retrieves the reference
  image at 12 seconds and the 10–18 second clip at 1×, 0.5× and 2× speed.
  Chromium plays the generated WebM with no media error. Session isolation and
  HTTP range streaming pass. A synthetic 100,000 × 512-vector native benchmark
  measured 123.38 ms median across five queries, excluding inference and I/O;
  this is not a p99 or end-to-end latency claim.

The real-media fixtures are regression checks, not broad accuracy benchmarks.
Fretwork's score is an editable draft; SceneSeek can be ambiguous on repetitive
or static scenes. Neither application's browser-session ownership is an
account-login system. Their release-readiness documents explain operational
limits and additional safeguards needed for sensitive or unrestricted use.

Evidence from this run is retained locally under
`/tmp/projects-ready-evidence/`; model weights, recordings, session secrets and
generated user data are excluded from source control. Detailed application
reports are in each project's `docs/RELEASE-READINESS.md` (and Detour's
[production checklist](PRODUCTION_READINESS.md)).

## Hosting and publication

The user's constraint is **existing AWS resources only**. No new AWS resources
have been created, and no instance sizing has been changed. The existing Detour
App Runner preview in `us-east-2` has 0.25 vCPU / 512 MB RAM and ephemeral H2
storage. Real ML workers used over 1 GB during verification. That preview cannot
host these model services unchanged or provide durable PostgreSQL/media
storage. The existing preview remains unchanged pending approval to reset its
temporary data.

A read-only scan of all 17 enabled regions found no EC2 instances, RDS
instances or ECS clusters. App Runner enumeration reported only the existing
preview; six regional App Runner checks failed, so this is not represented as
an exhaustive inventory of every AWS service or region endpoint.

All three have container configurations and CI build/test workflows. Docker
Desktop integration is unavailable in this WSL environment, so **full container
builds are not yet verified**. Native builds and runtime tests do not replace
that release gate. No remote CI run is claimed for these unpublished changes.

Fretwork and SceneSeek publication awaits the user's public/private repository
choice. Detour changes are kept local because a push may automatically redeploy
and erase the preview's temporary data. The portfolio has not been updated to
advertise unverified live demos.
