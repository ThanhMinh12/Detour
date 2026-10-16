# Production readiness

Detour has a complete product-shaped MVP and a repeatable AWS delivery path. It is suitable for a small private beta after HTTPS is configured, but not yet for unrestricted public registration.

## Current status

| Area | Status | Notes |
| --- | --- | --- |
| Core ledger | Ready for beta | Integer minor units, immutable shares, invariant checks, and settlement tests are in place. |
| Collaboration | MVP complete | Trips, invite joining, travelers, itinerary, voting, and reservations work. |
| Browser experience | Verified React/TypeScript client | Browser checks cover all split modes, reservation create/edit/vote, repayment, session restoration, and 320–1440 px layouts. |
| Database lifecycle | Verified locally with PostgreSQL | Flyway/Hibernate checks pass against PostgreSQL 16.13; HTTP sessions and trip data survive application restart. The live AWS H2 preview is still temporary. |
| AWS infrastructure | Ready to provision | CloudFormation covers ECR, ALB, ARM Fargate, RDS, Secrets Manager, Route 53/ACM integration, logs, health checks, autoscaling, alarms, and an optional budget. |
| Delivery | Ready to configure | GitHub Actions uses short-lived OIDC credentials and immutable commit images. |
| Authentication and authorization | Ready for private beta | BCrypt credentials, database sessions, CSRF protection, signed-in member identity, and private trip queries are implemented and integration-tested. |

## Missing parts, in priority order

### P0 — before public traffic

1. Require HTTPS before accepting real credentials. The AWS stack redirects HTTP only after an ACM certificate and domain are configured.
2. Add verified email, password reset, account recovery, and a safe account deletion/export flow—or migrate the same account IDs to a managed identity provider when that operational burden becomes worthwhile.
3. Replace permanent invite codes with hashed, expiring, revocable invite tokens and rate-limit login, registration, and join attempts.
4. Add edge/WAF throttling and operational alerts when scaling beyond one instance. A bounded per-IP/global single-instance limiter now covers authentication, joining, and writes; security headers and a restrictive CSP remain enabled.
5. Expand authorization integration coverage across every financial write and activity mutation, including attempts using member IDs copied from another trip.

### P1 — before a paid beta

1. Add edit/archive workflows for trips and auditable expense corrections instead of rewriting settled financial history.
2. Expand the existing real browser smoke suite with automated accessibility and broader collaboration scenarios.
3. Connect the supplied alarm email, then add EventBridge notifications for ECS deployment failures and RDS backup events before relying on unattended deployments.
4. Run a restore drill and document recovery time/recovery point objectives.
5. Add idempotency keys to financial writes and optimistic concurrency to collaborative edits.
6. Create a least-privilege database account for the running app and reserve the RDS master account for migrations/administration.

### P2 — after behavior is measured

1. Notifications, comments, attachments, and real-time collaboration.
2. Receipt image upload and human-confirmed OCR.
3. Offline-friendly/PWA behavior and richer itinerary maps.
4. Multi-currency display and explicit conversion workflows.

## Frontend direction

The client now lives in `web/` and uses React and strict TypeScript. Maven and
the multi-stage Docker build test/build it into the same-origin Spring Boot
service. The legacy imperative client was replaced. Per-trip pessimistic locks
serialize expense/reimbursement writes; a concurrency regression test verifies
that duplicate simultaneous repayments cannot both be committed.
