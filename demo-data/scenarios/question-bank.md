# Question Bank

Статус артефакта: `QUESTION_BANK_SOURCE_V1`

Этот файл:

- является source-банком demo-вопросов для ручного и API-наполнения;
- не является SQL migration;
- не создаёт данные сам по себе;
- не меняет production runtime.

## Summary

| Topic code | Planned question count | SINGLE_CHOICE count | MULTIPLE_CHOICE count | Final test size | Training/self test linkage |
|---|---:|---:|---:|---:|---|
| `TOPIC-OPS-INTRO` | `11` | `6` | `5` | `8` | `TEST-OPS-INTRO-FINAL`, `TEST-OPS-INTRO-SELF` |
| `TOPIC-OPS-UNIT-SAFE` | `11` | `6` | `5` | `9` | `TEST-OPS-UNIT-FINAL` |
| `TOPIC-OPS-INCIDENT` | `11` | `6` | `5` | `8` | `TEST-OPS-INCIDENT-FINAL`, `TEST-OPS-INCIDENT-SELF` |
| `TOPIC-PERMIT-PREP` | `10` | `7` | `3` | `7` | `TEST-PERMIT-PREP-FINAL`, `TEST-PERMIT-PREP-TRAIN` |
| `TOPIC-PERMIT-RISK` | `10` | `6` | `4` | `8` | `TEST-PERMIT-RISK-FINAL` |
| `TOPIC-PERMIT-ROLES` | `10` | `6` | `4` | `7` | `TEST-PERMIT-ROLES-FINAL`, `TEST-PERMIT-ROLES-TRAIN` |
| `TOPIC-OPO-SELF-RULES` | `11` | `7` | `4` | `0` | `TEST-OPO-SELF-RULES-SELF`, `TEST-OPO-SELF-GENERAL` |
| `TOPIC-OPO-SELF-INCIDENTS` | `11` | `7` | `4` | `0` | `TEST-OPO-SELF-INCIDENTS-SELF`, `TEST-OPO-SELF-GENERAL` |
| **Total** | **`85`** | **`51`** | **`34`** | - | - |

## Question Format Contract

Для каждого вопроса фиксируются поля:

- `Question code`: стабильный код вопроса, например `Q-OPS-INTRO-001`;
- `Topic code`: один из topic codes из `content-matrix.md`;
- `Question type`: только `SINGLE_CHOICE` или `MULTIPLE_CHOICE`;
- `Question text`: формулировка, пригодная для текущего content authoring runtime;
- `Options`: минимум 4 варианта ответа со stable option codes;
- `Correct option(s)`: ровно 1 correct option для `SINGLE_CHOICE`, минимум 2 correct options для `MULTIPLE_CHOICE`;
- `Explanation / analytics note`: краткое пояснение, почему ответ правильный или чем вопрос полезен для аналитики;
- `Intended tests`: один или несколько тестов из `content-matrix.md`, для которых вопрос подходит при ручной сборке `test_question`.

## Questions by Topic

## TOPIC-OPS-INTRO

### Q-OPS-INTRO-001

- Topic: `TOPIC-OPS-INTRO`
- Type: `SINGLE_CHOICE`
- Text: Какова основная цель вводного инструктажа по промышленной безопасности перед допуском работника к самостоятельной работе?
- Options:
  - `Q-OPS-INTRO-001-A`: Проверить скорость выполнения производственных операций
  - `Q-OPS-INTRO-001-B`: Дать работнику базовые требования безопасности и порядок действий на объекте
  - `Q-OPS-INTRO-001-C`: Заменить стажировку на рабочем месте
  - `Q-OPS-INTRO-001-D`: Назначить работнику постоянный маршрут обхода установки
- Correct:
  - `Q-OPS-INTRO-001-B`
- Explanation:
  Вводный инструктаж задаёт единый базовый уровень понимания опасностей, правил и эскалации.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`
  - `TEST-OPS-INTRO-SELF`

### Q-OPS-INTRO-002

- Topic: `TOPIC-OPS-INTRO`
- Type: `SINGLE_CHOICE`
- Text: Что работник должен сделать в первую очередь, если не понимает содержание выданной ему инструкции?
- Options:
  - `Q-OPS-INTRO-002-A`: Подписать ознакомление и уточнить позже у коллег
  - `Q-OPS-INTRO-002-B`: Самостоятельно выбрать удобный порядок действий
  - `Q-OPS-INTRO-002-C`: Обратиться за разъяснением к ответственному лицу до начала работы
  - `Q-OPS-INTRO-002-D`: Начать работу под наблюдением любого опытного сотрудника
- Correct:
  - `Q-OPS-INTRO-002-C`
- Explanation:
  Непонимание требований безопасности нельзя компенсировать догадками или неформальными советами.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`

### Q-OPS-INTRO-003

- Topic: `TOPIC-OPS-INTRO`
- Type: `SINGLE_CHOICE`
- Text: Какое действие допустимо при получении вводного инструктажа новым оператором?
- Options:
  - `Q-OPS-INTRO-003-A`: Пропустить раздел о нештатных ситуациях, если оператор уже работал на похожем объекте
  - `Q-OPS-INTRO-003-B`: Ограничиться просмотром видео без устного разъяснения правил
  - `Q-OPS-INTRO-003-C`: Уточнить маршруты эвакуации, точки сбора и порядок оповещения
  - `Q-OPS-INTRO-003-D`: Изучить только правила своей смены без общеплощадочных требований
- Correct:
  - `Q-OPS-INTRO-003-C`
- Explanation:
  Даже опытный сотрудник должен знать особенности именно этой площадки и её схемы реагирования.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`
  - `TEST-OPS-INTRO-SELF`

### Q-OPS-INTRO-004

- Topic: `TOPIC-OPS-INTRO`
- Type: `SINGLE_CHOICE`
- Text: Что означает требование использовать только актуальные локальные инструкции и памятки?
- Options:
  - `Q-OPS-INTRO-004-A`: Можно применять личные конспекты, если они удобнее
  - `Q-OPS-INTRO-004-B`: Нужно опираться на действующие утверждённые материалы, а не на устаревшие копии
  - `Q-OPS-INTRO-004-C`: Достаточно помнить требования по прошлому месту работы
  - `Q-OPS-INTRO-004-D`: Бумажные и электронные версии можно смешивать без проверки редакции
- Correct:
  - `Q-OPS-INTRO-004-B`
- Explanation:
  Вопрос полезен для аналитики: он выявляет привычку работать по неактуальным документам.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`

### Q-OPS-INTRO-005

- Topic: `TOPIC-OPS-INTRO`
- Type: `SINGLE_CHOICE`
- Text: Что из перечисленного лучше всего описывает безопасное поведение новичка на производственной площадке в первые смены?
- Options:
  - `Q-OPS-INTRO-005-A`: Самостоятельно ускорять операции, чтобы быстрее освоиться
  - `Q-OPS-INTRO-005-B`: Скрывать сомнения, чтобы не выглядеть неподготовленным
  - `Q-OPS-INTRO-005-C`: Действовать по установленным процедурам и задавать уточняющие вопросы при неопределённости
  - `Q-OPS-INTRO-005-D`: Ориентироваться прежде всего на советы коллег, а не на регламент
- Correct:
  - `Q-OPS-INTRO-005-C`
- Explanation:
  Безопасное поведение строится на следовании процедурам, а не на импровизации.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`
  - `TEST-OPS-INTRO-SELF`

### Q-OPS-INTRO-006

- Topic: `TOPIC-OPS-INTRO`
- Type: `SINGLE_CHOICE`
- Text: Какой признак говорит о том, что работник усвоил базовую логику вводного инструктажа?
- Options:
  - `Q-OPS-INTRO-006-A`: Он помнит только номера телефонов коллег своей смены
  - `Q-OPS-INTRO-006-B`: Он может объяснить, где искать требования, кому сообщать об отклонении и почему нельзя работать вне процедуры
  - `Q-OPS-INTRO-006-C`: Он быстро перемещается по объекту без сопровождения
  - `Q-OPS-INTRO-006-D`: Он знает только технологические показатели своей установки
- Correct:
  - `Q-OPS-INTRO-006-B`
- Explanation:
  Здесь проверяется не механическая память, а понимание базового контура безопасности.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`

### Q-OPS-INTRO-007

- Topic: `TOPIC-OPS-INTRO`
- Type: `MULTIPLE_CHOICE`
- Text: Какие сведения обязательно полезны работнику после вводного инструктажа?
- Options:
  - `Q-OPS-INTRO-007-A`: Порядок оповещения о происшествии
  - `Q-OPS-INTRO-007-B`: Расположение маршрутов эвакуации и точки сбора
  - `Q-OPS-INTRO-007-C`: Личные предпочтения начальника смены по форме отчёта
  - `Q-OPS-INTRO-007-D`: Базовые запреты на опасные действия вне утверждённых процедур
- Correct:
  - `Q-OPS-INTRO-007-A`
  - `Q-OPS-INTRO-007-B`
  - `Q-OPS-INTRO-007-D`
