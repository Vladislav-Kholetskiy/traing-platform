# Content Matrix

Статус артефакта: `PLANNING_BASELINE_V1`

Назначение:

- зафиксировать полный состав demo-контента для ручного или API-прогона;
- развести `ASSIGNED` и `SELF` контуры;
- подготовить основу для следующего шага: текстов material records и банков вопросов.

## Runtime-ограничения

- материалы описываются как `material records`, а не как реальные file uploads;
- текущий runtime материала опирается на metadata-поля: `topicId`, `name`, `description`, `materialType`, `sortOrder`;
- в матрице используются только `materialType`: `TEXT`, `PDF`, `DOCX`, `VIDEO`;
- в матрице используются только question types: `SINGLE_CHOICE`, `MULTIPLE_CHOICE`;
- автоматическая рандомизация вопросов не предполагается;
- избыточный банк вопросов нужен для ручной сборки альтернативных тестов;
- active final control tests используются только в mandatory assignment contour;
- self-visible tests публикуются отдельно и не являются `FINAL_TOPIC_CONTROL`.

## Summary

| Metric | Value |
|---|---|
| courses count | `3` |
| topics count | `8` |
| materials count | `27` |
| tests count | `13` |
| questions bank count | `85` |
| assigned courses | `2` |
| self courses | `1` |

## Courses

| Course code | Course name | Contour | Target audience | Publication requirement |
|---|---|---|---|---|
| `COURSE-OPS-SAFETY` | Промышленная безопасность оператора НПЗ | `ASSIGNED` | `OPS`, `CCK-UKK`, `CCK-UPV`, `CCK-MTBEO` | `PUBLISHED`, все темы `PUBLISHED`, по каждой теме есть `FINAL_TOPIC_CONTROL` |
| `COURSE-PERMIT-SAFETY` | Наряд-допуск и безопасная организация работ | `ASSIGNED` | managerial / `ITR` / `HEAD` contour | `PUBLISHED`, все темы `PUBLISHED`, по каждой теме есть `FINAL_TOPIC_CONTROL` |
| `COURSE-OPO-SELF-REFRESHER` | Самоподготовка по промышленной безопасности на ОПО | `SELF` | learner self-catalog | `PUBLISHED`, self-visible tests опубликованы отдельно от final-control semantics |

## Topics

| Topic code | Course code | Topic name | Contour | Has final control | Has self test | Planned bank size |
|---|---|---|---|---|---|---|
| `TOPIC-OPS-INTRO` | `COURSE-OPS-SAFETY` | Вводный инструктаж по промышленной безопасности | `ASSIGNED` | `YES` | `YES` | `16` |
| `TOPIC-OPS-UNIT-SAFE` | `COURSE-OPS-SAFETY` | Безопасная работа на технологической установке | `ASSIGNED` | `YES` | `NO` | `17` |
| `TOPIC-OPS-INCIDENT` | `COURSE-OPS-SAFETY` | Действия при нештатной ситуации | `ASSIGNED` | `YES` | `YES` | `15` |
| `TOPIC-PERMIT-PREP` | `COURSE-PERMIT-SAFETY` | Подготовка наряда-допуска | `ASSIGNED` | `YES` | `NO` | `12` |
| `TOPIC-PERMIT-RISK` | `COURSE-PERMIT-SAFETY` | Контроль рисков перед началом работ | `ASSIGNED` | `YES` | `NO` | `13` |
| `TOPIC-PERMIT-ROLES` | `COURSE-PERMIT-SAFETY` | Роли руководителя и ответственного исполнителя | `ASSIGNED` | `YES` | `NO` | `12` |
| `TOPIC-OPO-SELF-RULES` | `COURSE-OPO-SELF-REFRESHER` | Основные требования промышленной безопасности на ОПО | `SELF` | `NO` | `YES` | `10` |
| `TOPIC-OPO-SELF-INCIDENTS` | `COURSE-OPO-SELF-REFRESHER` | Типовые нарушения и действия работника при угрозе аварии | `SELF` | `NO` | `YES` | `10` |

## Materials

