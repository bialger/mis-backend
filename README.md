# M3301-Bigulov-backend
Backend for Medical Information System powered by Kotlin and Micronaut

## ERD (модель данных)

![ERD модели данных](./mis_erd.svg)


## Deployment (Docker)

The server must have an **`.env`** file in the same directory as `docker-compose.yml`. Docker Compose loads `.env` from the project directory when you run it.

**Where to put .env on the server:**  
Create **`/opt/mis/.env`** (next to `docker-compose.yml`). See `deploy/.env.example` for a template. The file must include **`MIS_IMAGE`** and **`APP_PORT`** (e.g. `MIS_IMAGE=ghcr.io/is-web-y27/m3301-bigulov-backend:latest`) so that `docker compose` commands work when run manually; CI sets these when deploying.

**Verify Postgres login and password on the server:**

```bash
cd /opt/mis
# Use your POSTGRES_USER and POSTGRES_DB from .env
docker compose exec postgres psql -U mis -d mis -c "SELECT 1"
```

If the command returns a row with `1`, the connection with that user and database works (an wrong password would have caused an error when entering the container).

To check user and database explicitly:

```bash
PGPASSWORD='your_password_from_.env' docker compose exec -T postgres psql -U mis -d mis -h localhost -c "SELECT current_user, current_database();"
```

---

## CI/CD

The workflow (`.github/workflows/ci-cd.yml`) builds the app, pushes a Docker image to GitHub Container Registry (GHCR), and deploys to the server via SCP and SSH.

### Workflow variables (env in the CI script)

Defined at the top of the workflow under `env:`; override or set in repository/environment if you need different values.

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_VERSION` | `21` | JDK version for the primary Java setup. |
| `GRADLE_JAVA_VERSION` | `21` | JDK version used by the Gradle build. |
| `HOST` | `mis.bialger.com` | Deployment server hostname (SSH/SCP). |
| `APP_PORT` | `8000` | Port the app listens on (and exposed by Docker). |

The Docker image name is derived from the repository: `ghcr.io/<owner>/<repo>:latest` (lowercase).

### GitHub secrets

Configure these in the repo: **Settings → Secrets and variables → Actions** (and in the **production** environment if you use one).

| Secret | Required | Used in | Description |
|--------|----------|---------|-------------|
| `SERVER_LOGIN` | Yes (for deploy) | deploy job | SSH/SCP username on the deployment server. |
| `SERVER_PASSWORD` | Yes (for deploy) | deploy job | SSH/SCP password for `SERVER_LOGIN`. |
| `GHCR_PULL_TOKEN` | Yes (for deploy) | deploy job | Token with `read:packages` scope so the server can `docker pull` the image from GHCR. Use your GitHub username as the registry user (same as `repository_owner`). |
| `SUBMODULES_TOKEN` | No | ci job | Optional. Used to clone private submodules; falls back to `github.token` if unset. |

The workflow uses the built-in `GITHUB_TOKEN` to push the image to GHCR (no extra secret needed for push).
