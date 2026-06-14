# Testing Guide

This document explains the testing infrastructure for the Arqulat auth backend, how the tests work, and the logic behind them.

---

## 1. Testing Infrastructure Overview

The application uses standard Spring Boot testing libraries:
- **JUnit 5 (Jupiter):** The core testing framework used for writing and executing tests.
- **Mockito:** Used for mocking dependencies (like databases or external services) in unit tests to isolate the code being tested.
- **Spring Boot Test (`@SpringBootTest`):** Used for integration tests to load the complete Spring application context.
- **MockMvc:** Used to simulate HTTP requests to the controllers without needing to start a real embedded Tomcat server.
- **H2 In-Memory Database:** Used exclusively during integration testing so we don't accidentally write test data to the live Supabase PostgreSQL database.

---

## 2. Types of Tests Implemented

### A. Unit Tests (Isolated Logic)
Unit tests focus on a single class or method, replacing external dependencies (like repositories) with "mocks" using Mockito. These are extremely fast.

1. **`JwtServiceTest`**
   - **Logic Tested:** Ensures tokens are generated correctly, contain the right email (`subject`), expire after the configured time, and that the app rejects expired tokens or tokens belonging to different users.
   - **How it works:** We mock the `UserDetails` object and use Java reflection to inject dummy values for `JWT_SECRET` and `JWT_EXPIRATION` into the service before running tests.

2. **`AuthServiceTest`**
   - **Logic Tested:** Tests the core business rules. For example, it ensures an exception is thrown if a user tries to register with an already existing email. It also ensures passwords are hashed before being saved.
   - **How it works:** We mock the `UserRepository`, `PasswordEncoder`, and `AuthenticationManager`. When `AuthService` calls `userRepository.existsByEmail()`, Mockito intercepts the call and returns a predetermined `true` or `false` to see how `AuthService` reacts.

3. **`RegisterRequestValidationTest`**
   - **Logic Tested:** Specifically tests the strict `@Pattern` regex on the password field.
   - **How it works:** It uses standard `ValidatorFactory` to programmatically validate custom `RegisterRequest` objects, asserting that passwords without an uppercase letter, lowercase letter, number, or special character are rejected *before* they ever reach the controller.

### B. Integration Tests (Full Flow)
Integration tests verify that all the pieces (Controllers, Services, Repositories, Database, Security Filters) work together correctly.

1. **`AuthControllerIntegrationTest`**
   - **Logic Tested:** End-to-end simulation of a user registering, logging in, and fetching their profile.
   - **How it works:** 
     - It uses `@SpringBootTest` to boot up the entire application.
     - It connects to an isolated **H2 in-memory database** (configured via `src/test/resources/application.properties`).
     - We use `MockMvc` to send a fake HTTP POST request to `/api/v1/auth/register` with JSON.
     - We then assert that the HTTP status is `201 Created` or `400 Bad Request` and verify the database state.
     - *Note on Endpoints:* Because the `register` and `login` endpoints are currently commented out in the actual controller (pending OTP implementation), their respective tests are annotated with `@Disabled` so the build doesn't fail.

---

## 3. The Test Database (H2)

To prevent tests from modifying real data, we use the H2 database. When you run tests, Spring looks for `src/test/resources/application.properties` and uses it *instead* of the main `application.properties`.

**Key configurations in test properties:**
```properties
# Connects to an in-memory database that is wiped immediately after tests finish
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS auth

# A dummy secret key so the Spring Context boots without needing a real .env file
application.security.jwt.secret-key=dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIZW0=
```

> **Why `MODE=PostgreSQL`?** H2 normally converts schema and table names to UPPERCASE. By enabling PostgreSQL mode, we force it to respect the lowercase `auth.users` schema definition used in production.

---

## 4. Key Takeaways from Our Implementation

1. **Security-First Approach:** We tested the manual registration and login endpoints thoroughly. Once we confirmed they worked perfectly, we disabled them (`/* commented out */`). We did this to prevent a critical "pre-account takeover" vulnerability where an attacker registers an email before the real owner logs in via Google.
2. **Handling API Authentication Errors:** Since this is an API, we configured `SecurityConfig` to return a `401 Unauthorized` response via `HttpStatusEntryPoint` when an unauthenticated request hits a secured endpoint (like `/api/v1/user/me`), rather than defaulting to a 302 redirect to the OAuth2 login page. The test `getCurrentUser_shouldReturn401_WhenNoCookieProvided` explicitly asserts this behavior.

---

## 5. How to Run Tests

To execute the test suite locally, run the following command in your terminal:

```bash
# Windows
.\mvnw.cmd test

# Linux/Mac
./mvnw test
```

Maven will compile the code, start the H2 database, execute all unit and integration tests, and output a summary of failures/successes.
