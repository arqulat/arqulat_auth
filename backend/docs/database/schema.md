# Database Schema

**Database:** PostgreSQL (hosted on Supabase)  
**Schema:** `auth`  
**Table:** `users`

---

## Table Definition

```sql
CREATE TABLE auth.users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL UNIQUE,
    name            VARCHAR(255),
    password_hash   VARCHAR(255),               -- NULL for Google-only users
    google_id       VARCHAR(255)    UNIQUE,     -- NULL for email/password-only users
    created_at      TIMESTAMP       NOT NULL DEFAULT now()
);
```

---

## Column Details

| Column | Type | Nullable | Unique | Default | Description |
|---|---|---|---|---|---|
| `id` | UUID | ❌ | ✅ (PK) | Auto-generated UUID v4 | Primary key, never changes |
| `email` | VARCHAR(255) | ❌ | ✅ | — | User's email address — the primary identifier used across the system |
| `name` | VARCHAR(255) | ✅ | ❌ | — | Display name. Set during registration or pulled from Google profile |
| `password_hash` | VARCHAR(255) | ✅ | ❌ | — | BCrypt-hashed password. `NULL` for users who only signed in via Google |
| `google_id` | VARCHAR(255) | ✅ | ✅ | — | Google's `sub` claim (unique per Google account). `NULL` for users who only registered via email/password |
| `created_at` | TIMESTAMP | ❌ | ❌ | `now()` | Auto-set on insert via Hibernate's `@CreationTimestamp`. Never updated |

---

## Indexes

| Index | Column(s) | Type | Source |
|---|---|---|---|
| Primary Key | `id` | Unique, B-tree | `@Id @GeneratedValue` |
| Unique Constraint | `email` | Unique, B-tree | `@Column(unique = true)` |
| Unique Constraint | `google_id` | Unique, B-tree (nullable) | `@Column(unique = true)` |

---

## User Account States

A user can exist in three states depending on how they signed up:

| State | `password_hash` | `google_id` | How Created |
|---|---|---|---|
| **Email-only** | `$2a$10$...` (BCrypt hash) | `NULL` | Registered via `/api/v1/auth/register` |
| **Google-only** | `NULL` | `10987654321...` | First login via Google OAuth |
| **Linked** | `$2a$10$...` (BCrypt hash) | `10987654321...` | Registered via email, then later logged in via Google (accounts auto-linked) |

---

## JPA Entity Mapping

The `User` entity is defined in `com.arqulat.auth.model.User`:

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
```

---

## Repository Queries

`UserRepository` extends `JpaRepository<User, UUID>` and provides:

| Method | Return Type | Generated SQL |
|---|---|---|
| `findByEmail(String email)` | `Optional<User>` | `SELECT * FROM auth.users WHERE email = ?` |
| `existsByEmail(String email)` | `boolean` | `SELECT COUNT(*) > 0 FROM auth.users WHERE email = ?` |
| `findByGoogleId(String googleId)` | `Optional<User>` | `SELECT * FROM auth.users WHERE google_id = ?` |

---

## Schema Management

| Environment | `ddl-auto` Setting | Strategy |
|---|---|---|
| Development | `update` | Hibernate auto-creates/alters tables based on entity definitions |
| Production (planned) | `validate` | Hibernate only validates schema matches entities. Migrations handled by Flyway/Liquibase |
