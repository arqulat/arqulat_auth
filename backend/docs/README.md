# Arqulat Auth — Backend Documentation

> **Version:** 0.0.1-SNAPSHOT  
> **Framework:** Spring Boot 4.0.6 · Java 17  
> **Database:** PostgreSQL (Supabase-hosted)  
> **Last Updated:** 2026-06-11

---

## 📚 Documentation Index

| Document | Description |
|---|---|
| [Architecture Overview](./architecture/overview.md) | High-level system design, request flow diagrams, and component responsibilities |
| [Project Structure](./architecture/project-structure.md) | Annotated file tree with descriptions of every file |
| [Authentication Flows](./auth/flows.md) | Sequence diagrams for register, login, OAuth, and logout |
| [API Reference](./api/endpoints.md) | Every endpoint with request/response schemas and status codes |
| [Database Schema](./database/schema.md) | Table definitions, column details, and indexes |
| [Configuration Reference](./config/reference.md) | All properties, environment variables, and `.env` template |
| [Security Architecture](./security/architecture.md) | Cookie config, JWT specs, CORS, password hashing, filter chain |
| [Error Handling](./api/error-handling.md) | Exception-to-HTTP-status mapping and response formats |
| [Running Locally](./setup/local-setup.md) | Step-by-step guide to run the backend on your machine |
| [Testing Guide](./testing/testing-guide.md) | Explanation of unit/integration tests, H2 database usage, and logic |
| [Known Issues & TODOs](./known-issues.md) | Tracked issues organized by severity |

---

## Quick Start

```bash
# 1. Clone and navigate
git clone <repo-url>
cd arqulat_auth/backend

# 2. Create .env (see docs/config/reference.md for template)

# 3. Run
.\mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run          # Linux/Mac
```

Server starts on **http://localhost:8080**.
