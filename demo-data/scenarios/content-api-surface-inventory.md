# Content API Surface Inventory

## 1. Статус артефакта

- Read-only inventory фактических backend API/DTO для content creation.
- Не является payload collection.
- Не является SQL.
- Не меняет production runtime и production code.

## 2. Summary verdict

`READY_WITH_BLOCKERS`

Общий вывод:
- Базовые mutation/read surfaces для `course`, `topic`, `material`, `question`, `answer option`, `test`, `test-question composition`, `publish`, `active final control` в коде есть.
- Material creation сейчас действительно metadata-only и не требует file upload / fileId / storage URL.
- Final-control separation для assigned topics реализована отдельным surface.
- Self-visible separation реализована read-side правилом `PUBLISHED && !isActiveFinalForTopic`.
- Есть 2 существенных ограничения для следующего шага:
  - в content DTO нет явных business-code полей для сохранения `COURSE-*`, `TOPIC-*`, `MAT-*`, `Q-*`, `TEST-*` как runtime identifiers;
  - в content DTO нет отдельного поля `contour/visibility`, поэтому `SELF`/`ASSIGNED` разводятся не явным флагом, а runtime-комбинацией publication + final-control + assignment usage.

## 3. Endpoint inventory table

| Surface | Method | Path | Controller | Request DTO | Response DTO | Required fields | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| Course list | `GET` | `/api/v1/expert/content/courses` | `CourseController` | none | `CourseResponse` list | none | `READY` | Calls `CourseQueryService.findAllCourses()` |
| Course detail | `GET` | `/api/v1/expert/content/courses/{id}` | `CourseController` | none | `CourseResponse` | path `id` | `READY` | Calls `CourseQueryService.findCourseById()` |
| Course create | `POST` | `/api/v1/expert/content/courses` | `CourseController` | `SaveCourseRequest` | `CourseResponse` | `name`, `sortOrder >= 0` if present | `READY` | Creates `DRAFT` course via `CourseCommandService.createCourse()` |
| Course update | `PATCH` | `/api/v1/expert/content/courses/{id}` | `CourseController` | `SaveCourseRequest` | `CourseResponse` | `name`, `sortOrder >= 0` if present | `READY` | Draft-only update via `CourseCommandService.updateCourse()` |
| Topic list by course | `GET` | `/api/v1/expert/content/topics?courseId={id}` | `TopicController` | none | `TopicResponse` list | `courseId` query param | `READY` | Calls `TopicQueryService.findTopicsByCourseId()` |
| Topic detail | `GET` | `/api/v1/expert/content/topics/{id}` | `TopicController` | none | `TopicResponse` | path `id` | `READY` | Calls `TopicQueryService.findTopicById()` |
| Topic create | `POST` | `/api/v1/expert/content/topics` | `TopicController` | `CreateTopicRequest` | `TopicResponse` | `courseId`, `name`, `sortOrder >= 0` | `READY` | Creates `DRAFT` topic; parent course must be not `ARCHIVED`; `sortOrder` unique within course |
| Topic update | `PATCH` | `/api/v1/expert/content/topics/{id}` | `TopicController` | `UpdateTopicRequest` | `TopicResponse` | `name`, `sortOrder >= 0` | `READY` | Draft-only update; `sortOrder` unique within course |
| Material list by topic | `GET` | `/api/v1/expert/content/materials?topicId={id}` | `MaterialController` | none | `MaterialResponse` list | `topicId` query param | `READY` | Calls `MaterialQueryService.findMaterialsByTopicId()` |
| Material detail | `GET` | `/api/v1/expert/content/materials/{id}` | `MaterialController` | none | `MaterialResponse` | path `id` | `READY` | Calls `MaterialQueryService.findMaterialById()` |
| Material create | `POST` | `/api/v1/expert/content/materials` | `MaterialController` | `CreateMaterialRequest` | `MaterialResponse` | `topicId`, `name`, `materialType`, `sortOrder >= 0` | `READY` | Metadata-only create; no upload/file fields; parent topic must be not `ARCHIVED`; `sortOrder` unique within topic |
| Material update | `PATCH` | `/api/v1/expert/content/materials/{id}` | `MaterialController` | `UpdateMaterialRequest` | `MaterialResponse` | `name`, `materialType`, `sortOrder >= 0` | `READY` | Draft-only update |
| Question list by topic | `GET` | `/api/v1/expert/content/questions?topicId={id}` | `QuestionController` | none | `QuestionResponse` list | `topicId` query param | `READY` | Calls `QuestionQueryService.findQuestionsByTopicId()` |
| Question detail | `GET` | `/api/v1/expert/content/questions/{id}` | `QuestionController` | none | `QuestionResponse` | path `id` | `READY` | Calls `QuestionQueryService.findQuestionById()` |
| Question create | `POST` | `/api/v1/expert/content/questions` | `QuestionController` | `CreateQuestionRequest` | `QuestionResponse` | `topicId`, `body`, `questionType`, `sortOrder >= 0` if present | `READY` | Creates `DRAFT` question under non-archived topic |
| Question update | `PATCH` | `/api/v1/expert/content/questions/{id}` | `QuestionController` | `UpdateQuestionRequest` | `QuestionResponse` | `body`, `questionType`, `sortOrder >= 0` if present | `READY` | Draft-only update |
| Answer option list | `GET` | `/api/v1/expert/content/questions/{questionId}/answer-options` | `QuestionController` | none | `AnswerOptionResponse` list | path `questionId` | `READY` | Calls `QuestionQueryService.findAnswerOptionsByQuestionId()` |
| Answer option create | `POST` | `/api/v1/expert/content/questions/{questionId}/answer-options` | `QuestionController` | `SaveAnswerOptionRequest` | `AnswerOptionResponse` | `body`, `answerOptionRole`, `displayOrder >= 0`; `isCorrect` required for `CHOICE_OPTION` | `READY` | Draft question only; display order must be unique within question |
| Answer option update | `PATCH` | `/api/v1/expert/content/questions/{questionId}/answer-options/{id}` | `QuestionController` | `SaveAnswerOptionRequest` | `AnswerOptionResponse` | same as create | `READY` | Draft question only; option must belong to question |
| Answer option delete | `DELETE` | `/api/v1/expert/content/questions/{questionId}/answer-options/{id}` | `QuestionController` | none | none | path ids | `READY` | Draft question only |
| Test list by topic | `GET` | `/api/v1/expert/content/tests?topicId={id}` | `TestController` | none | `TestResponse` list | `topicId` query param | `READY` | Calls `TestQueryService.findTestsByTopicId()` |
| Test detail | `GET` | `/api/v1/expert/content/tests/{id}` | `TestController` | none | `TestResponse` | path `id` | `READY` | Calls `TestQueryService.findTestById()` |
| Test-question list | `GET` | `/api/v1/expert/content/tests/{testId}/questions` | `TestController` | none | `TestQuestionResponse` list | path `testId` | `READY` | Explicit composition read-side |
| Test create | `POST` | `/api/v1/expert/content/tests` | `TestController` | `CreateTestRequest` | `TestResponse` | `topicId`, `name`, `testType`, `thresholdPercent`, `scoringPolicyCode`, `sortOrder >= 0` | `READY` | Creates `DRAFT` test under non-archived topic; `sortOrder` unique within topic |
| Test update | `PATCH` | `/api/v1/expert/content/tests/{id}` | `TestController` | `UpdateTestRequest` | `TestResponse` | `name`, `testType`, `thresholdPercent`, `scoringPolicyCode`, `sortOrder >= 0` | `READY` | Draft-only update |
| Test-question create | `POST` | `/api/v1/expert/content/tests/{testId}/questions` | `TestController` | `SaveTestQuestionRequest` | `TestQuestionResponse` | `questionId`, `displayOrder >= 0`, `weight > 0` | `READY` | Draft test only; question must belong to same topic; unique `questionId` and `displayOrder` within test |
| Test-question update | `PATCH` | `/api/v1/expert/content/tests/{testId}/questions/{id}` | `TestController` | `SaveTestQuestionRequest` | `TestQuestionResponse` | same as create | `READY` | Draft test only |
| Test-question delete | `DELETE` | `/api/v1/expert/content/tests/{testId}/questions/{id}` | `TestController` | none | none | path ids | `READY` | Draft test only |
| Topic active final assign | `POST` | `/api/v1/expert/content/topics/{topicId}/active-final-tests/{testId}/assign` | `TopicFinalControlController` | none | none | path `topicId`, `testId` | `READY` | Topic must be `PUBLISHED`; candidate test must be same-topic, `CONTROL`, `PUBLISHED` |
| Topic active final replace | `POST` | `/api/v1/expert/content/topics/{topicId}/active-final-tests/{testId}/replace` | `TopicFinalControlController` | none | none | path `topicId`, `testId` | `READY` | Same invariants as assign |
| Topic active final clear | `DELETE` | `/api/v1/expert/content/topics/{topicId}/active-final-test` | `TopicFinalControlController` | none | none | path `topicId` | `READY` | Topic must be `PUBLISHED` |
| Topic active final read | `GET` | `/api/v1/expert/content/topics/{topicId}/active-final-test` | `TopicFinalControlController` | none | `TestResponse` or `404` | path `topicId` | `READY` | Read-side for final-control check |
| Eligible final-control tests | `GET` | `/api/v1/expert/content/topics/{topicId}/tests?eligibleForFinalControl=true` | `TopicFinalControlController` | none | `TestResponse` list | path `topicId`, query `eligibleForFinalControl=true` | `READY` | Read-side for candidate filtering |
| Course lifecycle read | `GET` | `/api/v1/expert/content/lifecycle/courses/{id}` | `ContentLifecycleController` | none | `CourseResponse` | path `id` | `READY` | Lifecycle read surface |
| Topic lifecycle read | `GET` | `/api/v1/expert/content/lifecycle/topics/{id}` | `ContentLifecycleController` | none | `TopicResponse` | path `id` | `READY` | Lifecycle read surface |
| Material lifecycle read | `GET` | `/api/v1/expert/content/lifecycle/materials/{id}` | `ContentLifecycleController` | none | `MaterialResponse` | path `id` | `READY` | Lifecycle read surface |
| Question lifecycle read | `GET` | `/api/v1/expert/content/lifecycle/questions/{id}` | `ContentLifecycleController` | none | `QuestionResponse` | path `id` | `READY` | Lifecycle read surface |
| Test lifecycle read | `GET` | `/api/v1/expert/content/lifecycle/tests/{id}` | `ContentLifecycleController` | none | `TestResponse` | path `id` | `READY` | Lifecycle read surface |
| Course publish | `POST` | `/api/v1/expert/content/lifecycle/courses/{id}/publish` | `ContentLifecycleController` | none | `CourseResponse` | path `id` | `READY` | Course must be `DRAFT` |
| Topic publish | `POST` | `/api/v1/expert/content/lifecycle/topics/{id}/publish` | `ContentLifecycleController` | none | `TopicResponse` | path `id` | `READY` | Topic must be `DRAFT`; parent course must be `PUBLISHED` |
| Material publish | `POST` | `/api/v1/expert/content/lifecycle/materials/{id}/publish` | `ContentLifecycleController` | none | `MaterialResponse` | path `id` | `READY` | Material must be `DRAFT`; parent topic must be `PUBLISHED` |
| Question publish | `POST` | `/api/v1/expert/content/lifecycle/questions/{id}/publish` | `ContentLifecycleController` | none | `QuestionResponse` | path `id` | `READY` | Question must be `DRAFT`; parent topic must be `PUBLISHED`; answer composition validated |
| Test publish | `POST` | `/api/v1/expert/content/lifecycle/tests/{id}/publish` | `ContentLifecycleController` | none | `TestResponse` | path `id` | `READY` | Test must be `DRAFT`; parent topic must be `PUBLISHED`; composition must be non-empty and use only `PUBLISHED` same-topic questions |
| Self-visible test catalog | `GET` | `/api/v1/self-testing/tests` | `SelfVisibleTestingReadController` | none | `SelfVisibleTestCatalogEntryResponse` list | none | `READY` | Read-side proof that self tests are exposed without assignment |
| Self-visible test detail | `GET` | `/api/v1/self-testing/tests/{testId}` | `SelfVisibleTestingReadController` | none | `SelfVisibleTestResponse` | path `testId` | `READY` | Only returns tests that are `PUBLISHED` and not active-final |

