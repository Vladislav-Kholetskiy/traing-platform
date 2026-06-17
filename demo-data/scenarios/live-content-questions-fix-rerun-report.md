# Live Content Questions Fix Rerun Report

## 1. Run metadata

- Date: `2026-05-13`
- Workspace: `D:\Users\vladi\Desktop\Diplom\Java\training-platform`
- Backend profile: `dev`
- Auth mode: `DemoHeader`
- Demo actor: `X-Demo-Actor-Id: 1`
- Fresh DB: `training_platform_questions_fix_rerun`
- Dirty working tree caveat: repository was already dirty before this rerun.

## 2. Fresh DB / reset strategy

- Strategy: use a new clean database instead of continuing any previous partial content state.
- Old partial databases not reused:
  - `training_platform_questions_rerun`
  - `training_platform_materials_rerun`
  - `training_platform_first_material_fix`
  - `training_platform_first_material_rerun`
- Reset commands:

```powershell
docker exec training_platform_postgres psql -U postgres -d postgres -c "drop database if exists training_platform_questions_fix_rerun with (force);"
docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_questions_fix_rerun;"
```

## 3. Backend startup and auth evidence

- Datasource env:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5433/training_platform_questions_fix_rerun"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
```

- Health check: `GET /actuator/health` -> `200 OK`
- Auth check: `GET /api/v1/me` with `X-Demo-Actor-Id: 1` -> `200 OK`

## 4. Org/personnel baseline counts

- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 0`
- `user_role_assignment = 101`

Note:
- `user_organization_assignment = 0` did not block the controlled `Questions` run.
- It remains a future blocker for `assignment / learner / manager` stages.

## 5. Clean content baseline counts

- `course = 0`
- `topic = 0`
- `material = 0`
- `question = 0`
- `answer_option = 0`
- `test = 0`

Verdict:
- clean content baseline confirmed before helper run.

## 6. Helper configuration

- `AuthMode = "DemoHeader"`
- `DemoActorId = "1"`
- `DumpRequests = $true`
- `StopAfterSection = "Questions"`

Debug directory:
- `tmp/demo-content-run-logs`

## 7. Commands executed

| Command | Purpose | Result |
| --- | --- | --- |
| `git status --short` | preflight dirty tree capture | completed |
| `docker exec training_platform_postgres psql -U postgres -d postgres -c "drop database if exists training_platform_questions_fix_rerun with (force);"` | remove prior DB if present | completed |
| `docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_questions_fix_rerun;"` | create fresh DB | completed |
| backend start with `SPRING_DATASOURCE_*` env | start app on fresh DB | completed |
| `GET /actuator/health` | backend readiness | `200 OK` |
| demo bootstrap SQL + org import flow | restore baseline | completed |
| `GET /api/v1/me` with `X-Demo-Actor-Id: 1` | auth check | `200 OK` |
| `Remove-Item tmp/demo-content-run-logs -Recurse -Force` | clear old helper logs | completed |
| `.\demo-data\scripts\create-demo-content.ps1` | controlled rerun through `Questions` | failed during answer option create |

## 8. Questions rerun result

- Result: `QUESTIONS_STILL_FAILED_WITH_BODY`

What completed before failure:
- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 11`
- `answer_option = 40`
- `test = 0`

Failing operation:
- `create-option:Q-OPS-INTRO-011-A`

Failing endpoint:
- `POST /api/v1/expert/content/questions/11/answer-options`

## 9. Post-run counts

- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 11`
- `answer_option = 40`
- `test = 0`

## 10. Question type verification

Observed question types in partial state:
- `SINGLE_CHOICE = 6`
- `MULTIPLE_CHOICE = 5`

Other question types:
- not found

INFOSEC codes/text:
- not found in created `question` rows

## 11. Answer option verification

Observed answer option table:
- `answer_option`

Partial verification:
- options per successfully populated question: exactly `4`
- `SINGLE_CHOICE` questions with wrong correct-option count: `0`
- `MULTIPLE_CHOICE` questions with wrong correct-option count: `1`

Explanation:
- the single invalid case is caused by the failing question `Q-OPS-INTRO-011`, which was created but did not receive its first option because helper stopped at the first `400`.

INFOSEC codes/text:
- not found in created `answer_option` rows

## 12. Normalization verification

Expected after payload normalization fix:
- runtime request bodies should replace typographic quotes `« »` with regular quotes `"`.

Actual:
- the failing runtime payload still contained typographic quotes.

Captured failing request body:

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

Normalization verdict:
- normalization did **not** take effect for the failing answer option payload in this rerun.

## 13. Error details

- Status: `400 Bad Request`
- Response body:

```json
{"timestamp":"2026-05-13T17:38:31.588046400Z","status":400,"error":"Bad Request","message":"Request body is invalid","correlationId":"73eadecb-4890-4d4b-a8f9-5d8174fda43f"}
```

Relevant debug artifacts:
- [0091-create-option-Q-OPS-INTRO-011-A.request.json](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0091-create-option-Q-OPS-INTRO-011-A.request.json:1)
- [0091-create-option-Q-OPS-INTRO-011-A.response.json](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0091-create-option-Q-OPS-INTRO-011-A.response.json:1)
- [0091-create-option-Q-OPS-INTRO-011-A.error.txt](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0091-create-option-Q-OPS-INTRO-011-A.error.txt:1)

## 14. Future blocker note

- `user_organization_assignment = 0` remains unresolved for this baseline path.
- It does not block `content` creation, but it is expected to block or distort:
  - assignment targeting
  - learner assigned contour
  - managerial supervision / analytics

## 15. Final verdict

- `STILL_BLOCKED_BY_QUESTION_CREATE`

## 16. Recommended next step

- Inspect why helper runtime serialization still emits typographic quotes even after `Normalize-TextPayload` was introduced.
- Do not continue to `Tests` until `Questions` can be created end-to-end on a fresh clean DB.
