# Live Content First Material Rerun Report

## Run metadata

- Date/time: `2026-05-13 18:18:16 +03:00`
- Branch: `codex/front`
- Dirty working tree caveat: repository was already dirty before this rerun; this report covers only the clean rerun flow and new report artifact

## Reset strategy and commands

- Selected strategy: `Variant 2` — new clean database name for the demo rerun
- New database: `training_platform_first_material_rerun`
- Exact reset command:
  - `docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_first_material_rerun;"`
- Reason for choosing this strategy:
  - it does not touch the previous partial content state in `training_platform`
  - it avoids manual FK-ordered cleanup of content tables
  - it gives a clean causal trace for the first material attempt

## Backend startup/auth evidence

- Existing backend on `8080` was stopped before rerun
- Backend was restarted on the new DB with:
  - `SPRING_PROFILES_ACTIVE=dev`
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5433/training_platform_first_material_rerun`
  - `SPRING_DATASOURCE_USERNAME=postgres`
  - `SPRING_DATASOURCE_PASSWORD=postgres`
- Health evidence:
  - `GET /actuator/health` -> HTTP `200`, status `UP`
- Auth evidence:
  - after bootstrap, `GET /api/v1/me` with `X-Demo-Actor-Id: 1` -> HTTP `200`
  - actor: `DEMO-ADMIN-001`

## Org/personnel baseline counts

Restore path used:

1. `tmp/bootstrap-demo-import.sql`
2. org import via documented admin API using:
   - `demo-data/imports/org-structure/org-unit-types.csv`
   - `demo-data/imports/org-structure/org-structure-runtime.csv`
3. `tmp/personnel-demo-baseline.sql`

Counts after restore:

- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 51`
- `user_role_assignment = 101`

Baseline caveat:

- expected historical smoke baseline was `52 / 102` for `user_organization_assignment / user_role_assignment`
- fresh restore landed on `51 / 101`
- likely difference: current bootstrap path does not give `DEMO-ADMIN-001` both an org assignment and an extra role assignment
- this was not corrected manually in this step

## Clean content baseline counts

Before helper run on the new DB:

- `course = 0`
- `topic = 0`
- `material = 0`
- `question = 0`
- `test = 0`

Verdict: clean content baseline confirmed.

## Helper configuration

- `AuthMode = "DemoHeader"`
- `DemoActorId = "1"`
- `DumpRequests = $true`
- `StopAfterSection = "FirstMaterial"`
- debug log directory:
  - `tmp/demo-content-run-logs`

## Commands executed

| Command | Purpose | Result |
| --- | --- | --- |
| `git status --short` | Initial dirty-tree capture | Dirty tree confirmed |
| `docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"` | Confirm local demo Postgres container | PASS |
| `docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_first_material_rerun;"` | Create clean rerun DB | PASS |
| `Stop-Process -Id 6452 -Force` | Stop old backend bound to `8080` | PASS |
| `cmd /c mvnw.cmd -DskipTests spring-boot:run` with fresh DB env | Start backend on clean DB | PASS |
| `curl.exe -sS -i http://localhost:8080/actuator/health` | Health check | PASS |
| `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_first_material_rerun` | Seed demo admin | PASS |
| `curl.exe -sS -i -H "X-Demo-Actor-Id: 1" http://localhost:8080/api/v1/me` | Verify demo-admin auth | PASS |
| PowerShell CSV/API org import to `/api/v1/admin/org-unit-types` and `/api/v1/admin/org-units` | Restore org tree | PASS |
| `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_first_material_rerun` | Restore personnel baseline | PASS with count drift |
| `.\demo-data\scripts\create-demo-content.ps1` | Controlled helper rerun to `FirstMaterial` | FAIL on first material create with captured body |

## Result

`FIRST_MATERIAL_FAILED_WITH_BODY`

The helper created:

- `3` courses
- `8` topics

Then it failed on the first material operation:

- operation label: `create-material:MAT-OPS-INTRO-TEXT`
- method/path: `POST /api/v1/expert/content/materials`
- status code: `400`

## Post-run counts

- `course = 3`
- `topic = 8`
- `material = 0`
- `question = 0`
- `test = 0`

## Error details

- Debug artifacts created in:
  - `tmp/demo-content-run-logs`
- Relevant files:
  - `tmp/demo-content-run-logs/0013-create-material-MAT-OPS-INTRO-TEXT.request.json`
  - `tmp/demo-content-run-logs/0013-create-material-MAT-OPS-INTRO-TEXT.response.json`
  - `tmp/demo-content-run-logs/0013-create-material-MAT-OPS-INTRO-TEXT.error.txt`

Exact failing request body:

```json
{
  "sortOrder": 10,
  "description": "Базовые требования промышленной безопасности перед допуском к работе\n\nВводный инструктаж по промышленной безопасности на НПЗ нужен для того, чтобы работник понимал не только общий порядок поведения на площадке, но и логику безопасной работы на опасном производственном объекте. До допуска к самостоятельным действиям сотрудник должен знать, что на объекте приоритет имеют требования процедуры, локальные запреты и команды ответственных лиц, а не личный опыт или привычка.\n\nРаботник обязан понимать границы своего допуска. Если в инструкции, наряде, маршруте обхода или фактической обстановке есть неясность, безопасным считается только один вариант поведения: остановиться, уточнить условия и получить подтверждение от ответственного лица. Нельзя начинать действие, если его безопасный порядок понятен лишь частично.\n\nОтдельный блок вводного инструктажа посвящён перемещению по площадке. Работник должен знать разрешённые маршруты, зоны ограниченного доступа, точки сбора, порядок эвакуации и способы оповещения при обнаружении отклонения. Наличие опыта на другом объекте не отменяет обязанности изучить именно локальные требования конкретной установки и подразделения.\n\nБольшое значение имеет понимание ранних признаков небезопасной ситуации. К ним относятся необычный запах, задымление, подтёки, ненормальный шум, вибрация, нарушение ограждений, захламление проходов и другие отклонения, которые могут показаться «мелкими». Вводный инструктаж должен закрепить правило: замеченное отклонение нельзя нормализовать фразой «так уже бывало».\n\nРаботник должен знать, где искать актуальные требования и почему запрещено пользоваться устаревшими памятками, личными заметками и неформальными советами вместо утверждённых материалов. Задача инструктажа не в том, чтобы заставить запомнить все формулировки, а в том, чтобы сформировать устойчивую модель поведения: свериться с процедурой, оценить обстановку, при сомнении эскалировать, при отклонении сообщить.",
  "name": "Ключевые правила вводного инструктажа",
  "materialType": "TEXT",
  "topicId": 1
}
```

Exact response body:

```json
{
  "timestamp": "2026-05-13T15:17:44.882605400Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request body is invalid",
  "correlationId": "83ef79b7-ca99-4ab6-8056-fdfe4b12e319"
}
```

## Final verdict

`FIRST_MATERIAL_FAILED_WITH_BODY`

## Recommended next step

- Do not continue full dataset creation on this DB.
- Investigate why the same logical material payload is rejected by the helper path as `Request body is invalid`.
- Narrow next step:
  - compare the exact raw request produced by `Invoke-RestMethod` in helper with a known-good manual request path
  - focus on serialization/encoding/transport details, not on content DTO or business validation yet
