import dayjs from 'dayjs';

const statusLabels: Record<string, string> = {
  ACTIVE: 'Активно',
  ASSIGNED: 'Назначено',
  COMPLETED: 'Завершено',
  CLOSED: 'Закрыто',
  CANCELLED: 'Отменено',
  OVERDUE: 'Просрочено',
  PUBLISHED: 'Опубликовано',
  DRAFT: 'Черновик',
  ARCHIVED: 'Архив',
  STARTED: 'Начато',
  IN_PROGRESS: 'В процессе',
  SUBMITTED: 'Отправлено',
  PASSED: 'Пройдено',
  FAILED: 'Не пройдено',
  PENDING: 'Ожидает',
  SKIPPED: 'Пропущено',
  SENT: 'Отправлено',
  PROCESSING: 'Обрабатывается',
  APPLIED: 'Применено',
  NO_CHANGE: 'Без изменений',
  REQUIRES_REVIEW: 'Требует review',
  COMPLETED_WITH_ERRORS: 'Завершено с ошибками',
  INACTIVE: 'Неактивно',
};

const statusColors: Record<string, string> = {
  ACTIVE: 'processing',
  ASSIGNED: 'blue',
  COMPLETED: 'success',
  CLOSED: 'success',
  CANCELLED: 'default',
  OVERDUE: 'error',
  PUBLISHED: 'cyan',
  DRAFT: 'gold',
  ARCHIVED: 'default',
  STARTED: 'processing',
  IN_PROGRESS: 'processing',
  SUBMITTED: 'purple',
  PASSED: 'success',
  FAILED: 'error',
  PENDING: 'gold',
  SKIPPED: 'default',
  SENT: 'success',
  PROCESSING: 'processing',
  APPLIED: 'success',
  NO_CHANGE: 'default',
  REQUIRES_REVIEW: 'warning',
  COMPLETED_WITH_ERRORS: 'warning',
  INACTIVE: 'default',
};

const questionTypeLabels: Record<string, string> = {
  SINGLE_CHOICE: 'Один вариант ответа',
  MULTIPLE_CHOICE: 'Несколько вариантов ответа',
  MATCHING: 'Сопоставление',
  ORDERING: 'Упорядочивание',
};

const materialTypeLabels: Record<string, string> = {
  TEXT: 'Текстовый материал',
  PDF: 'PDF',
  DOCX: 'DOCX',
  VIDEO: 'Видео',
};

const roleLabels: Record<string, string> = {
  ADMIN: 'Администратор',
  EXPERT: 'Эксперт',
  LEARNER: 'Слушатель',
  MANAGER: 'Руководитель',
  OPERATOR: 'Оператор',
  ROLE_USER: 'Пользователь',
  ROLE_MANAGER: 'Руководитель',
  ROLE_OPERATIONS: 'Операционный персонал',
  SYSTEM_ADMIN: 'System Admin',
  SUPER_ADMIN: 'Super Admin',
};

const contentStatusLabels: Record<string, string> = {
  DRAFT: 'Черновик',
  PUBLISHED: 'Опубликовано',
  ARCHIVED: 'Архив',
};

const organizationalNodeKindLabels: Record<string, string> = {
  LINEAR: 'Линейный',
  FUNCTIONAL: 'Функциональный',
};

const organizationAssignmentTypeLabels: Record<string, string> = {
  PRIMARY: 'Основная',
  SECONDARY: 'Дополнительная',
  TEMPORARY: 'Временная',
};

const accessScopeTypeLabels: Record<string, string> = {
  GLOBAL: 'Глобальная',
  UNIT_ONLY: 'Только узел',
  UNIT_SUBTREE: 'Узел и поддерево',
};

const userStatusLabels: Record<string, string> = {
  ACTIVE: 'Активный',
  INACTIVE: 'Неактивный',
};

const organizationalUnitStatusLabels: Record<string, string> = {
  ACTIVE: 'Активный',
  ARCHIVED: 'В архиве',
};

const testTypeLabels: Record<string, string> = {
  CONTROL: 'Контрольный',
  TRAINING: 'Тренировочный',
  ENTRANCE: 'Входной',
  AUXILIARY: 'Вспомогательный',
  ALL_QUESTIONS: 'Все вопросы',
};

const answerOptionRoleLabels: Record<string, string> = {
  CHOICE_OPTION: 'Вариант выбора',
  MATCH_LEFT: 'Левая часть',
  MATCH_RIGHT: 'Правая часть',
  ORDER_ITEM: 'Элемент порядка',
};

const notificationStatusLabels: Record<string, string> = {
  PENDING: 'Ожидает отправки',
  SENT: 'Отправлено',
  FAILED: 'Ошибка',
  SKIPPED: 'Пропущено',
};

