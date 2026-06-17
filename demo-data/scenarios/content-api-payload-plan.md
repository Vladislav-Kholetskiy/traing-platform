# Content API Payload Plan

## 1. Статус артефакта

- Payload planning artifact для live создания demo-content через текущие backend API.
- Не является executable script.
- Не является SQL.
- Не меняет production code и production runtime.

## 2. Preconditions

- Backend запущен в `dev` profile.
- Доступен аутентифицированный `EXPERT` или `ADMIN` actor.
- Контентные endpoints из [content-api-surface-inventory.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/content-api-surface-inventory.md:1) доступны.
- Исходное demo-content baseline пустое или заранее известно и зафиксировано.
- Planning/source markdown-файлы согласованы:
  - [content-matrix.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/content-matrix.md:1)
  - [question-bank.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/question-bank.md:1)
  - [materials-source.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/materials-source.md:1)
  - [content-creation-runbook.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/content-creation-runbook.md:1)
- Перед первым live вызовом желательно вручную проверить, что `scoringPolicyCode = "DEFAULT"` принимается runtime.

## 3. Runtime ID mapping strategy

Runtime DTO не хранит stable business-codes вроде `COURSE-OPS-SAFETY`, `TOPIC-OPS-INTRO`, `Q-OPS-INTRO-001`.
Поэтому:
- planning code живёт только в orchestration/runbook слое;
- после каждого `create` нужно сохранить `runtime id` из response;
- все последующие request body должны использовать уже runtime id, а не planning code;
- при ошибке нельзя продолжать по памяти: нужно обновить mapping table и только потом двигаться дальше.

Blank mapping table template:

| Planning code | Entity type | Runtime ID | Created at step | Notes |
|---|---|---|---|---|
| `COURSE-OPS-SAFETY` | `course` |  | `6.1` |  |
| `TOPIC-OPS-INTRO` | `topic` |  | `6.2` | parent `COURSE-OPS-SAFETY` |
| `MAT-OPS-INTRO-TEXT-01` | `material` |  | `6.3` | parent `TOPIC-OPS-INTRO` |
| `Q-OPS-INTRO-001` | `question` |  | `6.4` | options tracked separately if needed |
| `TEST-OPS-INTRO-FINAL` | `test` |  | `6.5` | final-control candidate |

Recommended operator notation inside live run:
- `${courseId:COURSE-OPS-SAFETY}`
- `${topicId:TOPIC-OPS-INTRO}`
- `${questionId:Q-OPS-INTRO-001}`
- `${testId:TEST-OPS-INTRO-FINAL}`

## 4. Global creation order

1. Create `3` courses.
2. Create `8` topics under created courses.
3. Create `27` material records under topics.
4. Create `85` questions under topics.
5. Create answer options for each question.
6. Create `13` tests under topics.
7. Attach explicit question sets to each test via `test-question` links.
8. Publish parent courses needed for topic publication.
9. Publish topics that will own published materials/questions/tests.
10. Publish materials.
11. Publish questions.
12. Publish tests.
13. Set active final-control tests for assigned topics.
14. Verify self-visible tests remain published and not active-final.
15. Run content checkpoints from [acceptance-checkpoints.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/acceptance-checkpoints.md:1).

Important lifecycle nuance from runtime:
- topic publish requires parent course already `PUBLISHED`;
- material/question/test publish requires parent topic already `PUBLISHED`;
- test publish requires non-empty explicit composition and published questions;
- active final assign requires topic `PUBLISHED` and candidate test `CONTROL + PUBLISHED`.

Because of this circularity, live run should use this effective lifecycle sequence:
1. create all content in `DRAFT`;
2. publish courses first;
3. publish topics;
4. publish materials and questions;
5. publish tests;
6. set active final tests.

## 5. Request template section by surface

### Course create

- Method: `POST`
- Path: `/api/v1/expert/content/courses`
- Request body template:

```json
{
  "name": "${courseName}",
  "description": "${courseDescription}",
  "sortOrder": ${courseSortOrder}
}
```

- Response fields to capture:
  - `id`
  - `status`
  - `name`
- Dependency IDs required: none
- Notes:
  - initial status expected `DRAFT`
  - no business-code field exists

### Topic create

- Method: `POST`
- Path: `/api/v1/expert/content/topics`
- Request body template:

