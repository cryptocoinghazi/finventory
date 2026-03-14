# Finventory (Local Setup)

Finventory is a full-stack inventory + billing app:
- Backend: Spring Boot 3 (Java 21), PostgreSQL, Flyway
- Frontend: Next.js 14 (App Router), TypeScript, Tailwind + shadcn/ui

## Prerequisites

You can run everything with your own installed tooling, or (on Windows) use the bundled tools in `/tools`.

**Required**
- Git
- PostgreSQL 15+ (or Docker Desktop to run Postgres via docker-compose)

**Backend tooling**
- Java 21
- Maven 3.9+

**Frontend tooling**
- Node.js 20+
- npm (comes with Node)

## Quick Start (Windows, using bundled tools)

1) Open PowerShell in the repo root and activate tools:

```powershell
. .\activate.ps1
```

2) Start PostgreSQL (Docker):

```powershell
cd backend
docker compose up -d
```

3) Run the backend:

```powershell
cd backend
mvn spring-boot:run
```

Backend runs on:
- http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

4) Run the frontend (in a new terminal):

```powershell
cd frontend
npm install
npm run dev
```

Frontend runs on:
- http://localhost:3000

## Quick Start (macOS/Linux, with your own tooling)

1) Start PostgreSQL (Docker):

```bash
cd backend
docker compose up -d
```

2) Run the backend:

```bash
cd backend
mvn spring-boot:run
```

3) Run the frontend (new terminal):

```bash
cd frontend
npm install
npm run dev
```

## Default Database Configuration

The backend is configured in [application.yml](backend/src/main/resources/application.yml) to use:
- URL: `jdbc:postgresql://localhost:5432/finventory`
- User: `postgres`
- Password: `password`

If you use the provided docker-compose in [backend/docker-compose.yml](backend/docker-compose.yml), it matches these defaults.

## Login (Seed Users)

When the database is new, Flyway seeds users in SQL migrations. Common logins:
- `admin` / `admin123` (ADMIN)
- `demo` / `admin123` (ADMIN)
- `demo1` / `admin123` (USER)

## Environment Variables

**Frontend → Backend URL**
- `NEXT_PUBLIC_API_URL` (defaults to `http://localhost:8080`)

Example:

```bash
# macOS/Linux
export NEXT_PUBLIC_API_URL="http://localhost:8080"
```

```powershell
# Windows PowerShell
$env:NEXT_PUBLIC_API_URL="http://localhost:8080"
```

## Useful Commands

**Backend**
```bash
cd backend
mvn test
```

**Frontend**
```bash
cd frontend
npm run lint
npm run test
npm run build
```

## Troubleshooting

**Port already in use**
- Backend uses `8080`
- Frontend uses `3000`
- Postgres uses `5432`

**Database schema**
- Flyway runs automatically on backend startup.
- If you point to an existing DB, ensure it is compatible with the current migrations.

