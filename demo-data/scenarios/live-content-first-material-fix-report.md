# Live Content First Material Fix Report

## 1. Run metadata

- Date/time: `2026-05-13 18:35 Europe/Moscow`
- Branch: `codex/front`
- Dirty working tree caveat: repository was already dirty before this run; live verification was performed without cleaning unrelated user changes.
- Backend profile: `dev`
- Database: `training_platform_first_material_fix`
- Auth mode: `DemoHeader`
- Demo actor: `X-Demo-Actor-Id: 1`

## 2. Checked material contract

Inspected runtime contract:
- [MaterialController.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/controller/MaterialController.java:1)
- [CreateMaterialRequest.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/controller/dto/CreateMaterialRequest.java:1)
- [MaterialCommandServiceImplTest.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/test/java/com/vladislav/training/platform/content/service/MaterialCommandServiceImplTest.java:1)
- [MaterialControllerTest.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/test/java/com/vladislav/training/platform/content/controller/MaterialControllerTest.java:1)
- [V100__full_schema_stack.sql](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/resources/db/migration/V100__full_schema_stack.sql:1)

Confirmed DTO/validation shape:
- `topicId`: `@NotNull`
- `name`: `@NotBlank`
- `description`: no `@Size(max = ...)` constraint found
- `materialType`: `@NotNull`
- `sortOrder`: `@PositiveOrZero`

Schema-level findings:
- `material.description` is `text null`
- no DB length limit for `description`
- uniqueness is enforced on `(topic_id, sort_order)`

Conclusion:
- exact max length constraint for `description` was **not found**
- previous `400 Request body is invalid` was not explained by DTO/service/schema max-length validation
- helper payload was still incorrect semantically because it posted full educational source text into metadata `description`

## 3. Script change summary

Updated [create-demo-content.ps1](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/create-demo-content.ps1:1):
- `New-MaterialDescription` now returns only short metadata `description`
- full `SourceText` from [materials-source.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/materials-source.md:1) is no longer sent to `POST /api/v1/expert/content/materials`
- no changes to:
  - material codes
  - material count
  - topics
  - `materialType`
  - `sortOrder`
  - other payload groups

Documentation updated:
- [README-demo-content.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/README-demo-content.md:1)
- [content-api-payload-plan.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/content-api-payload-plan.md:1)

## 4. Reset strategy and commands

Strategy used:
- new clean demo database instead of continuing prior partial state

Commands:
- `docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_first_material_fix;"`
- backend restart with:
  - `SPRING_PROFILES_ACTIVE=dev`
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5433/training_platform_first_material_fix`
  - `SPRING_DATASOURCE_USERNAME=postgres`
  - `SPRING_DATASOURCE_PASSWORD=postgres`
- `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_first_material_fix`
- PowerShell CSV/API import to:
  - `POST /api/v1/admin/org-unit-types`
  - `POST /api/v1/admin/org-units`
- `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_first_material_fix`

## 5. Backend startup and auth evidence

- `GET /actuator/health` -> `200 OK`
- `GET /api/v1/me` with `X-Demo-Actor-Id: 1` -> `200 OK`
- actor:
  - `username = DEMO-ADMIN-001`
  - `roles = ["ADMIN"]`

## 6. Baseline counts before controlled run

Org/personnel baseline:
- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 51`
- `user_role_assignment = 101`

Content baseline:
- `course = 0`
- `topic = 0`
- `material = 0`
- `question = 0`
- `test = 0`

## 7. Helper configuration

- `AuthMode = "DemoHeader"`
- `DemoActorId = "1"`
- `DumpRequests = $true`
- `StopAfterSection = "FirstMaterial"`

## 8. Controlled run result

Verdict: `CREATED`

Observed sequence:
- created `3` courses
- created `8` topics
- created first material successfully
- helper stopped intentionally after `FirstMaterial`

Created first material:
- operation: `create-material:MAT-OPS-INTRO-TEXT`
- endpoint: `POST /api/v1/expert/content/materials`
- created `materialId = 1`
- returned status: `DRAFT`

Request debug artifact:
- [0013-create-material-MAT-OPS-INTRO-TEXT.request.json](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0013-create-material-MAT-OPS-INTRO-TEXT.request.json:1)

Response debug artifact:
- [0013-create-material-MAT-OPS-INTRO-TEXT.response.json](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0013-create-material-MAT-OPS-INTRO-TEXT.response.json:1)

Effective request body fragment:

```json
{
  "sortOrder": 10,
  "description": "Базовые требования промышленной безопасности перед допуском к работе",
  "name": "Ключевые правила вводного инструктажа",
  "materialType": "TEXT",
  "topicId": 1
}
```

## 9. Post-run counts

- `course = 3`
- `topic = 8`
- `material = 1`
- `question = 0`
- `test = 0`

## 10. Error details

- No material `400` reproduced in this controlled rerun.
- Previous blocker body remains documented in [live-content-first-material-rerun-report.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/live-content-first-material-rerun-report.md:1), but after helper mapping fix it is no longer reproducible for the first material create.

## 11. Final verdict

`FIRST_MATERIAL_FIXED_READY_FOR_MATERIALS_RUN`

## 12. Recommended next step

- Use a fresh clean demo DB again.
- Keep instrumentation enabled.
- Run helper through the full `Materials` section next.
- Do not proceed to questions/tests until all `27` materials are created successfully on a clean baseline.