Role/auth assumption visible in code:
- Path namespace suggests `expert` content mutation/read contour.
- `CanonicalCapabilityAdmissionPolicy` allows content commands for `ROLE_EXPERT`, `ROLE_ADMIN`, `ROLE_SYSTEM_ADMIN`, `ROLE_SUPER_ADMIN`, and matching role codes `EXPERT`, `ADMIN`, `SYSTEM_ADMIN`, `SUPER_ADMIN`.
- There is also `TemporaryFoundationAdminCapabilityAdmissionPolicy`, but code explicitly marks it as bootstrap-only fallback and `@ConditionalOnMissingBean`.

## 4. DTO field inventory

| DTO class | Fields | Required/optional if visible | Demo source mapping |
|---|---|---|---|
| `SaveCourseRequest` | `name`, `description`, `sortOrder` | `name` required; `sortOrder` optional non-negative | `content-matrix.md` course name/order |
| `CreateTopicRequest` | `courseId`, `name`, `description`, `sortOrder` | `courseId`, `name`, `sortOrder` required | `content-matrix.md` topics |
| `UpdateTopicRequest` | `name`, `description`, `sortOrder` | `name`, `sortOrder` required | topic corrections only |
| `CreateMaterialRequest` | `topicId`, `name`, `description`, `materialType`, `sortOrder` | `topicId`, `name`, `materialType`, `sortOrder` required | `materials-source.md` + `content-matrix.md` |
| `UpdateMaterialRequest` | `name`, `description`, `materialType`, `sortOrder` | `name`, `materialType`, `sortOrder` required | material metadata refinements |
| `CreateQuestionRequest` | `topicId`, `body`, `questionType`, `sortOrder` | `topicId`, `body`, `questionType` required; `sortOrder` optional non-negative | `question-bank.md` |
| `UpdateQuestionRequest` | `body`, `questionType`, `sortOrder` | `body`, `questionType` required | question corrections only |
| `SaveAnswerOptionRequest` | `body`, `answerOptionRole`, `isCorrect`, `displayOrder`, `pairingKey`, `canonicalOrderPosition` | `body`, `answerOptionRole`, `displayOrder` required; `isCorrect` required for `CHOICE_OPTION` | `question-bank.md` answer options |
| `CreateTestRequest` | `topicId`, `name`, `description`, `testType`, `thresholdPercent`, `scoringPolicyCode`, `sortOrder` | all except `description` required | `content-matrix.md` tests |
| `UpdateTestRequest` | `name`, `description`, `testType`, `thresholdPercent`, `scoringPolicyCode`, `sortOrder` | all except `description` required | test corrections only |
| `SaveTestQuestionRequest` | `questionId`, `displayOrder`, `weight` | all required; `weight > 0` | `content-matrix.md` test composition rules + `question-bank.md` selected `Q-*` |
| `CourseResponse` | `id`, `name`, `description`, `status`, `sortOrder`, timestamps | runtime-generated | used to resolve live IDs for later dependent creates |
| `TopicResponse` | `id`, `courseId`, `name`, `description`, `status`, `sortOrder`, timestamps | runtime-generated | used to resolve topic IDs |
| `MaterialResponse` | `id`, `topicId`, `name`, `description`, `materialType`, `status`, `sortOrder`, timestamps | runtime-generated | verifies metadata-only material shape |
| `QuestionResponse` | `id`, `topicId`, `body`, `questionType`, `status`, `sortOrder`, timestamps | runtime-generated | used to resolve question IDs |
| `AnswerOptionResponse` | `id`, `questionId`, `body`, `answerOptionRole`, `isCorrect`, `displayOrder`, `pairingKey`, `canonicalOrderPosition`, timestamps | runtime-generated | verifies correct flags and ordering |
| `TestResponse` | `id`, `topicId`, `name`, `description`, `testType`, `status`, `thresholdPercent`, `scoringPolicyCode`, `sortOrder`, timestamps | runtime-generated | used to resolve test IDs |
| `TestQuestionResponse` | `id`, `testId`, `questionId`, `displayOrder`, `weight`, timestamps | runtime-generated | explicit `test_question` composition proof |
| `SelfVisibleTestCatalogEntryResponse` | `id`, `courseId`, `courseName`, `topicId`, `topicName`, `name`, `description`, `testType` | runtime-generated | verifies self-visible catalog |
| `SelfVisibleTestResponse` | `id`, `topicId`, `name`, `description`, `testType`, `questions[]` | runtime-generated | verifies self-visible test detail without assignment |

