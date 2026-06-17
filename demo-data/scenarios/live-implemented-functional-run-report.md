## Run Metadata

- Date/time: `2026-05-14 Europe/Moscow`
- Branch: `codex/front`
- DB name: `training_platform_demo_fullcycle`
- Backend profile: `dev`
- Dirty working tree caveat: repository was already heavily dirty before this acceptance pass, including unrelated frontend, backend, analytics, tests, tmp, and untracked files. Only demo-data/demo-script artifacts produced in this run are treated as acceptance-run deliverables.

## Scope Statement

- All contours except maintenance contour were treated as implemented and therefore subject to live acceptance.
- maintenance contour was treated as implemented only through Stage 0-2.
- maintenance contour post-Stage-2 analytics rebuild, aggregate population, and managerial historical analytics were classified as `NOT_IMPLEMENTED_BY_SCOPE`, not as runtime blockers for this implemented-scope run.

## Changed Files

- `demo-data/scripts/reset-demo-runtime-slice.ps1`
- `demo-data/scenarios/implemented-functional-test-scope.md`
- `demo-data/scenarios/live-implemented-functional-run-report.md`

## Commands Executed

### Repository / environment checks

```powershell
git status --short
git branch --show-current
docker ps
```

### Backend startup on single demo DB

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5433/training_platform_demo_fullcycle"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
Start-Process -FilePath ".\mvnw.cmd" -ArgumentList "-DskipTests","spring-boot:run" -WorkingDirectory "D:\Users\vladi\Desktop\Diplom\Java\training-platform"
Invoke-RestMethod -Uri "http://127.0.0.1:8080/actuator/health"
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/me" -Headers @{ "X-Demo-Actor-Id" = "1" }
```

### Runtime slice reset and parser checks

```powershell
[void][System.Management.Automation.Language.Parser]::ParseFile("D:\Users\vladi\Desktop\Diplom\Java\training-platform\demo-data\scripts\reset-demo-runtime-slice.ps1",[ref]$null,[ref]$null)
[void][System.Management.Automation.Language.Parser]::ParseFile("D:\Users\vladi\Desktop\Diplom\Java\training-platform\demo-data\scripts\create-demo-content.ps1",[ref]$null,[ref]$null)
powershell -ExecutionPolicy Bypass -File .\demo-data\scripts\reset-demo-runtime-slice.ps1
```

### Content creation

```powershell
powershell -ExecutionPolicy Bypass -File .\demo-data\scripts\create-demo-content.ps1
rg -n "«|»" tmp/demo-content-run-logs
```

### Assignment campaigns

```powershell
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/admin/assignment-campaigns" -Headers @{ "X-Demo-Actor-Id" = "1"; "Content-Type" = "application/json" } -Body (Get-Content -Raw .\tmp\campaign-ops-ukk.json)
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/admin/assignment-campaigns" -Headers @{ "X-Demo-Actor-Id" = "1"; "Content-Type" = "application/json" } -Body (Get-Content -Raw .\tmp\campaign-ops-upv.json)
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/admin/assignment-campaigns" -Headers @{ "X-Demo-Actor-Id" = "1"; "Content-Type" = "application/json" } -Body (Get-Content -Raw .\tmp\campaign-permit-ukk.json)
```

### Live role execution

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/admin/org-units/tree" -Headers @{ "X-Demo-Actor-Id" = "1" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/admin/users" -Headers @{ "X-Demo-Actor-Id" = "1" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/expert/content/courses" -Headers @{ "X-Demo-Actor-Id" = "11" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/expert/content/topics/17/active-final-test" -Headers @{ "X-Demo-Actor-Id" = "11" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/assigned-learning/assignments" -Headers @{ "X-Demo-Actor-Id" = "13" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/assigned-learning/assignments/33/learning-context" -Headers @{ "X-Demo-Actor-Id" = "13" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/assigned-learning/assignments/33/assignment-tests/97/test-context" -Headers @{ "X-Demo-Actor-Id" = "13" }
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/assigned-attempt-entries/assignments/33/assignment-tests/97" -Headers @{ "X-Demo-Actor-Id" = "13" }
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/assigned-attempt-submissions/attempts/5" -Headers @{ "X-Demo-Actor-Id" = "13" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/assigned-learning/assignments" -Headers @{ "X-Demo-Actor-Id" = "21" }
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/assigned-attempt-entries/assignments/41/assignment-tests/121" -Headers @{ "X-Demo-Actor-Id" = "21" }
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/assigned-attempt-submissions/attempts/6" -Headers @{ "X-Demo-Actor-Id" = "21" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/self-testing/tests" -Headers @{ "X-Demo-Actor-Id" = "29" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/self-testing/tests/36" -Headers @{ "X-Demo-Actor-Id" = "29" }
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/self-attempt-entries/tests/36" -Headers @{ "X-Demo-Actor-Id" = "29" }
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/self-attempt-submissions/attempts/8" -Headers @{ "X-Demo-Actor-Id" = "29" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/self/results/history" -Headers @{ "X-Demo-Actor-Id" = "29" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/managerial/current-supervision" -Headers @{ "X-Demo-Actor-Id" = "4" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/managerial/historical-analytics/user-topic?periodStart=2026-01-01&periodEnd=2026-12-31" -Headers @{ "X-Demo-Actor-Id" = "4" }
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/managerial/historical-analytics/department-topic?periodStart=2026-01-01&periodEnd=2026-12-31" -Headers @{ "X-Demo-Actor-Id" = "4" }
```

