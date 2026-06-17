# Acceptance Checkpoints

Artifact status: `IMPLEMENTED_SCOPE_ACCEPTANCE_CHECKLIST_V3`

This file is the mandatory checkpoint list for the repeatable implemented-scope run on `training_platform_demo_fullcycle`.

Scope rule:
- All contours except maintenance contour are treated as implemented.
- maintenance contour is treated as implemented only through Stage 0-2.
- maintenance contour post-Stage-2 historical analytics, rebuild, and aggregate population are not mandatory checks and must be treated as `NOT_IMPLEMENTED_BY_SCOPE`.

## Global prerequisites

| Stage | Check ID | Purpose | Check | Expected result | Status rule |
| --- | --- | --- | --- | --- | --- |
| `GLOBAL` | `PREREQ-001` | Verify backend is reachable | `GET /actuator/health` | `200`, status `UP` | mandatory |
| `GLOBAL` | `PREREQ-002` | Verify dev actor surface | `GET /api/v1/me` with `X-Demo-Actor-Id: 1` | `200`, actor resolved | mandatory |
| `GLOBAL` | `PREREQ-003` | Verify baseline counts | SQL counts for org/personnel tables | `organizational_unit_type=6`, `organizational_unit=13`, `app_user=52`, `user_organization_assignment=51`, `user_role_assignment=103` | mandatory |
| `GLOBAL` | `PREREQ-004` | Verify safe cleanup tool | `reset-demo-runtime-slice.ps1` parser + allowlist guard | parser OK, demo-only DB guard present | mandatory |
| `GLOBAL` | `PREREQ-005` | Verify content helper preflight | `create-demo-content.ps1 -PreflightOnly` | normalization self-checks + composition preflight pass without API calls | mandatory |
| `GLOBAL` | `PREREQ-006` | Verify smoke/verification kit | parser check for verification and smoke scripts | parser OK | mandatory |

## Stage 1. Clean runtime slice and content creation

| Stage | Check ID | Purpose | Check | Expected result | Status rule |
| --- | --- | --- | --- | --- | --- |
| `STAGE-1` | `CONTENT-001` | Runtime slice cleaned safely | run `reset-demo-runtime-slice.ps1` | content/assignment/result slice returns to zero; org/personnel untouched | mandatory |
| `STAGE-1` | `CONTENT-002` | Courses created | SQL `select count(*) from course` | `3` | mandatory |
| `STAGE-1` | `CONTENT-003` | Topics created | SQL `select count(*) from topic` | `8` | mandatory |
| `STAGE-1` | `CONTENT-004` | Materials created | SQL `select count(*) from material` | `27` | mandatory |
| `STAGE-1` | `CONTENT-005` | Questions created | SQL `select count(*) from question` | `85` | mandatory |
| `STAGE-1` | `CONTENT-006` | Answer options created | SQL `select count(*) from answer_option` | `340` | mandatory |
| `STAGE-1` | `CONTENT-007` | Tests created | SQL `select count(*) from test` | `13` | mandatory |
| `STAGE-1` | `CONTENT-008` | Test-question links created | SQL `select count(*) from test_question` | `91` | mandatory |
| `STAGE-1` | `CONTENT-009` | Content types are constrained | SQL distinct `material_type`, `question_type`, `test_type` | only `DOCX,PDF,TEXT,VIDEO`; only `SINGLE_CHOICE,MULTIPLE_CHOICE`; only `CONTROL` | mandatory |
| `STAGE-1` | `CONTENT-010` | No INFOSEC drift | SQL content text scan | `0` INFOSEC matches in course/topic/material/question/answer/test fields | mandatory |
| `STAGE-1` | `CONTENT-011` | Question quality preserved | SQL invariants | every question has `4` options; single-choice has `1` correct; multiple-choice has at least `2` correct | mandatory |
| `STAGE-1` | `CONTENT-012` | Publication state reached | SQL/API | published learner-visible state for courses/topics/tests | mandatory |
| `STAGE-1` | `CONTENT-013` | Final-control semantics preserved | SQL/API | `6` active finals on assigned topics only; self tests are not active finals | mandatory |
| `STAGE-1` | `CONTENT-014` | Runtime payload normalization held | `rg -n "«|»" tmp/demo-content-run-logs` | no matches | mandatory |

## Stage 2. Assignment setup

| Stage | Check ID | Purpose | Check | Expected result | Status rule |
| --- | --- | --- | --- | --- | --- |
| `STAGE-2` | `ASSIGN-001` | Assignment campaigns launched | SQL `select count(*) from assignment_campaign` | `3` | mandatory |
| `STAGE-2` | `ASSIGN-002` | Assignments materialized | SQL `select count(*) from assignment` | `24` | mandatory |
| `STAGE-2` | `ASSIGN-003` | Assignment tests materialized | SQL `select count(*) from assignment_test` | `72` | mandatory |
| `STAGE-2` | `ASSIGN-004` | Targeting stayed leaf-based | runbook/API evidence | campaigns target valid campaign-enabled leaf units | mandatory |
| `STAGE-2` | `ASSIGN-005` | Assigned learning reads are visible | smoke script / API | OPERATOR-1 and OPERATOR-2 both see their own assignments only | mandatory |

