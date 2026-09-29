---
name: backend-engineering
description: Standards and guidelines for building robust, secure, and high-performance Spring Boot enterprise backend services. Covers transactional boundaries, JPA performance, security, and API design.
---

# Backend Engineering Best Practices

This skill guides the design, implementation, and maintenance of enterprise-grade backend services with Spring Boot and relational databases.

## 1. Architectural Integrity & Layering
Strictly maintain single-directional dependency flow:
`Controller -> Service -> Repository -> Database Entity`

- **Controllers**: Keep thin. Only handle HTTP serialization, query parameter validation (`@Valid`), status codes, and security annotations. Never invoke repositories or business calculations directly.
- **Services**: Encapsulate 100% of business logic, state transitions, validation, and authorization checks. Methods represent distinct use cases.
- **Repositories**: Exclusively handle persistence, filtering, and query execution.
- **DTOs**: Keep decoupled from JPA entities. Never expose internal entities directly in public API responses.

## 2. Transaction Management & ACID Guarantees
- Use `@Transactional(readOnly = true)` by default on query services to avoid unnecessary dirty checks and improve DB performance.
- Use explicit `@Transactional(rollbackFor = Exception.class)` on mutative workflows that modify more than one table or perform multi-step operations.
- Avoid external I/O (network calls, SMTP sending, file uploads) inside active DB transactions to prevent thread and connection pool starvation. Dispatch external calls after committing or asynchronously.

## 3. JPA & Database Performance
- **N+1 Problem**: Never load related collections lazily in loops. Use `JOIN FETCH`, `@EntityGraph`, or explicit DTO projections for read operations.
- **Pagination**: All list queries exposed to clients MUST be paginated (`Pageable`) with enforced upper limits (e.g., max 100 items per page).
- **Indexing**: Always index foreign keys, status columns, unique constraints, and high-cardinality search attributes (e.g., username, code, email).
- **Batch Processing**: Use batch inserts/updates (`rewriteBatchedStatements=true`) when dealing with bulk data.

## 4. Error Handling & Standard API Responses
- Centralize exception handling using `@RestControllerAdvice`.
- Adhere to HTTP status semantics:
  - `200 OK`: Successful read/update.
  - `201 Created`: Successful creation with resource payload.
  - `400 Bad Request`: Validation failure or semantic business rule violation.
  - `401 Unauthorized`: Missing or invalid authentication token.
  - `403 Forbidden`: Authenticated user lacks sufficient privileges/roles.
  - `404 Not Found`: Resource ID does not exist.
  - `409 Conflict`: Unique constraint violation (e.g., duplicate username/email).
- Return structured error payloads with clear machine-readable error codes and localized error messages. Never expose internal stack traces to clients.

## 5. Security & Data Protection
- Validate all incoming user input at the API boundary using `jakarta.validation` annotations (`@NotBlank`, `@Size`, `@Pattern`, `@Email`).
- Passwords must be hashed using strong one-way cryptographic algorithms (`BCryptPasswordEncoder` with strength >= 12).
- Apply Method Security (`@PreAuthorize("hasRole('...')")`) on sensitive business methods, verifying both role permissions and resource ownership.
- Never log sensitive credentials, passwords, or PII (Personally Identifiable Information).