### Final SQL verification

```powershell
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from organizational_unit_type;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from organizational_unit;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from app_user;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from user_organization_assignment;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from user_role_assignment;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from course;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from topic;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from material;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from question;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from answer_option;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from test;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from test_question;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from assignment_campaign;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from assignment;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from assignment_test;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from test_attempt;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from result;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from analytics_user_topic_aggregate;"
docker exec training_platform_postgres psql -U postgres -d training_platform_demo_fullcycle -c "select count(*) from analytics_department_topic_aggregate;"
```

## Fixes Performed During Run

### Helper / demo-script fixes

- Fixed `reset-demo-runtime-slice.ps1` so it executes `psql` input correctly through direct PowerShell piping instead of a broken nested `powershell -Command` shell-out.
- Fixed runtime-slice cleanup ordering for the cyclic `assignment_test.counted_result_id <-> result` relation by using a transaction-local `session_replication_role = replica` inside the allowlisted demo DB cleanup path.
- Preserved strict DB guardrails:
  - only allowlisted DB `training_platform_demo_fullcycle`;
  - no org/user/personnel deletion;
  - no broad cascade against baseline tables.

### Demo-data readiness

- Confirmed `create-demo-content.ps1` is usable for repeatable clean runs on a single DB after cleanup.
- Confirmed final-boundary payload normalization is active:
  - local normalization self-check passes;
  - runtime request logs contain no `«` / `»`;
  - answer-option path for `Q-OPS-INTRO-011-A` succeeds.
- Confirmed content-link composition preflight works with topic-local overlap across tests and no duplicate link inside one test.

### Production fixes

- None were required during this implemented-scope acceptance run.

## Final DB Counts

| Table / metric | Final value |
| --- | ---: |
| `organizational_unit_type` | 6 |
| `organizational_unit` | 13 |
| `app_user` | 52 |
| `user_organization_assignment` | 51 |
| `user_role_assignment` | 103 |
| `course` | 3 |
| `topic` | 8 |
| `material` | 27 |
| `question` | 85 |
| `answer_option` | 340 |
| `test` | 13 |
| `test_question` | 91 |
| `assignment_campaign` | 3 |
| `assignment` | 24 |
| `assignment_test` | 72 |
| `test_attempt` | 4 |
| `result` | 4 |
| `analytics_user_topic_aggregate` | 0 |
| `analytics_department_topic_aggregate` | 0 |

Analytics aggregate tables staying at `0` are expected under the current implemented-scope rule because maintenance contour post-Stage-2 aggregate runtime is outside current acceptance scope.

## Implemented Functionality Verification

### Admin, user, org, personnel

