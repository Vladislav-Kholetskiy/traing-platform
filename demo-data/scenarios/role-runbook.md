# Role Runbook

Artifact status: `IMPLEMENTED_SCOPE_ROLE_RUNBOOK_V2`

This runbook covers the manually verifiable role contour for all implemented functionality on `training_platform_demo_fullcycle`.

Scope rule:
- Historical managerial analytics and analytics rebuild are not mandatory in this runbook.
- Current managerial supervision is mandatory.
- Notification reads are checked if the runtime surfaces exist.
- Empty notification tables are acceptable if producer wiring is not part of the current runtime contour.

## Preconditions

- Backend is running in `dev` profile.
- Demo DB is `training_platform_demo_fullcycle`.
- Org/personnel baseline is restored.
- Runtime slice was cleaned with [reset-demo-runtime-slice.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/reset-demo-runtime-slice.ps1).
- Content was created with [create-demo-content.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/create-demo-content.ps1).
- Assignment campaigns were launched for:
  - `COURSE-OPS-SAFETY` on `CCK-UKK`
  - `COURSE-OPS-SAFETY` on `CCK-UPV`
  - `COURSE-PERMIT-SAFETY` on `CCK-UKK`

## Actor table

| Actor alias | Actor id | Expected role | Org contour | Mandatory scenario |
| --- | ---: | --- | --- | --- |
| `ADMIN` | 1 | `ADMIN` | system/admin contour | baseline, org, users, notification admin read |
| `EXPERT` | 11 | `EXPERT` | `EDU` | content visibility, final-control read |
| `OPERATOR-1` | 13 | `OPERATOR` | `CCK-UKK` | successful assigned pass |
| `OPERATOR-2` | 21 | `OPERATOR` | `CCK-UPV` | failed/lower-score assigned pass |
| `MANAGER-1` | 4 | `MANAGER` | `CCK` | current supervision |
| `LEARNER-SELF-1` | 29 | learner self contour | `CMT-MTBEO` | self-testing and self history |

## ADMIN

1. Open `GET /api/v1/me` with actor `1`.
Expected: `200`, admin actor resolved.

2. Open `GET /api/v1/admin/org-units/tree`.
Expected: non-empty org tree rooted at `/department`.

3. Open `GET /api/v1/admin/users`.
Expected: non-empty user list; baseline actors exist.

4. Open `GET /api/v1/admin/notifications`.
Expected: `200`.
Note: empty list is acceptable if business notification emission is not wired for the current contour.

## EXPERT

1. Open `GET /api/v1/expert/content/courses` with actor `11`.
Expected: `3` published demo courses corresponding to:
- `COURSE-OPS-SAFETY`
- `COURSE-PERMIT-SAFETY`
- `COURSE-OPO-SELF-REFRESHER`

2. Open active final read for one assigned topic:
- `GET /api/v1/expert/content/topics/{assignedTopicId}/active-final-test`
Expected: `200`, `CONTROL`, published final-control test.

3. Verify content semantics:
- only OPO-related content
- no `INFOSEC`
- self tests are published but not active final controls

## OPERATOR-1: successful assigned path

1. Open `GET /api/v1/assigned-learning/assignments` with actor `13`.
Expected: sees own assignments only.

2. Open one assignment detail:
- `GET /api/v1/assigned-learning/assignments/{assignmentId}`
Expected: assigned course detail loads.

3. Open learning context:
- `GET /api/v1/assigned-learning/assignments/{assignmentId}/learning-context`
Expected: topics, material metadata, and assignment tests are visible.

4. Open assigned test context:
- `GET /api/v1/assigned-learning/assignments/{assignmentId}/assignment-tests/{assignmentTestId}/test-context`
Expected: final assigned test detail loads.

5. Start assigned attempt:
- `POST /api/v1/assigned-attempt-entries/assignments/{assignmentId}/assignment-tests/{assignmentTestId}`
Expected: attempt is created.

