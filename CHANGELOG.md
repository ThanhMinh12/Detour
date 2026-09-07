# Changelog

## Unreleased

### Step 1 — Foundation

- Initialized the Spring Boot 3 / Java 21 project.
- Documented the modular-monolith architecture, money invariants, MVP scope, and roadmap.
- Added H2 local persistence with PostgreSQL runtime support and OpenAPI UI.

### Step 2 — Trip collaboration

- Added trips with organizer membership and shareable invite codes.
- Added traveler membership validation and duplicate protection.
- Added itinerary CRUD with proposal/confirmation status, reservation details, and voting.
- Added consistent validation and API problem responses.

### Step 3 — Expense splitting

- Added immutable expenses and participant shares backed by integer minor units.
- Added equal, exact, percentage, and itemized split modes.
- Added shared line items plus deterministic largest-remainder allocation for tax, tip, and rounding cents.
- Added receipt-level conservation and validation tests for the example dinner.
