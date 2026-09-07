# Deployment

## Container locally

The application defaults to embedded H2 for the quickest local start. To run the production-shaped stack with PostgreSQL:

```bash
docker compose up --build
```

The app is available at <http://localhost:8080>; health is reported at `/actuator/health`.

## AWS path

The modular monolith maps cleanly to a small first production footprint:

- Build the `Dockerfile` in GitHub Actions and publish it to Amazon ECR.
- Run at least two stateless tasks on ECS Fargate behind an Application Load Balancer.
- Use an RDS PostgreSQL Multi-AZ instance and keep credentials in Secrets Manager.
- Serve TLS with ACM and Route 53; ship structured container logs to CloudWatch.
- Add Flyway before the first production database deployment and switch Hibernate schema handling from `update` to `validate`.

## Evolution triggers

Do not split services merely for the label “distributed.” Extract a boundary when its operating profile demands it:

- Notification delivery can move behind an outbox and SQS because it is asynchronous and independently retryable.
- Receipt OCR can run as an S3-triggered worker because it is compute-heavy and human-confirmed.
- Settlement remains a synchronous, deterministic library unless group sizes or optimization workloads require a separate compute queue.
- Collaboration updates can add WebSocket fan-out and Redis when concurrent editing becomes a measured need.

The ledger database remains the financial source of truth. Derived balance projections may be cached, but writes must preserve the zero-sum invariant transactionally.

