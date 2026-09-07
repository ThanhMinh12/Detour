# Architecture

## Shape

Detour starts as a modular monolith. Collaboration, expenses, and settlement live in separate packages and transact against one relational database. This keeps the first release deployable as one service while preserving boundaries that can later become asynchronous services if load or team ownership warrants it.

```text
Browser / API client
        |
Spring MVC controllers
        |
Application services
  |          |          |
trips     expenses   settlement (pure algorithm)
        |
Spring Data JPA -> H2 locally / PostgreSQL in production
```

## Important decisions

- Monetary amounts are integer minor units (`long` cents for USD), never floating point.
- An expense persists immutable participant shares. Balances are derived from the ledger, not maintained as mutable pairwise debts.
- Tax and tip allocation uses largest-remainder rounding so every cent is assigned and the result is deterministic.
- Exact minimum-transfer search is bounded to small groups because the general optimization problem is exponential. Larger groups use a deterministic greedy strategy.
- Multi-currency conversion is not silently attempted. Each trip currently has one ISO-4217 currency.
- Authentication and notification delivery are post-MVP concerns; traveler membership is the authorization seam.

## Balance invariant

For every traveler:

```text
balance = expenses paid - assigned expense shares
          + reimbursements sent - reimbursements received
```

Across a trip, balances must always sum to zero.

