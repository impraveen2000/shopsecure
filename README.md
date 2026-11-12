# 🔐 ShopSecure: Spring Security + Microservices + React

A complete e-commerce learning project. It is built to teach **Spring Security** and **microservice architecture** in a realistic setting. Each security concept is implemented in real code, with comments explaining *why*.

## Architecture

```
                         ┌──────────────────────┐
   Browser (React) ────► │  frontend :5173/3000 │  Vite dev proxy / nginx  (same-origin → secure cookies)
                         └──────────┬───────────┘
                                    │ /api/**
                         ┌──────────▼───────────┐      ┌───────────┐
                         │  API Gateway  :18090  │─────►│  Redis    │ rate limiting
                         │  JWT check, routing, │      └───────────┘
                         │  rate limit, CB      │
                         └──┬──────┬──────┬─────┘
            lb://auth-service│      │      │lb://order-service
               ┌─────────────▼┐ ┌───▼────────────┐ ┌▼──────────────┐   ┌───────────────────────┐
               │ auth-service │ │ product-service│ │ order-service │──►│ product-service       │
               │   :18081      │ │    :18082       │ │    :18083      │   │ (SERVICE token, Feign,│
               │ issues JWTs  │ │ resource server│ │ resource srv  │   │  circuit breaker)     │
               │ JWKS, users  │ └───────┬────────┘ └──────┬────────┘   └───────────────────────┘
               └──────┬───────┘         │                 │
                      │ user-registered │                 │ order-events
                      └────────────► Kafka ◄──────────────┘
                                       │
                              ┌────────▼─────────────┐
                              │ notification-service │ :18084
                              └──────────────────────┘

  Platform: config-server :18888 (centralised config) · discovery-server :18761 (Eureka)
            PostgreSQL :5433 (authdb / productdb / orderdb) · Zipkin :9411 (tracing)
```

| Service | Port | What it teaches |
|---|---|---|
| **config-server** | 18888 | Spring Cloud Config, protecting config with Basic auth |
| **discovery-server** | 18761 | Eureka service registry, securing the registry |
| **api-gateway** | 18090 | Spring Cloud Gateway, reactive security (WebFlux), JWT validation at the edge, rate limiting, circuit breaker, blocking internal endpoints |
| **auth-service** | 18081 | Login/registration, BCrypt, `AuthenticationManager`, RS256 JWT signing, JWKS, refresh-token rotation and reuse detection, HttpOnly cookies, account lockout, admin user management |
| **product-service** | 18082 | OAuth2 Resource Server, `@PreAuthorize`, public vs protected endpoints, optimistic locking, security tests |
| **order-service** | 18083 | Service-to-service auth (client-credentials), OpenFeign, Resilience4j circuit breaker, saga with compensation, `@PostAuthorize` (IDOR protection), Kafka producer |
| **notification-service** | 18084 | Kafka consumers, event-driven design, PII masking in logs |
| **frontend** | 5173 | React, in-memory access token, silent refresh with axios interceptors, protected routes, role-based UI, JWT inspector |

## Ports used (chosen so they don't clash with other local Docker projects)
| Host port | Used by |
|---|---|
| 5173 / 3000 | frontend (dev / Docker) |
| 18090 | api-gateway |
| 18081-18084 | auth / product / order / notification |
| 18761, 18888 | Eureka, config-server |
| 5433 | PostgreSQL |
| 6380 | Redis |
| 9094 | Kafka |
| 9411 | Zipkin |

## Tech stack
Java 21 · Spring Boot 3.3 · Spring Security 6 · Spring Cloud 2023.0 (Gateway, Config, Eureka, OpenFeign) · Resilience4j · Spring Data JPA · PostgreSQL · Kafka · Redis · Micrometer Tracing + Zipkin · React 18 · Vite · React Router · Axios · Docker Compose

---

## 🚀 Quick start

### Prerequisites
- JDK 21, Maven 3.9+
- Node.js 18+
- Docker + Docker Compose

### Option A: Infrastructure in Docker, services in your IDE (best for learning/debugging)

