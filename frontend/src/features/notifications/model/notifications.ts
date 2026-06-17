export type SelfNotificationAssignmentRecipient = {
  userId?: number | null;
  fullName?: string | null;
  courseName?: string | null;
  companyName?: string | null;
  organizationalUnitName?: string | null;
};

export type SelfNotification = {
  id: number;
  title?: string | null;
  message?: string | null;
  channelCode?: string | null;
  createdAt?: string | null;
  readAt?: string | null;
  read?: boolean | null;
  notificationType?: string | null;
  companyName?: string | null;
  assignmentRecipients?: SelfNotificationAssignmentRecipient[] | null;
};

export type AdminNotification = {
  id: number;
  recipientUserId?: number | null;
  notificationType?: string | null;
  channelCode?: string | null;
  status?: string | null;
  sourceEntityType?: string | null;
  sourceEntityId?: string | number | null;
  scheduledAt?: string | null;
  sentAt?: string | null;
  readAt?: string | null;
  deliveryAttemptCount?: number | null;
  errorCode?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type MarkAllNotificationsReadResult = {
  updatedCount: number;
  readAt?: string | null;
};

export type DispatchNotificationsResult = {
  processedCount: number;
};
