# API Reference — Endpoints

## Base URL

```
http://localhost:8080   (development)
https://api.arqulat.com (production — when deployed)
```

---

## Public Endpoints (No Authentication Required)

### `POST /api/v1/auth/register`

Register a new user with email and password.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "MyStr0ng!Pass",
  "name": "John Doe"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `email` | string | ✅ | Must be a valid email format |
| `password` | string | ✅ | 8-100 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char |
| `name` | string | ❌ | Optional display name |

**Responses:**

| Status | Body | Description |
|---|---|---|
| `201 Created` | `AuthResponse` | User created successfully |
| `400 Bad Request` | Validation errors map | Input validation failed |
| `409 Conflict` | `ErrorResponse` | Email already registered |

**Example — Success:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "John Doe"
}
```

**Example — Validation Error:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "email": "Must be a valid email address",
  "password": "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
}
```

**Example — Duplicate Email:**
```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "timestamp": "2026-06-11T15:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Email is already registered",
  "path": "/api/v1/auth/register"
}
```

---

### `POST /api/v1/auth/login`

Authenticate with email and password. Sets a JWT cookie on success.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "MyStr0ng!Pass"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `email` | string | ✅ | Must be a valid email format |
| `password` | string | ✅ | Cannot be blank |

**Responses:**

| Status | Headers | Body | Description |
|---|---|---|---|
| `200 OK` | `Set-Cookie: arqulat_session=<jwt>` | `AuthResponse` | Login successful |
| `400 Bad Request` | — | Validation errors map | Input validation failed |
| `401 Unauthorized` | — | `ErrorResponse` | Invalid email or password |

**Example — Success:**
```http
HTTP/1.1 200 OK
Content-Type: application/json
Set-Cookie: arqulat_session=eyJhbGciOiJIUzI1NiJ9...; Path=/; Domain=localhost; Max-Age=604800; HttpOnly; Secure; SameSite=Lax

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "John Doe"
}
```

**Example — Bad Credentials:**
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "timestamp": "2026-06-11T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password.",
  "path": "/api/v1/auth/login"
}
```

---

### `GET /oauth2/authorization/google`

Initiates the Google OAuth 2.0 login flow. **This is NOT a JSON API** — it redirects the browser to Google's consent screen.

| Step | What Happens |
|---|---|
| 1 | Browser is redirected to Google consent screen |
| 2 | User approves → Google redirects back with auth code |
| 3 | Spring exchanges code for user info |
| 4 | `OAuth2LoginSuccessHandler` creates/links account |
| 5 | JWT cookie is set |
| 6 | Browser is redirected to `app.frontend.url` |

**On Error:**
```
Redirect → {frontendUrl}?error=oauth2_missing_attributes
```

---

## Protected Endpoints (JWT Cookie Required)

All endpoints below require a valid `arqulat_session` cookie. Requests without a valid cookie receive a `401 Unauthorized` response.

### `GET /api/v1/user/me`

Get the currently authenticated user's profile.

**Request:**
```http
GET /api/v1/user/me
Cookie: arqulat_session=eyJhbGciOiJIUzI1NiJ9...
```

**Responses:**

| Status | Body | Description |
|---|---|---|
| `200 OK` | `AuthResponse` | Current user's profile |
| `401 Unauthorized` | — | Missing or invalid JWT cookie |
| `404 Not Found` | `ErrorResponse` | User in JWT no longer exists in DB |

**Example — Success:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "John Doe"
}
```

---

### `POST /api/v1/user/logout`

Log out the current user by clearing the session cookie.

**Request:**
```http
POST /api/v1/user/logout
Cookie: arqulat_session=eyJhbGciOiJIUzI1NiJ9...
```

**Responses:**

| Status | Headers | Body | Description |
|---|---|---|---|
| `200 OK` | `Set-Cookie: arqulat_session=; Max-Age=0` | `"Logged out successfully"` | Cookie cleared |

---

## Response Schemas

### `AuthResponse`

Returned on successful registration, login, and profile retrieval.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "John Doe"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | UUID string | Unique user identifier |
| `email` | string | User's email address |
| `name` | string (nullable) | Display name. May be `null` if not provided |

### `ErrorResponse`

Returned on all error conditions (except validation errors).

```json
{
  "timestamp": "2026-06-11T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password.",
  "path": "/api/v1/auth/login"
}
```

| Field | Type | Description |
|---|---|---|
| `timestamp` | ISO 8601 datetime | When the error occurred |
| `status` | integer | HTTP status code |
| `error` | string | Error category |
| `message` | string | Human-readable description |
| `path` | string | The request URI that caused the error |

### Validation Error Map

Returned when `@Valid` input validation fails. A flat map of field name → error message.

```json
{
  "email": "Must be a valid email address",
  "password": "Password must be between 8 and 100 characters"
}
```