- Explanation:
  Пункты отражают практический минимум после вводного инструктажа.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`
  - `TEST-OPS-INTRO-SELF`

### Q-OPS-INTRO-008

- Topic: `TOPIC-OPS-INTRO`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия помогают избежать ошибок из-за самоуверенности после вводного инструктажа?
- Options:
  - `Q-OPS-INTRO-008-A`: Сверяться с действующей инструкцией перед редкой операцией
  - `Q-OPS-INTRO-008-B`: Уточнять спорные моменты до начала работы
  - `Q-OPS-INTRO-008-C`: Считать, что опыт на другом объекте полностью переносится на новый
  - `Q-OPS-INTRO-008-D`: Сообщать о замеченных отклонениях и небезопасных условиях
- Correct:
  - `Q-OPS-INTRO-008-A`
  - `Q-OPS-INTRO-008-B`
  - `Q-OPS-INTRO-008-D`
- Explanation:
  Аналитически полезный вопрос: выявляет склонность переоценивать переносимость прошлого опыта.
- Intended tests:
  - `TEST-OPS-INTRO-SELF`

### Q-OPS-INTRO-009

- Topic: `TOPIC-OPS-INTRO`
- Type: `MULTIPLE_CHOICE`
- Text: Какие ситуации требуют остановиться и запросить уточнение до начала действий?
- Options:
  - `Q-OPS-INTRO-009-A`: Инструкция противоречит фактическому состоянию рабочего места
  - `Q-OPS-INTRO-009-B`: На маршруте есть непонятное ограничение доступа
  - `Q-OPS-INTRO-009-C`: Задача простая и часто выполняется коллегами
  - `Q-OPS-INTRO-009-D`: Работник не уверен, кто является ответственным лицом по текущей операции
- Correct:
  - `Q-OPS-INTRO-009-A`
  - `Q-OPS-INTRO-009-B`
  - `Q-OPS-INTRO-009-D`
- Explanation:
  Неопределённость в условиях, ответственности и доступе нельзя игнорировать.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`

### Q-OPS-INTRO-010

- Topic: `TOPIC-OPS-INTRO`
- Type: `MULTIPLE_CHOICE`
- Text: Что относится к общеплощадочным правилам, которые важно знать уже на этапе вводного инструктажа?
- Options:
  - `Q-OPS-INTRO-010-A`: Порядок сообщения о травме, утечке или задымлении
  - `Q-OPS-INTRO-010-B`: Логика перемещения по опасным зонам и маршрутам
  - `Q-OPS-INTRO-010-C`: Индивидуальный стиль ведения сменного журнала коллегой
  - `Q-OPS-INTRO-010-D`: Требование не обходить блокировки и не действовать вне допуска
- Correct:
  - `Q-OPS-INTRO-010-A`
  - `Q-OPS-INTRO-010-B`
  - `Q-OPS-INTRO-010-D`
- Explanation:
  Вопрос закрывает базовые обязательные запреты и маршруты поведения на площадке.
- Intended tests:
  - `TEST-OPS-INTRO-FINAL`

### Q-OPS-INTRO-011

- Topic: `TOPIC-OPS-INTRO`
- Type: `MULTIPLE_CHOICE`
- Text: Какие признаки показывают, что работник подменяет требования безопасности бытовыми привычками?
- Options:
  - `Q-OPS-INTRO-011-A`: Он говорит, что «и так всё понятно» без обращения к инструкции
  - `Q-OPS-INTRO-011-B`: Он уточняет незнакомые сигналы и обозначения
  - `Q-OPS-INTRO-011-C`: Он ориентируется на «как обычно делают», а не на утверждённую процедуру
  - `Q-OPS-INTRO-011-D`: Он игнорирует расхождение между инструктажем и фактической обстановкой
- Correct:
  - `Q-OPS-INTRO-011-A`
  - `Q-OPS-INTRO-011-C`
  - `Q-OPS-INTRO-011-D`
- Explanation:
  Пограничный вопрос на культуру безопасности и склонность к неформальным практикам.
- Intended tests:
  - `TEST-OPS-INTRO-SELF`

## TOPIC-OPS-UNIT-SAFE

### Q-OPS-UNIT-SAFE-001

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `SINGLE_CHOICE`
- Text: Что оператор должен сделать перед началом стандартного обхода технологической установки?
- Options:
  - `Q-OPS-UNIT-SAFE-001-A`: Сначала отключить местную сигнализацию, чтобы она не мешала
  - `Q-OPS-UNIT-SAFE-001-B`: Убедиться, что маршрут, средства защиты и условия обхода соответствуют требованиям
  - `Q-OPS-UNIT-SAFE-001-C`: Ограничиться визуальной оценкой из операторной
  - `Q-OPS-UNIT-SAFE-001-D`: Попросить коллегу подписать обход заранее
- Correct:
  - `Q-OPS-UNIT-SAFE-001-B`
- Explanation:
  Безопасный обход начинается с проверки условий и собственной готовности.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-002

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `SINGLE_CHOICE`
- Text: Как правильно действовать, если на площадке установки обнаружено новое препятствие на маршруте обхода?
- Options:
  - `Q-OPS-UNIT-SAFE-002-A`: Обойти препятствие любым удобным путём
  - `Q-OPS-UNIT-SAFE-002-B`: Игнорировать, если обход занимает мало времени
  - `Q-OPS-UNIT-SAFE-002-C`: Оценить риск, сообщить ответственному и действовать по безопасному маршруту
  - `Q-OPS-UNIT-SAFE-002-D`: Продолжить обход только при отключении видеонаблюдения
- Correct:
  - `Q-OPS-UNIT-SAFE-002-C`
- Explanation:
  Самовольное изменение маршрута без оценки условий создаёт дополнительный риск.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-003

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `SINGLE_CHOICE`
- Text: Что наиболее опасно при выполнении привычной операции на установке?
- Options:
  - `Q-OPS-UNIT-SAFE-003-A`: Излишне медленное заполнение журнала
  - `Q-OPS-UNIT-SAFE-003-B`: Потеря внимания из-за эффекта «рутинной» задачи
  - `Q-OPS-UNIT-SAFE-003-C`: Слишком частая сверка с инструкцией
  - `Q-OPS-UNIT-SAFE-003-D`: Запрос подтверждения у старшего смены
- Correct:
  - `Q-OPS-UNIT-SAFE-003-B`
- Explanation:
  Вопрос помогает диагностировать риск автоматизма при рутинных операциях.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-004

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `SINGLE_CHOICE`
- Text: Что должен сделать оператор при обнаружении нехарактерного шума от оборудования?
- Options:
  - `Q-OPS-UNIT-SAFE-004-A`: Отложить сообщение до конца смены
  - `Q-OPS-UNIT-SAFE-004-B`: Самостоятельно разобрать узел без согласования
  - `Q-OPS-UNIT-SAFE-004-C`: Зафиксировать отклонение и сообщить по установленному порядку
  - `Q-OPS-UNIT-SAFE-004-D`: Сравнить шум с соседней установкой и ничего не делать
- Correct:
  - `Q-OPS-UNIT-SAFE-004-C`
- Explanation:
  Любое отклонение от нормального режима требует фиксации и эскалации.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-005

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `SINGLE_CHOICE`
- Text: Какой подход к показаниям приборов наиболее безопасен?
- Options:
  - `Q-OPS-UNIT-SAFE-005-A`: Ориентироваться только на тренд, не сверяя локальные условия
  - `Q-OPS-UNIT-SAFE-005-B`: Сопоставлять показания с режимом процесса и фактической обстановкой
  - `Q-OPS-UNIT-SAFE-005-C`: Считать любой стабильный показатель безопасным
  - `Q-OPS-UNIT-SAFE-005-D`: Полагаться на устную оценку предыдущей смены
- Correct:
  - `Q-OPS-UNIT-SAFE-005-B`
- Explanation:
  Надёжная оценка строится на сопоставлении данных, а не на одном источнике.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-006

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `SINGLE_CHOICE`
- Text: Как лучше всего описать безопасный запуск действий после кратковременного отвлечения оператора?
- Options:
  - `Q-OPS-UNIT-SAFE-006-A`: Продолжить с того места, которое кажется очевидным
  - `Q-OPS-UNIT-SAFE-006-B`: Вернуться к контрольной точке процедуры и перепроверить состояние
  - `Q-OPS-UNIT-SAFE-006-C`: Довериться памяти, чтобы не терять время
  - `Q-OPS-UNIT-SAFE-006-D`: Попросить коллегу завершить операцию без объяснений
- Correct:
  - `Q-OPS-UNIT-SAFE-006-B`
- Explanation:
  Возврат к контрольной точке снижает риск пропуска шага или неверного продолжения.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-007

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `MULTIPLE_CHOICE`
- Text: Какие признаки во время обхода установки требуют повышенного внимания и сообщения по установленному порядку?
- Options:
  - `Q-OPS-UNIT-SAFE-007-A`: Нехарактерный запах продукта
  - `Q-OPS-UNIT-SAFE-007-B`: Следы подтёка на оборудовании
  - `Q-OPS-UNIT-SAFE-007-C`: Привычное расположение инструмента на верстаке
  - `Q-OPS-UNIT-SAFE-007-D`: Необычная вибрация или шум
- Correct:
  - `Q-OPS-UNIT-SAFE-007-A`
  - `Q-OPS-UNIT-SAFE-007-B`
  - `Q-OPS-UNIT-SAFE-007-D`
- Explanation:
  Варианты отражают типичные ранние признаки отклонения оборудования.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-008

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия помогают снизить риск ошибки при переключениях на установке?
- Options:
  - `Q-OPS-UNIT-SAFE-008-A`: Выполнять действия по шагам утверждённой процедуры
  - `Q-OPS-UNIT-SAFE-008-B`: Подтверждать критические шаги по контрольным признакам
  - `Q-OPS-UNIT-SAFE-008-C`: Сокращать формальные проверки ради экономии времени
  - `Q-OPS-UNIT-SAFE-008-D`: Прекращать операцию при несоответствии фактического состояния ожидаемому
- Correct:
  - `Q-OPS-UNIT-SAFE-008-A`
  - `Q-OPS-UNIT-SAFE-008-B`
  - `Q-OPS-UNIT-SAFE-008-D`