## Stage 3. Multi-employee assigned execution

| Stage | Check ID | Purpose | Check | Expected result | Status rule |
| --- | --- | --- | --- | --- | --- |
| `STAGE-3` | `RUN-ASSIGNED-001` | OPERATOR-1 success path | manual/API sequence + SQL | passed assigned result recorded for actor `13` | mandatory |
| `STAGE-3` | `RUN-ASSIGNED-002` | OPERATOR-2 lower-score path | manual/API sequence + SQL | failed/lower-score assigned result recorded for actor `21` | mandatory |
| `STAGE-3` | `RUN-ASSIGNED-003` | Assigned attempts are separate | SQL by `test_attempt.user_id` | different attempt ids for OPERATOR-1 and OPERATOR-2 | mandatory |
| `STAGE-3` | `RUN-ASSIGNED-004` | Assigned results are separate | SQL by `result.user_id` | different result ids for OPERATOR-1 and OPERATOR-2 | mandatory |
| `STAGE-3` | `RUN-ASSIGNED-005` | Assigned linkage preserved | SQL on `result.assignment_id`, `result.assignment_test_id` | both assigned results keep assignment linkage | mandatory |
| `STAGE-3` | `RUN-ASSIGNED-006` | Manager sees both operators in current scope | `GET /api/v1/managerial/current-supervision` | non-empty payload including both assigned employees in manager scope | mandatory |

## Stage 4. Self execution

| Stage | Check ID | Purpose | Check | Expected result | Status rule |
| --- | --- | --- | --- | --- | --- |
| `STAGE-4` | `RUN-SELF-001` | Self catalog works | `GET /api/v1/self-testing/tests` | non-empty catalog for actor `29` | mandatory |
| `STAGE-4` | `RUN-SELF-002` | Self attempt executes | manual/API sequence + SQL | self attempt created and submitted | mandatory |
| `STAGE-4` | `RUN-SELF-003` | Self history works | `GET /api/v1/self/results/history` | self learner sees only self results | mandatory |
| `STAGE-4` | `RUN-SELF-004` | Self/assigned separation preserved | SQL | self results have null assignment linkage; assigned results do not leak into self history contour | mandatory |

## Stage 5. Notifications and managerial read scope

| Stage | Check ID | Purpose | Check | Expected result | Status rule |
| --- | --- | --- | --- | --- | --- |
| `STAGE-5` | `NOTIFY-001` | Notification admin read API is reachable | `GET /api/v1/admin/notifications` | `200` for admin actor | mandatory |
| `STAGE-5` | `NOTIFY-002` | Notification self read API is reachable | `GET /api/v1/self/notifications` | `200` for self learner actor | mandatory |
| `STAGE-5` | `NOTIFY-003` | Notification emission is classified correctly | SQL `select count(*) from notification` plus code inventory | empty table is acceptable and must be classified as `NOT_RUNTIME_SURFACE` if no producer wiring exists | mandatory |
| `STAGE-5` | `MANAGER-001` | Manager current supervision works | `GET /api/v1/managerial/current-supervision` | `200`, non-empty list after assigned runs | mandatory |
| `STAGE-5` | `MANAGER-002` | Historical analytics caveat is explicit | SQL + API | `analytics_*_aggregate = 0` and historical analytics may return `200 []` without failing acceptance | mandatory scope caveat |

## Negative checks

| Stage | Check ID | Purpose | Check | Expected result | Status rule |
| --- | --- | --- | --- | --- | --- |
| `NEGATIVE` | `NEG-001` | Operator cannot mutate expert content | `POST /api/v1/expert/content/courses` as actor `13` | `403` | mandatory |
| `NEGATIVE` | `NEG-002` | Learner cannot read foreign assignment | `GET /api/v1/assigned-learning/assignments/{foreignId}` as actor `29` | `404` or `403` fail-closed | mandatory |
| `NEGATIVE` | `NEG-003` | Manager cannot perform admin mutation | `POST /api/v1/admin/org-units/999999/archive` as actor `4` | `403` | mandatory |
| `NEGATIVE` | `NEG-004` | Operator cannot read managerial current supervision | `GET /api/v1/managerial/current-supervision` as actor `13` | `403` | mandatory |
| `NEGATIVE` | `NEG-005` | Learner cannot read managerial current supervision | `GET /api/v1/managerial/current-supervision` as actor `29` | `403` | mandatory |
| `NEGATIVE` | `NEG-006` | Learner cannot read admin notifications | `GET /api/v1/admin/notifications` as actor `29` | `403` | mandatory |
| `NEGATIVE` | `NEG-007` | Self tests never become active finals | SQL | `0` self-topic tests with `is_active_final_for_topic = true` | mandatory |

## Scope caveat

The following must never be reported as implemented-scope failures in this acceptance kit:

- `analytics_user_topic_aggregate = 0`
- `analytics_department_topic_aggregate = 0`
- `GET /api/v1/managerial/historical-analytics/user-topic?... -> 200 []`
- `GET /api/v1/managerial/historical-analytics/department-topic?... -> 200 []`

Reason:
- maintenance contour post-Stage-2 analytics rebuild/aggregate runtime is explicitly `NOT_IMPLEMENTED_BY_SCOPE`.
