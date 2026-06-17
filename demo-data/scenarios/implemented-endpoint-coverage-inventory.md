# Implemented Endpoint Coverage Inventory

This artifact is the endpoint-complete coverage ledger for all published Spring runtime controllers in `src/main/java`.

Inventory scope:
- Included: `@RestController` classes with `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, or `@DeleteMapping`.
- Excluded: `@RestControllerAdvice` (`GlobalExceptionHandler`) because it is not a published request endpoint.

Summary:
- Controllers found: `35`
- Published controller endpoints found: `130`
- Smoke-covered unique controller endpoints from `check-implemented-functional-api-smoke.ps1`: `16`
- Manual-runbook-covered unique controller endpoints from `role-runbook.md` and `implemented-functional-test-runbook.md`: `23`
- maintenance contour scope-caveat endpoints: `4`
- Unclassified endpoints: `0`

Status legend:
- `IMPLEMENTED_AND_PASSING`: published endpoint has direct live evidence through smoke, helper, or accepted run evidence.
- `IMPLEMENTED_BUT_NOT_SMOKE_TESTED`: published endpoint exists, but the current acceptance kit does not execute it directly.
- `MANUAL_RUNBOOK_ONLY`: published endpoint is part of the documented role/assignment runbook, not the smoke script.
- `NOT_IMPLEMENTED_BY_SCOPE`: published endpoint belongs to accepted maintenance contour post-Stage-2 analytics caveat.
- `NOT_RUNTIME_SURFACE`: reserved for non-controller capabilities; not used in this endpoint-only inventory.
- `INTERNAL_OR_DEV_ONLY`: reserved for dev-only controller surfaces; not used here.
- `DEPRECATED_OR_UNUSED`: reserved for dead published controllers; not used here.

## com.vladislav.training.platform.access.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `AccessAdministrationController` | `GET` | `/api/v1/admin/access-areas` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin read surface. |
| `AccessAdministrationController` | `GET` | `/api/v1/admin/management-relations` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin read surface. |
| `AccessAdministrationController` | `GET` | `/api/v1/admin/temporary-role-assignments` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin read surface. |
| `AccessAdministrationController` | `GET` | `/api/v1/admin/temporary-access-areas` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin read surface. |
| `AccessAdministrationController` | `GET` | `/api/v1/admin/temporary-management-delegations` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin read surface. |
| `AccessAdministrationController` | `GET` | `/api/v1/admin/management-relation-types` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin read surface. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/access-areas` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/access-areas/{id}/close` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/management-relations` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/management-relations/{id}/close` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/temporary-role-assignments` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/temporary-role-assignments/{id}/close` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/temporary-access-areas` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/temporary-access-areas/{id}/close` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/temporary-management-delegations` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `AccessAdministrationController` | `POST` | `/api/v1/admin/temporary-management-delegations/{id}/close` | `ADMIN` | Access administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |

## com.vladislav.training.platform.analytics.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ManagerialHistoricalAnalyticsController` | `GET` | `/api/v1/managerial/historical-analytics/user-topic` | `MANAGER` | Historical analytics | scope caveat | NOT_IMPLEMENTED_BY_SCOPE | Accepted scope excludes maintenance contour post-Stage-2 analytics | Live demo may return `200 []`. |
| `ManagerialHistoricalAnalyticsController` | `GET` | `/api/v1/managerial/historical-analytics/department-topic` | `MANAGER` | Historical analytics | scope caveat | NOT_IMPLEMENTED_BY_SCOPE | Accepted scope excludes maintenance contour post-Stage-2 analytics | Live demo may return `200 []`. |
| `AnalyticsAdminRebuildController` | `POST` | `/api/v1/admin/analytics/result-rebuild` | `ADMIN` | Analytics rebuild | scope caveat | NOT_IMPLEMENTED_BY_SCOPE | Published controller exists, but rebuild runtime is outside accepted scope | Do not force aggregate population for implemented-scope acceptance. |
| `ExpertQuestionAnalyticsController` | `GET` | `/api/v1/expert/question-analytics` | `EXPERT` | Expert analytics | scope caveat | NOT_IMPLEMENTED_BY_SCOPE | Controller reads aggregate-backed analytics | Depends on `analytics_question_aggregate`. |

## com.vladislav.training.platform.application.actor

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `CurrentActorController` | `GET` | `/api/v1/me` | `interactive actor` | Current actor profile | smoke | IMPLEMENTED_AND_PASSING | Smoke script and live run | Used as startup/auth gate in `dev`. |