- Explanation:
  Это вопрос про дисциплину переключений, а не про знание отдельного шага.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-009

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `MULTIPLE_CHOICE`
- Text: Что относится к безопасной организации предсменного осмотра?
- Options:
  - `Q-OPS-UNIT-SAFE-009-A`: Проверка доступности маршрута и видимых опасностей
  - `Q-OPS-UNIT-SAFE-009-B`: Сверка заметных отклонений с предыдущей сменой и журналом
  - `Q-OPS-UNIT-SAFE-009-C`: Отказ от осмотра, если оборудование работало стабильно ночью
  - `Q-OPS-UNIT-SAFE-009-D`: Проверка исправности необходимых средств защиты
- Correct:
  - `Q-OPS-UNIT-SAFE-009-A`
  - `Q-OPS-UNIT-SAFE-009-B`
  - `Q-OPS-UNIT-SAFE-009-D`
- Explanation:
  Вопрос полезен для аналитики ошибок, связанных с формальным предсменным обходом.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-010

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия недопустимы при работе на технологической установке?
- Options:
  - `Q-OPS-UNIT-SAFE-010-A`: Игнорировать отклонение только потому, что оно уже встречалось раньше
  - `Q-OPS-UNIT-SAFE-010-B`: Остановиться и уточнить порядок действий при расхождении с процедурой
  - `Q-OPS-UNIT-SAFE-010-C`: Самовольно обходить технические ограничения ради ускорения операции
  - `Q-OPS-UNIT-SAFE-010-D`: Сообщать о дефекте оборудования сменному руководителю
- Correct:
  - `Q-OPS-UNIT-SAFE-010-A`
  - `Q-OPS-UNIT-SAFE-010-C`
- Explanation:
  Здесь важны именно небезопасные поведенческие паттерны.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

### Q-OPS-UNIT-SAFE-011

- Topic: `TOPIC-OPS-UNIT-SAFE`
- Type: `MULTIPLE_CHOICE`
- Text: Какие признаки говорят, что оператор слишком полагается на привычку, а не на контроль режима?
- Options:
  - `Q-OPS-UNIT-SAFE-011-A`: Он пропускает контрольные сверки на знакомой операции
  - `Q-OPS-UNIT-SAFE-011-B`: Он фиксирует и сравнивает аномальные сигналы
  - `Q-OPS-UNIT-SAFE-011-C`: Он считает безопасным продолжать работу при непонятном отклонении
  - `Q-OPS-UNIT-SAFE-011-D`: Он подтверждает шаги по процедуре после отвлечения
- Correct:
  - `Q-OPS-UNIT-SAFE-011-A`
  - `Q-OPS-UNIT-SAFE-011-C`
- Explanation:
  Пограничный вопрос для отлова рутинной самоуверенности на установке.
- Intended tests:
  - `TEST-OPS-UNIT-FINAL`

## TOPIC-OPS-INCIDENT

### Q-OPS-INCIDENT-001

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `SINGLE_CHOICE`
- Text: Какое первое действие наиболее правильно при обнаружении признаков нештатной ситуации на установке?
- Options:
  - `Q-OPS-INCIDENT-001-A`: Немедленно продолжить технологическую операцию, чтобы завершить её быстрее
  - `Q-OPS-INCIDENT-001-B`: Игнорировать, если нет открытого пламени
  - `Q-OPS-INCIDENT-001-C`: Действовать по утверждённому алгоритму оповещения и обеспечения личной безопасности
  - `Q-OPS-INCIDENT-001-D`: Сначала собрать мнение коллег, а потом решать
- Correct:
  - `Q-OPS-INCIDENT-001-C`
- Explanation:
  В нештатной ситуации важна не импровизация, а быстрый переход к штатному алгоритму реагирования.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`
  - `TEST-OPS-INCIDENT-SELF`

### Q-OPS-INCIDENT-002

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `SINGLE_CHOICE`
- Text: Что недопустимо при первых действиях в аварийной или предаварийной ситуации?
- Options:
  - `Q-OPS-INCIDENT-002-A`: Ставить личную безопасность и оповещение выше попыток «героического» устранения
  - `Q-OPS-INCIDENT-002-B`: Самостоятельно идти в опасную зону без необходимости и без средств защиты
  - `Q-OPS-INCIDENT-002-C`: Использовать предусмотренные каналы связи
  - `Q-OPS-INCIDENT-002-D`: Выполнять указания аварийного алгоритма
- Correct:
  - `Q-OPS-INCIDENT-002-B`
- Explanation:
  Вопрос отделяет безопасную реакцию от опасного импульсивного поведения.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`

### Q-OPS-INCIDENT-003

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `SINGLE_CHOICE`
- Text: Почему важно фиксировать факт инцидента и ключевые наблюдения сразу после стабилизации ситуации?
- Options:
  - `Q-OPS-INCIDENT-003-A`: Чтобы заменить официальное расследование личными заметками
  - `Q-OPS-INCIDENT-003-B`: Чтобы сохранить данные для последующего разбора и предотвращения повторения
  - `Q-OPS-INCIDENT-003-C`: Чтобы избежать общения с руководителем
  - `Q-OPS-INCIDENT-003-D`: Чтобы ускорить закрытие смены
- Correct:
  - `Q-OPS-INCIDENT-003-B`
- Explanation:
  Фиксация наблюдений помогает расследованию и улучшению защитных мер.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`

### Q-OPS-INCIDENT-004

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `SINGLE_CHOICE`
- Text: Как правильно трактовать отсутствие визуально заметного пожара при наличии запаха продукта и сигнала отклонения?
- Options:
  - `Q-OPS-INCIDENT-004-A`: Как признак того, что ситуация неопасна
  - `Q-OPS-INCIDENT-004-B`: Как основание подождать до следующего обхода
  - `Q-OPS-INCIDENT-004-C`: Как возможный ранний признак инцидента, требующий реакции по процедуре
  - `Q-OPS-INCIDENT-004-D`: Как обычное сезонное явление
- Correct:
  - `Q-OPS-INCIDENT-004-C`
- Explanation:
  Аналитически полезный вопрос: показывает, кто склонен недооценивать ранние признаки инцидента.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`
  - `TEST-OPS-INCIDENT-SELF`

### Q-OPS-INCIDENT-005

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `SINGLE_CHOICE`
- Text: Что должен понимать оператор о своей роли при эскалации нештатной ситуации?
- Options:
  - `Q-OPS-INCIDENT-005-A`: Его задача только наблюдать и не передавать информацию
  - `Q-OPS-INCIDENT-005-B`: Он должен действовать в пределах полномочий, своевременно сообщая и не выходя за рамки процедуры
  - `Q-OPS-INCIDENT-005-C`: Он обязан лично устранить любую причину аварии
  - `Q-OPS-INCIDENT-005-D`: Он может отложить сообщение, если сам оценивает риск как низкий
- Correct:
  - `Q-OPS-INCIDENT-005-B`
- Explanation:
  Правильная эскалация строится на своевременной передаче информации и соблюдении границ роли.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`

### Q-OPS-INCIDENT-006

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `SINGLE_CHOICE`
- Text: Какое поведение после инцидента наиболее полезно для культуры безопасности?
- Options:
  - `Q-OPS-INCIDENT-006-A`: Скрыть мелкое отклонение, если последствия быстро устранены
  - `Q-OPS-INCIDENT-006-B`: Зафиксировать уроки и сообщить об обстоятельствах без искажения фактов
  - `Q-OPS-INCIDENT-006-C`: Считать инцидент завершённым сразу после восстановления режима
  - `Q-OPS-INCIDENT-006-D`: Передавать только устный пересказ без регистрации
- Correct:
  - `Q-OPS-INCIDENT-006-B`
- Explanation:
  Вопрос проверяет отношение к постинцидентному анализу, а не только к аварийному действию.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`

### Q-OPS-INCIDENT-007

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия обычно входят в безопасную первичную реакцию на инцидент?
- Options:
  - `Q-OPS-INCIDENT-007-A`: Оповещение по установленному каналу
  - `Q-OPS-INCIDENT-007-B`: Оценка личной безопасности и удаление из опасной зоны при необходимости
  - `Q-OPS-INCIDENT-007-C`: Самовольное выполнение нестандартных действий без координации
  - `Q-OPS-INCIDENT-007-D`: Передача фактической информации о месте и признаках отклонения
- Correct:
  - `Q-OPS-INCIDENT-007-A`
  - `Q-OPS-INCIDENT-007-B`
  - `Q-OPS-INCIDENT-007-D`
- Explanation:
  Это базовый набор правильных действий в первые минуты.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`
  - `TEST-OPS-INCIDENT-SELF`

### Q-OPS-INCIDENT-008

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `MULTIPLE_CHOICE`
- Text: Какие признаки могут указывать на развитие нештатной ситуации даже без явной аварии?
- Options:
  - `Q-OPS-INCIDENT-008-A`: Нехарактерный запах
  - `Q-OPS-INCIDENT-008-B`: Сигнализация отклонения и необычное поведение оборудования
  - `Q-OPS-INCIDENT-008-C`: Плановое оформление сменного задания
  - `Q-OPS-INCIDENT-008-D`: Подтёк, задымление или странный шум
- Correct:
  - `Q-OPS-INCIDENT-008-A`
  - `Q-OPS-INCIDENT-008-B`
  - `Q-OPS-INCIDENT-008-D`
- Explanation:
  Раннее распознавание событий часто определяет тяжесть последствий.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`

### Q-OPS-INCIDENT-009

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `MULTIPLE_CHOICE`
- Text: Какие ошибки особенно опасны при сообщении об инциденте?
- Options:
  - `Q-OPS-INCIDENT-009-A`: Передавать неточное место и расплывчатое описание
  - `Q-OPS-INCIDENT-009-B`: Замалчивать признаки, которые кажутся «незначительными»
  - `Q-OPS-INCIDENT-009-C`: Сообщать только проверенные факты и наблюдения
  - `Q-OPS-INCIDENT-009-D`: Задерживать сообщение ради самостоятельной перепроверки
