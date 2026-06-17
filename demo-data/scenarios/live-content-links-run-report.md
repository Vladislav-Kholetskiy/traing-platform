# Live Content Links Run Report

## Run metadata

- Date: `2026-05-13`
- Workspace: `D:\Users\vladi\Desktop\Diplom\Java\training-platform`
- Backend profile: `dev`
- Auth mode: `DemoHeader`
- Demo actor: `X-Demo-Actor-Id: 1`
- Fresh DB: `training_platform_links_rerun`
- Dirty working tree caveat: repository was already dirty before this controlled run.

## Fresh DB / reset strategy

- Strategy: create and use a brand-new clean database instead of continuing any prior partial state such as `training_platform_tests_rerun`.
- Reset commands:

```powershell
docker exec training_platform_postgres psql -U postgres -d postgres -c "drop database if exists training_platform_links_rerun with (force);"
docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_links_rerun;"
```

## Backend startup and auth evidence

- Datasource env used:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5433/training_platform_links_rerun"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
```

- `GET /actuator/health` -> `200 OK`
- Initial `GET /api/v1/me` before bootstrap returned `404 User not found by id: 1`, which was expected on a fresh DB.
- After bootstrap/personnel restore: `GET /api/v1/me` with `X-Demo-Actor-Id: 1` -> `200 OK`
- Auth actor: `DEMO-ADMIN-001`

## Org/personnel baseline counts

Restore path used:

1. `tmp/bootstrap-demo-import.sql`
2. admin API org import
3. `tmp/personnel-demo-baseline.sql`

Counts before helper:

- `organizational_unit_type = 6`
- `organizational_unit = 12`
- `app_user = 52`
- `user_organization_assignment = 0`
- `user_role_assignment = 101`

Baseline drift note:

- The restored org contour landed with `12` units and root `NPZ` instead of the previously observed `13`-unit contour with `DEPT` root.
- This did not block content creation or link creation flow up to the actual blocker below.

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
- `StopAfterSection = "Links"`
- debug request logs cleared before run: `tmp/demo-content-run-logs`

## Commands executed

| Command | Purpose | Result |
| --- | --- | --- |
| `docker exec ... drop database if exists training_platform_links_rerun with (force)` | clear previous DB if present | completed |
| `docker exec ... create database training_platform_links_rerun` | create fresh DB | completed |
| backend start with `SPRING_PROFILES_ACTIVE=dev` and fresh datasource env | run backend on clean DB | completed |
| `GET /actuator/health` | readiness check | `200 OK` |
| `GET /api/v1/me` with `X-Demo-Actor-Id: 1` | auth check after baseline restore | `200 OK` |
| `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i ...` | seed demo admin | completed |
| PowerShell admin API org import | restore org tree | completed with baseline drift |
| `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i ...` | restore personnel baseline | completed with `user_organization_assignment = 0` drift |
| `Remove-Item tmp/demo-content-run-logs -Recurse -Force` | clear previous helper logs | completed |
| `.\demo-data\scripts\create-demo-content.ps1` | controlled run through `Links` | failed during helper-side link composition validation |

## Links run result

- Result: `LINKS_FAILED_WITH_BODY`

Observed helper outcome before failure:

- `3` courses created
- `8` topics created
- `27` materials created
- `85` questions created
- `340` answer options created
- `13` tests created
- `44` `test_question` rows created

Blocking point:

- helper-side validation stopped at `TEST-PERMIT-PREP-TRAIN`
- no additional manual API calls were executed after the stop

## Post-run counts

- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 85`
- `answer_option = 340`
- `test = 13`
- `test_question = 44`

## Test-question composition verification

Integrity checks:

- links to missing `test` ids: `0`
- links to missing `question` ids: `0`
- duplicate `(test_id, question_id)` links: `0`
- duplicate `display_order` within one test: `0`

Per-test linked question counts at stop point:

- `TEST-OPS-INTRO-FINAL = 8`
- `TEST-OPS-INTRO-SELF = 6`
- `TEST-OPS-UNIT-FINAL = 9`
- `TEST-OPS-INCIDENT-FINAL = 8`
- `TEST-OPS-INCIDENT-SELF = 6`
- `TEST-PERMIT-PREP-FINAL = 7`
- `TEST-PERMIT-PREP-TRAIN = 0`
- `TEST-OPO-SELF-RULES-SELF = 0`
- `TEST-OPO-SELF-GENERAL = 0`
- `TEST-PERMIT-RISK-FINAL = 0`
- `TEST-PERMIT-ROLES-FINAL = 0`
- `TEST-PERMIT-ROLES-TRAIN = 0`
- `TEST-OPO-SELF-INCIDENTS-SELF = 0`

Expectation comparison:

- counts created before the blocker match the planned sizes from `content-matrix.md` for:
  - `TEST-OPS-INTRO-FINAL = 8`
  - `TEST-OPS-INTRO-SELF = 6`
  - `TEST-OPS-UNIT-FINAL = 9`
  - `TEST-OPS-INCIDENT-FINAL = 8`
  - `TEST-OPS-INCIDENT-SELF = 6`
  - `TEST-PERMIT-PREP-FINAL = 7`
- helper then stopped before `TEST-PERMIT-PREP-TRAIN`, whose planned size is `6`

Actual blocker:

- helper reported:

```text
[TEST-PERMIT-PREP-TRAIN] Not enough eligible questions for runtime owner topic TOPIC-PERMIT-PREP. Need 6, found 5.
```

Interpretation:

- this is a helper-side composition/mapping blocker, not an HTTP validation failure from the backend
- likely root cause family: selection logic for eligible questions for the second test on `TOPIC-PERMIT-PREP` exhausted the currently allowed subset before reaching the planned `6`

## Final-control/publication absence verification

- `is_active_final_for_topic = true` count = `0`
- `status = DRAFT` for all `13` tests
- no helper log evidence of:
  - `assign-final:`
  - publish operations
- `tmp/demo-content-run-logs` contains no `*.error.txt`

Therefore:

- active final controls were not assigned
- publication was not executed

## Error details

- Operation label: helper-side blocker at `TEST-PERMIT-PREP-TRAIN`
- Endpoint: no failing HTTP endpoint; the stop happened before the next link request was issued
- Request body: not applicable for the blocking step because helper threw before sending the next request
- Status code: not applicable
- Response body / exact blocker text:

```text
[TEST-PERMIT-PREP-TRAIN] Not enough eligible questions for runtime owner topic TOPIC-PERMIT-PREP. Need 6, found 5.
```

## Future blocker note for user_organization_assignment

- `user_organization_assignment = 0` did not block this controlled content/links run.
- It remains a future blocker for `assignment / learner / manager` stages on this particular baseline path.

## Final verdict

- `STILL_BLOCKED_BY_LINK_CREATE`
