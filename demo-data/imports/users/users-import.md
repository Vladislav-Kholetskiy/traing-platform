# Users Import Flow

Пакет рассчитан на уже загруженную оргструктуру НПЗ:
- [org-structure-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure-import.xlsx)

Важно:
- пакет описывает только работников предприятия;
- platform admin не входит в оргструктуру предприятия и не импортируется отсюда;
- роль `ADMIN` должна оставаться вне enterprise personnel contour.

## Этап 1. Ручной seed пользователей

Пока personnel Excel runtime в live-контуре может быть административно закрыт policy-слоем, первичную загрузку demo-пользователей нужно делать вручную через legacy package.

Порядок ручной загрузки:

1. Проверить наличие оргединиц `/department/npz/...`
2. Загрузить словарь ролей из [role-dictionary.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/role-dictionary.csv)
3. Создать пользователей из [users.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/users.csv)
4. Назначить `PRIMARY` home unit из [user-org-assignments.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/user-org-assignments.csv)
5. Назначить системные роли из [user-role-assignments.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/user-role-assignments.csv)

Почему именно такой порядок:
- для `OPERATOR` backend требует один активный `PRIMARY` home unit до назначения роли;
- поэтому `PRIMARY` org assignment должен идти раньше `OPERATOR` role assignment;
- для `MANAGER` и `EXPERT` это ограничение в demo-пакете не используется как критическое.

Принятое business -> system сопоставление в ручном seed:
- все `ИТР` пользователи идут как `MANAGER`
- специалист по учебным материалам идет как `EXPERT`
- операторы идут как `OPERATOR`

## Этап 2. Baseline workbook для personnel contour

После ручного seed состояние enterprise roster должно совпадать с [personnel-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/personnel-import.xlsx).

Это baseline workbook без кадровых изменений.  
Он нужен для:

- быстрой сверки, что personnel Excel contract собран корректно;
- dry-run/no-change сценария;
- проверки, что roster в Excel совпадает с уже вручную загруженными пользователями.

Runtime-коды позиций в baseline workbook:
- `HEAD` -> managerial contour
- `OPS` -> operator contour
- `DEV` -> specialist contour

## Этап 3. Workbook для auto-update проверки

После того как baseline users уже созданы вручную, для проверки auto-update нужно использовать:
- [personnel-update-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/personnel-update-import.xlsx)

Этот workbook содержит полный roster и несколько целевых изменений:

1. `NPZ-OP-UKK-001`  
   Перевод из `CCK-UKK` в `CCK-UPV` без смены operator contour.

2. `NPZ-ITR-HSE-001`  
   Повышение из `DEV` в `HEAD`.

3. `NPZ-ITR-CCK-001`  
   Смена managerial contour на specialist contour: `HEAD -> DEV`.

4. `NPZ-OP-MTBEO-001`  
   Временное назначение `ACTING_HEAD` в `CMT-MTBEO` на период `2026-06-01 .. 2026-06-30`.

5. `NPZ-OP-TAME-008`  
   Сценарий увольнения: `DISMISSED`.

Остальные строки остаются равными baseline roster, чтобы workbook был пригоден для полного повторного прогона.

## Что именно проверять этим сценарием

На `personnel-update-import.xlsx` нужно смотреть:

- transfer по `homeOrgUnitCode`
- смену `basePositionCode`
- temporary additive appointment
- dismissal path
- отсутствие лишних изменений у остальных пользователей
- idempotent `NO_CHANGE` для строк, которые не отличаются от baseline

## Что не использовать для первичного seed

- [personnel-update-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/personnel-update-import.xlsx)  
  не является стартовым roster-файлом;
- он нужен только после ручной загрузки baseline users.