- Correct:
  - `Q-OPS-INCIDENT-009-A`
  - `Q-OPS-INCIDENT-009-B`
  - `Q-OPS-INCIDENT-009-D`
- Explanation:
  Вопрос помогает отличить фактическую коммуникацию от запоздалой и размытой.
- Intended tests:
  - `TEST-OPS-INCIDENT-SELF`

### Q-OPS-INCIDENT-010

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `MULTIPLE_CHOICE`
- Text: Что относится к качественной постинцидентной фиксации?
- Options:
  - `Q-OPS-INCIDENT-010-A`: Время обнаружения и ключевые признаки
  - `Q-OPS-INCIDENT-010-B`: Какие действия были предприняты и кем
  - `Q-OPS-INCIDENT-010-C`: Эмоциональные оценки без фактов
  - `Q-OPS-INCIDENT-010-D`: Что требовало уточнения или вызвало затруднение
- Correct:
  - `Q-OPS-INCIDENT-010-A`
  - `Q-OPS-INCIDENT-010-B`
  - `Q-OPS-INCIDENT-010-D`
- Explanation:
  Фактология и трудности важнее эмоционального пересказа.
- Intended tests:
  - `TEST-OPS-INCIDENT-FINAL`
  - `TEST-OPS-INCIDENT-SELF`

### Q-OPS-INCIDENT-011

- Topic: `TOPIC-OPS-INCIDENT`
- Type: `MULTIPLE_CHOICE`
- Text: Какие ответы демонстрируют опасную недооценку развивающегося инцидента?
- Options:
  - `Q-OPS-INCIDENT-011-A`: «Подожду, вдруг сигнал сам исчезнет»
  - `Q-OPS-INCIDENT-011-B`: «Сначала сообщу и сверюсь с алгоритмом»
  - `Q-OPS-INCIDENT-011-C`: «Если нет открытого огня, можно не торопиться»
  - `Q-OPS-INCIDENT-011-D`: «Фиксирую признаки и передаю их точно»
- Correct:
  - `Q-OPS-INCIDENT-011-A`
  - `Q-OPS-INCIDENT-011-C`
- Explanation:
  Пограничный вопрос на распознавание ложного ощущения контроля над ситуацией.
- Intended tests:
  - `TEST-OPS-INCIDENT-SELF`

## TOPIC-PERMIT-PREP

### Q-PERMIT-PREP-001

- Topic: `TOPIC-PERMIT-PREP`
- Type: `SINGLE_CHOICE`
- Text: Какова главная задача подготовки наряда-допуска перед началом работ?
- Options:
  - `Q-PERMIT-PREP-001-A`: Только зафиксировать фамилии исполнителей
  - `Q-PERMIT-PREP-001-B`: Формально подтвердить, что работа существует в плане
  - `Q-PERMIT-PREP-001-C`: Согласовать условия безопасного выполнения конкретной работы и требуемые меры защиты
  - `Q-PERMIT-PREP-001-D`: Заменить все производственные инструкции одним документом
- Correct:
  - `Q-PERMIT-PREP-001-C`
- Explanation:
  Наряд-допуск фиксирует не только факт работы, но и условия её безопасного выполнения.
- Intended tests:
  - `TEST-PERMIT-PREP-FINAL`
  - `TEST-PERMIT-PREP-TRAIN`

### Q-PERMIT-PREP-002

- Topic: `TOPIC-PERMIT-PREP`
- Type: `SINGLE_CHOICE`
- Text: Когда наряд-допуск нельзя считать качественно подготовленным?
- Options:
  - `Q-PERMIT-PREP-002-A`: Когда в нём ясно определены место, объём и меры безопасности
  - `Q-PERMIT-PREP-002-B`: Когда условия работы описаны расплывчато и допускают разные трактовки
  - `Q-PERMIT-PREP-002-C`: Когда в нём указаны ответственные лица
  - `Q-PERMIT-PREP-002-D`: Когда он используется для конкретной работы
- Correct:
  - `Q-PERMIT-PREP-002-B`
- Explanation:
  Размытая формулировка разрушает проверяемость и контроль исполнения.
- Intended tests:
  - `TEST-PERMIT-PREP-FINAL`

### Q-PERMIT-PREP-003

- Topic: `TOPIC-PERMIT-PREP`
- Type: `SINGLE_CHOICE`
- Text: Что важнее всего при описании места выполнения работ в наряде-допуске?
- Options:
  - `Q-PERMIT-PREP-003-A`: Использовать привычные сокращения без пояснений
  - `Q-PERMIT-PREP-003-B`: Указать место так, чтобы исключить двусмысленность для исполнителей и допускающих
  - `Q-PERMIT-PREP-003-C`: Сослаться только на устную договорённость с мастером
  - `Q-PERMIT-PREP-003-D`: Ограничиться номером подразделения без объекта
- Correct:
  - `Q-PERMIT-PREP-003-B`
- Explanation:
  Точное место работ важно для контроля границ и условий допуска.
- Intended tests:
  - `TEST-PERMIT-PREP-FINAL`

### Q-PERMIT-PREP-004

- Topic: `TOPIC-PERMIT-PREP`
- Type: `SINGLE_CHOICE`
- Text: Как следует поступить, если в процессе подготовки выяснилось, что условия на месте работ отличаются от ожидаемых?
- Options:
  - `Q-PERMIT-PREP-004-A`: Оставить наряд как есть и предупредить устно
  - `Q-PERMIT-PREP-004-B`: Приостановить подготовку, уточнить условия и скорректировать меры до выдачи допуска
  - `Q-PERMIT-PREP-004-C`: Сохранить прежние меры, если работа срочная
  - `Q-PERMIT-PREP-004-D`: Передать решение любому исполнителю бригады
- Correct:
  - `Q-PERMIT-PREP-004-B`
- Explanation:
  Наряд должен отражать реальные условия, а не исходные предположения.
- Intended tests:
  - `TEST-PERMIT-PREP-FINAL`
  - `TEST-PERMIT-PREP-TRAIN`

### Q-PERMIT-PREP-005

- Topic: `TOPIC-PERMIT-PREP`
- Type: `SINGLE_CHOICE`
- Text: Зачем в наряде-допуске фиксируют конкретные меры подготовки рабочего места?
- Options:
  - `Q-PERMIT-PREP-005-A`: Чтобы не проверять их фактическое выполнение
  - `Q-PERMIT-PREP-005-B`: Чтобы связать допуск с измеримыми условиями безопасности
  - `Q-PERMIT-PREP-005-C`: Только ради архивного хранения
  - `Q-PERMIT-PREP-005-D`: Чтобы упростить оформление без участия руководителя
- Correct:
  - `Q-PERMIT-PREP-005-B`
- Explanation:
  Документ должен опираться на меры, которые можно проверить перед началом работы.
- Intended tests:
  - `TEST-PERMIT-PREP-FINAL`

### Q-PERMIT-PREP-006

- Topic: `TOPIC-PERMIT-PREP`
- Type: `SINGLE_CHOICE`
- Text: Какой подход к подготовке наряда-допуска является наиболее безопасным?
- Options:
  - `Q-PERMIT-PREP-006-A`: Копировать прошлый наряд без проверки текущих условий
  - `Q-PERMIT-PREP-006-B`: Подгонять описание под уже согласованное решение
  - `Q-PERMIT-PREP-006-C`: Формировать наряд под фактическую работу, место и реальные риски текущего дня
  - `Q-PERMIT-PREP-006-D`: Сначала допускать к работам, а детали уточнять в ходе выполнения
- Correct:
  - `Q-PERMIT-PREP-006-C`
- Explanation:
  Вопрос отсеивает формальное копирование старых шаблонов.
- Intended tests:
  - `TEST-PERMIT-PREP-FINAL`

### Q-PERMIT-PREP-007

- Topic: `TOPIC-PERMIT-PREP`
- Type: `SINGLE_CHOICE`
- Text: Какой признак лучше всего показывает, что наряд подготовлен формально, а не по существу?
- Options:
  - `Q-PERMIT-PREP-007-A`: В нём перечислены исполнители и условия допуска
  - `Q-PERMIT-PREP-007-B`: В нём учтены фактические границы работ
  - `Q-PERMIT-PREP-007-C`: Текст выглядит универсально и почти не связан с конкретной задачей
  - `Q-PERMIT-PREP-007-D`: Меры безопасности можно проверить на месте
- Correct:
  - `Q-PERMIT-PREP-007-C`
- Explanation:
  Пограничный вопрос на выявление шаблонного, оторванного от реальности оформления.
- Intended tests:
  - `TEST-PERMIT-PREP-TRAIN`

### Q-PERMIT-PREP-008

- Topic: `TOPIC-PERMIT-PREP`
- Type: `MULTIPLE_CHOICE`
- Text: Какие элементы обязательно полезны при подготовке наряда-допуска?
- Options:
  - `Q-PERMIT-PREP-008-A`: Чёткое описание места и объёма работ
  - `Q-PERMIT-PREP-008-B`: Конкретные меры подготовки рабочего места
  - `Q-PERMIT-PREP-008-C`: Неформальная устная договорённость вместо записи
  - `Q-PERMIT-PREP-008-D`: Определённые ответственные лица
- Correct:
  - `Q-PERMIT-PREP-008-A`
  - `Q-PERMIT-PREP-008-B`
  - `Q-PERMIT-PREP-008-D`