DTO mismatch note:
- Ни один create/update DTO не содержит поля business-code вроде `courseCode`, `topicCode`, `materialCode`, `questionCode`, `optionCode`, `testCode`.
- Значит stable коды из planning-файлов можно сохранить только во внешнем runbook/payload-plan или кодировать в `name/description/body` по согласованному соглашению.

## 5. Enum compatibility table

| Runtime enum | Required demo values | Supported yes/no | Notes |
|---|---|---|---|
| `MaterialType` | `TEXT`, `PDF`, `DOCX`, `VIDEO` | `YES` | Полное совпадение с demo-roadmap |
| `QuestionType` | `SINGLE_CHOICE`, `MULTIPLE_CHOICE` | `YES` | Runtime также поддерживает `MATCHING`, `ORDERING`, но для demo их использовать не нужно |
| `AnswerOptionRole` | `CHOICE_OPTION` | `YES` | Для demo choice questions нужен только `CHOICE_OPTION` |
| `TestType` | mandatory final-control `CONTROL`; self/training equivalents | `YES` | Поддерживаются `CONTROL`, `TRAINING`, `ENTRANCE`, `AUXILIARY`, `ALL_QUESTIONS`; для self-visible можно использовать любой published non-final test, но practically `TRAINING` safest |
| `ContentStatus` | `PUBLISHED` | `YES` | Lifecycle: `DRAFT -> PUBLISHED -> ARCHIVED` |
| Assignment runtime relation | `FINAL_TOPIC_CONTROL` | `YES` | В assignment слое это `AssignmentTestRole.FINAL_TOPIC_CONTROL`; в content слое это `isActiveFinalForTopic=true` на published control test |

