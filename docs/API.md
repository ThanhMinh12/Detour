# API examples

The interactive OpenAPI UI is served at `/docs` when the application is running.

All money fields use integer minor units. For USD, `11700` means `$117.00`.

The end-to-end flow is:

1. `POST /api/trips`
2. `POST /api/trips/{tripId}/members`
3. `POST /api/trips/{tripId}/activities`
4. `POST /api/trips/{tripId}/expenses`
5. `GET /api/trips/{tripId}/balances`
6. `GET /api/trips/{tripId}/settlements?strategy=OPTIMAL`

Trip collaboration also supports invite-code joining, itinerary CRUD, and one vote per traveler per activity. A vote value of `0` clears the traveler's preference without deleting its audit row.

## Itemized receipt

`POST /api/trips/{tripId}/expenses`

```json
{
  "description": "Dinner",
  "paidByMemberId": "<A member UUID>",
  "category": "FOOD",
  "splitMode": "ITEMIZED",
  "occurredOn": "2026-10-03",
  "subtotalCents": 9000,
  "taxCents": 900,
  "tipCents": 1800,
  "items": [
    {"name": "A's order", "amountCents": 2000, "participantIds": ["<A>"]},
    {"name": "B's order", "amountCents": 3000, "participantIds": ["<B>"]},
    {"name": "C's order", "amountCents": 2500, "participantIds": ["<C>"]},
    {"name": "D's order", "amountCents": 1500, "participantIds": ["<D>"]}
  ]
}
```

Shared items list every participant consuming that item. `EXACT` allocations use subtotal cents, `PERCENTAGE` allocations use basis points totaling `10000`, and `EQUAL` ignores allocation values.

The web client builds the same payloads interactively. Itemized mode derives `subtotalCents` from receipt lines and permits any subset of travelers on each shared item.

