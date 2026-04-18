# Documentation

This folder holds **curated** technical documentation for the MIS backend. For day-to-day setup and domain overview, see the [repository root README](../README.md).

## Documentation index

| Document | Description |
| -------- | ----------- |
| [**openapi.yaml**](./openapi.yaml) | OpenAPI 3.1 description of HTTP routes under `/api/**`, auth, permission matrix (`x-required-permission`), GraphQL/Swagger notes, and related endpoints. |
| [**erd.puml**](./erd.puml) | PlantUML entity–relationship diagram of the database (organizations, patients, scheduling, clinical, finance, inventory, etc.). Render with a PlantUML extension or CLI; an exported SVG may live at the repo root as `mis_erd.svg`. |

## Project structure (overview)

High-level layout of this repository:

| Path | Purpose |
| ---- | ------- |
| `src/main/kotlin/com/bialger/` | Kotlin application code |
| `…/api/` | JSON REST controllers (`/api/...`) consumed by SPA/clients |
| `…/auth/` | JWT login, access-control services, filters (`ApiAccessControlFilter`, HTML guards) |
| `…/application/` | Application services orchestrating use cases |
| `…/domain/` | Domain model: entities, repositories, MVC-style services per bounded context (e.g. `patient`, `scheduling`, `finance`, `inventory`, `clinical`, `laboratory`, `system`) |
| `…/graphql/` | GraphQL wiring, resolvers, facade over domain data |
| `…/infrastructure/` | Technical adapters (e.g. object storage) |
| `…/web/` | Web helpers, legacy shell bootstrap under `/mvc` |
| `PageController.kt` | Server-rendered HTML routes for CRM pages |
| `src/main/resources/` | `application.properties`, Flyway migrations (`db/migration/`), GraphQL schema (`graphql/schema.graphqls`), static assets, Thymeleaf templates |
| `src/test/kotlin/` | Tests (e.g. Kotest, Micronaut test) |
| `frontend/` | Optional frontend assets / OpenAPI draft (`frontend/api_data/api.yaml` may differ from real `/api` paths) |
| `deploy/` | Deployment configuration and env examples |
| `build.gradle.kts` | Gradle build: Micronaut 4, Kotlin, JDBC, Flyway, Security JWT, GraphQL, OpenAPI annotations |

Runtime defaults: **Micronaut** on **Netty**, **PostgreSQL** via JDBC, migrations with **Flyway**, security via **JWT** (Bearer + cookie `MIS_AUTH`).