## com.vladislav.training.platform.assignment.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ManagerialCurrentSupervisionController` | `GET` | `/api/v1/managerial/current-supervision` | `MANAGER` | Manager current supervision | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook, live run | Also negative-tested for operator and learner fail-closed. |
| `AssignmentCampaignLaunchController` | `POST` | `/api/v1/assignment-campaigns/launch` | `ADMIN` | Assignment campaign launch | runbook | MANUAL_RUNBOOK_ONLY | Implemented functional test runbook + live campaign launch | Exact payload sequence is documented in runbook. |
| `AssignmentAdministrativeActionController` | `POST` | `/api/v1/assignment-administrative-actions/cancel/{assignmentId}` | `ADMIN` | Assignment administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin action outside current kit. |
| `AssignmentAdministrativeActionController` | `POST` | `/api/v1/assignment-administrative-actions/deadline-extend/{assignmentId}` | `ADMIN` | Assignment administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin action outside current kit. |
| `AssignmentAdministrativeActionController` | `POST` | `/api/v1/assignment-administrative-actions/replace-with-new/{assignmentId}` | `ADMIN` | Assignment administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin action outside current kit. |
| `AssignmentSelfScopedReadController` | `GET` | `/api/v1/assigned-learning/assignments` | `OPERATOR self-scoped` | Assigned learning read | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Covered for OPERATOR-1 and OPERATOR-2. |
| `AssignmentSelfScopedReadController` | `GET` | `/api/v1/assigned-learning/assignments/{assignmentId}` | `OPERATOR self-scoped` | Assigned learning read | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook, negative foreign read | Also fail-closed for foreign learner path. |
| `AssignmentSelfScopedReadController` | `GET` | `/api/v1/assigned-learning/assignments/{assignmentId}/learning-context` | `OPERATOR self-scoped` | Assigned learning read | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Covered for both operator actors. |
| `AssignmentSelfScopedReadController` | `GET` | `/api/v1/assigned-learning/assignments/{assignmentId}/assignment-tests/{assignmentTestId}/test-context` | `OPERATOR self-scoped` | Assigned test context read | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Covered for both operator actors. |

## com.vladislav.training.platform.audit.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `AuditAdminReadController` | `GET` | `/api/v1/admin/audit-events` | `ADMIN` | Audit admin read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin read surface. |
| `AuditAdminReadController` | `GET` | `/api/v1/admin/audit-events/{auditEventId}` | `ADMIN` | Audit admin read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional admin read surface. |

