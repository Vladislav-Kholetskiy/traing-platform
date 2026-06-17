# Implemented Functional Test Runbook

This runbook describes the repeatable implemented-scope acceptance run on one persistent demo DB: `training_platform_demo_fullcycle`.

Scope statement:
- All contours except maintenance contour are treated as implemented.
- maintenance contour is treated as implemented only through Stage 0-2.
- maintenance contour post-Stage-2 analytics rebuild, aggregate population, and managerial historical analytics are scope caveats, not blockers.

## A. Prerequisites

- Docker and the local Postgres container are available.
- Backend can be started in `dev` profile.
- Demo DB name is fixed: `training_platform_demo_fullcycle`.
- Dev actor auth uses header `X-Demo-Actor-Id`.
- Acceptance kit scripts:
  - [reset-demo-runtime-slice.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/reset-demo-runtime-slice.ps1)
  - [create-demo-content.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/create-demo-content.ps1)
  - [verify-implemented-functional-state.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/verify-implemented-functional-state.ps1)
  - [check-implemented-functional-api-smoke.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/check-implemented-functional-api-smoke.ps1)

## B. Startup

```powershell
docker compose up -d

$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5433/training_platform_demo_fullcycle"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"

.\mvnw.cmd -DskipTests spring-boot:run
```

Startup smoke:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/actuator/health"
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/me" -Headers @{ "X-Demo-Actor-Id" = "1" }
```

Expected:
- health is `UP`
- actor `1` resolves successfully

## C. Baseline restore

1. Ensure the DB exists:

```powershell
docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_demo_fullcycle;"
```

Ignore the error if the DB already exists.

2. Apply current bootstrap SQL:
- [bootstrap-demo-import.sql](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/baseline/bootstrap-demo-import.sql)
- org import through the current runtime/API flow
- [personnel-demo-baseline.sql](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/baseline/personnel-demo-baseline.sql)

Suggested commands:

```powershell
Get-Content -Raw .\demo-data\imports\baseline\bootstrap-demo-import.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle
Get-Content -Raw .\demo-data\imports\baseline\personnel-demo-baseline.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle
```

3. Verify org/personnel baseline:

```powershell
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from organizational_unit_type;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from organizational_unit;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from app_user;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from user_organization_assignment;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from user_role_assignment;"
```

Expected:
- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 51`
- `user_role_assignment = 103`

If `user_organization_assignment = 0`, stop and fix the baseline before any assignment or managerial run.

## D. Cleanup

Run the safe runtime-slice reset:

```powershell
.\demo-data\scripts\reset-demo-runtime-slice.ps1
```

What it cleans:
- analytics aggregate tables
- result snapshots and results
- attempts and answers
- assignments, assignment tests, campaigns
- test-question links
- answer options
- tests
- questions
- materials
- topics
- courses

What it never cleans:
- `app_user`
- `organizational_unit`
- `organizational_unit_type`
- `user_role_assignment`
- `user_organization_assignment`

## E. Content creation

Optional no-API preflight:

```powershell
.\demo-data\scripts\create-demo-content.ps1 -PreflightOnly
```

Full content run:

```powershell
Remove-Item -Recurse -Force .\tmp\demo-content-run-logs -ErrorAction SilentlyContinue
.\demo-data\scripts\create-demo-content.ps1
```

Expected content counts:
- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 85`
- `answer_option = 340`
- `test = 13`
- `test_question = 91`

Important helper behavior:
- default auth is `X-Demo-Actor-Id: 1`
- normal mode is a full run, no `StopAfterSection`
- runtime JSON is normalized at final serialization boundary
- runtime logs must not contain `«` or `»`
- materials are metadata-only; source content remains in [materials-source.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/materials-source.md)

## F. Assignment setup

Resolve published course ids by name first:

```powershell
$expertHeaders = @{ "X-Demo-Actor-Id" = "11" }
$courses = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/expert/content/courses" -Headers $expertHeaders
$opsCourseId = ($courses | Where-Object { $_.name -eq "Промышленная безопасность оператора НПЗ" } | Select-Object -First 1).id
$permitCourseId = ($courses | Where-Object { $_.name -eq "Наряд-допуск и безопасная организация работ" } | Select-Object -First 1).id
```

Launch three campaigns through the runtime surface:

```powershell
$adminHeaders = @{ "X-Demo-Actor-Id" = "1"; "Content-Type" = "application/json" }

