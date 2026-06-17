export function localizeNotificationType(type?: string | null): string {
  switch (type) {
    case 'assignment_campaign_assigned':
      return 'Новое назначение на обучение';
    default:
      return type?.trim() || 'Уведомление';
  }
}

export function localizeNotificationChannel(channel?: string | null): string {
  switch (channel) {
    case 'IN_APP':
      return 'В приложении';
    case 'EMAIL':
      return 'Электронная почта';
    case 'SMS':
      return 'SMS';
    default:
      return channel?.trim() || 'Канал не указан';
  }
}

export function localizeNotificationSource(
  sourceEntityType?: string | null,
  sourceEntityId?: string | number | null,
): string {
  const sourceLabel =
    sourceEntityType === 'assignment_campaign'
      ? 'Назначение'
      : sourceEntityType?.trim() || 'Источник';

  if (sourceEntityId == null || sourceEntityId === '') {
    return sourceLabel;
  }

  return `${sourceLabel} №${sourceEntityId}`;
}
