# Configuration Reference

This document describes every configuration property used by the backend.

---

## `application.properties`

| Property | Env Variable | Default | Description |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | Supabase JDBC URL | PostgreSQL connection string with `?currentSchema=auth` |
| `spring.datasource.username` | `DB_USERNAME` | `postgres` | Database username |
| `spring.datasource.password` | `DB_PASSWORD` | — (required) | Database password |
| `spring.datasource.driver-class-name` | — | `org.postgresql.Driver` | JDBC driver class |
| `spring.jpa.properties.hibernate.default_schema` | — | `auth` | PostgreSQL schema name for all tables |
| `spring.jpa.hibernate.ddl-auto` | — | `update` | Schema management strategy (change to `validate` for production) |
| `spring.jpa.show-sql` | — | `true` | Log SQL statements to console (disable in production) |
| `application.security.jwt.secret-key` | `JWT_SECRET` | — (required) | Base64-encoded HMAC-SHA256 signing key (must be ≥ 32 bytes decoded) |
| `application.security.jwt.expiration` | `JWT_EXPIRATION` | `604800000` | JWT lifetime in **milliseconds** (default: 7 days) |
| `app.cookie.max-age` | `COOKIE_MAX_AGE` | `604800` | Cookie lifetime in **seconds** (default: 7 days) |
| `app.frontend.url` | `FRONTEND_URL` | `http://localhost:5173` | Frontend URL for OAuth redirect and CORS origin |
| `app.cookie.domain` | `COOKIE_DOMAIN` | `localhost` | Cookie domain (use `.arqulat.com` in production for subdomain SSO) |
| `spring.security.oauth2.client.registration.google.client-id` | `GOOGLE_CLIENT_ID` | — (required) | Google OAuth 2.0 client ID |
| `spring.security.oauth2.client.registration.google.client-secret` | `GOOGLE_CLIENT_SECRET` | — (required) | Google OAuth 2.0 client secret |
| `spring.security.oauth2.client.registration.google.scope` | — | `email,profile` | OAuth scopes requested from Google |

---

## `.env` File Template

Create a `.env` file in the `backend/` directory (it is gitignored):

```env
# ─── Database ────────────────────────────────────────────────────
DB_URL=jdbc:postgresql://<host>:5432/postgres?currentSchema=auth
DB_USERNAME=postgres
DB_PASSWORD=<your-db-password>

# ─── JWT ─────────────────────────────────────────────────────────
# Must be a Base64-encoded key of at least 32 bytes
JWT_SECRET=<base64-encoded-32-byte-key>

# Optional overrides (defaults shown):
# JWT_EXPIRATION=604800000         # 7 days in milliseconds
# COOKIE_MAX_AGE=604800            # 7 days in seconds
# COOKIE_DOMAIN=localhost
# FRONTEND_URL=http://localhost:5173

# ─── Google OAuth 2.0 ───────────────────────────────────────────
GOOGLE_CLIENT_ID=<your-client-id>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<your-client-secret>
```

---

## How Config is Loaded

1. Spring Boot starts and reads `application.properties`.
2. The directive `spring.config.import=optional:file:.env` imports the `.env` file as additional properties.
3. Properties use `${ENV_VAR:default}` syntax — reads the env variable, falls back to the default if not set.
4. The `optional:` prefix ensures the app doesn't crash if `.env` is missing (useful in production where env vars are injected by the platform).

---

## Generating a JWT Secret

The `JWT_SECRET` must be a **Base64-encoded** string that decodes to at least 32 bytes (256 bits) for HMAC-SHA256.

```bash
# Linux / macOS:
openssl rand -base64 32

# PowerShell (Windows):
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

Copy the output and paste it as the value of `JWT_SECRET` in your `.env` file.

---

## Environment-Specific Overrides

### Development (default)

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
app.cookie.domain=localhost
app.frontend.url=http://localhost:5173
```

### Production (recommended changes)

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
app.cookie.domain=.arqulat.com
app.frontend.url=https://app.arqulat.com
```

> In production, do NOT use a `.env` file. Instead, inject environment variables via your deployment platform (Render, Railway, AWS, etc.).