6. Save correct answers:
- `PUT /api/v1/assigned-attempt-answers/attempts/{attemptId}/questions/{questionId}`
Expected: each save returns success.

7. Submit:
- `POST /api/v1/assigned-attempt-submissions/attempts/{attemptId}`
Expected: passing assigned result is recorded.

8. Verify by SQL/API:
- result belongs to actor `13`
- result is `ASSIGNED`
- `assignment_id` and `assignment_test_id` are filled
- `passed = true`

## OPERATOR-2: failed/lower-score assigned path

1. Open `GET /api/v1/assigned-learning/assignments` with actor `21`.
Expected: sees own assignments only.

2. Start assigned attempt on own assignment test.
Expected: separate attempt id from OPERATOR-1.

3. Save wrong or partially wrong answers and submit.
Expected: result is recorded below threshold.

4. Verify by SQL/API:
- result belongs to actor `21`
- result is `ASSIGNED`
- result id differs from OPERATOR-1 result
- `passed = false` or lower-score outcome

5. Verify isolation:
- OPERATOR-2 cannot see OPERATOR-1 assignment/detail/attempt

## LEARNER-SELF-1

1. Open `GET /api/v1/self-testing/tests` with actor `29`.
Expected: non-empty self catalog from `COURSE-OPO-SELF-REFRESHER`.

2. Open one self test detail:
- `GET /api/v1/self-testing/tests/{testId}`
Expected: self-visible test loads.

3. Start self attempt:
- `POST /api/v1/self-attempt-entries/tests/{testId}`
Expected: self attempt is created.

4. Save answers and submit:
- `PUT /api/v1/self-attempt-answers/attempts/{attemptId}/questions/{questionId}`
- `POST /api/v1/self-attempt-submissions/attempts/{attemptId}`
Expected: self result is recorded.

5. Open self history:
- `GET /api/v1/self/results/history`
Expected: only self results are visible to actor `29`.

6. Open self notifications:
- `GET /api/v1/self/notifications`
Expected: `200`.
Note: empty list is acceptable if no emitter wiring exists.

7. Verify by SQL:
- self result rows have null `assignment_id`
- self results do not mix with assigned contour

## MANAGER-1

1. Open `GET /api/v1/managerial/current-supervision` with actor `4`.
Expected: non-empty current supervision payload.

2. Verify scope contents.
Expected: OPERATOR-1 and OPERATOR-2 are visible if both are inside the `CCK` managerial contour.

3. Historical analytics caveat.
Do not treat the following as failure:
- `GET /api/v1/managerial/historical-analytics/user-topic?... -> 200 []`
- `GET /api/v1/managerial/historical-analytics/department-topic?... -> 200 []`

Reason:
- this belongs to maintenance contour post-Stage-2 and is `NOT_IMPLEMENTED_BY_SCOPE`.

## Negative checks

1. Operator cannot mutate expert content.
- `POST /api/v1/expert/content/courses` as actor `13`
Expected: `403`.

2. Learner cannot open foreign assigned resource.
- `GET /api/v1/assigned-learning/assignments/{foreignAssignmentId}` as actor `29`
Expected: `404` or `403`.

3. Manager cannot perform admin mutation.
- `POST /api/v1/admin/org-units/999999/archive` as actor `4`
Expected: `403`.

4. Operator and learner cannot access managerial current supervision.
- actor `13` -> `GET /api/v1/managerial/current-supervision`
- actor `29` -> `GET /api/v1/managerial/current-supervision`
Expected: `403` in both cases.

5. Learner cannot access admin notifications.
- actor `29` -> `GET /api/v1/admin/notifications`
Expected: `403`.

## Evidence collection

Capture at minimum:
- admin `me`
- org tree
- users list
- expert course list
- active final-control read
- OPERATOR-1 assigned detail and successful result
- OPERATOR-2 assigned detail and failed/lower-score result
- self catalog and self history
- manager current supervision
- notification admin/self reads