| Material code | Topic code | Material type | Material name | Description | Sort order |
|---|---|---|---|---|---|
| `MAT-OPS-INTRO-TEXT` | `TOPIC-OPS-INTRO` | `TEXT` | Ключевые правила вводного инструктажа | Базовые требования промышленной безопасности перед допуском к работе | `10` |
| `MAT-OPS-INTRO-PDF` | `TOPIC-OPS-INTRO` | `PDF` | Памятка по промышленной безопасности | Material record для локального регламента вводного инструктажа | `20` |
| `MAT-OPS-INTRO-DOCX` | `TOPIC-OPS-INTRO` | `DOCX` | Чек-лист допуска к смене | Material record для документа с контрольным перечнем допуска | `30` |
| `MAT-OPS-INTRO-VIDEO` | `TOPIC-OPS-INTRO` | `VIDEO` | Вводный видеоинструктаж | Material record для учебного ролика по вводному инструктажу | `40` |
| `MAT-OPS-UNIT-TEXT` | `TOPIC-OPS-UNIT-SAFE` | `TEXT` | Безопасная работа на установке: тезисы | Конспект по безопасной эксплуатации технологической установки | `10` |
| `MAT-OPS-UNIT-PDF` | `TOPIC-OPS-UNIT-SAFE` | `PDF` | Инструкция по безопасной эксплуатации | Material record для PDF-регламента по рабочему месту оператора | `20` |
| `MAT-OPS-UNIT-DOCX` | `TOPIC-OPS-UNIT-SAFE` | `DOCX` | Чек-лист осмотра перед запуском | Material record для документа предсменной проверки установки | `30` |
| `MAT-OPS-UNIT-VIDEO` | `TOPIC-OPS-UNIT-SAFE` | `VIDEO` | Видеоразбор безопасного запуска | Material record для ролика с последовательностью безопасных действий | `40` |
| `MAT-OPS-INCIDENT-TEXT` | `TOPIC-OPS-INCIDENT` | `TEXT` | Алгоритм действий при инциденте | Краткий текстовый материал по первичным действиям в нештатной ситуации | `10` |
| `MAT-OPS-INCIDENT-PDF` | `TOPIC-OPS-INCIDENT` | `PDF` | Схема аварийного реагирования | Material record для PDF-схемы эскалации и оповещения | `20` |
| `MAT-OPS-INCIDENT-DOCX` | `TOPIC-OPS-INCIDENT` | `DOCX` | Памятка по взаимодействию со сменой | Material record для документа по коммуникации и фиксации инцидента | `30` |
| `MAT-OPS-INCIDENT-VIDEO` | `TOPIC-OPS-INCIDENT` | `VIDEO` | Видеоразбор аварийного сценария | Material record для короткого разбора нештатной ситуации | `40` |
| `MAT-PERMIT-PREP-TEXT` | `TOPIC-PERMIT-PREP` | `TEXT` | Подготовка наряда: основные шаги | Последовательность действий перед выпуском наряда-допуска | `10` |
| `MAT-PERMIT-PREP-PDF` | `TOPIC-PERMIT-PREP` | `PDF` | Образец наряда-допуска | Material record для PDF-шаблона и примера заполнения | `20` |
| `MAT-PERMIT-PREP-DOCX` | `TOPIC-PERMIT-PREP` | `DOCX` | Памятка по согласованию наряда | Material record для рабочего документа с этапами согласования | `30` |
| `MAT-PERMIT-RISK-TEXT` | `TOPIC-PERMIT-RISK` | `TEXT` | Контроль рисков перед началом работ | Краткая методичка по оценке и фиксации рисков | `10` |
| `MAT-PERMIT-RISK-PDF` | `TOPIC-PERMIT-RISK` | `PDF` | Карта типовых рисков | Material record для PDF-карты опасностей и защитных мер | `20` |
| `MAT-PERMIT-RISK-VIDEO` | `TOPIC-PERMIT-RISK` | `VIDEO` | Видеоразбор предработного обхода | Material record для ролика по обходу места работ и проверке условий | `30` |
| `MAT-PERMIT-ROLES-TEXT` | `TOPIC-PERMIT-ROLES` | `TEXT` | Роли в нарядной системе | Описание зон ответственности руководителя и исполнителя | `10` |
| `MAT-PERMIT-ROLES-PDF` | `TOPIC-PERMIT-ROLES` | `PDF` | Таблица распределения ответственности | Material record для PDF с матрицей ролей и эскалаций | `20` |
| `MAT-PERMIT-ROLES-DOCX` | `TOPIC-PERMIT-ROLES` | `DOCX` | Памятка ответственному исполнителю | Material record для документа по практическим обязанностям исполнителя | `30` |
| `MAT-OPO-SELF-RULES-TEXT` | `TOPIC-OPO-SELF-RULES` | `TEXT` | Базовые требования промышленной безопасности на ОПО | Краткий конспект по ключевым обязанностям работника на опасном производственном объекте | `10` |
| `MAT-OPO-SELF-RULES-PDF` | `TOPIC-OPO-SELF-RULES` | `PDF` | Памятка по обязательным требованиям на ОПО | Material record для PDF-памятки по базовым запретам, маршрутам и эскалации | `20` |
| `MAT-OPO-SELF-RULES-VIDEO` | `TOPIC-OPO-SELF-RULES` | `VIDEO` | Видеоразбор безопасного поведения на ОПО | Material record для ролика с примерами корректного поведения работника на площадке | `30` |
| `MAT-OPO-SELF-INCIDENTS-TEXT` | `TOPIC-OPO-SELF-INCIDENTS` | `TEXT` | Типовые нарушения и ранние признаки угрозы аварии | Текстовый материал по распознаванию отклонений и первичным действиям работника | `10` |
| `MAT-OPO-SELF-INCIDENTS-PDF` | `TOPIC-OPO-SELF-INCIDENTS` | `PDF` | Памятка по действиям при угрозе аварии | Material record для PDF-памятки по оповещению, личной безопасности и эскалации | `20` |
| `MAT-OPO-SELF-INCIDENTS-VIDEO` | `TOPIC-OPO-SELF-INCIDENTS` | `VIDEO` | Разбор типовых нарушений на ОПО | Material record для ролика с примерами опасного поведения и корректной реакции | `30` |

