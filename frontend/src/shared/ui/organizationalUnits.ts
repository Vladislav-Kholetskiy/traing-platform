const organizationDisplayNames: Record<string, string> = {
  department: 'Департамент',
  dept: 'Департамент',
  npz: 'Нефтеперерабатывающий завод',
  hq: 'Заводоуправление',
  zavodoupravlenie: 'Заводоуправление',
  production: 'Производство',
  cck: 'Комплекс каталитического крекинга',
  ukk: 'Установка каталитического крекинга',
  upv: 'Установка производства водорода',
  komt: 'Комплекс облагораживания моторных топлив',
  mtbeo: 'Установка по производству МТБЭ и олигомеризата',
  gobkk: 'Установка гидрооблагораживания бензина каталитического крекинга',
  tame: 'Установка по производству ТАМЭ',
  edu: 'Отдел по работе с учебными материалами',
  hse: 'Отдел охраны труда и промышленной безопасности',
};

const organizationTypeDisplayNames: Record<string, string> = {
  department: 'Департамент',
  enterprise: 'Предприятие',
  administrative_unit: 'Административное подразделение',
  production_block: 'Производственный блок',
  production_complex: 'Производственный комплекс',
  process_unit: 'Производственная установка',
};

export function humanizeOrganizationalUnit(
  unitName?: string | null,
  unitPath?: string | null,
  empty = 'Установка не указана',
): string {
  const pathTail = unitPath?.split('/').filter(Boolean).at(-1)?.trim().toLowerCase();
  if (pathTail) {
    const mapped = organizationDisplayNames[pathTail];
    if (mapped) {
      return mapped;
    }
  }

  const normalizedName = unitName?.trim().toLowerCase();
  if (normalizedName) {
    const mapped = organizationDisplayNames[normalizedName];
    if (mapped) {
      return mapped;
    }

    return unitName!.trim();
  }

  return empty;
}

export function humanizeOrganizationalPath(unitPath?: string | null, empty = 'Путь не указан'): string {
  const segments = unitPath?.split('/').filter(Boolean) ?? [];
  if (segments.length === 0) {
    return empty;
  }

  return segments
    .map((segment) => organizationDisplayNames[segment.trim().toLowerCase()] ?? segment)
    .join(' / ');
}

export function humanizeOrganizationalUnitType(
  typeName?: string | null,
  typeCode?: string | null,
  empty = 'Тип не указан',
): string {
  const normalizedCode = typeCode?.trim().toLowerCase();
  if (normalizedCode && organizationTypeDisplayNames[normalizedCode]) {
    return organizationTypeDisplayNames[normalizedCode];
  }

  const normalizedName = typeName?.trim().toLowerCase().replace(/\s+/g, '_');
  if (normalizedName && organizationTypeDisplayNames[normalizedName]) {
    return organizationTypeDisplayNames[normalizedName];
  }

  return typeName?.trim() || typeCode?.trim() || empty;
}
