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

Concrete payloads are added alongside each implemented API surface.

