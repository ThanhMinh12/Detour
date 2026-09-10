# Production readiness

Detour has a complete product-shaped MVP and a repeatable AWS delivery path. It is suitable for a private beta after the AWS stack is configured, but not yet for an unrestricted public launch.

## Current status

| Area | Status | Notes |
| --- | --- | --- |
| Core ledger | Ready for beta | Integer minor units, immutable shares, invariant checks, and settlement tests are in place. |
| Collaboration | MVP complete | Trips, invite joining, travelers, itinerary, voting, and reservations work. |
| Browser experience | Beta quality | Responsive dashboard, all split modes, explicit loading/error states, mobile navigation, and a temporary active-traveler selector. |
| Database lifecycle | Ready | Flyway migrations own the schema; Hibernate only validates it. |
| AWS infrastructure | Ready to provision | CloudFormation covers ECR, ALB, ARM Fargate, RDS, Secrets Manager, Route 53/ACM integration, logs, health checks, autoscaling, alarms, and an optional budget. |
| Delivery | Ready to configure | GitHub Actions uses short-lived OIDC credentials and immutable commit images. |
| Authentication and authorization | **Blocking public launch** | The API has no signed-in principal or trip-level access checks. |

## Missing parts, in priority order

### P0 — before public traffic

1. Add sign-in (Cognito or another OIDC provider) and map the subject claim to an application user.
2. Enforce trip membership in every query and mutation; never accept an arbitrary `memberId` as proof of identity.
3. Replace permanent invite codes with hashed, expiring, revocable invite tokens and rate-limit join attempts.
4. Add request throttling/WAF rules. TLS deployments already redirect HTTP to HTTPS; security headers and a restrictive Content Security Policy are enabled.
5. Add authorization integration tests proving one account cannot list, read, vote in, expense, or settle another account's trip.

### P1 — before a paid beta

1. Add edit/cancel workflows for trips and expenses using auditable correction entries rather than rewriting settled history.
2. Add frontend browser tests for trip creation, joining, all split modes, and payment recording, plus automated accessibility checks.
3. Connect the supplied alarm email, then add EventBridge notifications for ECS deployment failures and RDS backup events before relying on unattended deployments.
4. Run a restore drill and document recovery time/recovery point objectives.
5. Add idempotency keys to financial writes and optimistic concurrency to collaborative edits.
6. Create a least-privilege database account for the running app and reserve the RDS master account for migrations/administration.
7. Replace the temporary “viewing as” selector with the authenticated member profile.

### P2 — after behavior is measured

1. Notifications, comments, attachments, and real-time collaboration.
2. Receipt image upload and human-confirmed OCR.
3. Offline-friendly/PWA behavior and richer itinerary maps.
4. Multi-currency display and explicit conversion workflows.

## Frontend direction

The no-build HTML/CSS/JavaScript client is fast, attractive, and appropriate for validating the product. Keep it for the private beta. Move to a component framework only when authenticated routing, larger edit flows, and browser-test coverage make the current single-file state management costly; a rewrite by itself would not improve the customer experience.
