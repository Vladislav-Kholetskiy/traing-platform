import { apiClient } from '../../../shared/api/apiClient';
import type {
  AdminNotification,
  DispatchNotificationsResult,
  MarkAllNotificationsReadResult,
  SelfNotification,
} from '../model/notifications';

type NotificationChannelCodeDto =
  | string
  | {
      value?: string | null;
    }
  | null;

type SelfNotificationDto = Omit<SelfNotification, 'channelCode'> & {
  channelCode?: NotificationChannelCodeDto;
};

type AdminNotificationDto = Omit<AdminNotification, 'channelCode'> & {
  channelCode?: NotificationChannelCodeDto;
};

function normalizeChannelCode(channelCode?: NotificationChannelCodeDto): string | null | undefined {
  if (channelCode == null || typeof channelCode === 'string') {
    return channelCode;
  }

  return channelCode.value ?? null;
}

function mapSelfNotification(dto: SelfNotificationDto): SelfNotification {
  return {
    ...dto,
    channelCode: normalizeChannelCode(dto.channelCode),
  };
}

function mapAdminNotification(dto: AdminNotificationDto): AdminNotification {
  return {
    ...dto,
    channelCode: normalizeChannelCode(dto.channelCode),
  };
}

export function getSelfNotifications() {
  return apiClient
    .get<SelfNotificationDto[]>('/api/v1/self/notifications')
    .then((response) => response.map(mapSelfNotification));
}

export function getSelfNotification(notificationId: number) {
  return apiClient
    .get<SelfNotificationDto>(`/api/v1/self/notifications/${notificationId}`)
    .then(mapSelfNotification);
}

export function markSelfNotificationRead(notificationId: number) {
  return apiClient
    .post<SelfNotificationDto>(`/api/v1/self/notifications/${notificationId}/read`)
    .then(mapSelfNotification);
}

export function markAllSelfNotificationsRead() {
  return apiClient.post<MarkAllNotificationsReadResult>('/api/v1/self/notifications/read-all');
}

export function getAdminNotifications() {
  return apiClient
    .get<AdminNotificationDto[]>('/api/v1/admin/notifications')
    .then((response) => response.map(mapAdminNotification));
}

export function getAdminNotification(notificationId: number) {
  return apiClient
    .get<AdminNotificationDto>(`/api/v1/admin/notifications/${notificationId}`)
    .then(mapAdminNotification);
}

export function dispatchPendingNotifications(limit = 100) {
  return apiClient.post<DispatchNotificationsResult>(`/api/v1/admin/notifications/dispatch-pending?limit=${limit}`);
}
