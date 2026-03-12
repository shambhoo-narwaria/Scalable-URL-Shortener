# Scalable URL Shortener

> A high-performance, production-style URL shortening backend service built with Java 17, Spring Boot, Redis, and PostgreSQL.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7-red)](https://redis.io)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://docs.docker.com/compose)

---

## System Architecture

```text
+----------------------------------------------------+
|                Client (Browser/API)                |
|          [HTML5, CSS3, Vanilla JS, Fetch]          |
+----------------------------------------------------+
                          |  HTTP Requests (JSON)
                          v
+----------------------------------------------------+
|             Spring Boot 3 Application              |
|                  [Java 17, Maven]                  |
|                                                    |
|  +----------------------------------------------+  |
|  |         RateLimitFilter (Middleware)         |  |
|  |      [Token Bucket, OncePerRequestFilter]    |  |
|  +----------------------------------------------+  |
|                         |                          |
|                         v                          |
|  +----------------------------------------------+  |
|  |       UrlController (REST endpoint)          |  |
|  |      [@RestController, @PostMapping, GET]    |  |
|  +----------------------------------------------+  |
|                         |                          |
|                         v                          |
|  +----------------------------------------------+  |
|  |        UrlService & Analytics (Logic)        |  |
|  |     [Base62Encoder, @Async, @Transactional]  |  |
|  +----------------------------------------------+  |
|               |                        |           |
|               v                        |           |
|  +-------------------------+           |           |
|  | UrlRepository (JPA Data)|           |           |
|  |  [Spring Data JPA]      |           |           |
|  +-------------------------+           |           |
+---------------|------------------------|-----------+
                |                        | Cache-Aside
                | JPA / SQL              v
                v             +---------------------+
      +-------------------+   |    Redis (Cache)    |
      |  PostgreSQL (DB)  |   | [Spring Data Redis] |
      |  [Relational DB]  |   +---------------------+
      +-------------------+              |
                |                        |
                v                        v
+----------------------------------------------------+
|              Docker & Docker Compose               |
|        [Containerized Infrastructure Stack]        |
+----------------------------------------------------+
```
---

## Features

- **Sub-10ms Redirect Latency** via the Redis Cache-Aside pattern.
- **Base62 Encoding** — mathematically collision-free, generating 56+ billion unique short URLs using `BIGSERIAL` sequence logic.
- **Network Rate Limiting** — 100 req/min enforced network-wide per IP via a Redis Token Bucket Middleware Filter.
- **Asynchronous Click Analytics** — background threads handles stat updates so the database never blocks redirect queries.
- **URL Expiry** — optional TTL-based expiration with strict 404 responses on access.
- **Minimalist Developer UI** — highly interactive but simple testing utility built entirely out of Vanilla Javascript and CSS3.
- **Fully Dockerized** — instantly spin up the caching and database infrastructure with one command.

---

## Interactive API Documentation (Swagger)

This project features completely auto-generated, interactive **OpenAPI (Swagger)** documentation. 

Once the server is running, simply open your browser and navigate to:
**`http://localhost:8080/swagger-ui.html`**

From this beautiful UI, you can read about all the REST endpoints, see the exact JSON schema required, and even test the API directly from your browser without needing Postman!

---

## Tech Stack

| Layer      | Technology              |
|------------|-------------------------|
| Backend    | Java 17, Spring Boot 3  |
| Database   | PostgreSQL 15           |
| Cache      | Redis 7                 |
| Container  | Docker, Docker Compose  |
| Build      | Maven                   |
| Testing    | JUnit 5, Mockito        |

---

## Quick Start (Local)

### Prerequisites
- Java 17
- Maven 3.6+
- Docker Desktop

### 1. Start the Database & Cache (Terminal 1)
```bash
docker-compose up -d
```
*(You can verify they are running by typing `docker ps`)*

### 2. Run the Spring Boot API (Terminal 1)
Ensure you are using Java 17, then start the server:
```powershell
mvn spring-boot:run
```
API is now running at: `http://localhost:8080`

### 3. Stop Everything Cleanly
When you are entirely finished, run this script to kill the Spring Boot server and stop the Docker containers:
```powershell
.\stop-server.ps1
```

---

## API Endpoints

### Create a Short URL
```bash
POST /api/shorten
Content-Type: application/json

{
  "url": "https://example.com/very/long/path",
  "customCode": "myalias",    # optional
  "expiryDays": 30            # optional
}
```

Response `201 Created`:
```json
{
  "shortUrl": "http://localhost:8080/aB12x",
  "shortCode": "aB12x",
  "longUrl": "https://example.com/very/long/path",
  "expiryDate": "2026-04-10T23:00:00"
}
```

### Redirect
```bash
GET /aB12x
→ 302 Found → Location: https://example.com/very/long/path
```

### Analytics
```bash
GET /api/analytics/aB12x
```
Response `200 OK`:
```json
{
  "shortCode": "aB12x",
  "longUrl": "https://example.com/very/long/path",
  "clicks": 1243,
  "createdAt": "2026-03-10T12:00:00"
}
```

### Health Check
```bash
GET /actuator/health
```

---

## Key Engineering Concepts

### Cache-Aside Pattern
```
Client request
    ↓
Check Redis (fast, ~1ms)
    ↓
Cache HIT  → redirect immediately
Cache MISS → query PostgreSQL → populate Redis → redirect
```

### Base62 Encoding
- Characters: `[a-z A-Z 0-9]` = 62 chars
- 6-char codes = 62^6 = **56.8 billion unique URLs**
- Input: DB auto-increment ID → **zero collision guaranteed**

### Rate Limiting
- Strategy: Token bucket via Redis INCR + EXPIRE
- Limit: 100 requests/minute per IP
- Shared across all app instances (no bypass via scaling)

---

## Project Structure

```
src/main/java/com/urlshortener/
├── controller/     # HTTP endpoints (REST layer)
├── service/        # Business logic
├── repository/     # Database access (JPA)
├── model/          # JPA entities
├── dto/            # Request/Response objects
├── cache/          # Redis service
├── filter/         # Rate limiting filter
├── exception/      # Custom exceptions + global handler
├── config/         # Redis configuration
└── util/           # Base62 encoder
```
