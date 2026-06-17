update course
set description = 'Курс знакомит операторов с основными требованиями промышленной безопасности на НПЗ: производственными рисками, безопасным поведением на установке, обязанностями персонала и первичными действиями при признаках нештатной ситуации.'
where name = 'Промышленная безопасность оператора НПЗ'
  and description = 'Demo assigned course for operators and adjacent production contour on industrial safety at the refinery.';

update course
set description = 'Курс объясняет порядок оформления наряда-допуска, подготовки рабочего места, распределения ролей и контроля рисков перед началом работ. Он помогает безопасно организовать работы на производственном объекте и снизить вероятность инцидентов.'
where name = 'Наряд-допуск и безопасная организация работ'
  and description = 'Demo assigned course for permit-to-work and safe work organization for managerial and engineering contour.';

update course
set description = 'Курс для самостоятельного повторения ключевых требований промышленной безопасности на опасном производственном объекте. Он помогает освежить знания о правилах безопасной работы, типовых нарушениях и действиях при угрозе инцидента.'
where name = 'Самоподготовка по промышленной безопасности на ОПО'
  and description = 'Demo self-service refresher course on industrial safety at a hazardous production facility.';
