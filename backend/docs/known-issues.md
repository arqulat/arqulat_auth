# Known Issues & TODOs

Tracked issues organized by severity. Updated as issues are resolved.

---

## 🔴 Critical — Fix Before Deploying

| # | Issue | Details | Status |
|---|---|---|---|
| 1 | **Pre-account takeover via OAuth linking** | Without email verification, an attacker can register with a victim's email, and when the victim later logs in via Google, the accounts merge. The attacker retains access via the known password. | ⏳ Mitigated by plan to disable password registration; fully fixed when OTP verification is added |
| 2 | **`ddl-auto=update` in production** | Hibernate auto-modifying the schema risks data corruption. Can silently add columns, widen types, and never drops constraints. | ⏳ Acceptable for development. Use Flyway/Liquibase + `ddl-auto=validate` for production |

---

## 🟠 High — Serious Bugs / Security Gaps

| # | Issue | Details | Status |
|---|---|---|---|
| 3 | ~~**JWTs not revoked on logout**~~ | ~~Clearing the cookie doesn't invalidate the token server-side. Stolen tokens remain valid for up to 7 days.~~ | ✅ Resolved — Implemented database token blacklist storing JTIs |
| 4 | **No rate limiting** | Auth endpoints are vulnerable to brute-force and registration flooding. | ⏳ TODO — Add Bucket4j or API gateway rate limiting |
| 5 | **CSRF disabled with cookie-based auth** | Because browsers automatically attach cookies to requests, a malicious "cross-site" (like `evil-website.com`) can secretly forge requests to our API, making the server think the user clicked it. Our `SameSite=Lax` setting blocks this auto-attaching behavior on modern browsers, but isn't a 100% fix for older browsers or subdomain attacks. | ⏳ Accepted risk (SameSite is generally "good enough" for standard apps without the hassle of full CSRF tokens).|

---

## 🟡 Medium — Should Fix for Production Quality

| # | Issue | Details | Status |
|---|---|---|---|
| 6 | **Registration doesn't auto-login** | User must call `/login` after `/register` to get a JWT cookie. | ⏳ By design for now |
| 7 | **`SessionCreationPolicy.IF_REQUIRED`** | Mixes stateful and stateless patterns. Should be `STATELESS` with stateless OAuth2 state storage. when changing between arqulat to google oauth spring stores a variable `state` . custom class (an AuthorizationRequestRepository) | ⏳ Required for OAuth2 flow. when arqulat expanded to multiple server(1000's of users login/sec) this will be problem |
| 8 | **`show-sql=true`** | Floods logs with SQL in production, potentially exposing user data in query parameters. | ⏳ Dev only — disable for production |
| 9 | ~~**`AppUserDetails` missing `serialVersionUID`**~~ | ~~Will break deserialization if the class changes and sessions are serialized.~~ | ✅ Resolved — Added serialVersionUID |
| 10 | ~~**Google-only users cause 500 on `/login`**~~ | ~~`BCryptPasswordEncoder.matches(raw, null)` throws `IllegalArgumentException` instead of returning 401.~~ | ✅ Resolved — Return empty string instead of null for password in AppUserDetails |

---

## 🟢 Low — Best Practices / Nice to Have

| # | Issue | Details | Status |
|---|---|---|---|
| 11 | **Use constructor injection** | Replace `@Autowired` field injection with constructor injection across all classes. Easier to test, prevents circular dependencies. | ⏳ TODO |
| 12 | **Add request correlation IDs** | Add `X-Request-ID` header propagation via MDC for production debugging. | ⏳ TODO |
| 13 | **Expand CORS allowed headers** | Add `X-Request-ID`, `Cache-Control` to allowed headers in CORS config. | ⏳ TODO |
| 14 | **Add class-level `@RequestMapping`** | Each endpoint in `AuthController` repeats `/api/v1/auth`. Use class-level mapping. | ⏳ TODO |
| 15 | **Verify Maven artifact names** | `spring-boot-starter-webmvc` and `*-test` variants may not resolve from Maven Central. Run `mvnw dependency:resolve` to confirm. | ⏳ TODO |

---

## Resolution Log

| Date | Issue # | Action Taken |
|---|---|---|
| 2026-06-11 | — | Password validation added (`@Pattern` regex on `RegisterRequest.password`) |
| 2026-06-12 | 3 | Implemented database-backed JWT blacklist using JTI claims to ensure tokens are securely invalidated on logout. |
| 2026-06-12 | 9 | Added `serialVersionUID` to `AppUserDetails` class. |
| 2026-06-12 | 10 | Updated `AppUserDetails.getPassword()` to return empty string instead of null, preventing `IllegalArgumentException` in `BCryptPasswordEncoder`. |