```bash
# 1. Start Postgres, Redis, Kafka, Zipkin
docker compose up -d

# 2. Build everything once
mvn clean install -DskipTests

# 3. Start the services IN THIS ORDER (separate terminals, or run the main classes from IntelliJ)
mvn -pl config-server spring-boot:run
mvn -pl discovery-server spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl api-gateway spring-boot:run

# 4. Start the frontend
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173**

### Option B: Everything in Docker
```bash
docker compose --profile apps up -d --build
```
Open **http://localhost:3000**. The first build takes a few minutes.

### Demo accounts
| Username | Password | Roles |
|---|---|---|
| `admin` | `Admin@123` | ADMIN, USER |
| `john` | `User@1234` | USER |

### Useful URLs
| URL | What |
|---|---|
| http://localhost:18761 | Eureka dashboard (login `eureka` / `eureka-secret`) |
| http://localhost:9411 | Zipkin: follow one request across all services |
| http://localhost:18081/.well-known/jwks.json | Public key used to verify JWTs |
| http://localhost:18083/actuator/health | Circuit breaker state |
| http://localhost:18888/order-service/default | Config served to order-service (login `config` / `config-secret`) |

---

## 🧪 Things to try (each one demonstrates a security feature)

1. **Login as `john`**, then open **Profile** to see your decoded JWT, its claims and the expiry countdown.
2. **Wrong password 5 times** → the account is locked for 15 minutes (HTTP 423). An admin can unlock it under *Users*.
3. As `john`, visit `/admin/users` → redirected to *403*. Then call `GET /api/auth/admin/users` with john's token in `api-requests.http` → **403 from the server**. Hiding the UI is not security; the backend enforces access.
4. **Place an order**, then edit the cart price in browser DevTools → localStorage. Order again: the server still charges the real price.
5. Try `POST /api/products/1/reserve` through the gateway, even with the admin token → **blocked**. Only order-service (with a SERVICE token) may change stock.
6. **Stop product-service** and place an order → after a few failures the circuit breaker opens and you get fast `503`s. Start it again and it recovers.
7. **Reload the page**: you stay logged in. The access token was lost from memory, and the HttpOnly refresh cookie silently got a new one.
8. Copy the `refresh_token` cookie value from DevTools, let the app refresh (Profile → *Refresh now*), wait more than 10 seconds, then replay the **old** value with `curl -X POST localhost:18090/api/auth/refresh -H "Cookie: refresh_token=OLD_VALUE"` → **reuse detected**, all of that user's sessions are revoked.
9. Hammer `/api/auth/login` quickly → `429 Too Many Requests` from the gateway's Redis rate limiter.
10. Open **Zipkin**, search for `api-gateway`, and click a trace for `POST /api/orders` to see gateway → order → auth → product timing.
11. Restart **auth-service**. It generates a new signing key, so existing access tokens now fail with 401. The refresh tokens are stored in Postgres and survive, so the frontend silently gets a new JWT signed with the new key. Watch it happen in the browser's Network tab.

Use **`api-requests.http`** (IntelliJ HTTP Client / VS Code REST Client) to test every endpoint by hand.

---

## 📚 Learning path
Read **[docs/LEARNING_GUIDE.md](docs/LEARNING_GUIDE.md)**. It walks through every concept in a sensible order, points to the exact file that implements it, and ends with exercises to extend the project (Keycloak, OAuth2 social login, 2FA, mTLS, Kubernetes…).

## Project layout
```
shopsecure/
├── pom.xml                  parent POM (Spring Boot + Spring Cloud BOM)
├── config-server/           centralised configuration
├── discovery-server/        Eureka
├── api-gateway/             edge security, routing, rate limiting
├── auth-service/            identity provider: users, JWT, refresh tokens
├── product-service/         catalog (resource server)
├── order-service/           orders, Feign, circuit breaker, saga
├── notification-service/    Kafka consumer
├── frontend/                React SPA
├── docker/postgres/         DB init script (database-per-service)
├── docker-compose.yml
├── Dockerfile               one image recipe for every Java service
├── api-requests.http        ready-made API calls
└── docs/LEARNING_GUIDE.md
```

## ⚠️ Production checklist (what this demo simplifies)
- Load RSA keys from a keystore/vault and support key rotation (here they're regenerated on startup).
- Set `COOKIE_SECURE=true` and serve everything over HTTPS.
- Replace all default passwords/secrets (`config-secret`, `eureka-secret`, `order-service-secret`, DB passwords) with real secrets.
- Use Flyway/Liquibase instead of `ddl-auto: update`.
- Consider a dedicated authorization server (Spring Authorization Server or Keycloak) for full OAuth2/OIDC.
- Add the transactional outbox pattern so events are never lost.
