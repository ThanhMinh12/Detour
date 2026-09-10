# Deployment

## Container locally

The application defaults to embedded H2 for the quickest local start. To run the production-shaped stack with PostgreSQL:

```bash
docker compose up --build
```

The app is available at <http://localhost:8080>; health is reported at `/actuator/health`.

## AWS deployment

The repository includes a complete, cost-conscious AWS baseline:

```text
Route 53 / ACM (optional)
          |
Application Load Balancer
          |
ECS Fargate (1 small ARM task; scales to 2)
          |
RDS PostgreSQL (single-AZ micro; isolated subnets)
```

Fargate tasks receive public IPs only for outbound ECR, Secrets Manager, and CloudWatch access. Their security group accepts inbound traffic exclusively from the load balancer. The database has no internet route and accepts PostgreSQL only from the application security group. This avoids a NAT Gateway's fixed cost while preserving network isolation at the security-group boundary.

The application stack also enables immutable image tags, ECR scanning, RDS-managed credentials, encrypted storage, seven-day backups, deletion protection, seven-day structured logs, rolling deployment rollback, operational alarms, an optional monthly budget warning, and CPU/memory autoscaling. Flyway owns the schema and Hibernate validates it at startup.

### Why this is the default

This is the professional small-application path for the current architecture: CloudFormation owns every resource, GitHub Actions receives short-lived AWS credentials through OIDC, ECR stores immutable commit images, ECS replaces containers without SSH, and RDS owns backups and database credentials. EC2 is not wrong, but a hand-maintained VM and `docker compose pull` make patching, rollback, secrets, and recovery depend on that one machine.

The browser client is packaged into the same application image. Keeping one origin and one deployable artifact avoids a second hosting service, CORS configuration, and duplicated release coordination. Amplify Hosting or S3/CloudFront becomes useful after the frontend is split into an independently released application.

The economical defaults deliberately trade availability for cost: one 0.25-vCPU/512-MiB ARM task and a single-AZ `db.t4g.micro` database can be briefly unavailable during failures or maintenance. For a public production launch, set two desired tasks and enable Multi-AZ RDS. The Application Load Balancer and RDS are the main fixed-cost items; they remain because a stable HTTPS endpoint and managed relational data are more valuable here than the lowest possible bill.

### 1. Bootstrap the account once

Install and authenticate the AWS CLI, choose a region, and deploy the bootstrap stack. Replace the example GitHub subject with the exact `sub` claim used by the repository:

```bash
aws cloudformation deploy \
  --region us-east-2 \
  --stack-name detour-bootstrap \
  --template-file infra/aws/bootstrap.yml \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    GitHubSubjectClaim='repo:ThanhMinh12/Detour:ref:refs/heads/main' \
    MonthlyBudgetUsd=50
```

If the AWS account already has GitHub's OIDC provider, also pass `ExistingGitHubOidcProviderArn=arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com`. GitHub repositories using immutable OIDC subject claims should pass that exact owner/repository-ID form instead.

The bootstrap creates the ECR repository, a branch-scoped GitHub OIDC role, a separate CloudFormation execution role, and an account-wide monthly budget. It does not create access keys or any always-running compute. Pass `NotificationEmail=you@example.com` to receive warnings at 80% of forecast and 100% of actual budget after confirming the email subscription; without it, the budget is still visible in Billing.

### 2. Configure GitHub repository variables

Copy the bootstrap stack outputs into repository **Settings → Secrets and variables → Actions → Variables**:

| Variable | Value |
| --- | --- |
| `AWS_DEPLOY_ENABLED` | set `true` only when the paid environment should run; otherwise the deploy job stays skipped |
| `AWS_DEPLOY_ROLE_ARN` | `DeployRoleArn` output |
| `AWS_CLOUDFORMATION_ROLE_ARN` | `CloudFormationRoleArn` output |
| `AWS_ECR_REPOSITORY` | `EcrRepositoryName` output |
| `AWS_REGION` | defaults to the existing project region, `us-east-2` |
| `AWS_STACK_NAME` | optional; defaults to `detour-production` |
| `AWS_ALLOWED_INGRESS_CIDR` | optional; restrict to your testers' public IP range before authentication lands |
| `AWS_DESIRED_COUNT` / `AWS_MAXIMUM_COUNT` | optional; defaults to 1 / 2 Fargate tasks |
| `AWS_DATABASE_INSTANCE_CLASS` | optional; defaults to `db.t4g.micro` |
| `AWS_DATABASE_MULTI_AZ` | optional; set `true` for production availability |
| `AWS_NOTIFICATION_EMAIL` | optional; receives operational service alarms after confirmation |

The `deploy-aws` workflow verifies the app, publishes an image tagged with the commit SHA, deploys `infra/aws/app.yml`, waits for ECS/RDS health, and prints the public URL. It remains safely skipped until `AWS_DEPLOY_ENABLED` is exactly `true`, even when all role variables are configured.

### 3. Add a custom domain and TLS

Request or import an ACM certificate in the same region as the load balancer. Then set these additional GitHub variables and rerun the workflow:

| Variable | Value |
| --- | --- |
| `AWS_CERTIFICATE_ARN` | validated ACM certificate ARN |
| `AWS_DOMAIN_NAME` | for example `detour.example.com` |
| `AWS_HOSTED_ZONE_ID` | Route 53 public hosted zone ID |

The stack creates the HTTPS listener and Route 53 alias, then redirects HTTP to HTTPS automatically.

### Production warning

The infrastructure is deployable, but Detour is **not ready for an unrestricted public launch**. Authentication and per-trip authorization remain the launch blocker: today, any visitor who reaches the app can list and mutate all trips. Until that work lands, restrict access upstream (for example with an identity-aware proxy or a tightly scoped load-balancer ingress rule) and use the deployment only with trusted testers.

RDS deletion protection is intentionally enabled. Disable it explicitly before deleting the application stack. The database is retained as a final snapshot on deletion or replacement.

### Stop all Detour charges

Do not merely set the ECS desired count to zero: the load balancer, database, storage, and public IPv4 resources can still be billed. To retire the environment, first preserve or export any data you need, disable RDS deletion protection, and delete the application and bootstrap CloudFormation stacks. CloudFormation creates a final RDS snapshot; delete that snapshot too when it is no longer required. The bootstrap ECR repository is configured to empty when its stack is deleted.

## Evolution triggers

Do not split services merely for the label “distributed.” Extract a boundary when its operating profile demands it:

- Notification delivery can move behind an outbox and SQS because it is asynchronous and independently retryable.
- Receipt OCR can run as an S3-triggered worker because it is compute-heavy and human-confirmed.
- Settlement remains a synchronous, deterministic library unless group sizes or optimization workloads require a separate compute queue.
- Collaboration updates can add WebSocket fan-out and Redis when concurrent editing becomes a measured need.

The ledger database remains the financial source of truth. Derived balance projections may be cached, but writes must preserve the zero-sum invariant transactionally.