- Explanation:
  Это базовая структура качественно подготовленного допуска.
- Intended tests:
  - `TEST-PERMIT-PREP-FINAL`

### Q-PERMIT-PREP-009

- Topic: `TOPIC-PERMIT-PREP`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия повышают качество согласования наряда-допуска?
- Options:
  - `Q-PERMIT-PREP-009-A`: Сверка документа с фактическими условиями на месте
  - `Q-PERMIT-PREP-009-B`: Уточнение границ ответственности до начала работ
  - `Q-PERMIT-PREP-009-C`: Подмена конкретных мер общими фразами
  - `Q-PERMIT-PREP-009-D`: Проверка, что перечисленные меры можно реально выполнить и проконтролировать
- Correct:
  - `Q-PERMIT-PREP-009-A`
  - `Q-PERMIT-PREP-009-B`
  - `Q-PERMIT-PREP-009-D`
- Explanation:
  Вопрос подходит и для финального, и для тренировочного теста.
- Intended tests:
  - `TEST-PERMIT-PREP-FINAL`
  - `TEST-PERMIT-PREP-TRAIN`

### Q-PERMIT-PREP-010

- Topic: `TOPIC-PERMIT-PREP`
- Type: `MULTIPLE_CHOICE`
- Text: Какие ответы указывают на опасный формализм при подготовке наряда-допуска?
- Options:
  - `Q-PERMIT-PREP-010-A`: «Возьмём прошлый наряд, здесь почти то же самое»
  - `Q-PERMIT-PREP-010-B`: «Проверим, совпадают ли реальные условия с указанными»
  - `Q-PERMIT-PREP-010-C`: «Детали потом уточним устно на месте»
  - `Q-PERMIT-PREP-010-D`: «Сначала определим конкретные меры и ответственных»
- Correct:
  - `Q-PERMIT-PREP-010-A`
  - `Q-PERMIT-PREP-010-C`
- Explanation:
  Аналитический вопрос на склонность подменять допуск шаблоном и устными договорённостями.
- Intended tests:
  - `TEST-PERMIT-PREP-TRAIN`

## TOPIC-PERMIT-RISK

### Q-PERMIT-RISK-001

- Topic: `TOPIC-PERMIT-RISK`
- Type: `SINGLE_CHOICE`
- Text: Что является главной целью контроля рисков перед началом работ?
- Options:
  - `Q-PERMIT-RISK-001-A`: Сократить количество подписей в наряде
  - `Q-PERMIT-RISK-001-B`: Выявить опасности, оценить условия и подтвердить адекватность защитных мер
  - `Q-PERMIT-RISK-001-C`: Передать всю ответственность исполнителю
  - `Q-PERMIT-RISK-001-D`: Ускорить начало работ без дополнительного осмотра
- Correct:
  - `Q-PERMIT-RISK-001-B`
- Explanation:
  Контроль рисков нужен для сопоставления опасностей и фактических барьеров защиты.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-002

- Topic: `TOPIC-PERMIT-RISK`
- Type: `SINGLE_CHOICE`
- Text: Когда предрабочий осмотр площадки особенно критичен?
- Options:
  - `Q-PERMIT-RISK-002-A`: Только для новых работников
  - `Q-PERMIT-RISK-002-B`: Когда работа уже часто выполнялась в прошлом
  - `Q-PERMIT-RISK-002-C`: Когда фактические условия могут отличаться от ожидаемых или меняться во времени
  - `Q-PERMIT-RISK-002-D`: Только после завершения работ
- Correct:
  - `Q-PERMIT-RISK-002-C`
- Explanation:
  Риск-контроль должен учитывать изменчивость реальных условий на месте.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-003

- Topic: `TOPIC-PERMIT-RISK`
- Type: `SINGLE_CHOICE`
- Text: Что означает недостаточно качественная оценка риска перед началом работ?
- Options:
  - `Q-PERMIT-RISK-003-A`: Все опасности перечислены общими словами без связи с мерами защиты
  - `Q-PERMIT-RISK-003-B`: Риски соотнесены с конкретными условиями и барьерами
  - `Q-PERMIT-RISK-003-C`: Осмотр выполнен на месте работ
  - `Q-PERMIT-RISK-003-D`: Ответственные знают свои функции
- Correct:
  - `Q-PERMIT-RISK-003-A`
- Explanation:
  Перечень без связи с реальными барьерами мало помогает в управлении риском.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-004

- Topic: `TOPIC-PERMIT-RISK`
- Type: `SINGLE_CHOICE`
- Text: Какое действие наиболее уместно, если на месте работ обнаружен дополнительный источник опасности, не учтённый ранее?
- Options:
  - `Q-PERMIT-RISK-004-A`: Продолжить, если работа срочная
  - `Q-PERMIT-RISK-004-B`: Приостановить начало и пересмотреть меры контроля риска
  - `Q-PERMIT-RISK-004-C`: Ограничиться устным предупреждением бригады
  - `Q-PERMIT-RISK-004-D`: Скрыть новую информацию до завершения работ
- Correct:
  - `Q-PERMIT-RISK-004-B`
- Explanation:
  Новая опасность требует обновления условий допуска, а не устного компромисса.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-005

- Topic: `TOPIC-PERMIT-RISK`
- Type: `SINGLE_CHOICE`
- Text: Что лучше всего показывает зрелый подход к контролю рисков?
- Options:
  - `Q-PERMIT-RISK-005-A`: Формальная отметка без обхода места работ
  - `Q-PERMIT-RISK-005-B`: Ориентация на фразу «так делали раньше»
  - `Q-PERMIT-RISK-005-C`: Проверка того, что барьеры защиты действительно существуют и работают
  - `Q-PERMIT-RISK-005-D`: Передача оценки риска только исполнителю
- Correct:
  - `Q-PERMIT-RISK-005-C`
- Explanation:
  Важна проверка действенности барьеров, а не только запись о них.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-006

- Topic: `TOPIC-PERMIT-RISK`
- Type: `SINGLE_CHOICE`
- Text: Что делает вопрос по рискам аналитически полезным для разборов после инцидентов?
- Options:
  - `Q-PERMIT-RISK-006-A`: Он показывает, склонен ли человек путать перечень опасностей с реальной оценкой барьеров
  - `Q-PERMIT-RISK-006-B`: Он проверяет только скорость чтения документа
  - `Q-PERMIT-RISK-006-C`: Он исключает необходимость осмотра площадки
  - `Q-PERMIT-RISK-006-D`: Он не связан с качеством допуска
- Correct:
  - `Q-PERMIT-RISK-006-A`
- Explanation:
  Пограничный вопрос помогает выявить формальный, а не содержательный риск-контроль.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-007

- Topic: `TOPIC-PERMIT-RISK`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия усиливают контроль рисков перед началом работ?
- Options:
  - `Q-PERMIT-RISK-007-A`: Осмотр места работ до допуска
  - `Q-PERMIT-RISK-007-B`: Проверка наличия и применимости защитных мер
  - `Q-PERMIT-RISK-007-C`: Ориентация только на прошлый опыт без проверки текущих условий
  - `Q-PERMIT-RISK-007-D`: Уточнение изменений в обстановке с момента подготовки наряда
- Correct:
  - `Q-PERMIT-RISK-007-A`
  - `Q-PERMIT-RISK-007-B`
  - `Q-PERMIT-RISK-007-D`
- Explanation:
  Это вопрос на системную предрабочую проверку, а не на декларативное знание.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-008

- Topic: `TOPIC-PERMIT-RISK`
- Type: `MULTIPLE_CHOICE`
- Text: Какие признаки говорят, что оценка риска проведена поверхностно?
- Options:
  - `Q-PERMIT-RISK-008-A`: Указаны общие опасности без связи с текущим местом работ
  - `Q-PERMIT-RISK-008-B`: Не проверено фактическое наличие барьеров
  - `Q-PERMIT-RISK-008-C`: Меры безопасности соотнесены с конкретными опасностями
  - `Q-PERMIT-RISK-008-D`: Изменившиеся условия не отражены в допуске
- Correct:
  - `Q-PERMIT-RISK-008-A`
  - `Q-PERMIT-RISK-008-B`
  - `Q-PERMIT-RISK-008-D`
- Explanation:
  Вопрос различает номинальную и реальную оценку риска.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-009

- Topic: `TOPIC-PERMIT-RISK`
- Type: `MULTIPLE_CHOICE`
- Text: Какие ответы отражают правильное отношение к изменяющейся обстановке на месте работ?
- Options:
  - `Q-PERMIT-RISK-009-A`: Пересматривать решение о допуске при изменении условий
  - `Q-PERMIT-RISK-009-B`: Считать исходную оценку достаточной на весь день без повторной проверки
  - `Q-PERMIT-RISK-009-C`: Сообщать об обнаруженных новых опасностях до начала действий
  - `Q-PERMIT-RISK-009-D`: Связывать продолжение работ с актуальностью защитных мер
- Correct:
  - `Q-PERMIT-RISK-009-A`
  - `Q-PERMIT-RISK-009-C`
  - `Q-PERMIT-RISK-009-D`
- Explanation:
  Здесь проверяется динамический, а не статический взгляд на риск-контроль.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

### Q-PERMIT-RISK-010

- Topic: `TOPIC-PERMIT-RISK`
- Type: `MULTIPLE_CHOICE`
- Text: Какие суждения указывают на опасную подмену управления риском формальной отметкой?
- Options:
  - `Q-PERMIT-RISK-010-A`: «Раз в бланке всё отмечено, место можно не смотреть»
  - `Q-PERMIT-RISK-010-B`: «Проверим, что защитные меры реально доступны и выполнены»
  - `Q-PERMIT-RISK-010-C`: «Если условия изменились, пересмотрим допуск»
  - `Q-PERMIT-RISK-010-D`: «Прошлый раз обошлось, значит и сейчас всё в порядке»
