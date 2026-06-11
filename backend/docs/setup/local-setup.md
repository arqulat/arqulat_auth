# Running Locally

Step-by-step guide to get the backend running on your local machine.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 17 or higher |
| Maven | Included via `mvnw` / `mvnw.cmd` wrapper (no install needed) |
| PostgreSQL | Supabase free tier or local instance |
| Google Cloud Console | OAuth 2.0 credentials configured |

---

## Setup Steps

### 1. Clone the Repository

```bash
git clone <repo-url>
cd arqulat_auth/backend
```

### 2. Create the `.env` File

Create a file called `.env` in the `backend/` directory:

```env
DB_URL=jdbc:postgresql://<host>:5432/postgres?currentSchema=auth
DB_USERNAME=postgres
DB_PASSWORD=<your-db-password>

JWT_SECRET=<base64-encoded-32-byte-key>

GOOGLE_CLIENT_ID=<your-client-id>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<your-client-secret>
```

> See [Configuration Reference](../config/reference.md) for detailed explanations of each variable and how to generate a JWT secret.

### 3. Run the Application

```bash
# Windows:
.\mvnw.cmd spring-boot:run

# Linux / macOS:
./mvnw spring-boot:run
```

### 4. Verify

The server starts on **http://localhost:8080**.

Test with:
```bash
curl http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!@","name":"Test User"}'
```

Expected: `201 Created` with the user JSON.

---

## Google OAuth — Local Setup

For the Google OAuth flow to work on localhost:

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Credentials**.
2. Select your OAuth 2.0 Client ID (or create one).
3. Add to **Authorized Redirect URIs:**
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
4. Add to **Authorized JavaScript Origins:**
   ```
   http://localhost:8080
   ```
5. Save and wait a few minutes for changes to propagate.

### Test the OAuth Flow

1. Open your browser and navigate to:
   ```
   http://localhost:8080/oauth2/authorization/google
   ```
2. You should be redirected to Google's consent screen.
3. After approving, you'll be redirected to `http://localhost:5173` (your frontend URL) with the `arqulat_session` cookie set.

---

## Common Issues

### Port already in use

```
Web server failed to start. Port 8080 was already in use.
```

**Fix:** Kill the process using port 8080, or add to `application.properties`:
```properties
server.port=8081
```

### `.env` not found / properties not loading

The app should still start (`.env` is imported with `optional:` prefix), but it will fail to connect to the database. Make sure:
- The `.env` file is in the `backend/` directory (same level as `pom.xml`)
- The file is named exactly `.env` (no `.txt` extension)

### Invalid JWT Secret

```
java.lang.IllegalArgumentException: Last unit does not have enough valid bits
```

Your `JWT_SECRET` is not valid Base64. Generate a new one:
```bash
# Linux/Mac:
openssl rand -base64 32

# PowerShell:
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

### Google OAuth redirect mismatch

```
Error 400: redirect_uri_mismatch
```

The redirect URI in Google Cloud Console doesn't match. Make sure you added:
```
http://localhost:8080/login/oauth2/code/google
```
(not `8081`, not `https`, not missing `/login` prefix)

---

## Running Tests

```bash
# Windows:
.\mvnw.cmd test

# Linux / macOS:
./mvnw test
```

Currently, only a context-load smoke test exists (`AuthApplicationTests`).
