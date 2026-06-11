# Project Structure

```
backend/
├── .env                              # Environment variables (secrets — gitignored)
├── .gitignore
├── pom.xml                           # Maven dependencies & build config
├── mvnw / mvnw.cmd                   # Maven wrapper scripts
│
└── src/
    ├── main/
    │   ├── java/com/arqulat/auth/
    │   │   ├── AuthApplication.java              # Spring Boot entry point
    │   │   │
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java            # Spring Security filter chain,
    │   │   │                                      # CORS, auth providers, password encoder
    │   │   │
    │   │   ├── controller/
    │   │   │   └── AuthController.java            # REST API endpoints
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── AuthResponse.java              # Success response (id, email, name)
    │   │   │   ├── ErrorResponse.java             # Standardized error body (Java record)
    │   │   │   ├── LoginRequest.java              # Login payload (email, password)
    │   │   │   └── RegisterRequest.java           # Registration payload (email, password, name)
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── ApiException.java              # Generic business-logic exception
    │   │   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice — maps exceptions
    │   │   │   │                                  # to HTTP responses
    │   │   │   ├── ResourceNotFoundException.java # 404 — user not found
    │   │   │   └── UserAlreadyExistsException.java# 409 — duplicate email on registration
    │   │   │
    │   │   ├── model/
    │   │   │   └── User.java                      # JPA entity — users table
    │   │   │
    │   │   ├── repository/
    │   │   │   └── UserRepository.java            # Spring Data JPA interface
    │   │   │
    │   │   ├── security/
    │   │   │   ├── AppUserDetails.java            # UserDetails adapter wrapping User entity
    │   │   │   ├── AppUserDetailsService.java     # Loads user from DB by email
    │   │   │   ├── JwtAuthenticationFilter.java   # OncePerRequestFilter — extracts JWT
    │   │   │   │                                  # from cookie
    │   │   │   └── OAuth2LoginSuccessHandler.java # Post-Google-login: find/create user,
    │   │   │                                      # set cookie
    │   │   │
    │   │   └── service/
    │   │       ├── AuthService.java               # Registration, login, logout business logic
    │   │       └── JwtService.java                # JWT generation, parsing, validation
    │   │                                          # (HMAC-SHA256)
    │   │
    │   └── resources/
    │       └── application.properties             # Spring config (DB, JWT, OAuth, cookies)
    │
    └── test/
        └── java/com/arqulat/auth/
            └── AuthApplicationTests.java          # Context-load smoke test
```

---

## Package Descriptions

### `com.arqulat.auth`
The root package. Contains only `AuthApplication.java`, the `@SpringBootApplication` entry point.

### `com.arqulat.auth.config`
Spring configuration classes. Currently contains `SecurityConfig` which defines the entire security filter chain, CORS configuration, authentication provider, and password encoder beans.

### `com.arqulat.auth.controller`
REST controllers. `AuthController` exposes all HTTP endpoints. No class-level `@RequestMapping` — each method declares its full path.

### `com.arqulat.auth.dto`
Data Transfer Objects used for request/response serialization:
- **`RegisterRequest`** — Incoming registration payload with `@Valid` constraints
- **`LoginRequest`** — Incoming login payload with email/password validation
- **`AuthResponse`** — Outgoing user profile (id, email, name — never exposes password)
- **`ErrorResponse`** — Java `record` for standardized error bodies

### `com.arqulat.auth.exception`
Custom exception classes and the global exception handler:
- **`ApiException`** — Generic business-logic exception (→ 400)
- **`ResourceNotFoundException`** — Entity not found (→ 404)
- **`UserAlreadyExistsException`** — Duplicate email on registration (→ 409)
- **`GlobalExceptionHandler`** — `@RestControllerAdvice` that catches all exceptions and maps them to consistent `ErrorResponse` JSON

### `com.arqulat.auth.model`
JPA entities. `User` maps to the `auth.users` PostgreSQL table with UUID primary key, email, name, password hash, Google ID, and creation timestamp.

### `com.arqulat.auth.repository`
Spring Data JPA repository interfaces. `UserRepository` extends `JpaRepository<User, UUID>` and provides:
- `findByEmail(String email)` → `Optional<User>`
- `existsByEmail(String email)` → `boolean`
- `findByGoogleId(String googleId)` → `Optional<User>`

### `com.arqulat.auth.security`
Security infrastructure:
- **`AppUserDetails`** — Wraps a `User` entity to implement Spring's `UserDetails` interface
- **`AppUserDetailsService`** — Implements `UserDetailsService` to load users from the database by email
- **`JwtAuthenticationFilter`** — `OncePerRequestFilter` that reads the `arqulat_session` cookie, validates the JWT, and sets the `SecurityContext`
- **`OAuth2LoginSuccessHandler`** — Callback after a successful Google OAuth login. Handles account creation/linking and cookie setting

### `com.arqulat.auth.service`
Business logic:
- **`AuthService`** — Orchestrates registration (duplicate check, BCrypt hash, save), login (delegate to `AuthenticationManager`, generate JWT, set cookie), profile retrieval, and logout (clear cookie)
- **`JwtService`** — Low-level JWT operations: token generation with HMAC-SHA256 signing, claims extraction, username extraction, expiry checking, and validation
