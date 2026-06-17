# Live Content Questions Run Report

## Run metadata

- Date/time: `2026-05-13 20:24 Europe/Moscow`
- Branch: `codex/front`
- Dirty working tree caveat: repository already contained many unrelated local changes before this controlled live run.
- Backend profile: `dev`
- Database: `training_platform_questions_rerun`
- Auth mode: `DemoHeader`
- Demo actor header: `X-Demo-Actor-Id: 1`

## Fresh DB / reset strategy

Strategy used:
- new clean database instead of continuing any previous partial content state

Exact reset commands:
- `docker exec training_platform_postgres psql -U postgres -d postgres -c "drop database if exists training_platform_questions_rerun with (force);"`
- `docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_questions_rerun;"`

## Backend startup and auth evidence

Backend startup:
- datasource URL: `jdbc:postgresql://127.0.0.1:5433/training_platform_questions_rerun`
- datasource user: `postgres`

Readiness checks:
- `GET /actuator/health` -> `200 OK`
- initial `GET /api/v1/me` after bootstrap + personnel baseline returned `404 User not found by id: 1`
- after org import, `GET /api/v1/me` with `X-Demo-Actor-Id: 1` returned `200 OK`

Authenticated actor for helper run:
- `actorUserId = 1`
- `username = DEMO-ADMIN-001`
- `roles = ["ADMIN"]`

## Org/personnel baseline counts

Baseline restore path used:
- `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_questions_rerun`
- `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i training_platform_postgres psql -U postgres -d training_platform_questions_rerun`
- PowerShell CSV/API import to:
  - `POST /api/v1/admin/org-unit-types`
  - `POST /api/v1/admin/org-units`

Observed counts before helper:
- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 0`
- `user_role_assignment = 101`

## Clean content baseline counts

Before helper:
- `course = 0`
- `topic = 0`
- `material = 0`
- `question = 0`
- `test = 0`
- `answer_option = 0`

## Helper configuration

- `AuthMode = "DemoHeader"`
- `DemoActorId = "1"`
- `DumpRequests = $true`
- `StopAfterSection = "Questions"`

Debug logs directory:
- `tmp/demo-content-run-logs`

## Commands executed

| Command | Purpose | Result |
|---|---|---|
| `docker exec ... drop database if exists training_platform_questions_rerun with (force)` | clear old demo DB | PASS |
| `docker exec ... create database training_platform_questions_rerun` | create fresh DB | PASS |
| backend start with `SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5433/training_platform_questions_rerun` | run backend on clean DB | PASS |
| `curl.exe -i http://localhost:8080/actuator/health` | readiness check | PASS |
| `Get-Content -Raw tmp/bootstrap-demo-import.sql | docker exec -i ...` | demo admin bootstrap | PASS |
| `Get-Content -Raw tmp/personnel-demo-baseline.sql | docker exec -i ...` | restore personnel baseline | PASS with future assignment drift |
| PowerShell CSV/API import to org endpoints | restore org tree | PASS |
| `curl.exe -i -H "X-Demo-Actor-Id: 1" http://localhost:8080/api/v1/me` | auth check before helper | PASS |
| `powershell -File demo-data/scripts/create-demo-content.ps1` | controlled helper run through `Questions` | FAIL at answer option create |

## Questions run result

Verdict: `QUESTIONS_FAILED_WITH_BODY`

Observed helper behavior before failure:
- created `3` courses
- created `8` topics
- created `27` materials
- created `11` questions
- created `40` answer options
- failed on the first option for `Q-OPS-INTRO-011`

First failing operation:
- operation: `create-option:Q-OPS-INTRO-011-A`
- endpoint: `POST /api/v1/expert/content/questions/11/answer-options`
- HTTP status: `400 Bad Request`

Exact request body:

```json
{
  "body": "Он говорит, что «и так всё понятно» без обращения к инструкции",
  "pairingKey": null,
  "isCorrect": true,
  "displayOrder": 0,
  "canonicalOrderPosition": null,
  "answerOptionRole": "CHOICE_OPTION"
}
```

Exact response body:

```json
{"timestamp":"2026-05-13T17:24:43.794656700Z","status":400,"error":"Bad Request","message":"Request body is invalid","correlationId":"a036b3d3-6e11-4132-8f6b-2370d8d935ff"}
```

Error artifact:
- [0091-create-option-Q-OPS-INTRO-011-A.error.txt](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0091-create-option-Q-OPS-INTRO-011-A.error.txt:1)

Request artifact:
- [0091-create-option-Q-OPS-INTRO-011-A.request.json](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0091-create-option-Q-OPS-INTRO-011-A.request.json:1)

Response artifact:
- [0091-create-option-Q-OPS-INTRO-011-A.response.json](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0091-create-option-Q-OPS-INTRO-011-A.response.json:1)

## Post-run counts

- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 11`
- `test = 0`
- `answer_option = 40`

## Question type verification

Observed in partial state:
- `SINGLE_CHOICE = 6`
- `MULTIPLE_CHOICE = 5`

Unexpected question types:
- none observed in created rows

INFOSEC leakage checks:
- question bodies with INFOSEC/self-hygiene lexicon: `0`
- answer option bodies with INFOSEC/self-hygiene lexicon: `0`

## Answer option verification

Expected from `question-bank.md`:
- `340` option lines total

Actual at failure point:
- `40`

Partial-state invariants:
- created questions with option rows have `min = 4`, `max = 4` options
- `SINGLE_CHOICE` questions with invalid correct-count: `0`
- `MULTIPLE_CHOICE` questions with invalid correct-count: `1`

Why one `MULTIPLE_CHOICE` invariant is broken:
- `Q-OPS-INTRO-011` was created
- its first option failed before any options were stored
- helper correctly stopped and did not continue

## Error details

- no manual isolated question calls were executed after failure
- no attempt was made to continue helper past the broken runtime ID mapping point
- failure remains localized to answer option create for `Q-OPS-INTRO-011-A`

## Future blocker note

- `user_organization_assignment = 0` does not block questions creation
- but it remains a future blocker for assignment / learner / manager demo stages on this DB baseline

## Final verdict

`STILL_BLOCKED_BY_QUESTION_CREATE`

## Recommended next step

- inspect answer option create contract and runtime validation for `CHOICE_OPTION`
- compare a known-good answer option payload with the failing `Q-OPS-INTRO-011-A` payload
- rerun only after a fresh clean DB reset
