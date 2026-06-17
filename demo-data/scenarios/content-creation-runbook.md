# Content Creation Runbook

## 1. Статус артефакта

- runbook for live content creation;
- not SQL migration;
- not generated payload collection;
- no production changes;
- используется как planning-артефакт перед ручным UI/API-прогоном и перед подготовкой curl/Postman коллекции.

## 2. Preconditions

- backend запущен в `dev` profile;
- существует `ADMIN` или `EXPERT` actor с доступом к content authoring surfaces;
- доступна auth session или token для content creation;
- org/users bootstrap уже завершён либо не требуется для самого этапа создания контента;
- подготовлены source-файлы:
  - [content-matrix.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/content-matrix.md:1)
  - [question-bank.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/question-bank.md:1)
  - [materials-source.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/materials-source.md:1)
- известны и подтверждены runtime surfaces для content lifecycle, либо шаги помечаются `VERIFY_AGAINST_RUNTIME` до фактического прогона.

## 3. Creation Dependency Graph

Порядок создания должен быть строго таким:

1. create courses
2. create topics under courses
3. create material records under topics
4. create questions under topic question banks
5. create tests
6. attach questions to tests through explicit `test_question` composition
7. mark final-control tests for assigned topics
8. publish tests where lifecycle требует published state
9. publish topics
10. publish courses
11. verify counts, final-control separation and self-visible separation through acceptance checkpoints

Критические зависимости:

- topic нельзя создавать без созданного course;
- material/question нельзя создавать без topic;
- test нельзя считать готовым без явного question set;
- assigned topic нельзя публиковать в контуре mandatory rollout без final-control test;
- self tests не должны становиться `FINAL_TOPIC_CONTROL`.

## 4. Course Creation

| Course code | Course name | Contour | Expected publication state | Source section | API/UI surface candidate |
|---|---|---|---|---|---|
| `COURSE-OPS-SAFETY` | Промышленная безопасность оператора НПЗ | `ASSIGNED` | `PUBLISHED` after final-control readiness | `Courses` in `content-matrix.md` | `GET/POST /api/v1/expert/content/courses` or `VERIFY_AGAINST_RUNTIME` |
| `COURSE-PERMIT-SAFETY` | Наряд-допуск и безопасная организация работ | `ASSIGNED` | `PUBLISHED` after final-control readiness | `Courses` in `content-matrix.md` | `GET/POST /api/v1/expert/content/courses` or `VERIFY_AGAINST_RUNTIME` |
| `COURSE-OPO-SELF-REFRESHER` | Самоподготовка по промышленной безопасности на ОПО | `SELF` | `PUBLISHED` | `Courses` in `content-matrix.md` | `GET/POST /api/v1/expert/content/courses` or `VERIFY_AGAINST_RUNTIME` |

## 5. Topic Creation

| Topic code | Course code | Topic name | Contour | Expected publication state | Final-control required | Self-test required |
|---|---|---|---|---|---|---|
| `TOPIC-OPS-INTRO` | `COURSE-OPS-SAFETY` | Вводный инструктаж по промышленной безопасности | `ASSIGNED` | `PUBLISHED` | `YES` | `YES` |
| `TOPIC-OPS-UNIT-SAFE` | `COURSE-OPS-SAFETY` | Безопасная работа на технологической установке | `ASSIGNED` | `PUBLISHED` | `YES` | `NO` |
| `TOPIC-OPS-INCIDENT` | `COURSE-OPS-SAFETY` | Действия при нештатной ситуации | `ASSIGNED` | `PUBLISHED` | `YES` | `YES` |
| `TOPIC-PERMIT-PREP` | `COURSE-PERMIT-SAFETY` | Подготовка наряда-допуска | `ASSIGNED` | `PUBLISHED` | `YES` | `NO` |
| `TOPIC-PERMIT-RISK` | `COURSE-PERMIT-SAFETY` | Контроль рисков перед началом работ | `ASSIGNED` | `PUBLISHED` | `YES` | `NO` |
| `TOPIC-PERMIT-ROLES` | `COURSE-PERMIT-SAFETY` | Роли руководителя и ответственного исполнителя | `ASSIGNED` | `PUBLISHED` | `YES` | `NO` |
| `TOPIC-OPO-SELF-RULES` | `COURSE-OPO-SELF-REFRESHER` | Основные требования промышленной безопасности на ОПО | `SELF` | `PUBLISHED` | `NO` | `YES` |
| `TOPIC-OPO-SELF-INCIDENTS` | `COURSE-OPO-SELF-REFRESHER` | Типовые нарушения и действия работника при угрозе аварии | `SELF` | `PUBLISHED` | `NO` | `YES` |

