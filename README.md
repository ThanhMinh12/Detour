# Detour

Detour is a collaborative trip planner with fair, auditable group expense splitting. It keeps one net balance per traveler and turns the final balances into a small set of repayments.

## MVP capabilities

- Create trips and invite travelers.
- Build and vote on a shared itinerary; keep reservation details beside each plan.
- Log equal, exact, percentage, or itemized expenses.
- Split shared line items and allocate tax/tip proportionally using exact cent arithmetic.
- Record reimbursements and inspect current net balances.
- Generate greedy settlements or an exact minimum-transfer settlement for small groups.
- Use the responsive browser UI or the documented REST API at `/docs`.

## Run locally

Requirements: Java 21 and Maven 3.9+.

```bash
mvn spring-boot:run
```

Open <http://localhost:8080>. Data is stored under `./data` by default. To use PostgreSQL, set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.

## Verify

```bash
mvn verify
```

See [Architecture](docs/ARCHITECTURE.md), [API examples](docs/API.md), and the [roadmap](docs/ROADMAP.md).