## com.vladislav.training.platform.content.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `TopicFinalControlController` | `POST` | `/api/v1/expert/content/topics/{topicId}/active-final-tests/{testId}/assign` | `EXPERT` | Final-control assignment | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Positive assign path proven. |
| `TopicFinalControlController` | `POST` | `/api/v1/expert/content/topics/{topicId}/active-final-tests/{testId}/replace` | `EXPERT` | Final-control assignment | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Replace path not in current kit. |
| `TopicFinalControlController` | `DELETE` | `/api/v1/expert/content/topics/{topicId}/active-final-test` | `EXPERT` | Final-control assignment | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Remove path not in current kit. |
| `TopicFinalControlController` | `GET` | `/api/v1/expert/content/topics/{topicId}/active-final-test` | `EXPERT` | Final-control read | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Positive read proven. |
| `TopicFinalControlController` | `GET` | `/api/v1/expert/content/topics/{topicId}/tests?eligibleForFinalControl=true` | `EXPERT` | Final-control read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Eligible-test read not in current kit. |
| `TopicController` | `GET` | `/api/v1/expert/content/topics/{id}` | `EXPERT` | Topic read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in smoke. |
| `TopicController` | `GET` | `/api/v1/expert/content/topics?courseId=...` | `EXPERT` | Topic read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | List-by-course not in smoke. |
| `TopicController` | `POST` | `/api/v1/expert/content/topics` | `EXPERT` | Topic authoring | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Positive create path proven. |
| `TopicController` | `PATCH` | `/api/v1/expert/content/topics/{id}` | `EXPERT` | Topic authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Patch path not in current kit. |
| `ContentLifecycleController` | `GET` | `/api/v1/expert/content/lifecycle/courses/{id}` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Lifecycle detail read not in current kit. |
| `ContentLifecycleController` | `GET` | `/api/v1/expert/content/lifecycle/topics/{id}` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Lifecycle detail read not in current kit. |
| `ContentLifecycleController` | `GET` | `/api/v1/expert/content/lifecycle/materials/{id}` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Lifecycle detail read not in current kit. |
| `ContentLifecycleController` | `GET` | `/api/v1/expert/content/lifecycle/questions/{id}` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Lifecycle detail read not in current kit. |
| `ContentLifecycleController` | `GET` | `/api/v1/expert/content/lifecycle/tests/{id}` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Lifecycle detail read not in current kit. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/courses/{id}/publish` | `EXPERT` | Content lifecycle | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Publish path proven. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/courses/{id}/archive` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Archive path not in current kit. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/topics/{id}/publish` | `EXPERT` | Content lifecycle | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Publish path proven. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/topics/{id}/archive` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Archive path not in current kit. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/materials/{id}/publish` | `EXPERT` | Content lifecycle | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Publish path proven. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/materials/{id}/archive` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Archive path not in current kit. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/questions/{id}/publish` | `EXPERT` | Content lifecycle | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Publish path proven. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/questions/{id}/archive` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Archive path not in current kit. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/tests/{id}/publish` | `EXPERT` | Content lifecycle | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Publish path proven. |
| `ContentLifecycleController` | `POST` | `/api/v1/expert/content/lifecycle/tests/{id}/archive` | `EXPERT` | Content lifecycle | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Archive path not in current kit. |
| `CourseController` | `GET` | `/api/v1/expert/content/courses` | `EXPERT` | Course read | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Published list read proven. |
| `CourseController` | `GET` | `/api/v1/expert/content/courses/{id}` | `EXPERT` | Course read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `CourseController` | `POST` | `/api/v1/expert/content/courses` | `EXPERT` | Course authoring | helper, smoke-negative, runbook | IMPLEMENTED_AND_PASSING | Content helper full run; operator negative `403` also exercises routing/policy | Positive create path proven. |
| `CourseController` | `PATCH` | `/api/v1/expert/content/courses/{id}` | `EXPERT` | Course authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Patch path not in current kit. |
| `MaterialController` | `GET` | `/api/v1/expert/content/materials?topicId=...` | `EXPERT` | Material read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | List-by-topic not in current kit. |
| `MaterialController` | `GET` | `/api/v1/expert/content/materials/{id}` | `EXPERT` | Material read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `MaterialController` | `POST` | `/api/v1/expert/content/materials` | `EXPERT` | Material authoring | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Positive create path proven. |
| `MaterialController` | `PATCH` | `/api/v1/expert/content/materials/{id}` | `EXPERT` | Material authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Patch path not in current kit. |
| `QuestionController` | `GET` | `/api/v1/expert/content/questions?topicId=...` | `EXPERT` | Question read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | List-by-topic not in current kit. |
| `QuestionController` | `GET` | `/api/v1/expert/content/questions/{id}` | `EXPERT` | Question read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `QuestionController` | `GET` | `/api/v1/expert/content/questions/{questionId}/answer-options` | `EXPERT` | Answer option read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Read path not in current kit. |
| `QuestionController` | `POST` | `/api/v1/expert/content/questions` | `EXPERT` | Question authoring | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Positive create path proven. |
| `QuestionController` | `PATCH` | `/api/v1/expert/content/questions/{id}` | `EXPERT` | Question authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Patch path not in current kit. |
| `QuestionController` | `POST` | `/api/v1/expert/content/questions/{questionId}/answer-options` | `EXPERT` | Answer option authoring | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Positive create path proven. |
| `QuestionController` | `PATCH` | `/api/v1/expert/content/questions/{questionId}/answer-options/{id}` | `EXPERT` | Answer option authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Patch path not in current kit. |
| `QuestionController` | `DELETE` | `/api/v1/expert/content/questions/{questionId}/answer-options/{id}` | `EXPERT` | Answer option authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Delete path not in current kit. |
| `TestController` | `GET` | `/api/v1/expert/content/tests?topicId=...` | `EXPERT` | Test read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | List-by-topic not in current kit. |
| `TestController` | `GET` | `/api/v1/expert/content/tests/{id}` | `EXPERT` | Test read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `TestController` | `GET` | `/api/v1/expert/content/tests/{testId}/questions` | `EXPERT` | Test composition read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Composition read not in current kit. |
| `TestController` | `POST` | `/api/v1/expert/content/tests` | `EXPERT` | Test authoring | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Positive create path proven. |
| `TestController` | `PATCH` | `/api/v1/expert/content/tests/{id}` | `EXPERT` | Test authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Patch path not in current kit. |
| `TestController` | `POST` | `/api/v1/expert/content/tests/{testId}/questions` | `EXPERT` | Test composition authoring | helper | IMPLEMENTED_AND_PASSING | Content helper full run | Positive link path proven. |
| `TestController` | `PATCH` | `/api/v1/expert/content/tests/{testId}/questions/{id}` | `EXPERT` | Test composition authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Patch path not in current kit. |
| `TestController` | `DELETE` | `/api/v1/expert/content/tests/{testId}/questions/{id}` | `EXPERT` | Test composition authoring | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Delete path not in current kit. |

## com.vladislav.training.platform.integration.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ImportItemReviewController` | `POST` | `/api/v1/admin/import-job-items/{itemId}/apply-review` | `ADMIN` | Import administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Review path not in current kit. |
| `ImportItemReviewController` | `POST` | `/api/v1/admin/import-job-items/{itemId}/reject-review` | `ADMIN` | Import administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Review path not in current kit. |
| `ImportAdminReadController` | `GET` | `/api/v1/admin/import-jobs` | `ADMIN` | Import administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Read path not in current kit. |
| `ImportAdminReadController` | `GET` | `/api/v1/admin/import-jobs/{importJobId}` | `ADMIN` | Import administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Read path not in current kit. |
| `ImportAdminReadController` | `GET` | `/api/v1/admin/import-jobs/{importJobId}/items` | `ADMIN` | Import administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Read path not in current kit. |
| `ImportAdminReadController` | `GET` | `/api/v1/admin/import-job-items/{itemId}` | `ADMIN` | Import administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Read path not in current kit. |
| `ImportAdminCommandController` | `POST` | `/api/v1/admin/import-jobs` | `ADMIN` | Import administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Create path not in current kit. |