## 6. Material Creation

Материалы создавать только как metadata records. Никакой загрузки файлов, storage binding или URL rendering assumption на этом этапе не допускается.

| Material code | Topic code | Material type | Name | Source section | Sort order | Notes |
|---|---|---|---|---|---:|---|
| `MAT-OPS-INTRO-TEXT` | `TOPIC-OPS-INTRO` | `TEXT` | Ключевые правила вводного инструктажа | `TOPIC-OPS-INTRO` in `materials-source.md` | `10` | text content pasted from source file |
| `MAT-OPS-INTRO-PDF` | `TOPIC-OPS-INTRO` | `PDF` | Памятка по промышленной безопасности | `TOPIC-OPS-INTRO` in `materials-source.md` | `20` | future PDF content only, no file |
| `MAT-OPS-INTRO-DOCX` | `TOPIC-OPS-INTRO` | `DOCX` | Чек-лист допуска к смене | `TOPIC-OPS-INTRO` in `materials-source.md` | `30` | future DOCX structure only |
| `MAT-OPS-INTRO-VIDEO` | `TOPIC-OPS-INTRO` | `VIDEO` | Вводный видеоинструктаж | `TOPIC-OPS-INTRO` in `materials-source.md` | `40` | video synopsis only |
| `MAT-OPS-UNIT-TEXT` | `TOPIC-OPS-UNIT-SAFE` | `TEXT` | Безопасная работа на установке: тезисы | `TOPIC-OPS-UNIT-SAFE` in `materials-source.md` | `10` | text content pasted from source file |
| `MAT-OPS-UNIT-PDF` | `TOPIC-OPS-UNIT-SAFE` | `PDF` | Инструкция по безопасной эксплуатации | `TOPIC-OPS-UNIT-SAFE` in `materials-source.md` | `20` | future PDF content only |
| `MAT-OPS-UNIT-DOCX` | `TOPIC-OPS-UNIT-SAFE` | `DOCX` | Чек-лист осмотра перед запуском | `TOPIC-OPS-UNIT-SAFE` in `materials-source.md` | `30` | future DOCX structure only |
| `MAT-OPS-UNIT-VIDEO` | `TOPIC-OPS-UNIT-SAFE` | `VIDEO` | Видеоразбор безопасного запуска | `TOPIC-OPS-UNIT-SAFE` in `materials-source.md` | `40` | video synopsis only |
| `MAT-OPS-INCIDENT-TEXT` | `TOPIC-OPS-INCIDENT` | `TEXT` | Алгоритм действий при инциденте | `TOPIC-OPS-INCIDENT` in `materials-source.md` | `10` | text content pasted from source file |
| `MAT-OPS-INCIDENT-PDF` | `TOPIC-OPS-INCIDENT` | `PDF` | Схема аварийного реагирования | `TOPIC-OPS-INCIDENT` in `materials-source.md` | `20` | future PDF content only |
| `MAT-OPS-INCIDENT-DOCX` | `TOPIC-OPS-INCIDENT` | `DOCX` | Памятка по взаимодействию со сменой | `TOPIC-OPS-INCIDENT` in `materials-source.md` | `30` | future DOCX structure only |
| `MAT-OPS-INCIDENT-VIDEO` | `TOPIC-OPS-INCIDENT` | `VIDEO` | Видеоразбор аварийного сценария | `TOPIC-OPS-INCIDENT` in `materials-source.md` | `40` | video synopsis only |
| `MAT-PERMIT-PREP-TEXT` | `TOPIC-PERMIT-PREP` | `TEXT` | Подготовка наряда: основные шаги | `TOPIC-PERMIT-PREP` in `materials-source.md` | `10` | text content pasted from source file |
| `MAT-PERMIT-PREP-PDF` | `TOPIC-PERMIT-PREP` | `PDF` | Образец наряда-допуска | `TOPIC-PERMIT-PREP` in `materials-source.md` | `20` | future PDF content only |
| `MAT-PERMIT-PREP-DOCX` | `TOPIC-PERMIT-PREP` | `DOCX` | Памятка по согласованию наряда | `TOPIC-PERMIT-PREP` in `materials-source.md` | `30` | future DOCX structure only |
| `MAT-PERMIT-RISK-TEXT` | `TOPIC-PERMIT-RISK` | `TEXT` | Контроль рисков перед началом работ | `TOPIC-PERMIT-RISK` in `materials-source.md` | `10` | text content pasted from source file |
| `MAT-PERMIT-RISK-PDF` | `TOPIC-PERMIT-RISK` | `PDF` | Карта типовых рисков | `TOPIC-PERMIT-RISK` in `materials-source.md` | `20` | future PDF content only |
| `MAT-PERMIT-RISK-VIDEO` | `TOPIC-PERMIT-RISK` | `VIDEO` | Видеоразбор предработного обхода | `TOPIC-PERMIT-RISK` in `materials-source.md` | `30` | video synopsis only |
| `MAT-PERMIT-ROLES-TEXT` | `TOPIC-PERMIT-ROLES` | `TEXT` | Роли в нарядной системе | `TOPIC-PERMIT-ROLES` in `materials-source.md` | `10` | text content pasted from source file |
| `MAT-PERMIT-ROLES-PDF` | `TOPIC-PERMIT-ROLES` | `PDF` | Таблица распределения ответственности | `TOPIC-PERMIT-ROLES` in `materials-source.md` | `20` | future PDF content only |
| `MAT-PERMIT-ROLES-DOCX` | `TOPIC-PERMIT-ROLES` | `DOCX` | Памятка ответственному исполнителю | `TOPIC-PERMIT-ROLES` in `materials-source.md` | `30` | future DOCX structure only |
| `MAT-OPO-SELF-RULES-TEXT` | `TOPIC-OPO-SELF-RULES` | `TEXT` | Базовые требования промышленной безопасности на ОПО | `TOPIC-OPO-SELF-RULES` in `materials-source.md` | `10` | self-course text content |
| `MAT-OPO-SELF-RULES-PDF` | `TOPIC-OPO-SELF-RULES` | `PDF` | Памятка по обязательным требованиям на ОПО | `TOPIC-OPO-SELF-RULES` in `materials-source.md` | `20` | future PDF content only |
| `MAT-OPO-SELF-RULES-VIDEO` | `TOPIC-OPO-SELF-RULES` | `VIDEO` | Видеоразбор безопасного поведения на ОПО | `TOPIC-OPO-SELF-RULES` in `materials-source.md` | `30` | video synopsis only |
| `MAT-OPO-SELF-INCIDENTS-TEXT` | `TOPIC-OPO-SELF-INCIDENTS` | `TEXT` | Типовые нарушения и ранние признаки угрозы аварии | `TOPIC-OPO-SELF-INCIDENTS` in `materials-source.md` | `10` | self-course text content |
| `MAT-OPO-SELF-INCIDENTS-PDF` | `TOPIC-OPO-SELF-INCIDENTS` | `PDF` | Памятка по действиям при угрозе аварии | `TOPIC-OPO-SELF-INCIDENTS` in `materials-source.md` | `20` | future PDF content only |
| `MAT-OPO-SELF-INCIDENTS-VIDEO` | `TOPIC-OPO-SELF-INCIDENTS` | `VIDEO` | Разбор типовых нарушений на ОПО | `TOPIC-OPO-SELF-INCIDENTS` in `materials-source.md` | `30` | video synopsis only |

