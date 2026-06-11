# Error Handling

All exceptions are caught and mapped to standardized HTTP responses by `GlobalExceptionHandler`, a `@RestControllerAdvice` class.

---

## Exception → HTTP Status Mapping

| Exception | HTTP Status | Error Label | Message |
|---|---|---|---|
| `UserAlreadyExistsException` | `409 Conflict` | Conflict | "Email is already registered" |
| `ResourceNotFoundException` | `404 Not Found` | Not Found | Dynamic message (e.g., "User not found") |
| `BadCredentialsException` | `401 Unauthorized` | Unauthorized | "Invalid email or password." |
| `MethodArgumentNotValidException` | `400 Bad Request` | — | Field-level error map |
| `DataIntegrityViolationException` | `409 Conflict` | Database Conflict | "A database conflict occurred, likely a duplicate entry." |
| `ApiException` | `400 Bad Request` | Bad Request | Dynamic message |
| `Exception` (catch-all) | `500 Internal Server Error` | Internal Server Error | "Oops! Something went wrong on our end." |

---

## Response Formats

### Standard Error Response

All errors (except validation) return the `ErrorResponse` format:

```json
{
  "timestamp": "2026-06-11T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password.",
  "path": "/api/v1/auth/login"
}
```

### Validation Error Response

`@Valid` failures return a flat map of field → message:

```json
{
  "email": "Must be a valid email address",
  "password": "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
}
```

> **Note:** This format is different from `ErrorResponse`. Validation errors return `Map<String, String>` for easy frontend form binding.

---

## Exception Hierarchy

```
RuntimeException
├── ApiException                    → 400 Bad Request
├── ResourceNotFoundException       → 404 Not Found
├── UserAlreadyExistsException      → 409 Conflict
│
├── BadCredentialsException         → 401 Unauthorized  (Spring Security)
├── DataIntegrityViolationException → 409 Conflict      (Spring Data)
└── MethodArgumentNotValidException → 400 Bad Request   (Spring Validation)
```

---

## Logging

- The **catch-all `Exception` handler** logs the full stack trace via SLF4J:
  ```
  log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
  ```
- All other handlers do NOT log (the error is expected and communicated to the client).
- The generic "Oops! Something went wrong on our end." message ensures no internal details leak to clients.