- Correct:
  - `Q-PERMIT-RISK-010-A`
  - `Q-PERMIT-RISK-010-D`
- Explanation:
  Пограничный вопрос на склонность отождествлять заполненный документ и фактическую безопасность.
- Intended tests:
  - `TEST-PERMIT-RISK-FINAL`

## TOPIC-PERMIT-ROLES

### Q-PERMIT-ROLES-001

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `SINGLE_CHOICE`
- Text: В чём ключевой смысл распределения ролей в нарядной системе?
- Options:
  - `Q-PERMIT-ROLES-001-A`: В том, чтобы увеличить число подписей в документе
  - `Q-PERMIT-ROLES-001-B`: В том, чтобы разделить ответственность за подготовку, допуск, организацию и безопасное выполнение работ
  - `Q-PERMIT-ROLES-001-C`: В том, чтобы снять ответственность с исполнителей
  - `Q-PERMIT-ROLES-001-D`: В том, чтобы любую роль мог заменить любой присутствующий сотрудник
- Correct:
  - `Q-PERMIT-ROLES-001-B`
- Explanation:
  Роли задают управляемую структуру ответственности, а не просто подписи.
- Intended tests:
  - `TEST-PERMIT-ROLES-FINAL`
  - `TEST-PERMIT-ROLES-TRAIN`

### Q-PERMIT-ROLES-002

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `SINGLE_CHOICE`
- Text: Что наиболее важно для ответственного исполнителя перед началом работ?
- Options:
  - `Q-PERMIT-ROLES-002-A`: Только проверить наличие инструмента
  - `Q-PERMIT-ROLES-002-B`: Понять условия допуска, довести их до бригады и контролировать соблюдение на месте
  - `Q-PERMIT-ROLES-002-C`: Передать всю коммуникацию любому члену бригады
  - `Q-PERMIT-ROLES-002-D`: Считать, что за безопасность отвечает только выдающий наряд
- Correct:
  - `Q-PERMIT-ROLES-002-B`
- Explanation:
  Роль ответственного исполнителя критична для перевода мер из документа в практику.
- Intended tests:
  - `TEST-PERMIT-ROLES-FINAL`

### Q-PERMIT-ROLES-003

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `SINGLE_CHOICE`
- Text: Какую ошибку в распределении ролей можно считать опасной?
- Options:
  - `Q-PERMIT-ROLES-003-A`: Чёткое понимание, кто принимает решение о допуске
  - `Q-PERMIT-ROLES-003-B`: Ситуацию, когда обязанности между руководителем и исполнителем фактически не разграничены
  - `Q-PERMIT-ROLES-003-C`: Документированную коммуникацию между участниками
  - `Q-PERMIT-ROLES-003-D`: Проверку места работ до начала задания
- Correct:
  - `Q-PERMIT-ROLES-003-B`
- Explanation:
  Размытая ответственность быстро приводит к пропуску критических действий.
- Intended tests:
  - `TEST-PERMIT-ROLES-FINAL`

### Q-PERMIT-ROLES-004

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `SINGLE_CHOICE`
- Text: Что должен сделать руководитель работ, если обнаружил, что бригада не понимает одну из мер безопасности?
- Options:
  - `Q-PERMIT-ROLES-004-A`: Начать работу и пояснить по ходу
  - `Q-PERMIT-ROLES-004-B`: Остановить допуск до прояснения и доведения условия до понятного уровня
  - `Q-PERMIT-ROLES-004-C`: Передать решение только самому опытному работнику
  - `Q-PERMIT-ROLES-004-D`: Исключить эту меру из документа как избыточную
- Correct:
  - `Q-PERMIT-ROLES-004-B`
- Explanation:
  Недопонимание бригадой мер безопасности несовместимо с качественным допуском.
- Intended tests:
  - `TEST-PERMIT-ROLES-FINAL`

### Q-PERMIT-ROLES-005

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `SINGLE_CHOICE`
- Text: Почему в теме ролей важно проверять не только знание обязанностей, но и границы полномочий?
- Options:
  - `Q-PERMIT-ROLES-005-A`: Потому что люди часто берут на себя действия, которые не должны выполнять без согласования
  - `Q-PERMIT-ROLES-005-B`: Потому что это ускоряет оформление документа
  - `Q-PERMIT-ROLES-005-C`: Потому что это позволяет обойти руководителя работ
  - `Q-PERMIT-ROLES-005-D`: Потому что полномочия не связаны с безопасностью
- Correct:
  - `Q-PERMIT-ROLES-005-A`
- Explanation:
  Аналитически полезный вопрос на самовольное расширение роли.
- Intended tests:
  - `TEST-PERMIT-ROLES-FINAL`
  - `TEST-PERMIT-ROLES-TRAIN`

### Q-PERMIT-ROLES-006

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `SINGLE_CHOICE`
- Text: Какой подход к взаимодействию руководителя и ответственного исполнителя является корректным?
- Options:
  - `Q-PERMIT-ROLES-006-A`: Каждый считает, что критические проверки сделал другой
  - `Q-PERMIT-ROLES-006-B`: Роли дублируются хаотично без разграничения ответственности
  - `Q-PERMIT-ROLES-006-C`: Каждый понимает свою часть ответственности и своевременно передаёт необходимую информацию
  - `Q-PERMIT-ROLES-006-D`: Исполнитель самостоятельно меняет условия допуска без эскалации
- Correct:
  - `Q-PERMIT-ROLES-006-C`
- Explanation:
  В основе безопасной схемы не дублирование само по себе, а понятное взаимодействие ролей.
- Intended tests:
  - `TEST-PERMIT-ROLES-FINAL`

### Q-PERMIT-ROLES-007

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `MULTIPLE_CHOICE`
- Text: Что обычно входит в зону ответственности ответственного исполнителя?
- Options:
  - `Q-PERMIT-ROLES-007-A`: Довести условия допуска до бригады
  - `Q-PERMIT-ROLES-007-B`: Следить за соблюдением мер безопасности на месте
  - `Q-PERMIT-ROLES-007-C`: Самовольно изменять согласованные условия работ
  - `Q-PERMIT-ROLES-007-D`: Останавливать работы при выявлении небезопасных условий
- Correct:
  - `Q-PERMIT-ROLES-007-A`
  - `Q-PERMIT-ROLES-007-B`
  - `Q-PERMIT-ROLES-007-D`
- Explanation:
  Вопрос проверяет практическое понимание роли на площадке.
- Intended tests:
  - `TEST-PERMIT-ROLES-FINAL`
  - `TEST-PERMIT-ROLES-TRAIN`

### Q-PERMIT-ROLES-008

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `MULTIPLE_CHOICE`
- Text: Какие признаки указывают на опасное размывание ролей в нарядной системе?
- Options:
  - `Q-PERMIT-ROLES-008-A`: Никто не может чётко сказать, кто отвечает за допуск
  - `Q-PERMIT-ROLES-008-B`: Исполнитель считает, что документ сам по себе гарантирует безопасность
  - `Q-PERMIT-ROLES-008-C`: Обязанности проговорены и понятны всем участникам
  - `Q-PERMIT-ROLES-008-D`: Критические решения принимаются без явного ответственного
- Correct:
  - `Q-PERMIT-ROLES-008-A`
  - `Q-PERMIT-ROLES-008-B`
  - `Q-PERMIT-ROLES-008-D`
- Explanation:
  Это вопрос на организационные причины инцидентов, а не на терминологию.
- Intended tests:
  - `TEST-PERMIT-ROLES-TRAIN`

### Q-PERMIT-ROLES-009

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия руководителя работ помогают избежать потери управляемости?
- Options:
  - `Q-PERMIT-ROLES-009-A`: Проверять, что бригада понимает условия допуска
  - `Q-PERMIT-ROLES-009-B`: Игнорировать сигналы о смене условий на месте
  - `Q-PERMIT-ROLES-009-C`: Уточнять, кто и как контролирует выполнение мер
  - `Q-PERMIT-ROLES-009-D`: Останавливать работы при потере условий безопасного выполнения
- Correct:
  - `Q-PERMIT-ROLES-009-A`
  - `Q-PERMIT-ROLES-009-C`
  - `Q-PERMIT-ROLES-009-D`
- Explanation:
  Вопрос проверяет роль руководителя как носителя управляемости, а не формальной подписи.
- Intended tests:
  - `TEST-PERMIT-ROLES-FINAL`

### Q-PERMIT-ROLES-010

- Topic: `TOPIC-PERMIT-ROLES`
- Type: `MULTIPLE_CHOICE`
- Text: Какие ответы демонстрируют рискованное смешение ролей и полномочий?
- Options:
  - `Q-PERMIT-ROLES-010-A`: «Если срочно, исполнитель сам поменяет условия допуска»
  - `Q-PERMIT-ROLES-010-B`: «При изменении условий нужна координация и пересмотр решения»
  - `Q-PERMIT-ROLES-010-C`: «Неважно, кто именно отвечает, главное начать работу»
  - `Q-PERMIT-ROLES-010-D`: «Ответственность должна быть понятна до начала задания»
- Correct:
  - `Q-PERMIT-ROLES-010-A`
  - `Q-PERMIT-ROLES-010-C`
- Explanation:
  Пограничный вопрос на склонность заменять управляемость скоростью старта.
- Intended tests:
  - `TEST-PERMIT-ROLES-TRAIN`

## TOPIC-OPO-SELF-RULES

