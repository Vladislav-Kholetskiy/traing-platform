# Live Full Demo Run Report

## 1. Run metadata

- Date/time: `2026-05-13 Europe/Moscow`
- Branch: `codex/front`
- DB name: `training_platform_demo_fullcycle`
- Backend profile: `dev`
- Dirty working tree caveat:
  - worktree was already dirty before and during the run;
  - there are many unrelated frontend, analytics, tmp, archive and user-owned changes in `git status --short`;
  - demo execution changes below were made without reverting unrelated work.

## 2. Changed files

- Demo/helper scripts:
  - `demo-data/scripts/create-demo-content.ps1`
  - `demo-data/scripts/reset-demo-runtime-slice.ps1`
  - `tmp/personnel-demo-baseline.sql`
  - `demo-data/scenarios/live-full-demo-run-report.md`
- Production fixes for assignment launch and actor role normalization:
  - `src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandServiceImpl.java`
  - `src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignMandatoryRecipientEligibilitySeam.java`
  - `src/main/java/com/vladislav/training/platform/application/actor/CurrentActorReadService.java`
  - `src/main/java/com/vladislav/training/platform/userorg/service/CanonicalRoleCodeNormalizer.java`
  - `src/main/java/com/vladislav/training/platform/userorg/service/UserOperatorContourSupport.java`
  - `src/main/java/com/vladislav/training/platform/userorg/service/UserOrgFoundationStateReadServiceImpl.java`
- Production fixes for result scoring and self execution:
  - `src/main/java/com/vladislav/training/platform/result/service/CanonicalResultQuestionScoringEvaluator.java`
  - `src/main/java/com/vladislav/training/platform/testing/admission/SelfAttemptEntryFoundationStateReadServiceImpl.java`
  - `src/main/java/com/vladislav/training/platform/testing/admission/SelfExecutionAdmissionFoundationStateReadServiceImpl.java`
  - `src/main/java/com/vladislav/training/platform/testing/admission/SelfCurrentAttemptReadFoundationStateReadServiceImpl.java`
- Narrow regression tests updated/added:
  - `src/test/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandServiceImplHappyPathTest.java`
  - `src/test/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandServiceImplRejectPathTest.java`
  - `src/test/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignMandatoryRecipientEligibilitySeamTest.java`
  - `src/test/java/com/vladislav/training/platform/application/actor/CurrentActorControllerDevProfileWebMvcTest.java`
  - `src/test/java/com/vladislav/training/platform/result/service/CanonicalResultQuestionScoringEvaluatorTest.java`
  - `src/test/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImplTest.java`
  - `src/test/java/com/vladislav/training/platform/testing/admission/SelfAttemptEntryFoundationStateReadServiceTest.java`
  - `src/test/java/com/vladislav/training/platform/testing/admission/SelfExecutionAdmissionFoundationStateReadServiceTest.java`
  - `src/test/java/com/vladislav/training/platform/testing/admission/SelfCurrentAttemptReadFoundationStateReadServiceTest.java`
  - `src/test/java/com/vladislav/training/platform/testing/admission/SelfExecutionAdmissionFoundationStateReadServiceBoundaryTest.java`

## 3. Commands executed

- `git status --short`
- `git branch --show-current`
- `docker exec training_platform_postgres psql -U postgres -d postgres -c "drop database if exists training_platform_demo_fullcycle with (force);"`
- `docker exec training_platform_postgres psql -U postgres -d postgres -c "create database training_platform_demo_fullcycle;"`
- backend startup on the same DB with:
  - `SPRING_PROFILES_ACTIVE=dev`
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5433/training_platform_demo_fullcycle`
  - `SPRING_DATASOURCE_USERNAME=postgres`
  - `SPRING_DATASOURCE_PASSWORD=postgres`
  - `mvn -DskipTests spring-boot:run`
- health/auth checks:
  - `GET http://127.0.0.1:8080/actuator/health`
  - `GET http://127.0.0.1:8080/api/v1/me` with `X-Demo-Actor-Id: 1, 4, 11, 13, 29`
- baseline/content flow:
  - baseline bootstrap/import flow on the same DB
  - `.\demo-data\scripts\reset-demo-runtime-slice.ps1`
  - `.\demo-data\scripts\create-demo-content.ps1`
