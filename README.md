## Payment Processing API Design Exercise

### Goals
* Consistency - manual "two-phase commit" and optimistic locking per-account, thus all events for account are linearized
* Scalability - no restrictions on scaling the solution to process billions of messages
* Precision - amounts are stored as millis as integers (multiplied by 1000) to avoid floating point precision loss
* Simplicity - no proper logging, no proper persistence, no API versioning and isolated DTOs

### Shortcuts
* No security (neither authentication nor authorization)
* No API to create users/accounts - only hard-coded ones will be available
* No currency conversion - transfer is only accepted if source and destination accounts' currencies match
* No incident management draft, transaction log, reporting, metrics, fraud detection, you name it

### API

1. Get balance for account by account number

```
GET /balance/:nr
> 123450
```

2. Execute transfer from source (src) to destination (dst) account by amount (amt)

```
POST /transfer/:src/:dst/:amt
-
< 200 OK - transfer completed
< 400 Bad Request - amount is <= 0
< 404 Not Found - invalid account number
```