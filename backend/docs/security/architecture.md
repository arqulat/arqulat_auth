# Security Architecture

This document details the security mechanisms protecting the Arqulat auth backend.

---

## Cookie Configuration

All authentication state is carried in the `arqulat_session` cookie.

| Attribute | Value | Purpose |
|---|---|---|
| `name` | `arqulat_session` | Cookie name used across all Arqulat services |
| `httpOnly` | `true` | Prevents JavaScript access — protects against XSS attacks |
| `secure` | `true` | Cookie only transmitted over HTTPS |
| `sameSite` | `Lax` | Blocks cross-site POST requests — mitigates CSRF |
| `domain` | Configurable (`COOKIE_DOMAIN`) | Set to `.arqulat.com` in production for subdomain SSO |
| `maxAge` | 604800 seconds (7 days) | Cookie auto-expires after 7 days |
| `path` | `/` | Available on all paths |

### Cookie in Login vs Logout

| Operation | Cookie Value | Max-Age |
|---|---|---|
| Login / OAuth | Signed JWT string | 604800 (7 days) |
| Logout | Empty string `""` | 0 (browser deletes immediately) |

---

## JWT Token

| Property | Value |
|---|---|
| **Algorithm** | HMAC-SHA256 |
| **Library** | io.jsonwebtoken (jjwt) 0.12.6 |
| **Subject (`sub`)** | User's email address |
| **Issued At (`iat`)** | Current timestamp |
| **Expiration (`exp`)** | Current timestamp + 7 days (configurable via `JWT_EXPIRATION`) |
| **JWT ID (`jti`)** | Randomly generated UUID v4 (used for token revocation) |
| **Storage** | `arqulat_session` HttpOnly cookie |
| **Signing Key** | Base64-decoded `JWT_SECRET` → HMAC-SHA key |

### Token Lifecycle

```
Generate                    Validate                     Expire
   │                           │                            │
   │ JwtService.generateToken()│ JwtAuthFilter reads cookie │ After 7 days
   │ → sign with HMAC-SHA256   │ → extractUserName()        │ → isTokenExpired() = true
   │ → set sub, iat, exp, jti  │ → isTokenValid()           │ → user must re-login
   │ → return compact JWT      │ → set SecurityContext      │
   │                           │                            │
```

### Token Validation Checks

`JwtService.isTokenValid(token, userDetails)` performs:

1. **Username match** — The `sub` claim in the JWT must equal `userDetails.getUsername()` (the email).
2. **Expiry check** — The `exp` claim must be in the future.
3. **Blacklist check** — The `jti` claim must **not** exist in the `auth.blacklisted_tokens` database table (ensures tokens revoked on logout are blocked).

---

## Password Hashing

| Property | Value |
|---|---|
| **Algorithm** | BCrypt |
| **Implementation** | Spring Security's `BCryptPasswordEncoder` |
| **Strength** | 10 rounds (default) |
| **Storage** | `password_hash` column in `auth.users` table |

### How It Works

- **Registration:** `passwordEncoder.encode(rawPassword)` → generates a BCrypt hash like `$2a$10$...`
- **Login:** `BCryptPasswordEncoder.matches(rawPassword, storedHash)` → returns `true` if they match
- **Google-only users:** `password_hash` is `NULL` — they cannot use the `/login` endpoint

---

## CORS Policy

Configured in `SecurityConfig.corsConfigurationSource()`:

| Setting | Value |
|---|---|
| **Allowed Origins** | `https://*.arqulat.com`, `http://localhost:*` |
| **Allowed Methods** | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| **Allowed Headers** | `Authorization`, `Content-Type` |
| **Allow Credentials** | `true` (required for cookie-based auth) |

> `allowCredentials=true` is essential. Without it, the browser will not send the `arqulat_session` cookie on cross-origin requests.

---

## Spring Security Filter Chain

The filter chain is defined in `SecurityConfig.securityFilterChain()`:

```
Incoming Request
  │
  ├─▶ 1. CORS Filter (applied first)
  │
  ├─▶ 2. CSRF: DISABLED
  │       (because API uses cookies with SameSite=Lax)
  │
  ├─▶ 3. JwtAuthenticationFilter [CUSTOM]
  │       Reads arqulat_session cookie → validates JWT → sets SecurityContext
  │
  ├─▶ 4. UsernamePasswordAuthenticationFilter (Spring default)
  │
  ├─▶ 5. OAuth2AuthorizationRequestRedirectFilter
  │       Handles GET /oauth2/authorization/google → redirects to Google
  │
  ├─▶ 6. OAuth2LoginAuthenticationFilter
  │       Handles GET /login/oauth2/code/google → exchanges code for token
  │       → calls OAuth2LoginSuccessHandler on success
  │
  ├─▶ 7. Authorization Rules:
  │       /api/v1/auth/register  → permitAll
  │       /api/v1/auth/login     → permitAll
  │       /oauth2/**             → permitAll (implicit via Spring OAuth2)
  │       everything else        → authenticated
  │
  └─▶ 8. Controller handles the request
```

---

## Session Management

| Property | Value | Note |
|---|---|---|
| **Policy** | `SessionCreationPolicy.IF_REQUIRED` | Sessions are created only when needed |

Spring's OAuth2 login flow requires a temporary HTTP session to store the `state` parameter between the redirect to Google and the callback. Once the JWT cookie is set, the session is no longer needed.

> For a fully stateless setup, you would need to implement a custom `AuthorizationRequestRepository` that stores the OAuth2 state in an encrypted cookie instead of a server-side session.

---

## Authentication Provider

```java
DaoAuthenticationProvider
├── UserDetailsService: AppUserDetailsService
│     └── Loads User entity from DB by email
│     └── Wraps it in AppUserDetails (implements UserDetails)
│
└── PasswordEncoder: BCryptPasswordEncoder (10 rounds)
      └── Compares raw password against stored BCrypt hash
```

Used by `AuthenticationManager.authenticate()` during the email/password login flow.
