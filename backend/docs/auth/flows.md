# Authentication Flows

This document describes every authentication flow in the system with detailed sequence diagrams.

---

## 1. Email + Password Registration

```
POST /api/v1/auth/register
```

### Sequence Diagram

```
Client                        AuthController                AuthService               Database
  │                                │                            │                        │
  │ POST {email, password, name}   │                            │                        │
  │───────────────────────────────▶│                            │                        │
  │                                │ register(request)          │                        │
  │                                │───────────────────────────▶│                        │
  │                                │                            │ existsByEmail(email)   │
  │                                │                            │───────────────────────▶│
  │                                │                            │◀───────────────────────│
  │                                │                            │                        │
  │                                │                            │ [if exists → throw     │
  │                                │                            │  UserAlreadyExists]    │
  │                                │                            │                        │
  │                                │                            │ BCrypt.encode(password)│
  │                                │                            │ save(user)             │
  │                                │                            │───────────────────────▶│
  │                                │                            │◀───────────────────────│
  │                                │◀───────────────────────────│                        │
  │  201 CREATED {id, email, name} │                            │                        │
  │◀───────────────────────────────│                            │                        │
```

### What Happens

1. Client sends a `POST` request with `email`, `password`, and optional `name`.
2. `@Valid` annotation triggers Jakarta Bean Validation on `RegisterRequest`:
   - Email must be non-blank and properly formatted.
   - Password must be 8-100 characters with at least one uppercase, one lowercase, one digit, and one special character.
3. `AuthService.register()` checks if the email already exists in the database.
4. If duplicate → throws `UserAlreadyExistsException` → 409 Conflict.
5. Otherwise, the password is hashed with BCrypt and the user is saved.
6. Returns `201 Created` with user profile (no password in response).

> **Note:** Registration does NOT auto-login. The client must call `/login` afterwards to receive a JWT cookie.

### Password Validation Rules

| Rule | Constraint |
|---|---|
| Minimum length | 8 characters |
| Maximum length | 100 characters |
| Uppercase letter | At least one (A-Z) |
| Lowercase letter | At least one (a-z) |
| Digit | At least one (0-9) |
| Special character | At least one of `@#$%^&+=*!_?-` |

---

## 2. Email + Password Login

```
POST /api/v1/auth/login
```

### Sequence Diagram

```
Client                   AuthController      AuthService      AuthenticationManager      Database
  │                           │                   │                    │                     │
  │ POST {email, password}    │                   │                    │                     │
  │──────────────────────────▶│                   │                    │                     │
  │                           │ login(req, res)   │                    │                     │
  │                           │──────────────────▶│                    │                     │
  │                           │                   │ authenticate(...)  │                     │
  │                           │                   │───────────────────▶│                     │
  │                           │                   │                    │ loadUserByUsername() │
  │                           │                   │                    │────────────────────▶ │
  │                           │                   │                    │◀────────────────────│
  │                           │                   │                    │ BCrypt.matches()    │
  │                           │                   │◀───────────────────│                     │
  │                           │                   │                    │                     │
  │                           │                   │ generateToken(userDetails)               │
  │                           │                   │ setJwtCookie(response, token)             │
  │                           │◀──────────────────│                    │                     │
  │  200 OK {id, email, name} │                   │                    │                     │
  │  Set-Cookie: arqulat_session=<jwt>            │                    │                     │
  │◀──────────────────────────│                   │                    │                     │
```

### What Happens

1. Client sends `email` and `password`.
2. `@Valid` triggers validation on `LoginRequest`.
3. `AuthService.login()` delegates to Spring's `AuthenticationManager`.
4. The `AuthenticationManager` uses `AppUserDetailsService.loadUserByUsername(email)` to fetch the user from the DB.
5. It then compares the raw password against the BCrypt hash via `BCryptPasswordEncoder.matches()`.
6. If credentials are invalid → `BadCredentialsException` → 401 Unauthorized.
7. On success, a JWT is generated and set as an `arqulat_session` HttpOnly cookie.
8. Returns `200 OK` with user profile.

### Cookie Set on Login

| Attribute | Value |
|---|---|
| Name | `arqulat_session` |
| Value | Signed JWT |
| Domain | Configurable (`COOKIE_DOMAIN`) |
| Path | `/` |
| HttpOnly | `true` |
| Secure | `true` |
| SameSite | `Lax` |
| Max-Age | 604800 seconds (7 days) |

---

## 3. Google OAuth 2.0 Login

### Flow Overview