```json
{
  "courseId": ${courseId:COURSE-*},
  "name": "${topicName}",
  "description": "${topicDescription}",
  "sortOrder": ${topicSortOrder}
}
```

- Response fields to capture:
  - `id`
  - `courseId`
  - `status`
- Dependency IDs required:
  - `courseId`
- Notes:
  - `sortOrder` must be unique within course

### Material create

- Method: `POST`
- Path: `/api/v1/expert/content/materials`
- Request body template:

```json
{
  "topicId": ${topicId:TOPIC-*},
  "name": "${materialName}",
  "description": "${materialDescription}",
  "materialType": "${materialType}",
  "sortOrder": ${materialSortOrder}
}
```

- Response fields to capture:
  - `id`
  - `topicId`
  - `materialType`
  - `status`
- Dependency IDs required:
  - `topicId`
- Notes:
  - metadata-only surface
  - no file upload, no fileId, no storage URL
  - helper must send only short metadata description
  - full educational text/synopsis stays in `materials-source.md` and is not posted to API

### Question create

- Method: `POST`
- Path: `/api/v1/expert/content/questions`
- Request body template:

```json
{
  "topicId": ${topicId:TOPIC-*},
  "body": "${questionText}",
  "questionType": "${questionType}",
  "sortOrder": ${questionSortOrder}
}
```

- Response fields to capture:
  - `id`
  - `topicId`
  - `questionType`
  - `status`
- Dependency IDs required:
  - `topicId`
- Notes:
  - exact texts come from [question-bank.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/question-bank.md:1)

### Answer option create

- Method: `POST`
- Path: `/api/v1/expert/content/questions/{questionId}/answer-options`
- Request body template:

