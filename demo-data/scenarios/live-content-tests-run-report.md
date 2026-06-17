# Live Content Tests Run Report

## Run metadata

- Date: `2026-05-13`
- Workspace: `D:\Users\vladi\Desktop\Diplom\Java\training-platform`
- Backend profile: `dev`
- Auth mode: `DemoHeader`
- Demo actor: `X-Demo-Actor-Id: 1`
- Fresh DB: `training_platform_tests_rerun`
- Dirty working tree caveat: repository was already dirty before this controlled run.

## Fresh DB / reset strategy

- Strategy: use a brand-new clean database and do not continue any prior partial content state.
- Old partial DB state was not reused.
- Reset commands:

```powershell
docker exec training_platform_postgres psql -U postgres -d postgres -c "drop database if exists training_platform_tests_rerun with (force);"
docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_tests_rerun;"
```

## Backend startup and auth evidence

- Datasource env used:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5433/training_platform_tests_rerun"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
```

- `GET /actuator/health` -> `200 OK`
- `GET /api/v1/me` with `X-Demo-Actor-Id: 1` -> `200 OK`
- Auth actor: `DEMO-ADMIN-001`

## Org/personnel baseline counts

Restore path used:

1. `tmp/bootstrap-demo-import.sql`
2. admin API org import using:
   - `demo-data/imports/org-structure/org-unit-types.csv`
   - `demo-data/imports/org-structure/org-structure-runtime.csv`
3. `tmp/personnel-demo-baseline.sql`

Counts before helper:

- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 51`
- `user_role_assignment = 101`

## Clean content baseline counts

Before helper run:

- `course = 0`
- `topic = 0`
- `material = 0`
- `question = 0`
- `answer_option = 0`
- `test = 0`
- `test_question = 0`

Verdict:

- clean content baseline confirmed before helper run.

## Helper configuration

- `AuthMode = "DemoHeader"`
- `DemoActorId = "1"`
- `DumpRequests = $true`
- `StopAfterSection = "Tests"`
- debug request logs cleared before run: `tmp/demo-content-run-logs`

## Commands executed

| Command | Purpose | Result |
| --- | --- | --- |
| `docker exec ... drop database if exists training_platform_tests_rerun with (force)` | clear previous DB if present | completed |
| `docker exec ... create database training_platform_tests_rerun` | create fresh DB | completed |
| backend start with `SPRING_PROFILES_ACTIVE=dev` and fresh datasource env | run backend on clean DB | completed |
| `GET /actuator/health` | readiness check | `200 OK` |
| `GET /api/v1/me` with `X-Demo-Actor-Id: 1` | auth check | `200 OK` |
| `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i ...` | seed demo admin | completed |
| PowerShell admin API import to `/api/v1/admin/org-unit-types` and `/api/v1/admin/org-units` | restore org tree | completed |
| `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i ...` | restore personnel baseline | completed |
| `Remove-Item tmp/demo-content-run-logs -Recurse -Force` | clear previous helper logs | completed |
| `.\demo-data\scripts\create-demo-content.ps1` | controlled run through `Tests` | completed through `Tests`; expected controlled stop fired |

## Tests run result

- Result: `TESTS_CREATED`

Observed helper outcome:

- `3` courses created
- `8` topics created
- `27` materials created
- `85` questions created
- `340` answer options created
- `13` tests created
- `0` `test_question` links created

Stop behavior:

- helper terminated with `Controlled stop requested after section 'Tests'.`
- this was expected because `StopAfterSection = "Tests"` intentionally prevents `Links`, `FinalControls`, and `Publication`

## Post-run counts

- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 85`
- `answer_option = 340`
- `test = 13`
- `test_question = 0`

## Test type / scoring policy verification

Database verification:

- `test_type = CONTROL` for all `13` tests
- `status = DRAFT` for all `13` tests
- `scoring_policy_code = DEFAULT` for all `13` tests
- `is_active_final_for_topic = true` count = `0`
- `INFOSEC` in `test.name` / `test.description` count = `0`

Payload/log verification:

- sample request log [0465-create-test-TEST-PERMIT-ROLES-FINAL.request.json](D:\Users\vladi\Desktop\Diplom\Java\training-platform\tmp\demo-content-run-logs\0465-create-test-TEST-PERMIT-ROLES-FINAL.request.json:1) contains:
  - `testType = "CONTROL"`
  - `scoringPolicyCode = "DEFAULT"`

Interpretation:

- only runtime-supported test mode actually used in this run was `CONTROL`
- `DEFAULT` scoring policy was accepted by runtime without rejection
- pre-publication lifecycle state is `DRAFT`, which matches the expected create-before-links/publish sequence

## Link/final-control/publication absence verification

- `test_question = 0`
- no `*.error.txt` files were produced in `tmp/demo-content-run-logs`
- no helper log evidence of:
  - `link-test-question:`
  - `assign-final:`
  - publish operations

Therefore:

- no test-question links were created
- no active final controls were assigned
- no entity publication occurred

## Error details

- No runtime API error occurred during the `Tests` section.
- No `TESTS_FAILED_WITH_BODY` event occurred.
- No `BASELINE_NOT_CLEAN` event occurred.
- No `STARTUP_BLOCKED` final state occurred.
- The only non-zero exit at the shell level was the expected controlled stop after section `Tests`.

## Future blocker note for user_organization_assignment

- `user_organization_assignment = 51` on this baseline, so no user/org-assignment blocker was observed for this controlled run.

## Final verdict

- `TESTS_READY_FOR_LINKS_RUN`
