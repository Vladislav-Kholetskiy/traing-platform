# Training Platform

`training-platform` is a training management platform with a Spring Boot backend, a React frontend, and bundled local demo data for development and scenario verification.

## Stack

- Backend: Java 21, Spring Boot, Spring MVC, Spring Data JPA, Flyway, Quartz, Spring Security
- Frontend: React, TypeScript, Vite, Ant Design, TanStack Query
- Database: PostgreSQL 16

## Repository Layout

- `src/` - backend source code and tests
- `frontend/` - frontend application
- `demo-data/` - local demo data, helper scripts, and runbooks
- `docs/` - project documentation
- `FQW/` - thesis and supporting research materials

## Local Development

### 1. Start PostgreSQL

```powershell
docker compose up -d
```

Local defaults for development only:

- Database: `training_platform`
- User: `postgres`
- Password: `postgres`
- Port: `5433`

### 2. Start the backend

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The `dev` profile supports local demo actor switching through the `X-Demo-Actor-Id` header.

### 3. Start the frontend

```powershell
cd frontend
Copy-Item .env.example .env -Force
npm install
npm run dev
```

Default frontend environment:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_DEMO_ACTOR_ID=1
```

## Build

Backend compile:

```powershell
.\mvnw.cmd -DskipTests compile
```

Frontend production build:

```powershell
cd frontend
npm run build
```

## Notes For Public Distribution

- `frontend/.env` is local-only and must not be committed
- local logs, archives, IDE files, Maven cache, and temporary render outputs are intentionally ignored
- demo credentials in `docker-compose.yml` are for local development only
- review `demo-data/` before publishing if you want to replace bundled demo content with a smaller or more generic sample set

## Additional Docs

- [frontend/README.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/frontend/README.md)
- [docs/demo/demo-data-setup.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/docs/demo/demo-data-setup.md)
- [docs/README.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/docs/README.md)
