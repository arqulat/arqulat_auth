# Architecture Overview

## System Summary

This backend is a **stateless authentication micro-service** that handles user registration, login, and session management for the Arqulat platform. It supports two authentication strategies:

- **Email + Password** — Traditional credentials-based registration and login.
- **Google OAuth 2.0** — Social login via Google, with automatic account linking.

Authentication state is maintained via a **signed JWT** stored in an `HttpOnly` secure cookie (`arqulat_session`). There is no server-side session store — the JWT itself is the session.

---

## High-Level Request Flow

```
┌──────────┐       ┌──────────────────────┐       ┌──────────────────┐
│  Client  │──────▶│   JwtAuthFilter      │──────▶│  SecurityConfig  │
│ (Cookie) │       │ (extracts & validates │       │  (authorization  │
│          │       │  JWT from cookie)     │       │   rules)         │
│          │       └──────────────────────┘       └────────┬─────────┘
│          │                                               │
│          │       ┌──────────────────────┐                │
│          │◀──────│   AuthController     │◀───────────────┘
│          │       │  (REST endpoints)    │
│          │       └──────────┬───────────┘
│          │                  │
│          │       ┌──────────▼───────────┐       ┌────────────────┐
│          │       │    AuthService       │──────▶│ UserRepository │
│          │       │  (business logic)    │       │   (JPA/SQL)    │
│          │       └──────────┬───────────┘       └───────┬────────┘
│          │                  │                           │
│          │       ┌──────────▼───────────┐       ┌───────▼────────┐
│          │       │     JwtService       │       │   PostgreSQL   │
│          │       │  (token generation   │       │   (Supabase)   │
│          │       │   & validation)      │       └────────────────┘
│          │       └──────────────────────┘
└──────────┘
```

---

## Google OAuth Flow

```
┌──────────┐     ┌──────────────┐     ┌─────────────┐     ┌───────────────────────────┐
│  Client  │────▶│ Spring OAuth │────▶│   Google    │────▶│ OAuth2LoginSuccessHandler │
│          │     │   /oauth2/   │     │  Consent    │     │ (find-or-create user,     │
│          │◀────│ authorization│◀────│  Screen     │◀────│  set JWT cookie,          │
│ (cookie  │     │  /google     │     │             │     │  redirect to frontend)    │
│  is set) │     └──────────────┘     └─────────────┘     └───────────────────────────┘
└──────────┘
```

---

## Component Responsibilities

| Component | Layer | Responsibility |
|---|---|---|
| `SecurityConfig` | Config | Defines the Spring Security filter chain, CORS policy, auth providers, and password encoder |
| `JwtAuthenticationFilter` | Security Filter | Intercepts every request, extracts the JWT from the `arqulat_session` cookie, validates it, and populates the `SecurityContext` |
| `OAuth2LoginSuccessHandler` | Security Handler | Post-Google-login callback: resolves or creates the user, generates a JWT, sets the cookie, redirects to frontend |
| `AuthController` | Controller | Exposes REST endpoints for register, login, logout, and profile retrieval |
| `AuthService` | Service | Core business logic for registration (hashing, duplicate checks), login (delegation to `AuthenticationManager`), and cookie management |
| `JwtService` | Service | JWT generation (HMAC-SHA256 signing), parsing, claims extraction, and expiry validation |
| `AppUserDetailsService` | Security | Implements Spring's `UserDetailsService` — loads a `User` entity from the DB by email and wraps it in `AppUserDetails` |
| `AppUserDetails` | Security | Adapter that wraps the `User` JPA entity into Spring Security's `UserDetails` contract |
| `UserRepository` | Repository | Spring Data JPA interface for `User` entity — provides `findByEmail`, `existsByEmail`, `findByGoogleId` |
| `GlobalExceptionHandler` | Exception | `@RestControllerAdvice` that maps application exceptions to standardized HTTP error responses |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Security | Spring Security + OAuth2 Client |
| ORM | Hibernate / Spring Data JPA |
| Database | PostgreSQL (Supabase) |
| JWT | io.jsonwebtoken (jjwt) 0.12.6 |
| Validation | Jakarta Bean Validation |
| Build | Maven (with wrapper) |
| Boilerplate | Lombok |
