# Settlement engine

Detour derives one signed balance per traveler. Positive means the traveler should receive money; negative means the traveler owes money. Suggested transfers never become ledger entries until a reimbursement is recorded.

## Strategies

`GREEDY` sorts creditors and debtors by magnitude and repeatedly matches the largest remaining amounts. It is deterministic, fast, and uses at most `n - 1` transfers, but it does not always minimize the count.

`OPTIMAL` uses depth-first branch-and-bound search over opposite-signed balances. It prunes repeated equivalent balances and starts with the greedy result as its upper bound. The result has the minimum number of transfers. Since this problem is exponential in the worst case, exact search is limited to 12 non-zero balances; larger groups transparently fall back to `GREEDY` and report that fallback in the API response.

## Example

For balances `A +120`, `B -40`, `C +30`, and `D -110`, a valid minimum settlement has three payments. The exact pairings can vary while retaining the same minimum count and fully clearing every balance.

Reimbursements are validated against current balances so a debtor cannot accidentally overpay a creditor.

