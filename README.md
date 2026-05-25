# SafePay Schedule API

A production-grade RESTful API for scheduling financial payment transfers with dynamic tax computation, built as a portfolio project demonstrating enterprise Java/Spring Boot patterns.

## Tech Stack

![Java](https://img.shields.io/badge/Java-17-007396?style=flat&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=flat&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker)
![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?style=flat)

## Business Rules

- The scheduled date must be strictly in the future
- The transfer amount must be greater than zero
- **Dynamic regressive tax:** transfers scheduled more than 10 days ahead apply a **2% fee**; within 10 days, a **5% fee** applies

## Architecture

Layered / DDD-Lite with clean separation of concerns:

```
Controller  →  Service  →  Repository
                 ↓
           Domain (Entity, Policy, Exception)
                 ↓
              DTOs (Java Records)
```

| Layer | Responsibility |
|-------|---------------|
| `controller` | HTTP interface, input validation, status codes |
| `service` | Business logic orchestration, transaction boundary |
| `domain/policy` | Pure business rules (framework-free) |
| `domain/entity` | JPA persistence model (Builder pattern) |
| `dto` | Immutable request/response contracts (Java Records) |
| `repository` | Spring Data JPA persistence interface |

## API Reference

### Create a Payment Schedule

```http
POST /api/v1/payment-schedules
Content-Type: application/json

{
  "originAccount": "123456789",
  "destinationAccount": "987654321",
  "amount": 1500.00,
  "scheduledDate": "2026-08-01"
}
```

**Response `201 Created`**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "originAccount": "123456789",
  "destinationAccount": "987654321",
  "amount": 1500.00,
  "tax": 30.00,
  "scheduledDate": "2026-08-01",
  "createdAt": "2026-05-25T14:30:00"
}
```

### List All Payment Schedules

```http
GET /api/v1/payment-schedules
```

**Response `200 OK`** — array of schedules ordered by creation date (most recent first).

### Error Responses

All errors follow a consistent JSON structure:

```json
{
  "timestamp": "2026-05-25T14:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Scheduled date must be strictly in the future.",
  "path": "/api/v1/payment-schedules",
  "violations": []
}
```

| Status | Scenario |
|--------|---------|
| `400` | Missing/invalid fields or malformed JSON |
| `422` | Business rule violation |
| `500` | Unexpected server error |

## Running Locally

**Prerequisites:** Java 17+, Maven 3.8+, Docker

```bash
# Start PostgreSQL
docker compose up -d

# Run the API
mvn spring-boot:run
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Running Tests

```bash
mvn test
```

- `PaymentScheduleServiceTest` — unit tests (Mockito, no Spring context)
- `PaymentScheduleControllerTest` — web slice tests (MockMvc, `@WebMvcTest`)

## Key Design Decisions

**Why `TaxPolicy` is an isolated class?**
It's a pure function with no Spring dependencies — fully testable without a container, and encapsulates the business rule at the domain layer rather than leaking it into the service.

**Why Java Records for DTOs?**
Records are compiler-enforced immutable — no accidental mutation, no setters, and they make the contract between layers explicit and concise.

**Why UUID as primary key?**
Prevents IDOR attacks (OWASP A01:2021). Sequential IDs allow enumeration of resources; UUIDs do not.

**Why `BigDecimal` for monetary values?**
IEEE 754 `double` introduces floating-point representation errors (e.g., `0.1 + 0.2 ≠ 0.3`). `BigDecimal` with `HALF_UP` rounding is the financial industry standard.
