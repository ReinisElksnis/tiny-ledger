# Tiny Ledger

A small REST API for a multi-currency ledger. A customer holds at most one account per currency; deposits and
withdrawals move the account balance and are kept as transaction history.

## Stack

- Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- H2 in-memory database, schema managed by Flyway
- springdoc-openapi for the API documentation
- JUnit 5, Mockito, MockMvc

## Running

Requires JDK 25. The Gradle wrapper is included.

```bash
# empty database
./gradlew bootRun

# with demo data
./gradlew bootRun --args='--spring.profiles.active=demo'
```

The application listens on `http://localhost:8080`. The database is in memory, so all data is lost on restart.

## API documentation

With the application running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs

## Endpoints

All paths are under `/api/v1`.

| Method | Path | Description |
|---|---|---|
| `POST` | `/customers` | Create a customer |
| `GET` | `/customers` | List all customers with their accounts |
| `GET` | `/customers/{customerId}` | Get a customer |
| `POST` | `/customers/{customerId}/accounts` | Open an account in a currency |
| `GET` | `/customers/{customerId}/accounts` | List a customer's accounts |
| `GET` | `/customers/{customerId}/accounts/{currency}` | Get the account and balance for a currency |
| `POST` | `/customers/{customerId}/accounts/{currency}/transactions` | Deposit or withdraw |
| `GET` | `/customers/{customerId}/accounts/{currency}/transactions` | Transaction history, newest first |

Request bodies:

```jsonc
// POST /customers
{ "name": "Anna Ozola" }

// POST /customers/{customerId}/accounts
{ "currency": "EUR" }

// POST /customers/{customerId}/accounts/{currency}/transactions
{ "type": "DEPOSIT", "amount": 100.50, "description": "Salary" }   // type: DEPOSIT | WITHDRAWAL, description optional
```

### Rules

- Currencies are ISO 4217 codes and are case-insensitive (`eur` and `EUR` are the same account).
- A customer can have only one account per currency.
- Amounts must be greater than zero and may not have more decimal places than the currency allows:
  2 for EUR, 0 for JPY, 3 for KWD.
- A withdrawal may not exceed the balance; withdrawing the entire balance is allowed.
- Amounts in responses are always shown with the currency's decimal places (`100.50` EUR, `1000` JPY).

### Errors

Errors are returned as [problem details](https://www.rfc-editor.org/rfc/rfc9457). Business errors carry an extra
`reason` field; validation errors carry an `errors` object with one message per invalid field.

| Status | Reason | When |
|---|---|---|
| 400 | `INVALID_CURRENCY` | The currency is not an ISO 4217 code |
| 400 | `INVALID_AMOUNT` | The amount is not positive or has too many decimal places |
| 400 | – | Request validation failed (see `errors`), malformed JSON or id |
| 404 | `CUSTOMER_NOT_FOUND` | The customer does not exist |
| 404 | `ACCOUNT_NOT_FOUND` | The customer has no account in that currency |
| 409 | `ACCOUNT_ALREADY_EXISTS` | The customer already has an account in that currency |
| 422 | `INSUFFICIENT_FUNDS` | The withdrawal exceeds the balance |
| 500 | – | Unexpected error; details are logged, not returned |

```json
{
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "Cannot withdraw 500 EUR: balance is 100.500 EUR",
  "instance": "/api/v1/customers/00000000-0000-7000-8000-000000000001/accounts/EUR/transactions",
  "reason": "INSUFFICIENT_FUNDS"
}
```

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/customers/00000000-0000-7000-8000-000000000001/accounts/EUR/transactions",
  "errors": {
    "amount": "must not be null",
    "type": "must not be null"
  }
}
```

## Demo data

The `demo` profile loads `src/main/resources/db/demo/R__demo_data.sql` on startup. Ids are fixed so they can be
used directly.

| Customer | Id | Accounts |
|---|---|---|
| Anna Ozola | `00000000-0000-7000-8000-000000000001` | EUR 1250.50 (3 transactions), JPY 50000 (1 transaction) |
| Marta Liepa | `00000000-0000-7000-8000-000000000002` | KWD 12.345 (2 transactions), EUR 0.00 (no transactions) |

Try it:

```bash
BASE=http://localhost:8080/api/v1/customers
ANNA=00000000-0000-7000-8000-000000000001

# all customers with their accounts
curl $BASE

# accounts and balances
curl $BASE/$ANNA/accounts

# transaction history
curl $BASE/$ANNA/accounts/EUR/transactions

# withdraw 50.50 EUR
curl -X POST $BASE/$ANNA/accounts/EUR/transactions \
  -H 'Content-Type: application/json' \
  -d '{"type": "WITHDRAWAL", "amount": 50.50, "description": "Dinner"}'

# rejected: yen has no decimal places
curl -X POST $BASE/$ANNA/accounts/JPY/transactions \
  -H 'Content-Type: application/json' \
  -d '{"type": "DEPOSIT", "amount": 10.5}'

# open a new account
curl -X POST $BASE/$ANNA/accounts \
  -H 'Content-Type: application/json' \
  -d '{"currency": "USD"}'
```

## Tests

```bash
./gradlew test
```

- **Unit tests** (`*ServiceTests`, `*ControllerTests`, `ConverterTests`, `CurrencyUtilsTests`) use Mockito and run
  without a Spring context. Controller tests use a standalone MockMvc with mocked services.
- **Integration tests** (`*IntegrationTests`) start the application against the in-memory database.

## Project structure

```
src/main/java/lv/reinis/tinyledger
├── config        OpenAPI configuration
├── controller    REST controllers and the exception handler
├── converter     Entity to DTO converters
├── domain        JPA entities
├── dto           Request and response records
├── exception     Business exceptions
├── repository    Spring Data repositories
├── service       Business logic
└── util          Helpers

src/main/resources/db
├── migration     Flyway schema
└── demo          Demo data (demo profile only)
```

## Limitations

- No authentication or authorisation.
- No protection against concurrent updates of the same account: two simultaneous withdrawals could both pass the
  balance check.
