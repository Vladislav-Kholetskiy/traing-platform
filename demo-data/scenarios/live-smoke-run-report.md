# Live Smoke Run Report

## 1. Run metadata

- Date/time: `2026-05-12 23:06:46 +03:00`
- Branch: `codex/front`
- Git status caveat: working tree was already dirty before the smoke run; report below only covers runtime checks and the new report artifact
- Backend profile: `dev`
- DB container: `training_platform_postgres`
- DB runtime target: `training_platform`
- Backend base URL: `http://localhost:8080`

## 2. Commands executed

| Command | Purpose | Result |
| --- | --- | --- |
| `git status --short` | Capture initial working tree state | Dirty working tree confirmed; many unrelated modified and untracked files already present |
| `docker compose up -d` | Ensure demo Postgres is up | PASS; `training_platform_postgres` already running |
| `docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"` | Confirm DB container and port binding | PASS; `training_platform_postgres` exposed on `5433` |
| `Get-NetTCPConnection -LocalPort 8080` | Check whether backend is already listening | PASS; port `8080` already bound by Java process |
| `Get-CimInstance Win32_Process -Filter "ProcessId = 6004"` | Confirm backend command line / profile | PASS; Java backend already running with `--spring.profiles.active=dev` |
| `Get-Content tmp/demo-app.log -Tail 60` | Confirm current runtime DB target and startup status | PASS; backend connected to `jdbc:postgresql://localhost:5433/training_platform`, actuator exposed, app started successfully |
| `curl.exe -sS -i http://localhost:8080/actuator/health` | Health/basic readiness | PASS; HTTP `200`, status `UP` |
| `curl.exe -sS -i -H "X-Demo-Actor-Id: 1" http://localhost:8080/api/v1/me` | Demo-admin auth check via dev header | PASS; HTTP `200`, actor `DEMO-ADMIN-001`, role `ADMIN` |
| `docker exec training_platform_postgres psql -U postgres -d training_platform -c "select current_database() ..."` | Capture SQL counts baseline | PASS; org and user baseline present, content and assignment entities still `0` |
| `curl.exe -sS -i -H "X-Demo-Actor-Id: 1" http://localhost:8080/api/v1/admin/org-units/tree` | Org tree API readiness | PASS; HTTP `200`, root external id `DEPT` present |
| `curl.exe -sS -i -H "X-Demo-Actor-Id: 1" http://localhost:8080/api/v1/admin/users` | User list API readiness | PASS; HTTP `200`, `52` users returned |
| `curl.exe -sS -i -X POST -H "X-Demo-Actor-Id: 1" -F "file=@demo-data/imports/users/personnel-import.xlsx;type=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" http://localhost:8080/api/v1/admin/import/personnel-excel/dry-run` | Personnel import dry-run | PASS with drift; HTTP `200`, no `500`, no policy blocker, `4` rows with `PLANNED_CHANGES` |
| `docker exec training_platform_postgres psql -U postgres -d training_platform -c "select count(*) as app_user_count ..."` | Verify personnel counts after dry-run | PASS; dry-run did not mutate counts |
| `curl.exe -sS -i -H "X-Demo-Actor-Id: 1" http://localhost:8080/api/v1/expert/content/courses` | Content API safe-read readiness | PASS; HTTP `200`, empty list `[]` |
| `$null = [System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path 'demo-data/scripts/create-demo-content.ps1'), [ref]$null, [ref]$null); if ($?) { 'POWERSHELL_PARSE_OK' }` | Script syntax/preflight | PASS; parser returned `POWERSHELL_PARSE_OK` |
| `Get-Content demo-data/scripts/create-demo-content.ps1 -TotalCount 80` | Check helper config and auth assumptions | BLOCKED for execution; token placeholder still present, script uses bearer token only |

## 3. Stage results

