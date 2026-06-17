# Org Structure Import

Эта папка содержит согласованный набор данных для импорта оргструктуры demo-контура НПЗ.

Здесь находятся:
- финальный workbook: [org-structure-import.xlsx](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure-import.xlsx)
- описание источников и runtime-ограничений: [org-structure.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure.md)
- порядок фактической загрузки: [org-structure-import.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure-import.md)
- runtime-соответствие имен backend: [org-structure-runtime.csv](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/org-structure/org-structure-runtime.csv)

Текущий статус:
- словарь типов и дерево уже подтверждены на live backend `2026-05-11`;
- `runtime_name` согласован с тем, как backend строит `path`;
- набор можно повторно использовать как источник для ручного импорта и сверки.
