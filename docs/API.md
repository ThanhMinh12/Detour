# API examples

The interactive OpenAPI UI is served at `/docs` when the application is running.

All money fields use integer minor units. For USD, `11700` means `$117.00`.

## Authentication

Detour uses an HTTP-only `DETOUR_SESSION` cookie backed by the database. Start a browser-like API session by fetching `GET /api/auth/csrf`, then include the returned token in its named header on every `POST`, `PUT`, or `DELETE` request.

Create an account with `POST /api/auth/register`:

```json
{
  "displayName": "Alex",
  "email": "alex@example.com",
  "password": "at-least-8-characters"
}
```

Existing accounts use `POST /api/auth/login` with `email` and `password`. Both endpoints authenticate the new session. `GET /api/auth/me` returns the current account and `POST /api/auth/logout` invalidates the session. Clients must retain cookies across requests.

All trip endpoints require authentication and enforce joined membership. Creating a trip automatically makes the current account its organizer. Joining with `POST /api/trips/join/{inviteCode}` uses the current account and takes no request body. Activity proposals and votes likewise infer the member from the session instead of trusting a client-supplied identity.

The end-to-end flow is:

1. `GET /api/auth/csrf`, then `POST /api/auth/register` or `/login`
2. `POST /api/trips`
3. `POST /api/trips/{tripId}/members`
4. `POST /api/trips/{tripId}/activities`
5. `POST /api/trips/{tripId}/expenses`
6. `GET /api/trips/{tripId}/balances`
7. `GET /api/trips/{tripId}/settlements?strategy=OPTIMAL`

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

Suggested transfers can be accepted from the web client or recorded with `POST /api/trips/{tripId}/reimbursements`. This creates a ledger entry; requesting `/settlements` alone never changes balances.