Итого для live creation order: `27` material records.

## 7. Question Creation

Полные формулировки, опции и correct answers не дублируются здесь. Их source of truth — [question-bank.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/question-bank.md:1).

| Topic code | Question count | SINGLE_CHOICE count | MULTIPLE_CHOICE count | Source section | Special notes |
|---|---:|---:|---:|---|---|
| `TOPIC-OPS-INTRO` | `11` | `6` | `5` | `TOPIC-OPS-INTRO` in `question-bank.md` | shared bank for assigned final and supplementary self-visible test |
| `TOPIC-OPS-UNIT-SAFE` | `11` | `6` | `5` | `TOPIC-OPS-UNIT-SAFE` in `question-bank.md` | assigned only |
| `TOPIC-OPS-INCIDENT` | `11` | `6` | `5` | `TOPIC-OPS-INCIDENT` in `question-bank.md` | shared bank for assigned final and supplementary self-visible test |
| `TOPIC-PERMIT-PREP` | `10` | `7` | `3` | `TOPIC-PERMIT-PREP` in `question-bank.md` | used for final and training test |
| `TOPIC-PERMIT-RISK` | `10` | `6` | `4` | `TOPIC-PERMIT-RISK` in `question-bank.md` | final only |
| `TOPIC-PERMIT-ROLES` | `10` | `6` | `4` | `TOPIC-PERMIT-ROLES` in `question-bank.md` | used for final and training test |
| `TOPIC-OPO-SELF-RULES` | `11` | `7` | `4` | `TOPIC-OPO-SELF-RULES` in `question-bank.md` | self topic test plus cross-topic self general |
| `TOPIC-OPO-SELF-INCIDENTS` | `11` | `7` | `4` | `TOPIC-OPO-SELF-INCIDENTS` in `question-bank.md` | self topic test plus cross-topic self general |

