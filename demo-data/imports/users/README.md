# User Import Package

Эта папка содержит demo-пакет работников предприятия для уже загруженной НПЗ-оргструктуры `/department/npz/...`.

Важно:
- platform admin не входит в состав работников предприятия;
- администратор приложения обслуживает платформу вне enterprise-org contour;
- поэтому в этом пакете нет пользователя `ADMIN` и нет его `PRIMARY` привязки к подразделению предприятия.

## Что лежит в папке

- [users-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/users-import.xlsx)  
  legacy demo package для ручной поэтапной загрузки пользователей, оргпривязок и ролей.
- [users.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/users.csv)  
  исходный enterprise roster для ручного seed.
- [user-org-assignments.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/user-org-assignments.csv)  
  `PRIMARY` home unit assignments для ручного seed.
- [user-role-assignments.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/user-role-assignments.csv)  
  legacy system roles для ручного seed.
- [role-dictionary.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/role-dictionary.csv)  
  словарь ролей для ручного seed.
- [personnel-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/personnel-import.xlsx)  
  baseline workbook для personnel Excel contour. Он зеркалит тот же enterprise roster, который должен появиться после ручной загрузки.
- [personnel-import.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/personnel-import.csv)  
  плоское зеркало baseline workbook.
- [personnel-update-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/personnel-update-import.xlsx)  
  второй workbook для проверки auto-update после ручного seed пользователей.
- [personnel-update-import.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/personnel-update-import.csv)  
  плоское зеркало update workbook.
- [users-import.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/users-import.md)  
  порядок ручной загрузки и сценарий дальнейшей проверки personnel auto-update.

## Состав demo-пакета

- `40` операторов
- `8` начальников установок
- `2` начальника комплексов
- `1` начальник производства
- `1` генеральный директор
- `1` специалист по учебным материалам
- `1` специалист по ОТиПБ

Итого:
- `51` enterprise users

## Runtime-совместимое сопоставление позиций

Для personnel Excel contour baseline workbook использует текущие runtime коды:

- `HEAD` -> все начальники и managerial contour
- `OPS` -> операторы установок
- `DEV` -> non-manager specialist contour

Это означает:
- `personnel-import.xlsx` теперь является чистым baseline без кадровых изменений;
- `personnel-update-import.xlsx` содержит только сценарии обновления поверх уже вручную загруженного enterprise roster.

## Что проверяет второй workbook

[personnel-update-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/users/personnel-update-import.xlsx) содержит полный roster и несколько осмысленных изменений:

- перевод оператора `NPZ-OP-UKK-001` из `CCK-UKK` в `CCK-UPV`
- promotion `NPZ-ITR-HSE-001` из specialist contour в managerial contour
- смену позиции `NPZ-ITR-CCK-001` с managerial contour на specialist contour
- temporary acting head для `NPZ-OP-MTBEO-001`
- dismissal для `NPZ-OP-TAME-008`

Этот файл нужен именно для проверки auto-update, а не для первичного seed пользователей.