## 6. Demo creation feasibility table

| Demo entity/surface | Feasibility | Notes |
|---|---|---|
| Courses | `READY` | 3 courses can be created as draft and published later |
| Topics | `READY` | 8 topics can be created under courses |
| Materials | `READY` | 27 metadata-only material records can be created; no upload required |
| Questions | `READY` | 85 questions can be created with allowed types |
| Answer options | `READY` | Correct flags supported via `isCorrect`; one call per option |
| Tests | `READY` | 13 tests can be created with `TestType` + threshold + scoring policy |
| Test-question links | `READY` | Explicit composition is supported via `/tests/{testId}/questions` |
| Active final control | `READY` | Separate API exists; requires topic published and candidate test `CONTROL + PUBLISHED` |
| Publication | `READY` | Separate lifecycle API exists for course/topic/material/question/test |
| Stable planning codes as runtime fields | `PARTIAL` | No dedicated code field in DTO/domain |
| Explicit SELF contour flag | `PARTIAL` | No explicit `selfVisible`/`contour` field; self visibility is derived, not persisted as first-class DTO field |

## 7. Blockers and required decisions

1. No stable code fields in runtime DTO.
   - Impact: `COURSE-OPS-SAFETY`, `TOPIC-OPS-INTRO`, `MAT-*`, `Q-*`, `TEST-*`, `option codes` cannot be persisted as dedicated fields.
   - Required decision: agree whether codes will live only in external mapping docs or be prefixed into human-readable fields such as `name` / `body`.