- Backend dev actor surface works.
- Admin `me`, org tree, and users list work.
- Org/personnel baseline is stable enough for assignment and managerial current supervision contours.

### Content and publication

- Content helper completed end-to-end on a clean runtime slice.
- Final content counts match the expected implemented demo dataset:
  - `course=3`
  - `topic=8`
  - `material=27`
  - `question=85`
  - `answer_option=340`
  - `test=13`
  - `test_question=91`
- Only OPO-related content is present.
- `INFOSEC` does not appear in course/topic/material/question/answer-option/test text.
- All tests are `CONTROL`.
- All tests use `scoring_policy_code=DEFAULT`.
- All tests are in published learner-visible state after helper completion.
- Active final controls are assigned only for assigned topics.
- Self tests are not active final controls.

### Assignment and learner/operator execution

- Assignment campaigns were created successfully for:
  - `COURSE-OPS-SAFETY` on `UKK`
  - `COURSE-OPS-SAFETY` on `UPV`
  - `COURSE-PERMIT-SAFETY` on `UKK`
- Final assignment counts:
  - `assignment_campaign=3`
  - `assignment=24`
  - `assignment_test=72`
- OPERATOR-1 completed an assigned attempt successfully and produced a passing assigned result.
- OPERATOR-2 completed an assigned attempt with wrong answers and produced a failing assigned result.

### Self-testing execution

- Self-testing catalog and self test detail are visible to the self learner contour.
- Successful self attempt flow was completed on test `36`, including start, save answers, submit, and history verification.
- One earlier failed self try on test `35` was caused by a runner-side field mismatch while composing answer-save URLs, not by runtime behavior. The corrected self run passed on the same environment without production changes.

### Result recording

- Result facts are recorded for both assigned and self contours.
- Final result rows show required separation:
  - assigned results: `2`
  - self results: `2`
- Self result rows have no `assignment_id`.
- Assigned rows do not leak into self-only history.

### Manager implemented contour

- `GET /api/v1/managerial/current-supervision` is implemented and passing.
- Live response is non-empty and represents current assigned supervision state.

## Not Implemented By Scope

The following items were observed but are not treated as implemented-scope failures:

- maintenance contour post-Stage-2 analytics rebuild runtime
- maintenance contour post-Stage-2 analytics aggregate population runtime
- maintenance contour post-Stage-2 managerial historical analytics based on aggregate tables
- Rebuild/reconciliation/recovery runtime surfaces that belong to the unfinished maintenance contour post-Stage-2 contour

Observed evidence:

- `result=4`
- `analytics_user_topic_aggregate=0`
- `analytics_department_topic_aggregate=0`
- `GET /api/v1/managerial/historical-analytics/user-topic?...` -> `200 []`
- `GET /api/v1/managerial/historical-analytics/department-topic?...` -> `200 []`

Under the accepted scope contract, these are scope caveats, not runtime blockers.

## Negative Checks

- Operator cannot mutate expert content:
  - actor `13`
  - `POST /api/v1/expert/content/courses`
  - `403 Forbidden`
- Learner cannot read foreign assigned resource:
  - actor `29`
  - `GET /api/v1/assigned-learning/assignments/33`
  - `404 Not Found`
- Manager cannot perform admin mutation:
  - actor `4`
  - `POST /api/v1/admin/org-units/999999/archive`
  - `403 Forbidden`
- Operator cannot access managerial current supervision:
  - actor `13`
  - `GET /api/v1/managerial/current-supervision`
  - `403 Forbidden`
- Learner cannot access managerial current supervision:
  - actor `29`
  - `GET /api/v1/managerial/current-supervision`
  - `403 Forbidden`
- Data-separation invariants:
  - self results with non-null `assignment_id`: `0`
  - assigned results for self learner actor `29`: `0`
  - self tests marked as active final controls: `0`

## Remaining Blockers

- `none`

No blocker remains for the currently implemented functionality contour. Remaining analytics gaps are tracked as scope caveats because they belong to unfinished maintenance contour post-Stage-2 work.

## Final Verdict

`IMPLEMENTED_FUNCTIONALITY_READY_WITH_SCOPE_CAVEATS`
