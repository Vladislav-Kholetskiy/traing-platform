import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  dispatchPendingNotifications,
  getAdminNotification,
  getAdminNotifications,
  getSelfNotification,
  getSelfNotifications,
  markAllSelfNotificationsRead,
  markSelfNotificationRead,
} from '../api/notificationsApi';

export const notificationsKeys = {
  all: ['notifications'] as const,
  selfList: () => [...notificationsKeys.all, 'self', 'list'] as const,
  selfDetail: (id: number) => [...notificationsKeys.all, 'self', 'detail', id] as const,
  adminList: () => [...notificationsKeys.all, 'admin', 'list'] as const,
  adminDetail: (id: number) => [...notificationsKeys.all, 'admin', 'detail', id] as const,
};

export function useSelfNotifications(enabled = true) {
  return useQuery({
    queryKey: notificationsKeys.selfList(),
    queryFn: getSelfNotifications,
    enabled,
  });
}

export function useSelfNotification(notificationId?: number, enabled = true) {
  return useQuery({
    queryKey: notificationId
      ? notificationsKeys.selfDetail(notificationId)
      : [...notificationsKeys.all, 'self', 'detail', 'missing'],
    queryFn: () => getSelfNotification(notificationId as number),
    enabled: enabled && Boolean(notificationId),
  });
}

export function useAdminNotifications(enabled = true) {
  return useQuery({
    queryKey: notificationsKeys.adminList(),
    queryFn: getAdminNotifications,
    enabled,
  });
}

export function useAdminNotification(notificationId?: number, enabled = true) {
  return useQuery({
    queryKey: notificationId
      ? notificationsKeys.adminDetail(notificationId)
      : [...notificationsKeys.all, 'admin', 'detail', 'missing'],
    queryFn: () => getAdminNotification(notificationId as number),
    enabled: enabled && Boolean(notificationId),
  });
}

export function useMarkSelfNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markSelfNotificationRead,
    onSuccess: (notification) => {
      queryClient.invalidateQueries({ queryKey: notificationsKeys.selfList() });
      queryClient.setQueryData(notificationsKeys.selfDetail(notification.id), notification);
    },
  });
}

export function useMarkAllSelfNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markAllSelfNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationsKeys.selfList() });
    },
  });
}

export function useDispatchPendingNotifications() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: dispatchPendingNotifications,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationsKeys.adminList() });
    },
  });
}