2. No explicit contour/visibility field for content entities.
   - Impact: `SELF` vs `ASSIGNED` is not a direct DTO attribute on course/topic/test.
   - Runtime equivalent:
     - self-visible test = published test under published topic/course and `!isActiveFinalForTopic`
     - mandatory assigned final = active final published `CONTROL` test of topic
   - Required decision: payload plan must encode this as creation order and final-control assignment rules, not as a `contour` field.

3. `scoringPolicyCode` vocabulary is open string, not enum-constrained.
   - Impact: runtime accepts a non-blank string, but canonical production vocabulary is not self-documented here.
   - Evidence: tests repeatedly use `"DEFAULT"`.
   - Required decision: confirm `DEFAULT` as live demo value before payload drafting.

4. High call volume for question/answer-option/test-question creation.
   - Impact: no bulk create endpoints are visible; 85 questions plus options and 13 explicit compositions will require many sequential calls.
   - This is not a blocker, but the future payload plan should batch by topic and preserve ID mapping carefully.

No blocker found for material storage:
- `CreateMaterialRequest` contains only `topicId`, `name`, `description`, `materialType`, `sortOrder`.
- No `fileId`, `binary`, `multipart`, `url`, `storageKey`, `uploadToken` or similar field was found in the material create/update path.

## 8. Recommended next step

Можно переходить к `content-api-payload-plan.md`, но с двумя обязательными оговорками:
- payload plan должен явно ввести external mapping strategy для stable demo codes, потому что runtime DTO их не хранит;
- payload plan должен строить `SELF`/`ASSIGNED` разделение через publication/final-control composition, а не через несуществующий `contour` field.

Минимальный следующий артефакт:
- `demo-data/scenarios/content-api-payload-plan.md`

Что в нём нужно зафиксировать:
- create order с live ID mapping;
- как связывать planning codes с runtime ids;
- какое значение `scoringPolicyCode` использовать;
- topic-by-topic batches для materials/questions/options/tests/test-questions/final-control/publication.