- targeted Maven regression suites:
  - `mvn "-Dtest=AssignmentCampaignMandatoryRecipientEligibilitySeamTest,AssignmentCampaignCommandServiceImplHappyPathTest,AssignmentCampaignCommandServiceImplRejectPathTest,CurrentActorControllerDevProfileWebMvcTest,CurrentActorControllerDefaultProfileWebMvcTest" test`
  - `mvn "-Dtest=CanonicalResultQuestionScoringEvaluatorTest,ResultRecordingServiceImplTest,CurrentActorControllerDevProfileWebMvcTest,AssignmentCampaignMandatoryRecipientEligibilitySeamTest,AssignmentCampaignCommandServiceImplHappyPathTest,AssignmentCampaignCommandServiceImplRejectPathTest" test`
  - `mvn "-Dtest=CanonicalResultQuestionScoringEvaluatorTest,CurrentActorControllerDevProfileWebMvcTest,AssignmentCampaignMandatoryRecipientEligibilitySeamTest,AssignmentCampaignCommandServiceImplHappyPathTest,AssignmentCampaignCommandServiceImplRejectPathTest,SelfAttemptEntryFoundationStateReadServiceTest,SelfExecutionAdmissionFoundationStateReadServiceTest,SelfCurrentAttemptReadFoundationStateReadServiceTest,SelfExecutionAdmissionFoundationStateReadServiceBoundaryTest,SelfVisibleReadExecutionSemanticsRegressionTest,SelfVisibleReadStartMaterializationBoundaryAntiDriftTest,SelfCurrentAttemptReadServiceTest,SelfAttemptEntryControllerTest,ResultRecordingServiceImplTest" test`
- runtime API flow:
  - `POST /api/v1/assignment-campaigns/launch`
  - `GET /api/v1/assigned-learning/assignments`
  - `GET /api/v1/assigned-learning/assignments/{assignmentId}`
  - `GET /api/v1/assigned-learning/assignments/{assignmentId}/learning-context`
  - `GET /api/v1/assigned-learning/assignments/{assignmentId}/assignment-tests/{assignmentTestId}/test-context`
  - `POST /api/v1/assigned-attempt-entries/assignments/{assignmentId}/assignment-tests/{assignmentTestId}`
  - `PUT /api/v1/assigned-attempt-answers/attempts/{testAttemptId}/questions/{questionId}`
  - `POST /api/v1/assigned-attempt-submissions/attempts/{testAttemptId}`
  - `GET /api/v1/self-testing/tests`
  - `GET /api/v1/self-testing/tests/9`
  - `POST /api/v1/self-attempt-entries/tests/9`
  - `GET /api/v1/current-attempts/self/tests/9`
  - `PUT /api/v1/self-attempt-answers/attempts/4/questions/{questionId}`
  - `POST /api/v1/self-attempt-submissions/attempts/4`
  - `GET /api/v1/self/results/history`
  - `GET /api/v1/managerial/current-supervision`
  - `GET /api/v1/managerial/historical-analytics/user-topic?periodStart=2026-05-01T00:00:00Z&periodEnd=2026-05-14T00:00:00Z`
  - `GET /api/v1/managerial/historical-analytics/department-topic?periodStart=2026-05-01T00:00:00Z&periodEnd=2026-05-14T00:00:00Z`

## 4. Fixes performed during run

### Helper fixes

- Applied final-boundary recursive JSON normalization in `create-demo-content.ps1`.
- Added runtime guard that rejects serialized request JSON containing `«` or `»`.
- Added local normalization self-check.
- Fixed test-question composition logic for `Links`:
  - removed global per-topic “consumed question” restriction;
  - kept uniqueness only inside one test;
  - added deterministic overlap allowance across tests in the same topic;
  - added local composition preflight before API calls.

### Cleanup fixes

- Added `demo-data/scripts/reset-demo-runtime-slice.ps1`.
- The script is allowlisted only for `training_platform_demo_fullcycle`.
- It clears only demo-generated runtime/content slices in safe FK order and leaves:
  - `app_user`
  - `organizational_unit`
  - `organizational_unit_type`
  - `user_role_assignment`
  - `user_organization_assignment`

### Demo baseline fixes

- Fixed baseline drift in `tmp/personnel-demo-baseline.sql`:
  - restored missing `EXPERT` role for `NPZ-EXP-EDU-001`;
  - restored missing `ROLE_MANAGER` role for `NPZ-ITR-HSE-001`.
- This brought live actor coverage in line with `role-runbook.md`.

### Production fixes

1. Assignment launch recipient filtering fix.
   - Problem: campaign launch failed hard on first ineligible subtree candidate.
   - Fix: skip ineligible candidates and fail only if resulting recipient set is empty.