### Q-OPO-SELF-RULES-001

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `SINGLE_CHOICE`
- Text: Какова главная цель самоподготовки по промышленной безопасности на ОПО в self-contour?
- Options:
  - `Q-OPO-SELF-RULES-001-A`: Заменить обязательное назначение для всех сотрудников
  - `Q-OPO-SELF-RULES-001-B`: Дать работнику возможность самостоятельно обновить знания по безопасному поведению на ОПО
  - `Q-OPO-SELF-RULES-001-C`: Отменить необходимость локальных инструктажей
  - `Q-OPO-SELF-RULES-001-D`: Подменить расследование инцидентов самопроверкой
- Correct:
  - `Q-OPO-SELF-RULES-001-B`
- Explanation:
  Self-course нужен как добровольный контур повторения и проверки знаний без mandatory assignment.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-RULES-002

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `SINGLE_CHOICE`
- Text: Что работник должен сделать, если фактическая обстановка на ОПО не совпадает с известной ему безопасной процедурой?
- Options:
  - `Q-OPO-SELF-RULES-002-A`: Продолжить работу по привычке
  - `Q-OPO-SELF-RULES-002-B`: Свериться с актуальными требованиями и сообщить об отклонении до начала действий
  - `Q-OPO-SELF-RULES-002-C`: Ориентироваться на устные советы ближайшего коллеги
  - `Q-OPO-SELF-RULES-002-D`: Игнорировать расхождение, если задача срочная
- Correct:
  - `Q-OPO-SELF-RULES-002-B`
- Explanation:
  Самопроверка должна закреплять связь между отклонением условий и обязательной эскалацией.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-RULES-003

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `SINGLE_CHOICE`
- Text: Какое поведение на ОПО можно считать безопасным по умолчанию?
- Options:
  - `Q-OPO-SELF-RULES-003-A`: Выполнять действия только в пределах утверждённых процедур и полномочий
  - `Q-OPO-SELF-RULES-003-B`: Выбирать кратчайший путь через ограниченную зону
  - `Q-OPO-SELF-RULES-003-C`: Откладывать сообщение о мелких нарушениях
  - `Q-OPO-SELF-RULES-003-D`: Подменять проверку документации личным опытом
- Correct:
  - `Q-OPO-SELF-RULES-003-A`
- Explanation:
  Базовый принцип промышленной безопасности - не выходить за рамки процедуры и полномочий.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`

### Q-OPO-SELF-RULES-004

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `SINGLE_CHOICE`
- Text: Что означает требование немедленно сообщать о небезопасных условиях на ОПО?
- Options:
  - `Q-OPO-SELF-RULES-004-A`: Сообщать только после окончания смены
  - `Q-OPO-SELF-RULES-004-B`: Передавать информацию по установленному порядку сразу после обнаружения отклонения
  - `Q-OPO-SELF-RULES-004-C`: Сообщать только если есть прямой ущерб
  - `Q-OPO-SELF-RULES-004-D`: Передавать только коллегам в своём звене
- Correct:
  - `Q-OPO-SELF-RULES-004-B`
- Explanation:
  Раннее сообщение нужно, чтобы не дать отклонению перерасти в инцидент.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-RULES-005

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `SINGLE_CHOICE`
- Text: Почему на ОПО опасно опираться на принцип «я уже это делал много раз»?
- Options:
  - `Q-OPO-SELF-RULES-005-A`: Потому что это мешает заполнению отчётности
  - `Q-OPO-SELF-RULES-005-B`: Потому что рутинность снижает внимание к отклонениям и фактическим условиям
  - `Q-OPO-SELF-RULES-005-C`: Потому что опыт запрещён локальными актами
  - `Q-OPO-SELF-RULES-005-D`: Потому что опыт нужен только руководителям
- Correct:
  - `Q-OPO-SELF-RULES-005-B`
- Explanation:
  Вопрос диагностирует риск автоматизма и самоуверенности в знакомых операциях.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-RULES-006

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `SINGLE_CHOICE`
- Text: Что лучше всего показывает, что работник понимает базовые требования промышленной безопасности на ОПО?
- Options:
  - `Q-OPO-SELF-RULES-006-A`: Он может объяснить, где проходят границы его полномочий и когда нужно эскалировать отклонение
  - `Q-OPO-SELF-RULES-006-B`: Он запоминает только привычный маршрут обхода
  - `Q-OPO-SELF-RULES-006-C`: Он предпочитает действовать без уточнений, чтобы не терять время
  - `Q-OPO-SELF-RULES-006-D`: Он полагается на самый свежий устный совет коллеги
- Correct:
  - `Q-OPO-SELF-RULES-006-A`
- Explanation:
  Понимание правил проявляется в умении распознавать границы допустимых действий.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`

### Q-OPO-SELF-RULES-007

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `SINGLE_CHOICE`
- Text: Какой ответ наиболее полезен для аналитики ошибок в теме базовых требований на ОПО?
- Options:
  - `Q-OPO-SELF-RULES-007-A`: Убеждение, что знакомая операция безопасна даже при расхождении с процедурой
  - `Q-OPO-SELF-RULES-007-B`: Готовность перепроверить требования перед нестандартным действием
  - `Q-OPO-SELF-RULES-007-C`: Понимание порядка эскалации
  - `Q-OPO-SELF-RULES-007-D`: Привычка сверять фактические условия с инструкцией
- Correct:
  - `Q-OPO-SELF-RULES-007-A`
- Explanation:
  Пограничный вопрос на склонность ставить привычку выше процедуры.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-RULES-008

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия соответствуют базовым требованиям промышленной безопасности на ОПО?
- Options:
  - `Q-OPO-SELF-RULES-008-A`: Сообщать об обнаруженных небезопасных условиях
  - `Q-OPO-SELF-RULES-008-B`: Выполнять действия только в рамках утверждённой процедуры
  - `Q-OPO-SELF-RULES-008-C`: Игнорировать мелкие отклонения до конца смены
  - `Q-OPO-SELF-RULES-008-D`: Уточнять порядок действий при неопределённости
- Correct:
  - `Q-OPO-SELF-RULES-008-A`
  - `Q-OPO-SELF-RULES-008-B`
  - `Q-OPO-SELF-RULES-008-D`
- Explanation:
  Это ядро безопасного поведения работника на объекте.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-RULES-009

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `MULTIPLE_CHOICE`
- Text: Какие признаки указывают на опасный формализм в отношении требований безопасности?
- Options:
  - `Q-OPO-SELF-RULES-009-A`: Убеждение, что подпись об ознакомлении заменяет понимание правил
  - `Q-OPO-SELF-RULES-009-B`: Ориентация на фразу «так всегда делали»
  - `Q-OPO-SELF-RULES-009-C`: Сверка процедуры с фактической обстановкой
  - `Q-OPO-SELF-RULES-009-D`: Игнорирование расхождения между документом и реальной площадкой
- Correct:
  - `Q-OPO-SELF-RULES-009-A`
  - `Q-OPO-SELF-RULES-009-B`
  - `Q-OPO-SELF-RULES-009-D`
- Explanation:
  Вопрос помогает отделить реальное соблюдение требований от имитации их выполнения.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`

### Q-OPO-SELF-RULES-010

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `MULTIPLE_CHOICE`
- Text: Что из перечисленного помогает снизить риск ошибки при самостоятельной работе на ОПО?
- Options:
  - `Q-OPO-SELF-RULES-010-A`: Возвращаться к контрольной точке процедуры после отвлечения
  - `Q-OPO-SELF-RULES-010-B`: Самостоятельно ускорять редкую операцию без сверки
  - `Q-OPO-SELF-RULES-010-C`: Перепроверять фактические условия перед действием
  - `Q-OPO-SELF-RULES-010-D`: Запрашивать уточнение при сомнениях
- Correct:
  - `Q-OPO-SELF-RULES-010-A`
  - `Q-OPO-SELF-RULES-010-C`
  - `Q-OPO-SELF-RULES-010-D`
- Explanation:
  Здесь проверяется дисциплина безопасной работы, а не память на формулировки.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-RULES-011

- Topic: `TOPIC-OPO-SELF-RULES`
- Type: `MULTIPLE_CHOICE`
- Text: Какие высказывания отражают правильное отношение к работе на ОПО?
- Options:
  - `Q-OPO-SELF-RULES-011-A`: «Если условия изменились, нужно остановиться и уточнить порядок действий»
  - `Q-OPO-SELF-RULES-011-B`: «Личный опыт важнее локальной процедуры»
  - `Q-OPO-SELF-RULES-011-C`: «Отклонение лучше сообщить сразу, даже если оно кажется небольшим»
  - `Q-OPO-SELF-RULES-011-D`: «Вне своих полномочий действовать нельзя, даже если задача выглядит простой»
- Correct:
  - `Q-OPO-SELF-RULES-011-A`
  - `Q-OPO-SELF-RULES-011-C`
  - `Q-OPO-SELF-RULES-011-D`
- Explanation:
  Вопрос годится как для тематической самопроверки, так и для общего self refresher.
- Intended tests:
  - `TEST-OPO-SELF-RULES-SELF`
  - `TEST-OPO-SELF-GENERAL`

## TOPIC-OPO-SELF-INCIDENTS

### Q-OPO-SELF-INCIDENTS-001

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `SINGLE_CHOICE`
- Text: Что работник должен сделать первым при обнаружении признаков угрозы аварии на ОПО?
- Options:
  - `Q-OPO-SELF-INCIDENTS-001-A`: Продолжить работу до завершения операции
  - `Q-OPO-SELF-INCIDENTS-001-B`: Действовать по алгоритму оповещения и обеспечить личную безопасность
  - `Q-OPO-SELF-INCIDENTS-001-C`: Сначала обсудить ситуацию с коллегами неофициально
  - `Q-OPO-SELF-INCIDENTS-001-D`: Попробовать скрыть отклонение до следующей смены
- Correct:
  - `Q-OPO-SELF-INCIDENTS-001-B`
- Explanation:
  Первичная реакция должна быть быстрой, процедурной и безопасной для самого работника.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-INCIDENTS-002

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `SINGLE_CHOICE`
- Text: Что из перечисленного лучше всего описывает типовое опасное нарушение со стороны работника?
- Options:
  - `Q-OPO-SELF-INCIDENTS-002-A`: Сообщить о странном шуме по установленному порядку
  - `Q-OPO-SELF-INCIDENTS-002-B`: Игнорировать ранние признаки отклонения, если они уже встречались ранее
  - `Q-OPO-SELF-INCIDENTS-002-C`: Уточнить границы безопасной зоны
  - `Q-OPO-SELF-INCIDENTS-002-D`: Перепроверить маршрут эвакуации
- Correct:
  - `Q-OPO-SELF-INCIDENTS-002-B`
- Explanation:
  Вопрос показывает риск недооценки повторяющихся ранних сигналов.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`

