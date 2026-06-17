# Org Structure Import Flow

Этот файл фиксирует фактический порядок загрузки оргструктуры для demo-контура после проверки на live backend `2026-05-11`.

Артефакты:
- [org-structure-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure-import.xlsx)
- [org-unit-types.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-unit-types.csv)
- [org-structure.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure.csv)
- [org-structure-runtime.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure-runtime.csv)

Что подтверждено:
- `organizational_unit_type` загружается через `POST /api/v1/admin/org-unit-types`;
- `organizational_unit` загружается через `POST /api/v1/admin/org-units`;
- поле `organizational_unit.name` формирует path segment;
- поэтому для runtime-загрузки нужно использовать `runtime_name`, а не русские display-наименования;
- повторная попытка создать уже существующий type code `department` возвращает ожидаемый `409 Conflict`.

Рекомендуемый порядок:
1. Загрузить словарь типов из листа `Unit Types`.
2. Получить id типов по `code`.
3. Загрузить оргединицы из листа `Org Units` сверху вниз по иерархии.
4. Для каждой оргединицы использовать:
   - `parentId` по уже созданному `parent_external_id`;
   - `organizationalUnitTypeId` по `type_code`;
   - `name` из `runtime_name`;
   - `externalId` из `external_id`.

Минимальное сопоставление полей для `POST /api/v1/admin/org-units`:
- `external_id` -> `externalId`
- `runtime_name` -> `name`
- `type_code` -> `organizationalUnitTypeId` через lookup по уже созданному type
- `parent_external_id` -> `parentId` через lookup по уже созданному parent unit

Итоговый runtime-корень:
- `DEPT`
- path: `/department`

Проверенное состояние backend на `2026-05-11`:
- 6 type codes созданы;
- 13 оргединиц созданы;
- дерево успешно читается через `GET /api/v1/admin/org-units/tree`.