```json
{
  "body": "${optionText}",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": ${isCorrect},
  "displayOrder": ${displayOrder},
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

- Response fields to capture:
  - `id`
  - `questionId`
  - `isCorrect`
  - `displayOrder`
- Dependency IDs required:
  - `questionId`
- Notes:
  - for this demo only `CHOICE_OPTION`
  - `displayOrder` must be unique within question

### Test create

- Method: `POST`
- Path: `/api/v1/expert/content/tests`
- Request body template:

```json
{
  "topicId": ${topicId:TOPIC-*},
  "name": "${testName}",
  "description": "${testDescription}",
  "testType": "${testType}",
  "thresholdPercent": ${thresholdPercent},
  "scoringPolicyCode": "DEFAULT",
  "sortOrder": ${testSortOrder}
}
```

- Response fields to capture:
  - `id`
  - `topicId`
  - `testType`
  - `status`
  - `scoringPolicyCode`
- Dependency IDs required:
  - `topicId`
- Notes:
  - `DEFAULT` is verified from current code/tests, but recheck if backend rejects it

### Test-question link create

- Method: `POST`
- Path: `/api/v1/expert/content/tests/{testId}/questions`
- Request body template:

```json
{
  "questionId": ${questionId:Q-*},
  "displayOrder": ${displayOrder},
  "weight": ${weight}
}
```

- Response fields to capture:
  - `id`
  - `testId`
  - `questionId`
  - `displayOrder`
  - `weight`
- Dependency IDs required:
  - `testId`
  - `questionId`
- Notes:
  - test and question must belong to same topic
  - composition is explicit; no random sampling assumption

### Set active final test

- Method: `POST`
- Path: `/api/v1/expert/content/topics/{topicId}/active-final-tests/{testId}/assign`
- Request body template: none
- Response fields to capture:
  - no body expected
  - verify by follow-up `GET /api/v1/expert/content/topics/{topicId}/active-final-test`
- Dependency IDs required:
  - `topicId`
  - `testId`
- Notes:
  - topic must already be `PUBLISHED`
  - test must already be `PUBLISHED`
  - test must have `testType = CONTROL`

### Publish lifecycle

- Generic method: `POST`
- Generic path:
  - `/api/v1/expert/content/lifecycle/courses/{id}/publish`
  - `/api/v1/expert/content/lifecycle/topics/{id}/publish`
  - `/api/v1/expert/content/lifecycle/materials/{id}/publish`
  - `/api/v1/expert/content/lifecycle/questions/{id}/publish`
  - `/api/v1/expert/content/lifecycle/tests/{id}/publish`
- Request body: none
- Response fields to capture:
  - `id`
  - `status`
- Dependency IDs required:
  - target runtime id
- Notes:
  - questions must have valid answer composition before publish
  - tests must have explicit question composition using published questions

## 6. Concrete payload plan by entity group

### 6.1 Courses

| Planning code | Name | Contour semantics | Request template | Capture |
|---|---|---|---|---|
| `COURSE-OPS-SAFETY` | `Промышленная безопасность оператора НПЗ` | assigned contour later by final-control + assignment usage | `POST /courses` with course name/description/sort order from content-matrix | `courseId` |
| `COURSE-PERMIT-SAFETY` | `Наряд-допуск и безопасная организация работ` | assigned contour later by final-control + assignment usage | same pattern | `courseId` |
| `COURSE-OPO-SELF-REFRESHER` | `Самоподготовка по промышленной безопасности на ОПО` | self contour later by published non-final tests | same pattern | `courseId` |

Recommended create order:
1. `COURSE-OPS-SAFETY`
2. `COURSE-PERMIT-SAFETY`
3. `COURSE-OPO-SELF-REFRESHER`

### 6.2 Topics

| Planning code | Parent course | Name | Request template | Capture |
|---|---|---|---|---|
| `TOPIC-OPS-INTRO` | `COURSE-OPS-SAFETY` | `Вводный инструктаж по промышленной безопасности` | use `${courseId:COURSE-OPS-SAFETY}` | `topicId` |
| `TOPIC-OPS-UNIT-SAFE` | `COURSE-OPS-SAFETY` | `Безопасная работа на технологической установке` | same | `topicId` |
| `TOPIC-OPS-INCIDENT` | `COURSE-OPS-SAFETY` | `Действия при нештатной ситуации` | same | `topicId` |
| `TOPIC-PERMIT-PREP` | `COURSE-PERMIT-SAFETY` | `Подготовка наряда-допуска` | use `${courseId:COURSE-PERMIT-SAFETY}` | `topicId` |
| `TOPIC-PERMIT-RISK` | `COURSE-PERMIT-SAFETY` | `Контроль рисков перед началом работ` | same | `topicId` |
| `TOPIC-PERMIT-ROLES` | `COURSE-PERMIT-SAFETY` | `Роли руководителя и ответственного исполнителя` | same | `topicId` |
| `TOPIC-OPO-SELF-RULES` | `COURSE-OPO-SELF-REFRESHER` | `Основные требования промышленной безопасности на ОПО` | use `${courseId:COURSE-OPO-SELF-REFRESHER}` | `topicId` |
| `TOPIC-OPO-SELF-INCIDENTS` | `COURSE-OPO-SELF-REFRESHER` | `Типовые нарушения и действия работника при угрозе аварии` | same | `topicId` |

### 6.3 Materials

All `27` materials are created via `POST /api/v1/expert/content/materials`.
Long source text is not duplicated here; operator must take `name`, short metadata `description`, `materialType`, and semantic source from [materials-source.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/materials-source.md:1).
Full educational content from `materials-source.md` remains outside runtime payload and is not sent in `description`.

| Planning code | Topic | Material type | Name | Description source | Sort order |
|---|---|---|---|---|---|
| `MAT-OPS-INTRO-TEXT-01` | `TOPIC-OPS-INTRO` | `TEXT` | from materials-source | `materials-source -> TOPIC-OPS-INTRO` | `0` |
| `MAT-OPS-INTRO-PDF-02` | `TOPIC-OPS-INTRO` | `PDF` | from materials-source | same | `1` |
| `MAT-OPS-INTRO-DOCX-03` | `TOPIC-OPS-INTRO` | `DOCX` | from materials-source | same | `2` |
| `MAT-OPS-INTRO-VIDEO-04` | `TOPIC-OPS-INTRO` | `VIDEO` | from materials-source | same | `3` |
| `MAT-OPS-UNIT-TEXT-01` | `TOPIC-OPS-UNIT-SAFE` | `TEXT` | from materials-source | `materials-source -> TOPIC-OPS-UNIT-SAFE` | `0` |
| `MAT-OPS-UNIT-PDF-02` | `TOPIC-OPS-UNIT-SAFE` | `PDF` | from materials-source | same | `1` |
| `MAT-OPS-UNIT-VIDEO-03` | `TOPIC-OPS-UNIT-SAFE` | `VIDEO` | from materials-source | same | `2` |
| `MAT-OPS-INCIDENT-TEXT-01` | `TOPIC-OPS-INCIDENT` | `TEXT` | from materials-source | `materials-source -> TOPIC-OPS-INCIDENT` | `0` |
| `MAT-OPS-INCIDENT-PDF-02` | `TOPIC-OPS-INCIDENT` | `PDF` | from materials-source | same | `1` |
| `MAT-OPS-INCIDENT-DOCX-03` | `TOPIC-OPS-INCIDENT` | `DOCX` | from materials-source | same | `2` |
| `MAT-OPS-INCIDENT-VIDEO-04` | `TOPIC-OPS-INCIDENT` | `VIDEO` | from materials-source | same | `3` |
| `MAT-PERMIT-PREP-TEXT-01` | `TOPIC-PERMIT-PREP` | `TEXT` | from materials-source | `materials-source -> TOPIC-PERMIT-PREP` | `0` |
| `MAT-PERMIT-PREP-PDF-02` | `TOPIC-PERMIT-PREP` | `PDF` | from materials-source | same | `1` |
| `MAT-PERMIT-PREP-DOCX-03` | `TOPIC-PERMIT-PREP` | `DOCX` | from materials-source | same | `2` |
| `MAT-PERMIT-RISK-TEXT-01` | `TOPIC-PERMIT-RISK` | `TEXT` | from materials-source | `materials-source -> TOPIC-PERMIT-RISK` | `0` |
| `MAT-PERMIT-RISK-PDF-02` | `TOPIC-PERMIT-RISK` | `PDF` | from materials-source | same | `1` |
| `MAT-PERMIT-RISK-DOCX-03` | `TOPIC-PERMIT-RISK` | `DOCX` | from materials-source | same | `2` |
| `MAT-PERMIT-RISK-VIDEO-04` | `TOPIC-PERMIT-RISK` | `VIDEO` | from materials-source | same | `3` |
| `MAT-PERMIT-ROLES-TEXT-01` | `TOPIC-PERMIT-ROLES` | `TEXT` | from materials-source | `materials-source -> TOPIC-PERMIT-ROLES` | `0` |
| `MAT-PERMIT-ROLES-PDF-02` | `TOPIC-PERMIT-ROLES` | `PDF` | from materials-source | same | `1` |
| `MAT-PERMIT-ROLES-DOCX-03` | `TOPIC-PERMIT-ROLES` | `DOCX` | from materials-source | same | `2` |
| `MAT-PERMIT-ROLES-VIDEO-04` | `TOPIC-PERMIT-ROLES` | `VIDEO` | from materials-source | same | `3` |
| `MAT-OPO-SELF-RULES-TEXT-01` | `TOPIC-OPO-SELF-RULES` | `TEXT` | from materials-source | `materials-source -> TOPIC-OPO-SELF-RULES` | `0` |
| `MAT-OPO-SELF-RULES-PDF-02` | `TOPIC-OPO-SELF-RULES` | `PDF` | from materials-source | same | `1` |
| `MAT-OPO-SELF-RULES-VIDEO-03` | `TOPIC-OPO-SELF-RULES` | `VIDEO` | from materials-source | same | `2` |
| `MAT-OPO-SELF-INCIDENTS-TEXT-01` | `TOPIC-OPO-SELF-INCIDENTS` | `TEXT` | from materials-source | `materials-source -> TOPIC-OPO-SELF-INCIDENTS` | `0` |
| `MAT-OPO-SELF-INCIDENTS-PDF-02` | `TOPIC-OPO-SELF-INCIDENTS` | `PDF` | from materials-source | same | `1` |
| `MAT-OPO-SELF-INCIDENTS-VIDEO-03` | `TOPIC-OPO-SELF-INCIDENTS` | `VIDEO` | from materials-source | same | `2` |

### 6.4 Questions

Question creation is sequential by topic.
Exact text, options, correct flags and intended tests come from [question-bank.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/question-bank.md:1).

Per-topic batches:

| Topic code | Question count | SINGLE_CHOICE | MULTIPLE_CHOICE | Source |
|---|---|---|---|---|
| `TOPIC-OPS-INTRO` | `11` | `6` | `5` | `question-bank -> TOPIC-OPS-INTRO` |
| `TOPIC-OPS-UNIT-SAFE` | `11` | `6` | `5` | `question-bank -> TOPIC-OPS-UNIT-SAFE` |
| `TOPIC-OPS-INCIDENT` | `11` | `6` | `5` | `question-bank -> TOPIC-OPS-INCIDENT` |
| `TOPIC-PERMIT-PREP` | `10` | `7` | `3` | `question-bank -> TOPIC-PERMIT-PREP` |
| `TOPIC-PERMIT-RISK` | `10` | `6` | `4` | `question-bank -> TOPIC-PERMIT-RISK` |
| `TOPIC-PERMIT-ROLES` | `10` | `6` | `4` | `question-bank -> TOPIC-PERMIT-ROLES` |
| `TOPIC-OPO-SELF-RULES` | `11` | `7` | `4` | `question-bank -> TOPIC-OPO-SELF-RULES` |
| `TOPIC-OPO-SELF-INCIDENTS` | `11` | `7` | `4` | `question-bank -> TOPIC-OPO-SELF-INCIDENTS` |

Full `SINGLE_CHOICE` example:

Planning source: `Q-OPS-INTRO-001`

Question create:

```json
{
  "topicId": ${topicId:TOPIC-OPS-INTRO},
  "body": "Какое действие работник должен выполнить до начала самостоятельной работы на технологической установке после первичного допуска?",
  "questionType": "SINGLE_CHOICE",
  "sortOrder": 0
}
```

Then create options:

```json
{
  "body": "Начать работу под наблюдением коллеги без дополнительного инструктажа",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": false,
  "displayOrder": 0,
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

```json
{
  "body": "Убедиться, что инструктаж пройден, маршрут эвакуации понятен, а рабочее место принято безопасно",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": true,
  "displayOrder": 1,
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

```json
{
  "body": "Сразу отключить локальную сигнализацию, чтобы не отвлекала",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": false,
  "displayOrder": 2,
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

```json
{
  "body": "Приступить к операциям по указанию любого опытного оператора соседней смены",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": false,
  "displayOrder": 3,
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

Full `MULTIPLE_CHOICE` example:

Planning source: `Q-OPO-SELF-INCIDENTS-003`

Question create:

```json
{
  "topicId": ${topicId:TOPIC-OPO-SELF-INCIDENTS},
  "body": "Какие действия работник должен выполнить при обнаружении признаков возможной аварийной ситуации на ОПО?",
  "questionType": "MULTIPLE_CHOICE",
  "sortOrder": 2
}
```

Then create options:

```json
{
  "body": "Сообщить непосредственному руководителю или диспетчеру по установленному каналу связи",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": true,
  "displayOrder": 0,
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

```json
{
  "body": "Самостоятельно продолжить работу, если оборудование пока не остановилось",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": false,
  "displayOrder": 1,
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

```json
{
  "body": "Прекратить опасные действия и выйти из опасной зоны, если это предусмотрено инструкцией",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": true,
  "displayOrder": 2,
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

```json
{
  "body": "Игнорировать запах газа до подтверждения коллегой",
  "answerOptionRole": "CHOICE_OPTION",
  "isCorrect": false,
  "displayOrder": 3,
  "pairingKey": null,
  "canonicalOrderPosition": null
}
```

For all other questions:
- create question from source text in `question-bank.md`;
- capture `questionId` under its `Q-*` planning code;
- create all listed answer options with exact `isCorrect` flags;
- for `SINGLE_CHOICE` exactly one option must be `true`;
- for `MULTIPLE_CHOICE` all source-marked correct options must be `true`.

### 6.5 Tests

All tests use `scoringPolicyCode = "DEFAULT"`.

| Planning code | Topic/course | Runtime testType | Name | Threshold | Capture |
|---|---|---|---|---|---|
| `TEST-OPS-INTRO-FINAL` | `TOPIC-OPS-INTRO` / `COURSE-OPS-SAFETY` | `CONTROL` | from content-matrix | from content-matrix | `testId` |
| `TEST-OPS-INTRO-SELF` | `TOPIC-OPS-INTRO` / `COURSE-OPS-SAFETY` | `TRAINING` | from content-matrix | from content-matrix | `testId` |
| `TEST-OPS-UNIT-FINAL` | `TOPIC-OPS-UNIT-SAFE` / `COURSE-OPS-SAFETY` | `CONTROL` | from content-matrix | from content-matrix | `testId` |
| `TEST-OPS-INCIDENT-FINAL` | `TOPIC-OPS-INCIDENT` / `COURSE-OPS-SAFETY` | `CONTROL` | from content-matrix | from content-matrix | `testId` |
| `TEST-OPS-INCIDENT-SELF` | `TOPIC-OPS-INCIDENT` / `COURSE-OPS-SAFETY` | `TRAINING` | from content-matrix | from content-matrix | `testId` |
| `TEST-PERMIT-PREP-FINAL` | `TOPIC-PERMIT-PREP` / `COURSE-PERMIT-SAFETY` | `CONTROL` | from content-matrix | from content-matrix | `testId` |
| `TEST-PERMIT-PREP-TRAIN` | `TOPIC-PERMIT-PREP` / `COURSE-PERMIT-SAFETY` | `TRAINING` | from content-matrix | from content-matrix | `testId` |
| `TEST-PERMIT-RISK-FINAL` | `TOPIC-PERMIT-RISK` / `COURSE-PERMIT-SAFETY` | `CONTROL` | from content-matrix | from content-matrix | `testId` |
| `TEST-PERMIT-ROLES-FINAL` | `TOPIC-PERMIT-ROLES` / `COURSE-PERMIT-SAFETY` | `CONTROL` | from content-matrix | from content-matrix | `testId` |
| `TEST-PERMIT-ROLES-TRAIN` | `TOPIC-PERMIT-ROLES` / `COURSE-PERMIT-SAFETY` | `TRAINING` | from content-matrix | from content-matrix | `testId` |
| `TEST-OPO-SELF-RULES-SELF` | `TOPIC-OPO-SELF-RULES` / `COURSE-OPO-SELF-REFRESHER` | `TRAINING` | from content-matrix | from content-matrix | `testId` |
| `TEST-OPO-SELF-INCIDENTS-SELF` | `TOPIC-OPO-SELF-INCIDENTS` / `COURSE-OPO-SELF-REFRESHER` | `TRAINING` | from content-matrix | from content-matrix | `testId` |
| `TEST-OPO-SELF-GENERAL` | `COURSE-OPO-SELF-REFRESHER` topic-level source per content-matrix | `TRAINING` | from content-matrix | from content-matrix | `testId` |

### 6.6 Test-question links

Each test must receive an explicit source question set.
Operator should take the exact selected `Q-*` codes from [content-matrix.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/content-matrix.md:1) and [question-bank.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/question-bank.md:1).

Per-test composition plan:

| Test planning code | Source question code set | Link rule |
|---|---|---|
| `TEST-OPS-INTRO-FINAL` | selected final subset from `TOPIC-OPS-INTRO` bank | `8` explicit links |
| `TEST-OPS-INTRO-SELF` | selected self/training subset from `TOPIC-OPS-INTRO` bank | `6` explicit links |
| `TEST-OPS-UNIT-FINAL` | selected final subset from `TOPIC-OPS-UNIT-SAFE` bank | `9` explicit links |
| `TEST-OPS-INCIDENT-FINAL` | selected final subset from `TOPIC-OPS-INCIDENT` bank | `8` explicit links |
| `TEST-OPS-INCIDENT-SELF` | selected self subset from `TOPIC-OPS-INCIDENT` bank | `6` explicit links |
| `TEST-PERMIT-PREP-FINAL` | selected final subset from `TOPIC-PERMIT-PREP` bank | `7` explicit links |
| `TEST-PERMIT-PREP-TRAIN` | selected training subset from `TOPIC-PERMIT-PREP` bank | `6` explicit links |
| `TEST-PERMIT-RISK-FINAL` | selected final subset from `TOPIC-PERMIT-RISK` bank | `8` explicit links |
| `TEST-PERMIT-ROLES-FINAL` | selected final subset from `TOPIC-PERMIT-ROLES` bank | `7` explicit links |
| `TEST-PERMIT-ROLES-TRAIN` | selected training subset from `TOPIC-PERMIT-ROLES` bank | `6` explicit links |
| `TEST-OPO-SELF-RULES-SELF` | selected self subset from `TOPIC-OPO-SELF-RULES` bank | `6` explicit links |
| `TEST-OPO-SELF-INCIDENTS-SELF` | selected self subset from `TOPIC-OPO-SELF-INCIDENTS` bank | `6` explicit links |
| `TEST-OPO-SELF-GENERAL` | combined self subset from self-course banks per content-matrix | `8` explicit links |

Per-link request template:

```json
{
  "questionId": ${questionId:Q-*},
  "displayOrder": ${n},
  "weight": 1
}
```

Rule:
- preserve explicit order from manual selection sheet;
- use weight `1` unless runtime-specific weighting is intentionally varied;
- do not assume random pool assembly.

### 6.7 Active final control

Assigned topics:

| Topic planning code | Final test planning code | Request |
|---|---|---|
| `TOPIC-OPS-INTRO` | `TEST-OPS-INTRO-FINAL` | `POST /api/v1/expert/content/topics/${topicId}/active-final-tests/${testId}/assign` |
| `TOPIC-OPS-UNIT-SAFE` | `TEST-OPS-UNIT-FINAL` | same pattern |
| `TOPIC-OPS-INCIDENT` | `TEST-OPS-INCIDENT-FINAL` | same pattern |
| `TOPIC-PERMIT-PREP` | `TEST-PERMIT-PREP-FINAL` | same pattern |
| `TOPIC-PERMIT-RISK` | `TEST-PERMIT-RISK-FINAL` | same pattern |
| `TOPIC-PERMIT-ROLES` | `TEST-PERMIT-ROLES-FINAL` | same pattern |

Self topics:
- `TOPIC-OPO-SELF-RULES`: no active final control
- `TOPIC-OPO-SELF-INCIDENTS`: no active final control

Important separation rule:
- `ASSIGNED` final-control tests become mandatory later because they are published `CONTROL` tests linked as active final tests.
- `SELF` tests remain self-visible because they are published and not linked as active final tests.

### 6.8 Publication

Recommended runtime-safe publication sequence:

1. Publish all courses:
   - `/api/v1/expert/content/lifecycle/courses/{courseId}/publish`
2. Publish all topics:
   - `/api/v1/expert/content/lifecycle/topics/{topicId}/publish`
3. Publish all materials:
   - `/api/v1/expert/content/lifecycle/materials/{materialId}/publish`
4. Publish all questions:
   - `/api/v1/expert/content/lifecycle/questions/{questionId}/publish`
5. Publish all tests:
   - `/api/v1/expert/content/lifecycle/tests/{testId}/publish`
6. Set active final tests for assigned topics.
7. Verify self-visible tests are still visible through `/api/v1/self-testing/tests`.

Why this order:
- topic publish needs parent course published;
- material/question/test publish needs parent topic published;
- test publish needs published questions and completed explicit composition;
- final-control assign needs topic and final test published.

## 7. Validation after payload run

Check against [acceptance-checkpoints.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/acceptance-checkpoints.md:1):

- `CONTENT-001`: `course = 3`
- `CONTENT-002`: `topic = 8`
- `CONTENT-003`: `material = 27`
- `CONTENT-004`: `test = 13`
- `CONTENT-005`: `question = 85`
- final-control exists for all assigned topics
- self tests are `PUBLISHED`
- self tests are not active final controls
- only `TEXT/PDF/DOCX/VIDEO` material types used
- only `SINGLE_CHOICE/MULTIPLE_CHOICE` question types used

Operational spot checks after create:
- `GET /api/v1/expert/content/topics/{topicId}/active-final-test`
- `GET /api/v1/self-testing/tests`
- `GET /api/v1/expert/content/tests/{testId}/questions`
- lifecycle reads for sample entities

## 8. Known blockers/risks

- No business-code persistence in runtime DTO/domain.
- Manual runtime ID mapping is mandatory.
- `scoringPolicyCode = "DEFAULT"` is evidence-based from code/tests, but first live backend response must confirm it.
- Publication ordering may need runtime adjustment if hidden validations appear.
- No bulk create endpoints means long manual or semi-manual run.
- If one call fails, stop immediately, fix mapping/validation issue, and only then continue.
- If runtime unexpectedly requires material upload/file binding, treat it as blocker and do not invent storage workaround.
- If self-visible behavior differs from inventory assumption, verify whether test was accidentally linked as active final control or not yet published.