Итого question load target: `85` questions.

## 8. Test Creation

Тесты должны собираться через явные question links. Никакой random sampling assumption здесь не допускается.

| Test code | Topic/course code | Test name | Contour | Type | Expected publication state | Question count | Source rule | Final-control flag |
|---|---|---|---|---|---|---:|---|---|
| `TEST-OPS-INTRO-FINAL` | `TOPIC-OPS-INTRO` | Итоговый контроль: вводный инструктаж | `ASSIGNED` | `CONTROL` | `PUBLISHED` | `8` | explicit set from `TOPIC-OPS-INTRO` bank | `YES` |
| `TEST-OPS-INTRO-SELF` | `TOPIC-OPS-INTRO` | Самопроверка: вводный инструктаж | `SELF` | `CONTROL` | `PUBLISHED` | `6` | explicit set from same topic bank, separate from final | `NO` |
| `TEST-OPS-UNIT-FINAL` | `TOPIC-OPS-UNIT-SAFE` | Итоговый контроль: безопасная работа на установке | `ASSIGNED` | `CONTROL` | `PUBLISHED` | `9` | explicit set from `TOPIC-OPS-UNIT-SAFE` bank | `YES` |
| `TEST-OPS-INCIDENT-FINAL` | `TOPIC-OPS-INCIDENT` | Итоговый контроль: действия при инциденте | `ASSIGNED` | `CONTROL` | `PUBLISHED` | `8` | explicit set from `TOPIC-OPS-INCIDENT` bank | `YES` |
| `TEST-OPS-INCIDENT-SELF` | `TOPIC-OPS-INCIDENT` | Самопроверка: действия при инциденте | `SELF` | `CONTROL` | `PUBLISHED` | `6` | explicit set from same topic bank, separate from final | `NO` |
| `TEST-PERMIT-PREP-FINAL` | `TOPIC-PERMIT-PREP` | Итоговый контроль: подготовка наряда-допуска | `ASSIGNED` | `CONTROL` | `PUBLISHED` | `7` | explicit set from `TOPIC-PERMIT-PREP` bank | `YES` |
| `TEST-PERMIT-PREP-TRAIN` | `TOPIC-PERMIT-PREP` | Тренировочный тест: подготовка наряда-допуска | `ASSIGNED` | `CONTROL` | `PUBLISHED` | `6` | separate explicit set from same topic bank | `NO` |
| `TEST-PERMIT-RISK-FINAL` | `TOPIC-PERMIT-RISK` | Итоговый контроль: контроль рисков | `ASSIGNED` | `CONTROL` | `PUBLISHED` | `8` | explicit set from `TOPIC-PERMIT-RISK` bank | `YES` |
| `TEST-PERMIT-ROLES-FINAL` | `TOPIC-PERMIT-ROLES` | Итоговый контроль: роли руководителя и исполнителя | `ASSIGNED` | `CONTROL` | `PUBLISHED` | `7` | explicit set from `TOPIC-PERMIT-ROLES` bank | `YES` |
| `TEST-PERMIT-ROLES-TRAIN` | `TOPIC-PERMIT-ROLES` | Тренировочный тест: роли и ответственность | `ASSIGNED` | `CONTROL` | `PUBLISHED` | `6` | separate explicit set from same topic bank | `NO` |
| `TEST-OPO-SELF-RULES-SELF` | `TOPIC-OPO-SELF-RULES` | Самопроверка: базовые требования промышленной безопасности на ОПО | `SELF` | `CONTROL` | `PUBLISHED` | `6` | explicit set from `TOPIC-OPO-SELF-RULES` bank | `NO` |
| `TEST-OPO-SELF-INCIDENTS-SELF` | `TOPIC-OPO-SELF-INCIDENTS` | Самопроверка: нарушения и действия при угрозе аварии | `SELF` | `CONTROL` | `PUBLISHED` | `6` | explicit set from `TOPIC-OPO-SELF-INCIDENTS` bank | `NO` |
| `TEST-OPO-SELF-GENERAL` | `COURSE-OPO-SELF-REFRESHER` | Самопроверка: общий refresher по промышленной безопасности на ОПО | `SELF` | `CONTROL` | `PUBLISHED` | `8` | cross-topic explicit set from both OPO self topic banks | `NO` |