2. Canonical role alias normalization fix.
   - Problem: baseline/personnel contour created `ROLE_OPERATIONS` and `ROLE_MANAGER`, but live actor/runtime capabilities expected canonical `OPERATOR` and `MANAGER`.
   - Fix: normalize role aliases for runtime actor sections and assignment eligibility.

3. Result scoring policy alias fix.
   - Problem: content/runtime stores `scoring_policy_code = DEFAULT`, while result scoring accepted only `DEFAULT_PARTIAL_CREDIT_V1` and `STANDARD`.
   - Fix: accept `DEFAULT` as a supported canonical alias in `CanonicalResultQuestionScoringEvaluator`.

4. Self execution content-read boundary fix.
   - Problem: self-visible catalog used self-visible read policy, but self start/current-attempt/submit supporting readers still depended on `TestQueryService` and `CONTENT_AUTHORING`, which caused `403 Actor is not authorized to read test data` for `LEARNER-SELF-1`.
   - Fix: self execution foundation readers now resolve published test/topic/course via repositories directly and apply only the local published/final eligibility filters.

## 5. Final DB counts

| Table | Count |
|---|---:|
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
| `result` | 3 |
| `analytics_user_topic_aggregate` | 0 |
| `analytics_department_topic_aggregate` | 0 |

## 6. Content verification

- Content counts match the full controlled demo expectation for one full published contour:
  - `course = 3`
  - `topic = 8`
  - `material = 27`
  - `question = 85`
  - `answer_option = 340`
  - `test = 13`
  - `test_question = 91`
- Question bank quality checks:
  - `SINGLE_CHOICE = 51`
  - `MULTIPLE_CHOICE = 34`
  - bad question option count `!= 4` = `0`
  - bad `SINGLE_CHOICE` correct count `!= 1` = `0`
  - bad `MULTIPLE_CHOICE` correct count `< 2` = `0`
  - duplicate `(test_id, question_id)` links = `0`
  - duplicate `display_order` within one test = `0`
- Publication:
  - all `13` tests are `PUBLISHED`
  - expert read of `GET /api/v1/expert/content/courses` returned `3` published courses
- Final controls:
  - active final tests count = `6`
  - active final topic set = topic ids `1..6`
  - self-visible tests used in learner contour are not active finals:
    - `self_visible_final_conflict = 0`
- Self/assigned separation in immutable result facts:
  - result rows:
    - `#1 ASSIGNED user=13 test=6 score=100 passed=true`
    - `#2 ASSIGNED user=21 test=4 score=0 passed=false`
    - `#3 SELF user=29 test=9 score=100 passed=true`
  - `SELF` rows with non-null `assignment_id` = `0`
  - actor `29` assigned result rows = `0`
- `INFOSEC` absence:
  - course/topic/material/question/answer_option/test `INFOSEC` checks all returned `0`

## 7. Role scenario verification

### ADMIN

- `GET /api/v1/me` with `X-Demo-Actor-Id: 1` worked.
- `GET /api/v1/admin/org-units/tree` returned the expected org tree rooted at `/department`.
- Baseline and content/org visibility are available.

### EXPERT

- `GET /api/v1/me` with `X-Demo-Actor-Id: 11` returned roles `["EXPERT","ROLE_USER"]` and enabled sections `["EXPERT_CONTENT","EXPERT_QUESTION_ANALYTICS"]`.
- `GET /api/v1/expert/content/courses` returned all `3` published courses.
- `GET /api/v1/expert/content/topics/1/active-final-test` returned published final test id `4`.

### OPERATOR-1

- Actor `13`.
- `GET /api/v1/assigned-learning/assignments` returned assigned items including course `1` and course `2`.
- `GET /api/v1/assigned-learning/assignments/1` and `/learning-context` returned assignment/test/course/topic/material context.
- `GET /api/v1/assigned-learning/assignments/1/assignment-tests/2/test-context` returned full question context.
- `POST /api/v1/assigned-attempt-entries/assignments/1/assignment-tests/2` created attempt `3`.
- Answer mutations succeeded for all questions.
- `POST /api/v1/assigned-attempt-submissions/attempts/3` completed successfully and created `resultId = 1`.
- After submit, `GET /api/v1/current-attempts/assigned/assignments/1/assignment-tests/2` correctly returned `404 Active assigned attempt not found`.

### OPERATOR-2

