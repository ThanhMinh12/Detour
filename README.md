# Detour

Detour is a Java/Spring Boot + React/TypeScript trip planner with PostgreSQL persistence and auditable group expense splitting. It keeps one net balance per traveler and turns the final balances into a small set of repayments.

## MVP capabilities

- Create an account, sign in once, and keep a database-backed browser session.
- Create trips and invite travelers.
- Keep each trip private to its joined members; identity-sensitive actions use the signed-in account.
- Build and vote on a shared itinerary; keep reservation details beside each plan.
- Log equal, exact, percentage, or itemized expenses.
- Split shared line items and allocate tax/tip proportionally using exact cent arithmetic.
- Record reimbursements and inspect current net balances.
- Generate greedy settlements or an exact minimum-transfer settlement for small groups.
- Record suggested repayments and immediately recalculate the remaining balances.
- Use the responsive browser UI or the documented REST API at `/docs`.

## Run locally

The closest match to production uses Docker and PostgreSQL:

```bash
docker compose up --build
```

For the lighter embedded-database option, use Java 21 and Maven 3.9+. Maven installs a project-local Node 22 runtime, tests/builds the React client, and packages it into the same Spring Boot application:

```bash
mvn spring-boot:run
```

Open <http://localhost:8080> and create an account. Docker keeps PostgreSQL data in a named volume; the Maven option stores H2 data under `./data`. A standalone PostgreSQL instance can be selected with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.

After signing in, choose **Explore sample trip** to create the four-traveler dinner example plus a complete itinerary. The browser client supports all four split modes; the same workflows are available over REST.

Invite links carry the trip's invite code through sign-in or registration and then join the current account. An account sees only trips it has created or joined.

## Deploy to AWS

The repository includes a cost-conscious CloudFormation baseline: an ALB, one small ARM ECS Fargate task, isolated single-AZ RDS PostgreSQL, managed secrets, short-retention logs, alarms, an optional budget, autoscaling, optional Route 53/ACM, and a GitHub Actions OIDC deployment workflow. Start with [AWS deployment](docs/DEPLOYMENT.md), then review the honest [production-readiness checklist](docs/PRODUCTION_READINESS.md) before opening access beyond trusted testers.

## Verify

```bash
mvn verify
```

Container users can run `docker compose up --build` for the app plus PostgreSQL.

For frontend development, run `npm ci && npm run dev` in `web/` with Node 22.12+
while Spring Boot is listening on port 8080. Vite proxies the authenticated API.

Optional real browser and PostgreSQL restart checks are in
`scripts/smoke_browser.py` and `scripts/check_persistence.py`. They intentionally
require a disposable local database. The latter verifies that both trip data
and the signed-in session survive restarting the application.

Financial writes take a per-trip database lock so concurrent reimbursements
cannot both spend the same debt. Single-instance authentication, join, and write
rate limits bound simple abuse. These are portfolio/private-beta safeguards,
not a substitute for managed identity, recovery, monitoring, and an edge limiter
for a customer-facing service.

The existing AWS URL remains a disposable H2 preview until a durable database is
available. Local PostgreSQL passing tests does not make that live preview durable.

See the [three-project release report](docs/PROJECT_RELEASE_STATUS.md) for current
verification results, hosting constraints, and publication gates.

See [Architecture](docs/ARCHITECTURE.md), [API examples](docs/API.md), the [settlement algorithm](docs/SETTLEMENT.md), [deployment](docs/DEPLOYMENT.md), [production readiness](docs/PRODUCTION_READINESS.md), and the [roadmap](docs/ROADMAP.md).
