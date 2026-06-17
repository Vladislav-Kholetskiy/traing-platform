import { Descriptions } from 'antd';
import { useParams } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAdminNotification } from '../../features/notifications/model/useNotifications';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatText, formatUiDate, localizeNotificationStatus } from '../../shared/ui/presentation';

export function AdminNotificationDetailPage() {
  const { notificationId: notificationIdParam } = useParams();
  const notificationId = Number(notificationIdParam);
  const { data: actor } = useCurrentActor();
  const notificationQuery = useAdminNotification(notificationId, Boolean(actor));

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }
  if (notificationQuery.isLoading) {
    return <LoadingView title="Загрузка уведомления" />;
  }
  if (notificationQuery.isError) {
    return <ErrorView title="Не удалось загрузить уведомление" error={notificationQuery.error} />;
  }

  const notification = notificationQuery.data;
  if (!notification) {
    return <ErrorView title="Уведомление не найдено" />;
  }

  return (
    <>
      <PageIntro
        title={`Уведомление #${notification.id}`}
        description="Подробная карточка уведомления и его состояния доставки."
      />
      <SectionCard title="Карточка уведомления">
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Получатель">{formatText(notification.recipientUserId)}</Descriptions.Item>
          <Descriptions.Item label="Статус">
            {localizeNotificationStatus(notification.status ?? undefined)}
          </Descriptions.Item>
          <Descriptions.Item label="Тип уведомления">
            {localizeNotificationType(notification.notificationType)}
          </Descriptions.Item>
          <Descriptions.Item label="Канал">{formatText(notification.channelCode)}</Descriptions.Item>
          <Descriptions.Item label="Источник">
            {`${localizeNotificationSourceType(notification.sourceEntityType)} / ${formatText(notification.sourceEntityId)}`}
          </Descriptions.Item>
          <Descriptions.Item label="Запланировано">
            {formatUiDate(notification.scheduledAt ?? undefined)}
          </Descriptions.Item>
          <Descriptions.Item label="Отправлено">
            {formatUiDate(notification.sentAt ?? undefined, 'Не отправлено')}
          </Descriptions.Item>
          <Descriptions.Item label="Прочитано">
            {formatUiDate(notification.readAt ?? undefined, 'Нет')}
          </Descriptions.Item>
          <Descriptions.Item label="Попыток отправки">
            {formatText(notification.deliveryAttemptCount)}
          </Descriptions.Item>
          <Descriptions.Item label="Код ошибки">{formatText(notification.errorCode)}</Descriptions.Item>
        </Descriptions>
      </SectionCard>
    </>
  );
}

function localizeNotificationType(notificationType?: string | null): string {
  switch (notificationType) {
    case 'assignment_assigned_manager_notice':
      return 'Назначение сотруднику';
    case 'assignment_deadline_manager_notice':
      return 'Напоминание о сроке назначения';
    default:
      return notificationType?.trim() || 'Уведомление';
  }
}

function localizeNotificationSourceType(sourceType?: string | null): string {
  switch (sourceType?.trim().toUpperCase()) {
    case 'ASSIGNMENT':
      return 'Назначение';
    case 'ASSIGNMENT_CAMPAIGN':
      return 'Кампания назначений';
    default:
      return sourceType?.trim() || 'Источник';
  }
}
