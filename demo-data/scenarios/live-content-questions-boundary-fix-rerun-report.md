# Live Content Questions Boundary Fix Rerun Report

## Run metadata

- Date: `2026-05-13`
- Workspace: `D:\Users\vladi\Desktop\Diplom\Java\training-platform`
- Backend profile: `dev`
- Auth mode: `DemoHeader`
- Demo actor: `X-Demo-Actor-Id: 1`
- Fresh DB: `training_platform_questions_boundary_fix_rerun`
- Dirty working tree caveat: repository was already dirty before this rerun.

## Fresh DB / reset strategy

- Strategy: create and use a brand-new clean database instead of continuing any previous partial content state.
- Old partial databases were not reused.
- Reset commands:

```powershell
docker exec training_platform_postgres psql -U postgres -d postgres -c "drop database if exists training_platform_questions_boundary_fix_rerun with (force);"
docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_questions_boundary_fix_rerun;"
```

## Backend startup and auth evidence

- Datasource env:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5433/training_platform_questions_boundary_fix_rerun"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
```

- `GET /actuator/health` -> `200 OK`
- `GET /api/v1/me` with `X-Demo-Actor-Id: 1` -> `200 OK`
- Auth actor: `DEMO-ADMIN-001`

Startup note:

- One failed backend start happened before the successful run because `cmd set VAR=value && ...` left a trailing space in the database username.
- One intermediate backend run also returned `401` because it was not the final clean `dev`-profile process on port `8080`.
- Final evidence and all helper work below were taken only after the single clean `dev` backend was confirmed on the fresh DB.

## Org/personnel baseline counts

Restore path used:

1. `tmp/bootstrap-demo-import.sql`
2. Admin API org import using:
   - `demo-data/imports/org-structure/org-unit-types.csv`
   - `demo-data/imports/org-structure/org-structure-runtime.csv`
3. `tmp/personnel-demo-baseline.sql`

Counts before helper:

- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 0`
- `user_role_assignment = 101`

## Clean content baseline counts

Before helper run:

- `course = 0`
- `topic = 0`
- `material = 0`
- `question = 0`
- `answer_option = 0`
- `test = 0`

Verdict:

- clean content baseline confirmed before helper run.

## Helper configuration

- `AuthMode = "DemoHeader"`
- `DemoActorId = "1"`
- `DumpRequests = $true`
- `StopAfterSection = "Questions"`
- Debug directory cleared before run: `tmp/demo-content-run-logs`

## Commands executed

| Command | Purpose | Result |
| --- | --- | --- |
| `docker exec ... drop database if exists training_platform_questions_boundary_fix_rerun with (force)` | remove prior DB if present | completed |
| `docker exec ... create database training_platform_questions_boundary_fix_rerun` | create fresh DB | completed |
| backend start with `SPRING_PROFILES_ACTIVE=dev` and fresh datasource env | start backend on clean DB | completed |
| `GET /actuator/health` | readiness check | `200 OK` |
| `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i ...` | seed demo admin | completed |
| PowerShell admin API import to `/api/v1/admin/org-unit-types` and `/api/v1/admin/org-units` | restore org tree | completed |
| `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i ...` | restore personnel baseline | completed with `user_organization_assignment = 0` drift |
| `GET /api/v1/me` with `X-Demo-Actor-Id: 1` | auth check | `200 OK` |
| `Remove-Item tmp/demo-content-run-logs -Recurse -Force` | clear previous helper request dumps | completed |
| `.\demo-data\scripts\create-demo-content.ps1` | controlled rerun through `Questions` | completed through `Questions`; stopped by expected controlled stop |

## Questions rerun result

- Result: `QUESTIONS_CREATED`

Observed helper outcome:

- `3` courses created
- `8` topics created
- `27` materials created
- `85` questions created
- `340` answer options created
- `0` tests created

Stop behavior:

- helper terminated with `Controlled stop requested after section 'Questions'.`
- this was expected because `StopAfterSection = "Questions"` intentionally prevents `Tests / Links / FinalControls / Publication`

## Post-run counts

- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 85`
- `answer_option = 340`
- `test = 0`

## Question type verification

- only question types present:
  - `SINGLE_CHOICE = 51`
  - `MULTIPLE_CHOICE = 34`
- no other question types were created.

## Answer option verification

- every question has exactly `4` answer options.
- invalid question count for `answer_option count != 4`: `0`
- invalid `SINGLE_CHOICE` question count with correct-options count `!= 1`: `0`
- invalid `MULTIPLE_CHOICE` question count with correct-options count `< 2`: `0`

## Normalization verification

- `rg -n "«|»" tmp/demo-content-run-logs` returned no matches.
- No `*.error.txt` files were produced in `tmp/demo-content-run-logs`.
- `0091-create-option-Q-OPS-INTRO-011-A.request.json` now contains:
  - `\"и так всё понятно\"`
  - no `«`
  - no `»`
- Runtime request log and actual HTTP request body came from the same serialized helper path after final-boundary normalization.

## Error details

- No runtime API error occurred during the `Questions` section.
- No `NORMALIZATION_GUARD_BLOCKED` event occurred.
- No `CONTENT-BASELINE-NOT-CLEAN` event occurred.
- No `STARTUP_BLOCKED` final state occurred.

## Future blocker note for user_organization_assignment

- `user_organization_assignment = 0` did not block this controlled `Questions` rerun.
- It remains a future blocker for `assignment / learner / manager` stages on this baseline path.

## Final verdict

- `QUESTIONS_READY_FOR_TESTS_RUN`
