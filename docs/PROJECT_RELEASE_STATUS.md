# Project release status — October 16, 2026

The three résumé projects have working, verified implementations. Detour's
updated AWS preview is live. Fretwork and SceneSeek now also have separate
**live, real-model Gradio demos on free Hugging Face ZeroGPU**. Their original
React/FastAPI applications remain available as working local/container releases,
not fully hosted stacks. These checks are not a certification for unrestricted
public production traffic.

| Project | Implemented and verified | Remaining release gate |
| --- | --- | --- |
| Detour | Real React/strict TypeScript client; Spring Boot APIs; all four expense split modes; reservations, voting, settlements and repayment; PostgreSQL migrations and restart persistence. Updated AWS preview deployed and browser-verified. | Durable PostgreSQL before relying on hosted data. |
| Fretwork | Real two-model audio inference, consensus, chord labels, MusicXML/TAB exports, private asynchronous jobs, and engraved sheet-music previews in React. Separate free Gradio demo deployed and real-inference verified. | Full React/API hosting with durable storage; public demo has temporary files and GPU quotas. |
| SceneSeek | Real CLIP text/image retrieval, native C++ similarity search, variable-speed clip matching, private libraries, and browser-compatible video playback. Separate free Gradio demo deployed and real-inference verified. | Full React/API hosting with durable storage; public demo has temporary libraries and GPU quotas. |

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
  The same functional browser checks pass against the approved, disposable
  [AWS preview](https://mkmuk8wzzz.us-east-2.awsapprunner.com), including all six
  responsive widths. The test creates only its own synthetic account/trip.
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

The user's initial constraint was **existing AWS resources only**; they later
authorized **free non-AWS hosting**. No new AWS resources have been created,
and no instance sizing has been changed. The existing Detour
App Runner preview in `us-east-2` has 0.25 vCPU / 512 MB RAM and ephemeral H2
storage. Real ML workers used over 1 GB during verification. That preview cannot
host these model services unchanged or provide durable PostgreSQL/media
storage. The user approved resetting the temporary preview data; redeployment
completed successfully without resizing the service or provisioning a database.
The old preview data was not retained. Application image commit:
`b565e53137862f4de8158db7dafdba3834b80780`.

After secure Hugging Face sign-in as `ThanhMinh12`, two new public Spaces
were created on explicitly selected free `zero-a10g` hardware. Both report
`RUNNING` with requested and actual `zero-a10g`; no paid plan, hardware or
credits were enabled. Existing unrelated Spaces were left untouched.

- [Fretwork demo](https://huggingface.co/spaces/ThanhMinh12/Fretwork): actual
  GAPS + Basic Pitch, 30-second recording, **99 notes / 29 chord labels**, two
  engraved score pages, and five artifact downloads passed. Seven additional
  hosted-adapter tests cover limits, isolated workspaces and engraving.
- [SceneSeek demo](https://huggingface.co/spaces/ThanhMinh12/SceneSeek): actual
  CLIP + C++, 30-second indexing, text retrieval, image retrieval at **12 s**,
  double-speed reference matched at **10–18.08 s / 2×**, and foreign-session
  library isolation passed. Five hosted-adapter tests and four Transformers
  4/5 compatibility regressions supplement the original 26 tests.

These are separate Gradio demo profiles, preserving the original React/API
apps and CPU locks. They have short upload limits, bounded queues, temporary
workspaces, hour-long cache retention, and visitor GPU quotas/queues. Their
file URLs are shareable capability links, not the full-stack apps' private
artifact routes. Only non-sensitive recordings/videos should be uploaded.
Full React/API deployments with durable storage remain a separate release gate.

A read-only scan of all 17 enabled regions found no EC2 instances, RDS
instances or ECS clusters. App Runner enumeration reported only the existing
preview; six regional App Runner checks failed, so this is not represented as
an exhaustive inventory of every AWS service or region endpoint.

All three have passing CI build/test workflows, including Detour's
PostgreSQL/browser/restart checks and SceneSeek's C++ tests. Full container
builds pass. Fretwork and SceneSeek additionally start the images as non-root
users and verify API/frontend availability and private session-secret file
permissions. Real model inference is separately verified by the native media
regressions; container health checks are not inference benchmarks. Docker
Desktop integration is still unavailable locally.

Release evidence:

- [Detour application verification](https://github.com/ThanhMinh12/Detour/actions/runs/38979441246)
  and [successful existing-preview deployment](https://github.com/ThanhMinh12/Detour/actions/runs/38979441324).
- [Fretwork tests, web build, image build and non-root startup](https://github.com/ThanhMinh12/Fretwork/actions/runs/38979949440).
- [SceneSeek native/API tests, web build, image build and non-root startup](https://github.com/ThanhMinh12/SceneSeek/actions/runs/38979950518).

Public source is available for
[Fretwork](https://github.com/ThanhMinh12/Fretwork) and
[SceneSeek](https://github.com/ThanhMinh12/SceneSeek). The updated
[portfolio](https://thanhminh12.github.io/) links to both real hosted demos and
their source, and identifies the demos as Gradio with temporary files. Its GitHub Pages
deployment and responsive checks pass.
