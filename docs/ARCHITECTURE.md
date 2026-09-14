# Architecture

## Shape

Detour starts as a modular monolith. Collaboration, expenses, and settlement live in separate packages and transact against one relational database. This keeps the first release deployable as one service while preserving boundaries that can later become asynchronous services if load or team ownership warrants it.

```text
Browser / API client
        |
Spring Security + database-backed session
        |
Spring MVC controllers
        |
Application services + membership authorization
  |          |          |
trips     expenses   settlement (pure algorithm)
        |
Spring Data JPA -> H2 for lightweight local use / PostgreSQL in Docker and production
```

## Important decisions

- Monetary amounts are integer minor units (`long` cents for USD), never floating point.
- An expense persists immutable participant shares. Balances are derived from the ledger, not maintained as mutable pairwise debts.
- Tax and tip allocation uses largest-remainder rounding so every cent is assigned and the result is deterministic.
- Exact minimum-transfer search is bounded to small groups because the general optimization problem is exponential. Larger groups use a deterministic greedy strategy.
- Multi-currency conversion is not silently attempted. Each trip currently has one ISO-4217 currency.
- Local email/password accounts use adaptive BCrypt hashes. Authentication lives in an HTTP-only, SameSite session cookie; Spring Session stores sessions in the relational database so restarts and multiple application tasks share login state.
- A trip member can optionally reference an account. Every trip read and mutation resolves membership from the authenticated account, while an invited email remains a placeholder until that account joins with the invite code.
- Keeping authentication in the existing application avoids a separate identity-service bill and a confusing hosted-login redirect for the beta. Email verification, password recovery, and stronger abuse controls remain required before unrestricted public registration.
- Notification delivery remains a post-MVP concern.

## Balance invariant

For every traveler:

```text
balance = expenses paid - assigned expense shares
          + reimbursements sent - reimbursements received
```

Across a trip, balances must always sum to zero.

Suggested settlements are projections only. Recording a reimbursement adds a ledger entry and changes the derived balances; generating a suggestion never mutates financial history.
