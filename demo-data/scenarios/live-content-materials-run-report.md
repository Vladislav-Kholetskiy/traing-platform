# Live Content Materials Run Report

## Run metadata

- Date/time: `2026-05-13 20:18 Europe/Moscow`
- Branch: `codex/front`
- Dirty working tree caveat: repository already contained many unrelated local changes before this controlled live run.
- Backend profile: `dev`
- Database: `training_platform_materials_rerun`
- Auth mode: `DemoHeader`
- Demo actor header: `X-Demo-Actor-Id: 1`

## Fresh DB / reset strategy

Strategy used:
- new clean database instead of continuing any previous partial content state

Exact reset commands:
- `docker exec training_platform_postgres psql -U postgres -d postgres -c "drop database if exists training_platform_materials_rerun with (force);"`
- `docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_materials_rerun;"`

## Backend startup and auth evidence

Backend startup:
- datasource URL: `jdbc:postgresql://127.0.0.1:5433/training_platform_materials_rerun`
- datasource user: `postgres`

Readiness checks:
- `GET /actuator/health` -> `200 OK`
- `GET /api/v1/me` with `X-Demo-Actor-Id: 1` -> `200 OK`

Authenticated actor:
- `actorUserId = 1`
- `username = DEMO-ADMIN-001`
- `roles = ["ADMIN"]`

## Org/personnel baseline counts

Baseline restore path used:
- `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_materials_rerun`
- PowerShell CSV/API import to:
  - `POST /api/v1/admin/org-unit-types`
  - `POST /api/v1/admin/org-units`
- `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_materials_rerun`

Observed counts before helper:
- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 0`
- `user_role_assignment = 101`

Important note:
- `user_organization_assignment = 0` and `user_role_assignment = 101` do not block content creation
- this baseline is **not sufficient** for future assignment/learner/manager demo stages without additional restore work

## Clean content baseline counts

Before helper:
- `course = 0`
- `topic = 0`
- `material = 0`
- `question = 0`
- `test = 0`

## Helper configuration

- `AuthMode = "DemoHeader"`
- `DemoActorId = "1"`
- `DumpRequests = $true`
- `StopAfterSection = "Materials"`

Debug logs directory:
- `tmp/demo-content-run-logs`

## Commands executed

| Command | Purpose | Result |
|---|---|---|
| `docker exec ... drop database if exists training_platform_materials_rerun with (force)` | clear old demo DB | PASS |
| `docker exec ... create database training_platform_materials_rerun` | create fresh DB | PASS |
| backend start with `SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5433/training_platform_materials_rerun` | run backend on clean DB | PASS |
| `curl.exe -i http://localhost:8080/actuator/health` | readiness check | PASS |
| `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i ...` | demo admin bootstrap | PASS |
| `curl.exe -i -H "X-Demo-Actor-Id: 1" http://localhost:8080/api/v1/me` | auth check | PASS |
| PowerShell CSV/API import to org endpoints | restore org tree | PASS |
| `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i ...` | restore personnel baseline | PASS with org-assignment drift |
| `powershell -File demo-data/scripts/create-demo-content.ps1` | controlled helper run to end of `Materials` | PASS with expected controlled stop |

## Materials run result

Verdict: `MATERIALS_CREATED`

Observed helper behavior:
- created `3` courses
- created `8` topics
- created `27` materials
- stopped intentionally after section `Materials`
- did not create any questions
- did not create any tests

Controlled stop message:
- `Controlled stop requested after section 'Materials'.`
- this stop is expected and is not treated as failure for this run

## Post-run counts

- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 0`
- `test = 0`

## Material type verification

Verified in DB:
- `TEXT = 8`
- `PDF = 8`
- `DOCX = 5`
- `VIDEO = 6`

Lifecycle state:
- `DRAFT = 27`

Additional consistency check:
- INFOSEC/self-hygiene terms were not found in created material names/descriptions

## Error details

- No material create error reproduced in this run.
- No `*.error.txt` files were needed for blocker triage.
- Request/response debug artifacts were generated for every successful material create in `tmp/demo-content-run-logs`.

## Final verdict

`MATERIALS_READY_FOR_QUESTIONS_RUN`

## Recommended next step

- Use a fresh clean demo DB again.
- Keep helper instrumentation enabled.
- Move to a new controlled run through `Questions`.
- Do not start assignment/learner stages until user/org assignment baseline is restored beyond the current `user_organization_assignment = 0` drift.