## Tests

| Test code | Topic/course code | Test name | Test type | Contour | Role in scenario | Questions in test | Bank size | Final-control flag |
|---|---|---|---|---|---|---|---|---|
| `TEST-OPS-INTRO-FINAL` | `TOPIC-OPS-INTRO` | Итоговый контроль: вводный инструктаж | `CONTROL` | `ASSIGNED` | mandatory topic final | `8` | `16` | `YES` |
| `TEST-OPS-INTRO-SELF` | `TOPIC-OPS-INTRO` | Самопроверка: вводный инструктаж | `CONTROL` | `SELF` | self-visible supplementary topic test | `6` | `16` | `NO` |
| `TEST-OPS-UNIT-FINAL` | `TOPIC-OPS-UNIT-SAFE` | Итоговый контроль: безопасная работа на установке | `CONTROL` | `ASSIGNED` | mandatory topic final | `9` | `17` | `YES` |
| `TEST-OPS-INCIDENT-FINAL` | `TOPIC-OPS-INCIDENT` | Итоговый контроль: действия при инциденте | `CONTROL` | `ASSIGNED` | mandatory topic final | `8` | `15` | `YES` |
| `TEST-OPS-INCIDENT-SELF` | `TOPIC-OPS-INCIDENT` | Самопроверка: действия при инциденте | `CONTROL` | `SELF` | self-visible supplementary topic test | `6` | `15` | `NO` |
| `TEST-PERMIT-PREP-FINAL` | `TOPIC-PERMIT-PREP` | Итоговый контроль: подготовка наряда-допуска | `CONTROL` | `ASSIGNED` | mandatory topic final | `7` | `12` | `YES` |
| `TEST-PERMIT-PREP-TRAIN` | `TOPIC-PERMIT-PREP` | Тренировочный тест: подготовка наряда-допуска | `CONTROL` | `ASSIGNED` | assigned alternative training test | `6` | `12` | `NO` |
| `TEST-PERMIT-RISK-FINAL` | `TOPIC-PERMIT-RISK` | Итоговый контроль: контроль рисков | `CONTROL` | `ASSIGNED` | mandatory topic final | `8` | `13` | `YES` |
| `TEST-PERMIT-ROLES-FINAL` | `TOPIC-PERMIT-ROLES` | Итоговый контроль: роли руководителя и исполнителя | `CONTROL` | `ASSIGNED` | mandatory topic final | `7` | `12` | `YES` |
| `TEST-PERMIT-ROLES-TRAIN` | `TOPIC-PERMIT-ROLES` | Тренировочный тест: роли и ответственность | `CONTROL` | `ASSIGNED` | assigned alternative training test | `6` | `12` | `NO` |
| `TEST-OPO-SELF-RULES-SELF` | `TOPIC-OPO-SELF-RULES` | Самопроверка: базовые требования промышленной безопасности на ОПО | `CONTROL` | `SELF` | self-visible topic test | `6` | `10` | `NO` |
| `TEST-OPO-SELF-INCIDENTS-SELF` | `TOPIC-OPO-SELF-INCIDENTS` | Самопроверка: нарушения и действия при угрозе аварии | `CONTROL` | `SELF` | self-visible topic test | `6` | `10` | `NO` |
| `TEST-OPO-SELF-GENERAL` | `COURSE-OPO-SELF-REFRESHER` | Самопроверка: общий refresher по промышленной безопасности на ОПО | `CONTROL` | `SELF` | cross-topic self-visible course test | `8` | `20` | `NO` |