```
┌──────────┐     ┌──────────────┐     ┌─────────────┐     ┌───────────────────────────┐
│  Client  │────▶│ Spring OAuth │────▶│   Google    │────▶│ OAuth2LoginSuccessHandler │
│          │     │   /oauth2/   │     │  Consent    │     │ (find-or-create user,     │
│          │◀────│ authorization│◀────│  Screen     │◀────│  set JWT cookie,          │
│ (cookie  │     │  /google     │     │             │     │  redirect to frontend)    │
│  is set) │     └──────────────┘     └─────────────┘     └───────────────────────────┘
└──────────┘
```

### Detailed Steps

1. **Client** navigates to `GET /oauth2/authorization/google`.
2. **Spring Security** generates an OAuth2 authorization URL with the configured `client-id`, `scope=email,profile`, and a `state` parameter (stored in the server session).
3. Browser is **redirected to Google's consent screen**.
4. User approves → Google redirects back to `GET /login/oauth2/code/google?code=...&state=...`.
5. Spring exchanges the authorization code for an access token and fetches user info (email, sub, name).
6. `OAuth2LoginSuccessHandler.onAuthenticationSuccess()` fires.

### Account Resolution Logic

The handler resolves the user account in this priority order:

| Step | Lookup | Condition | Action |
|---|---|---|---|
| 1 | `findByGoogleId(sub)` | Google ID exists in DB | Use existing account (returning Google user) |
| 2 | `findByEmail(email)` | Email exists but no Google ID | **Link** the Google ID to the existing account |
| 3 | Neither found | — | **Create** a brand-new user with email + googleId + name |

### Guard Clause

If Google does not provide `email` or `sub` (unlikely but possible), the handler redirects to:
```
{frontendUrl}?error=oauth2_missing_attributes
```

### After Resolution

1. Wraps the `User` in `AppUserDetails`.
2. Generates a JWT via `JwtService.generateToken()`.
3. Sets the `arqulat_session` cookie (same attributes as login).
4. **Redirects** the browser to `app.frontend.url`.

---

## 4. Authenticated Request (JWT Validation)

Every request to a protected endpoint goes through the `JwtAuthenticationFilter`.

### Sequence Diagram

```
Client                     JwtAuthFilter        JwtService       UserDetailsService    SecurityContext
  │                             │                    │                  │                     │
  │ GET /api/v1/user/me         │                    │                  │                     │
  │ Cookie: arqulat_session=xyz │                    │                  │                     │
  │────────────────────────────▶│                    │                  │                     │
  │                             │ extract cookie     │                  │                     │
  │                             │ extractUserName()  │                  │                     │
  │                             │───────────────────▶│                  │                     │
  │                             │◀───────────────────│                  │                     │
  │                             │                    │                  │                     │
  │                             │ loadUserByUsername(email)             │                     │
  │                             │─────────────────────────────────────▶│                     │
  │                             │◀─────────────────────────────────────│                     │
  │                             │                    │                  │                     │
  │                             │ isTokenValid()     │                  │                     │
  │                             │───────────────────▶│                  │                     │
  │                             │◀───────────────────│                  │                     │
  │                             │                    │                  │                     │
  │                             │ setAuthentication(userDetails)       │                     │
  │                             │──────────────────────────────────────────────────────────── ▶│
  │                             │                    │                  │                     │
  │  200 OK {id, email, name}   │                    │                  │                     │
  │◀────────────────────────────│                    │                  │                     │
```

### What Happens

1. The filter checks for cookies on the request.
2. Looks for a cookie named `arqulat_session`.
3. If found, extracts the JWT value and calls `jwtService.extractUserName(token)`.
4. If extraction fails (expired, malformed, invalid signature) → the exception is caught silently, and the user remains **unauthenticated** (no 401 thrown at this stage).
5. If a valid username is extracted AND no authentication exists in the current `SecurityContext`:
   - Loads `AppUserDetails` from the database via `AppUserDetailsService`.
   - Calls `jwtService.isTokenValid(token, userDetails)` — checks username match AND expiry.
   - If valid → sets a `UsernamePasswordAuthenticationToken` in the `SecurityContext`.
6. The filter chain continues. If the endpoint requires authentication and the `SecurityContext` has no authentication → Spring returns `401 Unauthorized`.

---

## 5. Logout

```
POST /api/v1/user/logout
```

### What Happens

1. `AuthService.logout()` calls `setJwtCookie(response, "", 0)`.
2. This sets the `arqulat_session` cookie to an empty string with `maxAge=0`.
3. The browser deletes the cookie immediately.
4. Returns `200 OK` with `"Logged out successfully"`.

> **Important:** The JWT itself is NOT invalidated server-side. If the token was intercepted before logout, it remains valid until its natural expiry (7 days). See [Known Issues](../known-issues.md) for details.
