# M3301-Bigulov-backend

Backend for Medical Information System powered by Kotlin and Micronaut

## ERD (Data Model)

![ERD data model](./mis_erd.svg)

---

## Domain and Entities

**Domain:** Medical Information System (MIS) for managing a multi-branch healthcare organization. It covers patient registration, appointments, clinical documentation, laboratory orders, payments, inventory, notifications, and integrations with external systems (SMS, labs, government).

### Core entities

| Entity                 | Description                                                                         |
| ---------------------- | ----------------------------------------------------------------------------------- |
| **Organization**       | Healthcare organization (name, OKPO/OKUD codes, address).                           |
| **Branch**             | Physical location/facility within an organization.                                  |
| **Room**               | Consulting or treatment room within a branch.                                       |
| **Employee**           | Staff member (doctor, nurse, etc.) with credentials and optional digital signature. |
| **Specialty**          | Medical specialty (e.g., cardiology).                                               |
| **Role**               | Security role (doctor, admin, etc.).                                                |
| **Permission**         | Fine-grained permission code.                                                       |
| **EmployeeSpecialty**  | Links employees to their specialties.                                               |
| **EmployeeBranch**     | Links employees to branches.                                                        |
| **EmployeeRole**       | Links employees to roles.                                                           |
| **RolePermission**     | Links roles to permissions.                                                         |
| **EmployeePermission** | Per-employee permission overrides (grant/revoke).                                   |

### Patients and consent

| Entity             | Description                                                                          |
| ------------------ | ------------------------------------------------------------------------------------ |
| **Patient**        | Patient with demographics (card number, OMS policy, SNILS, addresses, contact info). |
| **PatientTagType** | Tag category (VIP, diabetic, etc.).                                                  |
| **PatientTag**     | Tag applied to a patient.                                                            |
| **PatientConsent** | Consent record (e.g., gov data transfer, marketing).                                 |

### Scheduling and services

| Entity                 | Description                                                       |
| ---------------------- | ----------------------------------------------------------------- |
| **Service**            | Billable service (consultation, procedure) with price per branch. |
| **TimeSlot**           | Slots in a calendar (employee, room, branch, date, time).         |
| **Appointment**        | Visit appointment (patient, doctor, slot, status, source).        |
| **AppointmentService** | Services rendered during an appointment (quantity, price).        |

### Clinical documentation

| Entity            | Description                                                               |
| ----------------- | ------------------------------------------------------------------------- |
| **MedicalRecord** | Visit record (complaints, anamnesis, examination, epicrisis, signatures). |
| **Template**      | Document template (standard, clinical guideline, personal).               |
| **Diagnosis**     | ICD diagnosis linked to a medical record.                                 |
| **Prescription**  | Prescription (procedure, medication, lab test).                           |
| **InsertSheet**   | Scanned form/insert (referral, etc.) for a patient visit.                 |

### Laboratory

| Entity           | Description                                                      |
| ---------------- | ---------------------------------------------------------------- |
| **Laboratory**   | External lab (name, integration config).                         |
| **LabTest**      | Laboratory test definition and price.                            |
| **LabOrder**     | Order for lab tests (linked to medical record, patient, doctor). |
| **LabOrderItem** | Single test in an order.                                         |
| **LabResult**    | Result for one order item (manual or from integration).          |

### Finance

| Entity           | Description                                                |
| ---------------- | ---------------------------------------------------------- |
| **Payment**      | Payment for an appointment (amount, method, status).       |
| **SalaryRecord** | Employee salary/compensation record per branch and period. |

### Inventory

| Entity                   | Description                                                    |
| ------------------------ | -------------------------------------------------------------- |
| **InventoryCategory**    | Category of inventory items.                                   |
| **InventoryItem**        | Stock item (by category, branch, room) with quantity and cost. |
| **InventoryOperation**   | Stock movement (incoming, write-off) with quantity.            |
| **InventoryAccess**      | Access rules (by employee or role) to inventory categories.    |
| **ServiceInventoryItem** | Quantity of inventory consumed per service.                    |

### Attachments and integrations

| Entity           | Description                                               |
| ---------------- | --------------------------------------------------------- |
| **Attachment**   | File attached to patient, appointment, or medical record. |
| **Integration**  | External integration (SMS, lab, gov system) with config.  |
| **Notification** | Outbound notification (SMS, email) for appointments.      |

### System

| Entity            | Description                                      |
| ----------------- | ------------------------------------------------ |
| **AuditLog**      | Audit trail (action, entity, old/new value, IP). |
| **SystemSetting** | Key-value settings (global or per branch).       |

---

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

If the command returns a row with `1`, the connection with that user and database works (a wrong password would have caused an error when entering the container).

To check user and database explicitly:

```bash
PGPASSWORD='your_password_from_.env' docker compose exec -T postgres psql -U mis -d mis -h localhost -c "SELECT current_user, current_database();"
```

### Local run (Docker + local image)

Run Postgres and the app from a locally built image (no pull from GHCR):

1. **Start Docker Desktop** (required).
2. From the project root, run:

   ```bash
   bash deploy/run-local.sh
   ```

   The script: builds JAR → builds image `mis-backend:local` → creates `deploy/.env` with `MIS_IMAGE=mis-backend:local` → runs `docker compose up -d`.

   App: http://localhost:8000  
   Postgres: localhost:5432 (user=`mis`, password=`mis`, db=`mis`).

3. To stop: `cd deploy && docker compose down`.

---

## CI/CD

The workflow (`.github/workflows/ci-cd.yml`) builds the app, pushes a Docker image to GitHub Container Registry (GHCR), and deploys to the server via SCP and SSH.

### Workflow variables (env in the CI script)

Defined at the top of the workflow under `env:`; override or set in repository/environment if you need different values.

| Variable              | Default           | Description                                      |
| --------------------- | ----------------- | ------------------------------------------------ |
| `JAVA_VERSION`        | `21`              | JDK version for the primary Java setup.          |
| `GRADLE_JAVA_VERSION` | `21`              | JDK version used by the Gradle build.            |
| `HOST`                | `mis.bialger.com` | Deployment server hostname (SSH/SCP).            |
| `APP_PORT`            | `8000`            | Port the app listens on (and exposed by Docker). |

The Docker image name is derived from the repository: `ghcr.io/<owner>/<repo>:latest` (lowercase).

### GitHub secrets

Configure these in the repo: **Settings → Secrets and variables → Actions** (and in the **production** environment if you use one).

| Secret             | Required         | Used in    | Description                                                                                                                                                       |
| ------------------ | ---------------- | ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SERVER_LOGIN`     | Yes (for deploy) | deploy job | SSH/SCP username on the deployment server.                                                                                                                        |
| `SERVER_PASSWORD`  | Yes (for deploy) | deploy job | SSH/SCP password for `SERVER_LOGIN`.                                                                                                                              |
| `GHCR_PULL_TOKEN`  | Yes (for deploy) | deploy job | Token with `read:packages` scope so the server can `docker pull` the image from GHCR. Use your GitHub username as the registry user (same as `repository_owner`). |
| `SUBMODULES_TOKEN` | No               | ci job     | Optional. Used to clone private submodules; falls back to `github.token` if unset.                                                                                |

The workflow uses the built-in `GITHUB_TOKEN` to push the image to GHCR (no extra secret needed for push).