$opsUkk = @{
  name = "OPS Safety UKK Campaign"
  description = "Implemented-scope assigned demo for UKK operators"
  sourceType = "MANUAL"
  sourceRef = "/department/npz/production/cck/ukk"
  sourceNameSnapshot = "CCK-UKK"
  courseIds = @($opsCourseId)
  targeting = @{ basisType = "ORG_UNIT"; basisRef = "/department/npz/production/cck/ukk" }
  deadlinePolicy = @{ deadlineAt = "2026-12-31T23:59:59Z" }
} | ConvertTo-Json -Depth 10

$opsUpv = @{
  name = "OPS Safety UPV Campaign"
  description = "Implemented-scope assigned demo for UPV operators"
  sourceType = "MANUAL"
  sourceRef = "/department/npz/production/cck/upv"
  sourceNameSnapshot = "CCK-UPV"
  courseIds = @($opsCourseId)
  targeting = @{ basisType = "ORG_UNIT"; basisRef = "/department/npz/production/cck/upv" }
  deadlinePolicy = @{ deadlineAt = "2026-12-31T23:59:59Z" }
} | ConvertTo-Json -Depth 10

$permitUkk = @{
  name = "Permit Safety UKK Campaign"
  description = "Implemented-scope permit demo for UKK contour"
  sourceType = "MANUAL"
  sourceRef = "/department/npz/production/cck/ukk"
  sourceNameSnapshot = "CCK-UKK"
  courseIds = @($permitCourseId)
  targeting = @{ basisType = "ORG_UNIT"; basisRef = "/department/npz/production/cck/ukk" }
  deadlinePolicy = @{ deadlineAt = "2026-12-31T23:59:59Z" }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/assignment-campaigns/launch" -Headers $adminHeaders -Body $opsUkk
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/assignment-campaigns/launch" -Headers $adminHeaders -Body $opsUpv
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/assignment-campaigns/launch" -Headers $adminHeaders -Body $permitUkk
```

Expected post-launch counts:
- `assignment_campaign = 3`
- `assignment = 24`
- `assignment_test = 72`

## G. Verification and smoke

Read-only DB verification:

```powershell
.\demo-data\scripts\verify-implemented-functional-state.ps1
```

API smoke:

```powershell
.\demo-data\scripts\check-implemented-functional-api-smoke.ps1
```

What the smoke script covers:
- health
- admin `me`
- admin org tree
- admin users
- admin notifications read
- expert course read
- active final-control read
- OPERATOR-1 assigned list/detail/context
- OPERATOR-2 assigned list/detail/context
- self-testing catalog
- self result history
- self notifications read
- manager current supervision
- negative fail-closed checks

## H. Manual role checks

Use [role-runbook.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/role-runbook.md).

Mandatory manual role flows:
- `ADMIN`
- `EXPERT`
- `OPERATOR-1` successful assigned pass
- `OPERATOR-2` failed/lower-score assigned pass
- `LEARNER-SELF-1` self-testing pass
- `MANAGER-1` current supervision

## I. Expected final counts

- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 51`
- `user_role_assignment = 103`
- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 85`
- `answer_option = 340`
- `test = 13`
- `test_question = 91`
- `assignment_campaign = 3`
- `assignment = 24`
- `assignment_test = 72`
- `test_attempt > 0`
- `result > 0`

## J. Expected scope caveats

Do not fail the implemented functional run if the following remain true:

- `analytics_user_topic_aggregate = 0`
- `analytics_department_topic_aggregate = 0`
- `GET /api/v1/managerial/historical-analytics/user-topic?... -> 200 []`
- `GET /api/v1/managerial/historical-analytics/department-topic?... -> 200 []`

Reason:
- these belong to maintenance contour post-Stage-2 and are `NOT_IMPLEMENTED_BY_SCOPE`.
