# Production readiness

Detour has a complete product-shaped MVP and a repeatable AWS delivery path. It is suitable for a small private beta after HTTPS is configured, but not yet for unrestricted public registration.

## Current status

| Area | Status | Notes |
| --- | --- | --- |
| Core ledger | Ready for beta | Integer minor units, immutable shares, invariant checks, and settlement tests are in place. |
| Collaboration | MVP complete | Trips, invite joining, travelers, itinerary, voting, and reservations work. |
| Browser experience | Beta quality | Responsive account flow and dashboard, all split modes, explicit loading/error states, and mobile navigation. |
| Database lifecycle | Ready | Flyway migrations own the schema; Hibernate only validates it. |
| AWS infrastructure | Ready to provision | CloudFormation covers ECR, ALB, ARM Fargate, RDS, Secrets Manager, Route 53/ACM integration, logs, health checks, autoscaling, alarms, and an optional budget. |
| Delivery | Ready to configure | GitHub Actions uses short-lived OIDC credentials and immutable commit images. |
| Authentication and authorization | Ready for private beta | BCrypt credentials, database sessions, CSRF protection, signed-in member identity, and private trip queries are implemented and integration-tested. |

## Missing parts, in priority order

### P0 — before public traffic

1. Require HTTPS before accepting real credentials. The AWS stack redirects HTTP only after an ACM certificate and domain are configured.
2. Add verified email, password reset, account recovery, and a safe account deletion/export flow—or migrate the same account IDs to a managed identity provider when that operational burden becomes worthwhile.
3. Replace permanent invite codes with hashed, expiring, revocable invite tokens and rate-limit login, registration, and join attempts.
4. Add request throttling/WAF rules and alerting for repeated authentication failures. Security headers and a restrictive Content Security Policy are already enabled.
5. Expand authorization integration coverage across every financial write and activity mutation, including attempts using member IDs copied from another trip.

### P1 — before a paid beta

1. Add edit/archive workflows for trips and auditable expense corrections instead of rewriting settled financial history.
2. Add frontend browser tests for trip creation, joining, all split modes, and payment recording, plus automated accessibility checks.
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

The no-build HTML/CSS/JavaScript client is fast, attractive, and appropriate for validating the product. It now has account registration, login, logout, invite continuation, and an authenticated member profile. Keep it for the private beta. Move to a component framework only when larger edit flows and browser-test coverage make the current single-file state management costly; a rewrite by itself would not improve the customer experience.
