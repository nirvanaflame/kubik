# spring-playground — monorepo

One Git repository hosting multiple independent Spring microservices, modules and
libraries. Structured as a **Gradle multi-project** build (Spring Modulith style:
loosely-coupled units of functionality, each with its own lifecycle and build).

## Repository layout

```
spring-playground/          ← existing main Spring Boot service (:spring-playground, port 8080)
mock-server/                ← standalone dummy API stub (:mock-server, port 8081)
[ future libraries/modules can be added as sibling folders ]
```

- `settings.gradle.kts` — registers every module with `include(...)`. Add new
  microservices/libraries by dropping a folder in and registering it here.
- Root `build.gradle.kts` — single place for plugin versions (`apply false`),
  the repo `group`/`version`, and shared `repositories`. Modules opt-in to the
  plugins/dependencies they need.
- Each module has its own `build.gradle.kts` and `src/` folder.

## Prerequisites

- JDK 25+ to compile (runtime requires Java 17+ for Spring Boot 4).
- Git/Gradle wrapper (included).

## Build & run

```bash
# Build everything (compile + tests + jars)
./gradlew build

# Run the existing service on :8080
./gradlew :spring-playground:bootRun

# Run the mock server on :8081
./gradlew :mock-server:bootRun
```

Each Spring Boot module is independently runnable and produces its own fat jar:

```bash
./gradlew :spring-playground:bootJar
./gradlew :mock-server:bootJar
```

## mock-server

A tiny dummy API stub that mimics `httpbin.org/json` and attaches extra response
headers on every request (via `MockServerHeadersInterceptor`):

| Endpoint                  | Description                                        |
|---------------------------|----------------------------------------------------|
| `GET /json`               | httpbin.org/json payload + extra headers           |
| `GET /json-with-headers`  | same, with an explicit extra header                |
| `GET /anything`           | echo-style stub                                    |
| `GET /`                   | service banner                                     |

Extra headers added to every response: `X-Mock-Server`, `X-Served-By`,
`X-Request-Id`, `X-Cache`, `Access-Control-Allow-Origin`, `Cache-Control`.

## Adding a shared library (example)

1. Create `libs/my-lib/` with its own `build.gradle.kts` (no Spring Boot plugin).
2. Add `include("libs:my-lib")` to `settings.gradle.kts`.
3. Depend on it from a module: `implementation(project(":libs:my-lib"))`.
