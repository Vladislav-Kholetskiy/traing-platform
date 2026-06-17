# Org Structure Dataset

Источник для первого этапа demo-импорта оргструктуры.

Файл данных:
- [org-unit-types.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-unit-types.csv)
- [org-structure.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure.csv)
- [org-structure-runtime.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure-runtime.csv)

Поля:
- `code` — стабильный код подразделения;
- `full_name` — полное официальное наименование;
- `short_name` — краткое отображаемое наименование;
- `parent_code` — код родительского подразделения, пусто только у корня;
- `type` — тип подразделения.

Зафиксированные правила:
- `ADMIN` не входит в оргструктуру предприятия;
- оргструктура описывает только реальный контур предприятия;
- верхний уровень дерева — `DEPT`;
- `NPZ` входит в `DEPT`;
- `EDU` и `HSE` относятся к `administrative_unit`.

Runtime-ограничение текущего backend:
- поле `organizational_unit.name` сейчас используется как канонический path-segment;
- поэтому для реальной загрузки через owner API используются slug-имена из `org-structure-runtime.csv`;
- бизнес-коды подразделений сохраняются в `externalId`;
- русские полные/краткие наименования остаются в `org-structure.csv` как согласованный demo-источник.

Следующий шаг после согласования:
- подготовить Excel-файлы на основе `org-unit-types.csv` и `org-structure.csv`;
- загрузить сначала `organizational_unit_type`, затем `organizational_unit`;
- определить способ фактического импорта в running backend.