const importJobStatusLabels: Record<string, string> = {
  PENDING: 'В очереди',
  IN_PROGRESS: 'В работе',
  COMPLETED: 'Завершено',
  COMPLETED_WITH_ERRORS: 'Завершено с ошибками',
  FAILED: 'Завершилось с ошибкой',
};

const importItemStatusLabels: Record<string, string> = {
  PENDING: 'В очереди',
  PROCESSING: 'Обрабатывается',
  APPLIED: 'Применено',
  NO_CHANGE: 'Без изменений',
  FAILED: 'Ошибка',
  REQUIRES_REVIEW: 'Требует review',
};

export function formatUiDate(value?: string, empty = 'Не указано'): string {
  if (!value) {
    return empty;
  }

  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('DD.MM.YYYY HH:mm') : value;
}

export function localizeStatus(status?: string, empty = 'Не указано'): string {
  if (!status) {
    return empty;
  }

  return statusLabels[status] ?? status;
}

export function getStatusColor(status?: string): string | undefined {
  if (!status) {
    return undefined;
  }

  return statusColors[status];
}

export function localizeQuestionType(type?: string, empty = 'Не указано'): string {
  if (!type) {
    return empty;
  }

  return questionTypeLabels[type] ?? type;
}

export function localizeMaterialType(type?: string, empty = 'Материал'): string {
  if (!type) {
    return empty;
  }

  return materialTypeLabels[type] ?? type;
}

export function localizeContentStatus(status?: string, empty = 'Не указано'): string {
  if (!status) {
    return empty;
  }

  return contentStatusLabels[status] ?? status;
}

export function localizeRole(role?: string, empty = 'Пользователь'): string {
  if (!role) {
    return empty;
  }

  return roleLabels[role] ?? role;
}

export function localizeOrganizationalNodeKind(kind?: string, empty = 'Не указано'): string {
  if (!kind) {
    return empty;
  }

  return organizationalNodeKindLabels[kind] ?? kind;
}

export function localizeOrganizationAssignmentType(type?: string, empty = 'Не указано'): string {
  if (!type) {
    return empty;
  }

  return organizationAssignmentTypeLabels[type] ?? type;
}

export function localizeAccessScopeType(type?: string, empty = 'Не указано'): string {
  if (!type) {
    return empty;
  }

  return accessScopeTypeLabels[type] ?? type;
}

export function localizeUserStatus(status?: string, empty = 'Не указано'): string {
  if (!status) {
    return empty;
  }

  return userStatusLabels[status] ?? status;
}

export function localizeOrganizationalUnitStatus(status?: string, empty = 'Не указано'): string {
  if (!status) {
    return empty;
  }

  return organizationalUnitStatusLabels[status] ?? status;
}

export function localizeTestType(type?: string, empty = 'Не указано'): string {
  if (!type) {
    return empty;
  }

  return testTypeLabels[type] ?? type;
}

export function localizeAnswerOptionRole(role?: string, empty = 'Не указано'): string {
  if (!role) {
    return empty;
  }

  return answerOptionRoleLabels[role] ?? role;
}

export function localizeNotificationStatus(status?: string, empty = 'Не указано'): string {
  if (!status) {
    return empty;
  }

  return notificationStatusLabels[status] ?? status;
}

export function localizeImportJobStatus(status?: string, empty = 'Не указано'): string {
  if (!status) {
    return empty;
  }

  return importJobStatusLabels[status] ?? status;
}

export function localizeImportItemStatus(status?: string, empty = 'Не указано'): string {
  if (!status) {
    return empty;
  }

  return importItemStatusLabels[status] ?? status;
}

export function formatPercent(value?: string | number | null, empty = 'Не указано'): string {
  if (value == null || value === '') {
    return empty;
  }

  const normalized = String(value).replace('%', '').trim();
  if (!normalized) {
    return empty;
  }

  const numericValue = Number(normalized);
  if (Number.isNaN(numericValue)) {
    return `${normalized}%`;
  }

  return `${numericValue.toFixed(2).replace(/\.?0+$/, '')}%`;
}

export function formatPassedLabel(passed?: boolean): string {
  if (passed == null) {
    return 'Статус не указан';
  }

  return passed ? 'Тест пройден' : 'Тест не пройден';
}

export function formatText(value?: string | number | null, empty = 'Не указано'): string {
  if (value == null || value === '') {
    return empty;
  }

  return String(value);
}

export function formatBoolean(
  value?: boolean | null,
  trueLabel = 'Да',
  falseLabel = 'Нет',
  empty = 'Не указано',
): string {
  if (value == null) {
    return empty;
  }

  return value ? trueLabel : falseLabel;
}

export function formatJson(value: unknown): string {
  if (value == null) {
    return '';
  }

  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }

  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}