- Actor `21`.
- `POST /api/v1/assigned-attempt-entries/assignments/9/assignment-tests/25` created attempt `2`.
- Wrong answers were saved intentionally.
- `POST /api/v1/assigned-attempt-submissions/attempts/2` completed successfully and created `resultId = 2`.
- Immutable result row confirms failed/lower-score path:
  - `attempt_mode = ASSIGNED`
  - `score_percent = 0.0000`
  - `passed = false`

### LEARNER-SELF-1

- Actor `29`, mapped in `role-runbook.md` to `npz-op-mtbeo-001`.
- `GET /api/v1/self-testing/tests` returned a non-empty catalog including course `3` self test `id = 9`.
- `GET /api/v1/self-testing/tests/9` returned detailed test composition.
- `POST /api/v1/self-attempt-entries/tests/9` created self attempt `4`.
- `GET /api/v1/current-attempts/self/tests/9` returned the active self attempt before submit.
- Answer mutations succeeded for all six questions.
- `POST /api/v1/self-attempt-submissions/attempts/4` created `resultId = 3`.
- `GET /api/v1/self/results/history` returned exactly one self result row for actor `29`.
- After submit, `GET /api/v1/current-attempts/self/tests/9` correctly returned `404 Active self attempt not found`.

### MANAGER-1

- Actor `4`.
- `GET /api/v1/me` returned roles `["MANAGER","ROLE_USER"]` and enabled sections `["MANAGER_CURRENT_SUPERVISION","MANAGER_HISTORICAL_ANALYTICS"]`.
- `GET /api/v1/managerial/current-supervision` returned a non-empty list (`72` rows in the live response).
- `GET /api/v1/managerial/historical-analytics/user-topic?...` returned `200 []`.
- `GET /api/v1/managerial/historical-analytics/department-topic?...` returned `200 []`.
- At the same time:
  - `result = 3`
  - `analytics_user_topic_aggregate = 0`
  - `analytics_department_topic_aggregate = 0`

## 8. Negative checks

- Operator cannot perform expert content mutation:
  - actor `13`
  - `POST /api/v1/expert/content/courses`
  - result: `403 Forbidden`
  - body: `Authenticated actor is not authorized for command-flow capability admission`
- Learner cannot open чужой assigned resource:
  - actor `29`
  - `GET /api/v1/assigned-learning/assignments/1`
  - result: `404 Not Found`
  - body: `Assignment not found in self scope: actorUserId=29, assignmentId=1`
- Manager cannot perform admin mutation:
  - actor `4`
  - `POST /api/v1/admin/org-units/999999/archive`
  - result: `403 Forbidden`
  - body: `Authenticated actor is not authorized for command-flow capability admission`
- Self result is not mixed with assigned result:
  - `SELF` rows with non-null assignment = `0`
  - actor `29` assigned result rows = `0`
- Self tests did not become final controls:
  - `self_visible_final_conflict = 0`

## 9. Remaining blockers

- `analytics / managerial historical analytics`
  - module: `src/main/java/com/vladislav/training/platform/analytics`
  - evidence:
    - `GET /api/v1/managerial/historical-analytics/user-topic?periodStart=2026-05-01T00:00:00Z&periodEnd=2026-05-14T00:00:00Z` -> `200 []`
    - `GET /api/v1/managerial/historical-analytics/department-topic?periodStart=2026-05-01T00:00:00Z&periodEnd=2026-05-14T00:00:00Z` -> `200 []`
    - `result = 3`
    - `analytics_user_topic_aggregate = 0`
    - `analytics_department_topic_aggregate = 0`
  - exact blocker:
    - historical analytics aggregates are not materialized automatically from recorded results in this contour;
    - no public/runtime rebuild endpoint is exposed by `ManagerialHistoricalAnalyticsController`, which remains read-only on `/user-topic` and `/department-topic`;
    - therefore manager historical analytics cannot be completed end-to-end through an allowed runtime surface.

## 10. Final verdict

`BLOCKED_BY_ANALYTICS`

Everything except historical analytics is working on one persistent demo DB `training_platform_demo_fullcycle`:

- org/personnel baseline is restored and usable;
- full content lifecycle is created and published;
- assignment campaigns are launched;
- assigned success and assigned fail paths are recorded into immutable results;
- self-testing path is recorded into immutable results;
- admin/expert/operator/learner current contours work;
- manager current supervision works;
- manager historical analytics remains blocked because aggregate rebuild/materialization is not available through an exposed runtime surface and aggregate tables stay empty after result recording.