## com.vladislav.training.platform.integration.personnel.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `PersonnelExcelImportController` | `POST` | `/api/v1/admin/import/personnel-excel/dry-run` | `ADMIN` | Personnel import | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional import tool, outside current mandatory kit. |
| `PersonnelExcelImportController` | `POST` | `/api/v1/admin/import/personnel-excel/apply` | `ADMIN` | Personnel import | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Optional import tool, outside current mandatory kit. |

## com.vladislav.training.platform.notification.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `NotificationSelfReadController` | `GET` | `/api/v1/self/notifications` | `LEARNER or OPERATOR self` | Notification read | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Empty list is acceptable. |
| `NotificationSelfReadController` | `GET` | `/api/v1/self/notifications/{notificationId}` | `LEARNER or OPERATOR self` | Notification read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `NotificationAdminReadController` | `GET` | `/api/v1/admin/notifications` | `ADMIN` | Notification read | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Empty list is acceptable. |
| `NotificationAdminReadController` | `GET` | `/api/v1/admin/notifications/{notificationId}` | `ADMIN` | Notification read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |

## com.vladislav.training.platform.result.query

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `SelfHistoricalResultController` | `GET` | `/api/v1/self/results/history` | `LEARNER self` | Self result history | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Self-only visibility proven. |

## com.vladislav.training.platform.testing.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `SelfVisibleTestingReadController` | `GET` | `/api/v1/self-testing/tests` | `LEARNER self` | Self testing catalog | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Non-empty catalog proven. |
| `SelfVisibleTestingReadController` | `GET` | `/api/v1/self-testing/tests/{testId}` | `LEARNER self` | Self testing detail | runbook | MANUAL_RUNBOOK_ONLY | Role runbook | Uses dynamic id from catalog. |
| `SelfAttemptSubmitController` | `POST` | `/api/v1/self-attempt-submissions/attempts/{testAttemptId}` | `LEARNER self` | Self attempt submit | runbook | MANUAL_RUNBOOK_ONLY | Role runbook + live result | Submit path proven manually. |
| `SelfAttemptEntryController` | `POST` | `/api/v1/self-attempt-entries/tests/{testId}` | `LEARNER self` | Self attempt start | runbook | MANUAL_RUNBOOK_ONLY | Role runbook + live result | Start path proven manually. |
| `SelfAttemptAnswerMutationController` | `PUT` | `/api/v1/self-attempt-answers/attempts/{testAttemptId}/questions/{questionId}` | `LEARNER self` | Self attempt save answer | runbook | MANUAL_RUNBOOK_ONLY | Role runbook + live result | Save path proven manually. |
| `SelfAttemptAnswerMutationController` | `DELETE` | `/api/v1/self-attempt-answers/attempts/{testAttemptId}/questions/{questionId}` | `LEARNER self` | Self attempt delete answer | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Delete path not in current kit. |
| `SelfAttemptAbandonController` | `POST` | `/api/v1/self-attempt-abandonments/attempts/{testAttemptId}` | `LEARNER self` | Self attempt abandon | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Abandon path not in current kit. |
| `AssignedAttemptEntryController` | `POST` | `/api/v1/assigned-attempt-entries/assignments/{assignmentId}/assignment-tests/{assignmentTestId}` | `OPERATOR self-scoped` | Assigned attempt start | runbook | MANUAL_RUNBOOK_ONLY | Role runbook + live result | Start path proven manually. |
| `AssignedAttemptAnswerMutationController` | `PUT` | `/api/v1/assigned-attempt-answers/attempts/{testAttemptId}/questions/{questionId}` | `OPERATOR self-scoped` | Assigned attempt save answer | runbook | MANUAL_RUNBOOK_ONLY | Role runbook + live result | Save path proven manually. |
| `AssignedAttemptAnswerMutationController` | `DELETE` | `/api/v1/assigned-attempt-answers/attempts/{testAttemptId}/questions/{questionId}` | `OPERATOR self-scoped` | Assigned attempt delete answer | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Delete path not in current kit. |
| `AssignedAttemptSubmitController` | `POST` | `/api/v1/assigned-attempt-submissions/attempts/{testAttemptId}` | `OPERATOR self-scoped` | Assigned attempt submit | runbook | MANUAL_RUNBOOK_ONLY | Role runbook + live results | Submit path proven manually. |
| `CurrentAttemptReadController` | `GET` | `/api/v1/current-attempts/assigned/assignments/{assignmentId}/assignment-tests/{assignmentTestId}` | `OPERATOR self-scoped` | Current attempt read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Recovery read not in current kit. |
| `CurrentAttemptReadController` | `GET` | `/api/v1/current-attempts/self/tests/{testId}` | `LEARNER self` | Current attempt read | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Recovery read not in current kit. |