Итого test creation target: `13` tests.

## 9. Active Final Control

Assigned topics that must receive active final-control binding:

| Topic code | Final control test code | Must become active final test |
|---|---|---|
| `TOPIC-OPS-INTRO` | `TEST-OPS-INTRO-FINAL` | `YES` |
| `TOPIC-OPS-UNIT-SAFE` | `TEST-OPS-UNIT-FINAL` | `YES` |
| `TOPIC-OPS-INCIDENT` | `TEST-OPS-INCIDENT-FINAL` | `YES` |
| `TOPIC-PERMIT-PREP` | `TEST-PERMIT-PREP-FINAL` | `YES` |
| `TOPIC-PERMIT-RISK` | `TEST-PERMIT-RISK-FINAL` | `YES` |
| `TOPIC-PERMIT-ROLES` | `TEST-PERMIT-ROLES-FINAL` | `YES` |

Self topics:

- `TOPIC-OPO-SELF-RULES`: no `FINAL_TOPIC_CONTROL`, self-visible only
- `TOPIC-OPO-SELF-INCIDENTS`: no `FINAL_TOPIC_CONTROL`, self-visible only
- `TEST-OPS-INTRO-SELF`, `TEST-OPS-INCIDENT-SELF`, `TEST-OPO-SELF-*`: self-visible or supplementary only, never active final

## 10. Publication Order

Рекомендуемый safe order:

1. create all tests in draft state
2. attach explicit question sets to every test
3. set active final-control tests for every assigned topic
4. publish tests required for assigned and self contours
5. publish assigned and self topics
6. publish assigned and self courses

Publication guards:

- не публиковать assigned topic, если у неё не определён final-control test
- не публиковать assigned course, если хотя бы одна assigned topic не имеет active final test
- не переводить self tests в active final topic binding

## 11. Verification

После live creation сверять с [acceptance-checkpoints.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/acceptance-checkpoints.md:1):

- `CONTENT-001`: `course count = 3`
- `CONTENT-002`: `topic count = 8`
- `CONTENT-003`: `material count = 27`
- `CONTENT-004`: `test count = 13`
- `CONTENT-005`: `question count = 85`
- `CONTENT-008` and `CONTENT-009`: final-control readiness for assigned topics
- `CONTENT-010` and `CONTENT-011`: self-visible test publication and no-final separation
- `CONTENT-012`: allowed material types only
- `CONTENT-013`: allowed question types only
- `CONTENT-014`: explicit `test_question` composition present
- `CONTENT-015`: learner material rendering remains metadata-only

## 12. Failure Handling Notes

- if endpoint is absent or runtime shape is unclear: stop and record blocker as `VERIFY_AGAINST_RUNTIME`
- if UI unexpectedly requires material upload or file storage binding: do not invent storage workaround, record runtime mismatch
- if final-control flag cannot be set for assigned topics: assignment stage must not start
- if question type required by source is unsupported in UI/runtime: stop and record blocker
- if test-question explicit linking cannot be created: do not assume random sampling, stop and record blocker
- if course/topic lifecycle cannot reach `PUBLISHED`: do not proceed to mandatory assignment preparation
