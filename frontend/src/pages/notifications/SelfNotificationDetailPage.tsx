import { Button, Descriptions, Space, Tag } from 'antd';
import { useParams } from 'react-router';
import { localizeNotificationChannel } from '../../features/notifications/model/notificationPresentation';
import {
  useMarkSelfNotificationRead,
  useSelfNotification,
} from '../../features/notifications/model/useNotifications';
import { ErrorView } from '../../shared/ui/ErrorView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate } from '../../shared/ui/presentation';

export function SelfNotificationDetailPage() {
  const { notificationId: notificationIdParam } = useParams();
  const notificationId = Number(notificationIdParam);
  const notificationQuery = useSelfNotification(notificationId);
  const markReadMutation = useMarkSelfNotificationRead();

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
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title={notification.title || 'Уведомление'}
        description={notification.message || 'Карточка уведомления показывает суть события без технических деталей доставки.'}
      />
      <SectionCard
        title="Детали уведомления"
        extra={
          notification.read ? (
            <Tag>Прочитано</Tag>
          ) : (
            <Button
              onClick={() => markReadMutation.mutate(notification.id)}
              loading={markReadMutation.isPending}
            >
              Прочитано
            </Button>
          )
        }
      >
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Событие">{notification.title || 'Уведомление'}</Descriptions.Item>
          <Descriptions.Item label="Описание">{notification.message || 'Описание не указано'}</Descriptions.Item>
          <Descriptions.Item label="Канал">{localizeNotificationChannel(notification.channelCode)}</Descriptions.Item>
          <Descriptions.Item label="Получено">{formatUiDate(notification.createdAt ?? undefined)}</Descriptions.Item>
          <Descriptions.Item label="Прочитано">{formatUiDate(notification.readAt ?? undefined, 'Нет')}</Descriptions.Item>
        </Descriptions>
      </SectionCard>
    </Space>
  );
}