### Q-OPO-SELF-INCIDENTS-003

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `SINGLE_CHOICE`
- Text: Почему опасно откладывать сообщение о небольшом подтёке или необычном запахе?
- Options:
  - `Q-OPO-SELF-INCIDENTS-003-A`: Потому что это усложняет оформление журнала
  - `Q-OPO-SELF-INCIDENTS-003-B`: Потому что ранний признак может указывать на развитие более серьёзного отклонения
  - `Q-OPO-SELF-INCIDENTS-003-C`: Потому что сообщение обязательно делает руководителя виновным
  - `Q-OPO-SELF-INCIDENTS-003-D`: Потому что это запрещает дальнейший обход
- Correct:
  - `Q-OPO-SELF-INCIDENTS-003-B`
- Explanation:
  Ранние признаки особенно важны для предупреждения инцидента.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-INCIDENTS-004

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `SINGLE_CHOICE`
- Text: Какое действие недопустимо при угрозе аварии?
- Options:
  - `Q-OPO-SELF-INCIDENTS-004-A`: Уточнить безопасный маршрут отхода
  - `Q-OPO-SELF-INCIDENTS-004-B`: Передать фактическую информацию об отклонении
  - `Q-OPO-SELF-INCIDENTS-004-C`: Самовольно входить в опасную зону без необходимости и без защищённого порядка
  - `Q-OPO-SELF-INCIDENTS-004-D`: Выполнять предписанный алгоритм реагирования
- Correct:
  - `Q-OPO-SELF-INCIDENTS-004-C`
- Explanation:
  Нештатная ситуация не оправдывает импровизацию с риском для жизни и объекта.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`

### Q-OPO-SELF-INCIDENTS-005

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `SINGLE_CHOICE`
- Text: Что означает корректная фиксация типового нарушения или инцидентного признака?
- Options:
  - `Q-OPO-SELF-INCIDENTS-005-A`: Эмоционально описать, кто виноват
  - `Q-OPO-SELF-INCIDENTS-005-B`: Кратко и точно зафиксировать время, место, признак и принятые действия
  - `Q-OPO-SELF-INCIDENTS-005-C`: Передать информацию только устно знакомому коллеге
  - `Q-OPO-SELF-INCIDENTS-005-D`: Описать только последствия без признаков
- Correct:
  - `Q-OPO-SELF-INCIDENTS-005-B`
- Explanation:
  Для последующего анализа важны фактология и последовательность действий.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-INCIDENTS-006

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `SINGLE_CHOICE`
- Text: Какой ответ наиболее полезен для аналитики в теме нарушений и угрозы аварии?
- Options:
  - `Q-OPO-SELF-INCIDENTS-006-A`: Убеждение, что без открытого огня любое отклонение можно считать неопасным
  - `Q-OPO-SELF-INCIDENTS-006-B`: Готовность сообщить о раннем признаке и действовать по алгоритму
  - `Q-OPO-SELF-INCIDENTS-006-C`: Умение назвать точку сбора
  - `Q-OPO-SELF-INCIDENTS-006-D`: Привычка вести записи в журнале
- Correct:
  - `Q-OPO-SELF-INCIDENTS-006-A`
- Explanation:
  Пограничный вопрос на опасную недооценку ранних признаков аварийного развития.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-INCIDENTS-007

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `SINGLE_CHOICE`
- Text: Какой подход к типовым нарушениям наиболее безопасен?
- Options:
  - `Q-OPO-SELF-INCIDENTS-007-A`: Делить нарушения на важные и неважные по личному ощущению
  - `Q-OPO-SELF-INCIDENTS-007-B`: Рассматривать нарушение как сигнал для остановки, сообщения и проверки условий
  - `Q-OPO-SELF-INCIDENTS-007-C`: Сообщать только о нарушениях с видимыми последствиями
  - `Q-OPO-SELF-INCIDENTS-007-D`: Исправлять всё самостоятельно без фиксации
- Correct:
  - `Q-OPO-SELF-INCIDENTS-007-B`
- Explanation:
  Корректная реакция строится на фиксации и управляемости, а не на личной оценке «мелкости».
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`

### Q-OPO-SELF-INCIDENTS-008

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `MULTIPLE_CHOICE`
- Text: Какие признаки могут указывать на развивающуюся угрозу аварии на ОПО?
- Options:
  - `Q-OPO-SELF-INCIDENTS-008-A`: Нехарактерный запах продукта
  - `Q-OPO-SELF-INCIDENTS-008-B`: Подтёк, задымление или необычный шум
  - `Q-OPO-SELF-INCIDENTS-008-C`: Плановая смена исполнителя в графике
  - `Q-OPO-SELF-INCIDENTS-008-D`: Ненормальное поведение оборудования или сигнализация отклонения
- Correct:
  - `Q-OPO-SELF-INCIDENTS-008-A`
  - `Q-OPO-SELF-INCIDENTS-008-B`
  - `Q-OPO-SELF-INCIDENTS-008-D`
- Explanation:
  Это набор типовых ранних признаков, требующих реакции по процедуре.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-INCIDENTS-009

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `MULTIPLE_CHOICE`
- Text: Какие действия относятся к правильной первичной реакции работника при угрозе аварии?
- Options:
  - `Q-OPO-SELF-INCIDENTS-009-A`: Передать фактическую информацию по установленному каналу
  - `Q-OPO-SELF-INCIDENTS-009-B`: Обеспечить личную безопасность и не входить без необходимости в опасную зону
  - `Q-OPO-SELF-INCIDENTS-009-C`: Отложить сообщение до момента, когда ситуация станет полностью очевидной
  - `Q-OPO-SELF-INCIDENTS-009-D`: Действовать в рамках утверждённого алгоритма
- Correct:
  - `Q-OPO-SELF-INCIDENTS-009-A`
  - `Q-OPO-SELF-INCIDENTS-009-B`
  - `Q-OPO-SELF-INCIDENTS-009-D`
- Explanation:
  Здесь важен полный первичный контур: безопасность, сообщение, алгоритм.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-INCIDENTS-010

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `MULTIPLE_CHOICE`
- Text: Какие ответы демонстрируют опасную недооценку нарушения или инцидентного признака?
- Options:
  - `Q-OPO-SELF-INCIDENTS-010-A`: «Раз нет явной аварии, можно подождать»
  - `Q-OPO-SELF-INCIDENTS-010-B`: «Лучше сразу сообщить и свериться с алгоритмом»
  - `Q-OPO-SELF-INCIDENTS-010-C`: «Если такое уже бывало, можно не торопиться»
  - `Q-OPO-SELF-INCIDENTS-010-D`: «Ранний признак тоже важен для безопасности объекта»
- Correct:
  - `Q-OPO-SELF-INCIDENTS-010-A`
  - `Q-OPO-SELF-INCIDENTS-010-C`
- Explanation:
  Пограничный вопрос на ложное ощущение контроля над знакомым отклонением.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`
  - `TEST-OPO-SELF-GENERAL`

### Q-OPO-SELF-INCIDENTS-011

- Topic: `TOPIC-OPO-SELF-INCIDENTS`
- Type: `MULTIPLE_CHOICE`
- Text: Какие высказывания отражают правильное отношение работника к типовым нарушениям на ОПО?
- Options:
  - `Q-OPO-SELF-INCIDENTS-011-A`: «Нарушение лучше зафиксировать точно, а не передавать слухами»
  - `Q-OPO-SELF-INCIDENTS-011-B`: «Если нет последствий, можно не сообщать»
  - `Q-OPO-SELF-INCIDENTS-011-C`: «При угрозе аварии важно не выходить за пределы своей роли и алгоритма»
  - `Q-OPO-SELF-INCIDENTS-011-D`: «Даже небольшой признак может быть важен для предотвращения развития события»
- Correct:
  - `Q-OPO-SELF-INCIDENTS-011-A`
  - `Q-OPO-SELF-INCIDENTS-011-C`
  - `Q-OPO-SELF-INCIDENTS-011-D`
- Explanation:
  Вопрос объединяет точность фиксации, роль работника и профилактику развития инцидента.
- Intended tests:
  - `TEST-OPO-SELF-INCIDENTS-SELF`
  - `TEST-OPO-SELF-GENERAL`

## Final Consistency

- total questions = `85`;
- only allowed question types used: `SINGLE_CHOICE`, `MULTIPLE_CHOICE`;
- each question has at least `4` options;
- each `SINGLE_CHOICE` question has exactly `1` correct option;
- each `MULTIPLE_CHOICE` question has at least `2` correct options;
- each question references existing topic code from `content-matrix.md`;
- question bank is intended for explicit manual assembly of tests and does not assume random question sampling.