## Question Distribution

| Topic code | Total bank size | Single choice count | Multiple choice count | Final test size | Training/self test size | Notes |
|---|---|---|---|---|---|---|
| `TOPIC-OPS-INTRO` | `16` | `10` | `6` | `8` | `6` | Один банк используется для assigned final и отдельного self-visible test; тесты разводятся отдельными `test_question` наборами |
| `TOPIC-OPS-UNIT-SAFE` | `17` | `10` | `7` | `9` | `0` | Только mandatory contour, альтернативный набор может быть собран вручную позже |
| `TOPIC-OPS-INCIDENT` | `15` | `9` | `6` | `8` | `6` | Банк позволяет собрать final и self-visible тест без active-final overlap |
| `TOPIC-PERMIT-PREP` | `12` | `7` | `5` | `7` | `6` | Один final и один assigned training test из общего банка |
| `TOPIC-PERMIT-RISK` | `13` | `8` | `5` | `8` | `0` | Только final-control test для managerial mandatory contour |
| `TOPIC-PERMIT-ROLES` | `12` | `7` | `5` | `7` | `6` | Final и assigned training test собираются вручную из общего банка |
| `TOPIC-OPO-SELF-RULES` | `10` | `6` | `4` | `0` | `6` | Self-visible topic test плюс участие вопросов в `TEST-OPO-SELF-GENERAL`, без final-control semantics |
| `TOPIC-OPO-SELF-INCIDENTS` | `10` | `6` | `4` | `0` | `6` | Self-visible topic test плюс участие вопросов в `TEST-OPO-SELF-GENERAL`, без assignment linkage |

## Acceptance Notes

После создания контента должны быть проверяемы следующие признаки:

- созданы `3` курса и `8` тем с предсказуемыми стабильными кодами;
- создано `27` material records без расширения scope до file-storage или upload;
- создано `13` тестов, из них `6` active final control tests для mandatory assignment;
- self-visible tests отделены от `FINAL_TOPIC_CONTROL`;
- общий банк вопросов составляет `85` вопросов и превышает состав каждого отдельного теста;
- каждую тему можно проверить по материалам, по финальному тесту или по self/training тесту согласно contour;
- матрица достаточно конкретна, чтобы следующим шагом подготовить тексты materials и вопросный банк с кодами вида `Q-OPS-INTRO-001`.

SQL/API checkpoints здесь не детализируются и должны быть вынесены в:

- `demo-data/scenarios/acceptance-checkpoints.md` для численных SQL-проверок;
- будущий `demo-data/scenarios/role-runbook.md` для пошагового ручного прогона по актёрам.