## com.vladislav.training.platform.userorg.controller

| Controller | Method | Path | Expected role / surface | Functional area | Coverage | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `UserAdministrationController` | `GET` | `/api/v1/admin/users` | `ADMIN` | User administration | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Baseline actor list proven. |
| `UserAdministrationController` | `GET` | `/api/v1/admin/users/{id}` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `UserAdministrationController` | `GET` | `/api/v1/admin/users/{id}/roles` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `UserAdministrationController` | `GET` | `/api/v1/admin/users/{id}/organization-assignments` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `UserAdministrationController` | `GET` | `/api/v1/admin/roles` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Lookup read not in current kit. |
| `UserAdministrationController` | `POST` | `/api/v1/admin/users` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `UserAdministrationController` | `PATCH` | `/api/v1/admin/users/{id}` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `UserAdministrationController` | `POST` | `/api/v1/admin/users/{id}/deactivate` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `UserAdministrationController` | `POST` | `/api/v1/admin/users/{id}/roles` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `UserAdministrationController` | `POST` | `/api/v1/admin/users/{id}/roles/{assignmentId}/close` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `UserAdministrationController` | `POST` | `/api/v1/admin/users/{id}/organization-assignments` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `UserAdministrationController` | `POST` | `/api/v1/admin/users/{id}/organization-assignments/{assignmentId}/close` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `UserAdministrationController` | `POST` | `/api/v1/admin/users/{id}/primary-home-unit/replace` | `ADMIN` | User administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `OrganizationController` | `GET` | `/api/v1/admin/org-unit-types` | `ADMIN` | Organization administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Lookup read not in current kit. |
| `OrganizationController` | `GET` | `/api/v1/admin/org-units/tree` | `ADMIN` | Organization administration | smoke, runbook | IMPLEMENTED_AND_PASSING | Smoke script, role runbook | Org tree proven. |
| `OrganizationController` | `GET` | `/api/v1/admin/org-units/{id}` | `ADMIN` | Organization administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Detail read not in current kit. |
| `OrganizationController` | `GET` | `/api/v1/admin/org-units` | `ADMIN` | Organization administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | List read not in current kit. |
| `OrganizationController` | `POST` | `/api/v1/admin/org-unit-types` | `ADMIN` | Organization administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `OrganizationController` | `PATCH` | `/api/v1/admin/org-unit-types/{id}` | `ADMIN` | Organization administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `OrganizationController` | `POST` | `/api/v1/admin/org-units` | `ADMIN` | Organization administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `OrganizationController` | `PATCH` | `/api/v1/admin/org-units/{id}` | `ADMIN` | Organization administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `OrganizationController` | `POST` | `/api/v1/admin/org-units/{id}/move` | `ADMIN` | Organization administration | inventory | IMPLEMENTED_BUT_NOT_SMOKE_TESTED | Code inventory only | Positive mutation not in current kit. |
| `OrganizationController` | `POST` | `/api/v1/admin/org-units/{id}/archive` | `ADMIN` | Organization administration | smoke-negative, runbook | IMPLEMENTED_AND_PASSING | Negative smoke proves published route and fail-closed policy | Positive admin archive is not in current kit. |
