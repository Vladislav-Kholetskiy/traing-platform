# Live Content Creation Run Report

## Run metadata

- Date/time: `2026-05-13 17:52:45 +03:00`
- Branch: `codex/front`
- Dirty working tree caveat: repository was already dirty before this run; this report covers only the helper-script adaptation, README update, and content run evidence
- Backend profile: `dev`
- Auth mode used: `DemoHeader`

## Files changed

- [create-demo-content.ps1](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/create-demo-content.ps1:1)
- [README-demo-content.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/README-demo-content.md:1)
- [live-content-creation-run-report.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/live-content-creation-run-report.md:1)

## Commands executed

| Command | Purpose | Result |
| --- | --- | --- |
| `git status --short` | Confirm starting working tree state | Dirty working tree confirmed |
| `Get-Content demo-data/scripts/create-demo-content.ps1` | Inspect helper before edit | Token-only auth confirmed |
| `Get-Content demo-data/scripts/README-demo-content.md` | Inspect README before edit | Bearer-only instructions confirmed |
| `PowerShell parser check for demo-data/scripts/create-demo-content.ps1` | Syntax preflight after auth edit | `POWERSHELL_PARSE_OK` |
| `docker exec training_platform_postgres psql -U postgres -d training_platform -c "select count(*) from course/topic/material/question/test"` | Baseline gate before run | All five counts were `0` |
| `.\demo-data\scripts\create-demo-content.ps1` | Controlled helper run attempt #1 | Failed before API calls because of PowerShell parser bug in helper string interpolation |
| `curl.exe -sS -i http://localhost:8080/actuator/health` | Check backend after helper retry failure | Failed; backend was down on `8080` |
| `cmd /c mvnw.cmd -DskipTests spring-boot:run` with only `SPRING_PROFILES_ACTIVE=dev` | Restart backend | Failed; datasource env was missing |
| `cmd /c mvnw.cmd -DskipTests spring-boot:run` with `SPRING_PROFILES_ACTIVE=dev`, `SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5433/training_platform`, `SPRING_DATASOURCE_USERNAME=postgres`, `SPRING_DATASOURCE_PASSWORD=postgres` | Restart backend with explicit demo DB env | PASS; backend returned `200` on `/actuator/health` |
| `curl.exe -sS -i -H "X-Demo-Actor-Id: 1" http://localhost:8080/api/v1/me` | Confirm dev-header auth after restart | PASS; `DEMO-ADMIN-001` |
| `.\demo-data\scripts\create-demo-content.ps1` | Controlled helper run attempt #2 | Created `3` courses and `8` topics, then failed on first `POST /api/v1/expert/content/materials` with HTTP `400` |
| `docker exec training_platform_postgres psql -U postgres -d training_platform -c "...counts and statuses..."` | Capture partial state after failure | `3` courses, `8` topics, `0` materials, `0` questions, `0` tests immediately after helper failure |
| `curl.exe -sS -i -H "X-Demo-Actor-Id: 1" -H "Content-Type: application/json" --data-binary @tmp/material-create-payload.json http://localhost:8080/api/v1/expert/content/materials` | Isolated repro of first failing material endpoint | Unexpectedly succeeded with HTTP `200`; created material `id=1` |
| `Invoke-RestMethod -Method Post ... /api/v1/expert/content/materials` with the second material payload | Check whether PowerShell transport itself reproduces the failure | Unexpectedly succeeded; created material `id=2` |
| `git status --short` | Capture final working tree state | Helper and report files modified; repo still broadly dirty |

## Script changes summary

- `DemoHeader` auth mode added as default.
- `Bearer` mode preserved as optional.
- Safe preflight `GET /api/v1/me` added before any content creation.
- No payload data, counts, or endpoint paths were changed.
- Helper parser bug fixed for context strings using `:${var}` interpolation.

## Baseline counts before run

| Entity | Count |
| --- | ---: |
| `course` | `0` |
| `topic` | `0` |
| `material` | `0` |
| `question` | `0` |
| `test` | `0` |

## Content creation result

- Verdict: `FAIL`
- Helper run reached live backend successfully.
- Helper created:
  - `3` courses
  - `8` topics
- First failing endpoint:
  - `POST /api/v1/expert/content/materials`
- First failing symptom:
  - helper run attempt #2 stopped on first material create with HTTP `400 Bad Request`
- Response body from the failing helper call:
  - not surfaced by the current helper exception output
- Important follow-up fact:
  - isolated repros against the same material endpoint succeeded with both `curl.exe` and `Invoke-RestMethod`, so the `400` is not yet stably reproduced outside the failed helper run

## Counts after run

| Entity | Count |
| --- | ---: |
| `course` | `3` |
| `topic` | `8` |
| `material` | `2` |
| `question` | `0` |
| `test` | `0` |

Additional state:

- `active final control tests = 0`
- `course` publication states: all current rows are `DRAFT`
- `topic` publication states: all current rows are `DRAFT`
- `self tests` and `final control` checks are not applicable yet because no `test` rows were created

## Blockers

| ID | Symptom | Endpoint/command | Expected | Actual | Suspected module | Recommended next remediation |
| --- | --- | --- | --- | --- | --- | --- |
| `CONTENT-SCRIPT-PARSER-BLOCKER` | Helper could not start on first attempt because of invalid PowerShell string interpolation with `:$var` | `.\demo-data\scripts\create-demo-content.ps1` | Script should pass parser preflight and start execution | Parser failed before any API call | `demo-data/scripts` | Fixed in helper; no further action unless more parser defects appear |
| `CONTENT-RUNTIME-STARTUP-ENV-BLOCKER` | Direct `spring-boot:run` restart failed without datasource env | `cmd /c mvnw.cmd -DskipTests spring-boot:run` with only `SPRING_PROFILES_ACTIVE=dev` | Backend should start against demo DB | Startup failed with `Failed to configure a DataSource: 'url' attribute is not specified` | local runtime bootstrap | Use explicit `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` when restarting backend for demo runs |
| `CONTENT-MATERIAL-CREATE-400-BLOCKER` | Helper run stopped on first material creation | `POST /api/v1/expert/content/materials` from helper attempt #2 | First material should be created and helper should continue to questions/tests | Helper received HTTP `400 Bad Request` and stopped; body was not surfaced in helper output | helper runtime interaction around material create | Instrument helper to print HTTP error body and serialized request body for the failing call, then rerun only from a clean baseline |
| `CONTENT-PARTIAL-STATE-BLOCKER` | Demo content baseline is no longer empty after failed helper and isolated repro calls | SQL counts after run | Either full successful content creation or clean zero baseline after failure | Partial state now exists: `3` courses, `8` topics, `2` materials, `0` questions, `0` tests | demo DB state management | Do not rerun helper on this DB without a separate cleanup/reset decision |
| `CONTENT-REPRO-MISMATCH` | The first helper `400` could not be reproduced by isolated manual calls to the same endpoint | `curl.exe` and `Invoke-RestMethod` isolated calls to `/api/v1/expert/content/materials` | Manual isolated repro should fail the same way if backend contract is the issue | Both isolated calls returned HTTP `200` and created materials `id=1` and `id=2` | helper execution path or transport details | Capture helper request/response body exactly on next run; compare with isolated request payload byte-for-byte |

## Final verdict

`CONTENT_PARTIALLY_CREATED_BLOCKED`

The helper adaptation for `DemoHeader` auth is in place and the live run can reach the backend. The content creation run is blocked because the helper stopped on the first material create after creating courses and topics, and the DB now contains partial content state. A clean rerun should happen only after the failing material request is instrumented and the demo DB baseline is reset by separate decision.
