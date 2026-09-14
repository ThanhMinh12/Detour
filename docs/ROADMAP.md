# Roadmap

## MVP

- Trip, traveler, itinerary, voting, and reservation workflows.
- Equal, exact, percentage, and itemized expense splits.
- Net balances, reimbursements, and settlement suggestions.
- Responsive web client and REST/OpenAPI documentation.
- Email/password accounts, persistent database sessions, CSRF protection, and enforced trip membership.

## Next

- Email verification, password recovery, hashed expiring invite links, rate limiting, and HTTPS-only traffic (public-launch blockers).
- Broader authorization isolation tests and security-event alerting.
- Auditable expense corrections, idempotent financial writes, and optimistic concurrency.
- Browser end-to-end and accessibility coverage for the complete trip and settlement loop.
- Comments, attachments, notifications, and real-time collaboration.
- Receipt OCR with a human-confirmation workflow.
- Currency-specific minor-unit metadata and explicit FX conversion records.
- Append-only audit/event log for corrections and security-sensitive changes.
- Outbox-driven notifications and cached read models on AWS.