| Stage | Expected | Actual | Verdict | Evidence |
| --- | --- | --- | --- | --- |
| `A. Demo DB and backend startup` | Postgres up, backend on `localhost:8080`, health responds | DB container already up, backend already running in `dev`, `/actuator/health` returned `{"status":"UP"}` | `PASS` | `docker compose up -d`, `docker ps`, `tmp/demo-app.log`, `GET /actuator/health` |
| `B. Demo-admin bootstrap and auth` | Demo admin exists and can authenticate with enough rights for content creation | `DEMO-ADMIN-001` already exists as user `1`; `GET /api/v1/me` with `X-Demo-Actor-Id: 1` returned `ADMIN` actor and expert/admin sections | `PASS` | SQL `app_user`, `GET /api/v1/me` |
| `C. Org structure import` | Org types `6`, org units `13`, root `DEPT`, tree readable | Counts already match expected; tree API returned root with `externalId = "DEPT"`; import itself not re-run because state already present and idempotency was not re-validated | `PASS` | SQL counts, `GET /api/v1/admin/org-units/tree` |
| `D. Personnel import current behavior` | Dry-run works, no fatal `403`/`500`, enterprise roster available for demo | Dry-run returned HTTP `200`; baseline roster mostly aligned, `4` rows reported `PLANNED_CHANGES`; `apply` intentionally not executed to avoid mutating existing demo state during smoke run | `PASS` | `POST /api/v1/admin/import/personnel-excel/dry-run`, SQL counts unchanged |
| `E. Content API readiness` | Safe content endpoint available; helper script preflight clean; content creation script runnable if auth configured | `GET /api/v1/expert/content/courses` returned `[]`; PowerShell parse passed; execution blocked because `create-demo-content.ps1` still contains `$Token = "<PASTE_TOKEN_HERE>"` and is not configured for current dev-header auth flow | `BLOCKED` | `GET /api/v1/expert/content/courses`, script header block |
| `F. Assignment readiness` | Launch campaigns for assigned courses after content exists | Not attempted; content counts still zero, so assignment campaign launch would be premature | `SKIPPED` | SQL `course/topic/material/question/test = 0` |
| `G. Learner / self / manager readiness` | Learner and manager flows run only after content and assignments exist | Not attempted; users exist, but content and assignments are absent | `SKIPPED` | SQL `assignment_* = 0`, `test_attempt = 0`, `result = 0` |

## 4. SQL/API evidence

| Evidence key | Actual value |
| --- | --- |
| `organizational_unit_type` | `6` |
| `organizational_unit` | `13` |
| `app_user` | `52` |
| `user_organization_assignment` | `52` |
| `user_role_assignment` | `102` |
| `course` | `0` |
| `topic` | `0` |
| `material` | `0` |
| `question` | `0` |
| `test` | `0` |
| `assignment_campaign` | `0` |
| `assignment` | `0` |
| `assignment_test` | `0` |
| `test_attempt` | `0` |
| `result` | `0` |
| `GET /actuator/health` | HTTP `200`, body `{"groups":["liveness","readiness"],"status":"UP"}` |
| `GET /api/v1/me` | HTTP `200`, actor `DEMO-ADMIN-001`, roles `["ADMIN"]` |
| `GET /api/v1/admin/org-units/tree` | HTTP `200`, root `externalId = "DEPT"` |
| `GET /api/v1/admin/users` | HTTP `200`, response contains `52` users |
| `POST /api/v1/admin/import/personnel-excel/dry-run` | HTTP `200`, `4` rows with `PLANNED_CHANGES`, no `403`, no `500` |
| `GET /api/v1/expert/content/courses` | HTTP `200`, body `[]` |

## 5. Blockers

| ID | Exact symptom | Exact endpoint/command | Expected | Actual | Suspected module | Recommended next remediation step |
| --- | --- | --- | --- | --- | --- | --- |
| `SCRIPT-TOKEN-BLOCKER` | Demo content helper is not executable as-is in current live smoke setup | `Get-Content demo-data/scripts/create-demo-content.ps1 -TotalCount 80` | Script ready to run against live demo backend after minimal config insert | Script still contains `$Token = "<PASTE_TOKEN_HERE>"` and throws `Replace the token placeholder before running the script.`; current successful smoke auth path used `X-Demo-Actor-Id: 1`, not bearer token | `demo-data/scripts` / auth integration assumptions | Prepare a narrow follow-up to align helper auth with current `dev` runtime, then rerun content creation only on demo DB |
| `PERSONNEL-DRIFT-FINDING` | Personnel workbook is not a pure no-change import against current DB snapshot | `POST /api/v1/admin/import/personnel-excel/dry-run` | Dry-run ideally returns only `NO_CHANGE` rows for baseline workbook | HTTP `200`, but rows `4`, `12`, `13`, `52` returned `PLANNED_CHANGES` for access, management relation, org assignment, and missing base role/access setup | personnel import / demo seed alignment | Review whether current DB should be reconciled with `personnel-import.xlsx` before full learner/manager demo, then decide whether to run `apply` in a controlled follow-up |
| `ASSIGNMENT-BLOCKED-BY-CONTENT` | Assignment stage cannot start yet | Assignment campaign launch not executed | Published assigned content and active final controls available | `course/topic/material/question/test` counts remain `0`, so no campaign-safe target content exists | content creation pipeline | First unblock content creation helper, then rerun content stage and only after that enter assignment smoke |

## 6. Final verdict

`READY_FOR_CONTENT_ONLY_DEMO`

Current live runtime is healthy enough for startup, demo-admin auth, org tree access, users baseline inspection, and personnel dry-run validation. Full demo progression is not yet possible because demo content has not been created and the current helper script is blocked by its auth placeholder / bearer-token assumption.
