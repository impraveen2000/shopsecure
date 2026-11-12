# ShopSecure learning guide

Work through these modules in order. Each one names the concept, explains it briefly, and points to the files where it is implemented. Read the code comments: they carry most of the explanation.

---

## Module 1: Microservice fundamentals

| Concept | Where |
|---|---|
| Multi-module Maven + Spring Cloud BOM | `pom.xml` |
| **Database per service**: no shared tables | `docker/postgres/init.sql`, each service's `application.yml` |
| **Service discovery**: call services by name, not IP | `discovery-server/`, `eureka.client.*` in every service, `lb://...` in the gateway |
| **Centralised configuration** | `config-server/src/main/resources/configs/*.yml`, `spring.config.import` in each service |
| **API Gateway**: single entry point | `api-gateway/src/main/resources/application.yml` (routes) |

**Exercise:** Start two instances of product-service (`mvn -pl product-service spring-boot:run -Dspring-boot.run.arguments=--server.port=18092`). Look at Eureka, then watch the gateway load-balance between them in the logs.

---

## Module 2: Spring Security core concepts (auth-service)

1. **SecurityFilterChain**: every request flows through a chain of filters. Read `auth-service/.../config/SecurityConfig.java` line by line.
   - `authorizeHttpRequests`: URL-based rules, evaluated top to bottom; the first match wins.
   - `sessionManagement(STATELESS)`: no `HttpSession`, so it scales horizontally.
   - Why `csrf().disable()` is OK here (Bearer tokens) and what protects the cookie instead (SameSite).
   - Security headers: `X-Frame-Options`, `Content-Security-Policy`.
   - Turn on `logging.level.org.springframework.security=TRACE` to watch every filter run.
2. **Authentication flow** (`AuthService.login`):
   `UsernamePasswordAuthenticationToken` → `AuthenticationManager` → `DaoAuthenticationProvider` → `UserDetailsService` (`DatabaseUserDetailsService`) → `PasswordEncoder.matches()`.
3. **Password storage**: `BCryptPasswordEncoder(12)` in `SecurityConfig`. Look at the `users` table: you'll see `$2a$12$...` hashes, never plain text.
4. **User enumeration protection**: the same "Invalid username or password" message for both cases (`GlobalExceptionHandler`).
5. **Brute-force protection**: `failedLoginAttempts` + `lockedUntil` in `User`, logic in `AuthService.recordFailedAttempt`, plus gateway rate limiting (Module 6).
6. **Input validation**: `@Valid`, `@Pattern`, `@Size` in `RegisterRequest`. Note the BCrypt 72-byte limit.
7. **Never trust the client**: `register()` always assigns `Role.USER`. `UserResponse` never exposes the password hash.

**Exercise:** Add a "change password" endpoint that requires the current password, then revoke all refresh tokens afterwards.

---

## Module 3: JWT done right

| Topic | Where |
|---|---|
| JWT structure (header.payload.signature) | Frontend **Profile** page, `TokenService` |
| **RS256 (asymmetric)** vs HS256 (shared secret) | `RsaKeyConfig` comments |
| Standard claims `iss`, `sub`, `exp`, `iat`, `jti` + custom `roles`, `uid` | `TokenService.createAccessToken` |
| **JWKS**: publishing the public key | `JwksController` → `/.well-known/jwks.json` |
| `kid` header for key rotation | `RsaKeyConfig.rsaKey()` |
| Short-lived access tokens (15 min) | `app.jwt.access-token-ttl` |

Key insight: JWT payloads are **readable by anyone**. The signature only guarantees *integrity* and *authenticity*.

**Exercise:** Paste a token into jwt.io, change `"roles":["USER"]` to `["ADMIN"]`, and send the modified token. Every service rejects it, because the signature no longer matches.

---

## Module 4: Refresh tokens and secure token storage in a SPA

| Topic | Where |
|---|---|
| Opaque refresh tokens stored **hashed** (SHA-256) | `RefreshToken`, `RefreshTokenService.hash` |
| **Rotation**: every refresh gives a new token | `RefreshTokenService.rotate` |
| **Reuse detection** → revoke all sessions | `RefreshTokenService.rotate` |
| **HttpOnly + Secure + SameSite=Strict + Path** cookie | `AuthController.refreshCookie` |
| Access token in **memory only** | `frontend/src/api/client.js` |
| Silent refresh + single-flight + request replay | `client.js` interceptors |
| Session restore on reload | `AuthContext.jsx` `useEffect` |
| Logout revokes server-side | `AuthController.logout` |

Why not `localStorage`? Any XSS bug can read it. An HttpOnly cookie can't be read by JavaScript at all.

**Exercise:** Add "Log out from all devices" (hint: `RefreshTokenService.revokeAll`).

---

## Module 5: Resource servers and authorization

| Topic | Where |
|---|---|
| OAuth2 Resource Server with a JWKS URI | `product-service/.../SecurityConfig.java` |
| Issuer validation | `JwtValidators.createDefaultWithIssuer` |
| Mapping the `roles` claim to `ROLE_*` authorities | `JwtGrantedAuthoritiesConverter` |
| `@PreAuthorize("hasRole('ADMIN')")` | `ProductController`, `AdminUserController` |
| `@PostAuthorize`: ownership checks (**IDOR**) | `OrderService.getOrder` |
| Programmatic ownership check | `OrderService.cancel` |
| `@AuthenticationPrincipal Jwt` | `ProductController.create`, `OrderController` |
| **Defense in depth**: URL rules + method rules + gateway rules | Compare the gateway, auth-service and controller annotations |
| **Zero trust**: every service re-validates the JWT | Every service has its own resource-server config |
| Testing security with `spring-security-test` | `ProductControllerSecurityTest`, `AuthControllerSecurityTest` |

