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

### Step 4 — Balances and settlement

- Added derived per-traveler net balances and an invariant check that the trip ledger sums to zero.
- Added validated reimbursement records that reduce current balances without creating pairwise debt state.
- Added deterministic greedy settlement and exact branch-and-bound minimum-transfer settlement for up to 12 non-zero balances.
- Documented optimization behavior, complexity, fallback reporting, and non-mutating settlement projections.
- Renamed the persisted vote column to remain compatible with both H2 and PostgreSQL reserved words.

### Step 5 — Web experience

- Added a responsive trip dashboard with itinerary, expenses, balances, and minimum-transfer settlement views.
- Added browser workflows for trips, travelers, activities, reservations, votes, and all four expense split modes.
- Added a one-click, API-backed sample trip using the four-person itemized dinner example.
- Added accessible empty, loading, error, mobile-navigation, and modal-form states without a frontend build dependency.

### Step 6 — Delivery

- Added a multi-stage, non-root container image and PostgreSQL Compose stack.
- Added Spring Boot health probes for container and load-balancer checks.
- Added GitHub Actions verification on pushes and pull requests.
- Documented a practical ECS, ECR, RDS, Secrets Manager, and CloudWatch production path plus evidence-based service extraction triggers.

### Step 7 — Complete the collaboration loop

- Added invite-code joining from both copied links and the empty state.
- Added one-click recording of suggested repayments with immediate balance and settlement refresh.
- Documented the trusted-access constraint until authentication and authorization land.