**Exercise:** Add a `MANAGER` role that can update products but not delete them. Update auth-service, product-service, the gateway and the frontend, then write tests for it.

---

## Module 6: Edge security at the API gateway (reactive)

| Topic | Where |
|---|---|
| `ServerHttpSecurity` (WebFlux) vs `HttpSecurity` (servlet) | `api-gateway/.../SecurityConfig.java` |
| `denyAll()` for internal endpoints | `/api/auth/service-token`, `/reserve`, `/release` |
| **Rate limiting** (token bucket in Redis) | `application.yml` `RequestRateLimiter`, `RateLimitConfig` |
| Stricter limits for login (credential stuffing) | `auth-service` route filter |
| CORS (and why the same-origin proxy avoids it) | `globalcors`, `vite.config.js`, `nginx.conf` |
| HSTS, Referrer-Policy, X-Frame-Options | `headers(...)` |
| Custom `GlobalFilter` | `RequestLoggingFilter` |

**Exercise:** Add a filter that removes any incoming `X-User-Id` header (so clients can't spoof it). Then add one that sets it from the validated JWT for downstream services.

---

## Module 7: Service-to-service security

order-service needs to change stock in product-service. Should it forward the **user's** token? No: users must not be allowed to call `/reserve` directly.

Instead, order-service authenticates **as itself** (client-credentials pattern):
1. `ServiceTokenProvider` posts `clientId/clientSecret` to `auth-service /api/auth/service-token`.
2. It receives a 5-minute JWT with `roles: ["SERVICE"]` and caches it.
3. `ServiceAuthFeignConfig` adds it to every `ProductClient` call.
4. product-service allows `/reserve` only for `hasRole('SERVICE')`.
5. The gateway blocks both endpoints from the outside.

Also notice: constant-time secret comparison (`MessageDigest.isEqual`) in `AuthService.serviceToken`.

**Exercise:** Replace the shared secret with **mTLS** between services, or move to Spring Authorization Server's real `client_credentials` grant.

---

## Module 8: Resilience and distributed data

| Topic | Where |
|---|---|
| OpenFeign declarative clients | `ProductClient`, `AuthClient` |
| Timeouts | `spring.cloud.openfeign.client.config.default` |
| **Circuit breaker** (Resilience4j), ignoring business errors | `ProductGateway`, `resilience4j.*` in YAML |
| Gateway circuit breaker + fallback | `GatewayCircuitBreakerConfig`, `FallbackController` |
| **Saga with compensation** (no distributed transactions) | `OrderService.placeOrder` |
| Server-side pricing (never trust client prices) | `OrderService`, `OrderItem` |
| **Optimistic locking** against overselling | `Product.@Version` |
| N+1 avoidance with `@EntityGraph` | `OrderRepository` |

**Exercise:** Implement the **transactional outbox** pattern so `ORDER_PLACED` events are never lost if Kafka is down.

---

## Module 9: Event-driven communication

| Topic | Where |
|---|---|
| Kafka producers (JSON) | `UserEventPublisher`, `OrderEventPublisher` |
| Kafka consumers | `NotificationListener` |
| Loose coupling: auth-service doesn't know notification-service exists | Compare the code |
| No secrets/PII in events or logs | Event records, `mask()` |

**Exercise:** Add a dead-letter topic with `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`.

---

## Module 10: Observability

- **Distributed tracing**: Micrometer Tracing + Brave → Zipkin. The `traceId` appears in every log line (`logging.pattern.level`).
- **Actuator**: `/actuator/health`, `/metrics`, `/circuitbreakers`. Note that only `health` and `info` are public.

**Exercise:** Add Prometheus + Grafana to `docker-compose.yml` and secure `/actuator/prometheus` with a dedicated role.

---

## Module 11: React security patterns

| Topic | Where |
|---|---|
| Auth state with Context | `AuthContext.jsx` |
| Route guards with roles | `ProtectedRoute.jsx` |
| Role-based navigation | `Navbar.jsx` |
| Central API client with interceptors | `api/client.js` |
| Showing server validation errors | `errorMessage()` |
| React escapes output by default (XSS); never use `dangerouslySetInnerHTML` with user data | All pages |
| CSP headers in production | `frontend/nginx.conf` |

---

## 🏆 Big extension projects (once you finish the modules)

1. **Replace auth-service with Keycloak** (or Spring Authorization Server). Use OAuth2 Authorization Code + PKCE in React. Resource servers barely change, which is the whole point of JWKS.
2. **Social login** ("Login with Google/GitHub") using `spring-boot-starter-oauth2-client`.
3. **Two-factor authentication (TOTP)** with Google Authenticator.
4. **Email verification + password reset** with signed, single-use, expiring tokens.
5. **Persist RSA keys and rotate them**: publish the old and new keys in JWKS during the rotation window.
6. **Access-token deny-list** in Redis (by `jti`) for instant revocation.
7. **Audit log service**: consume security events (login, lockout, role change) from Kafka.
8. **Kubernetes deployment** with Helm, Secrets, NetworkPolicies, and an Ingress with TLS.
9. **Integration tests with Testcontainers** (Postgres, Kafka, Redis) for the full login → order flow.
10. **OWASP ZAP scan** against the gateway, then fix what it finds.
