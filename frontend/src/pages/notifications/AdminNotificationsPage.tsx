import { Button, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import {
  useAdminNotifications,
  useDispatchPendingNotifications,
} from '../../features/notifications/model/useNotifications';
import type { AdminUser } from '../../features/admin-users/model/adminUsers';
import { useAdminUsers } from '../../features/admin-users/model/useAdminUsers';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeNotificationStatus } from '../../shared/ui/presentation';

const UI_TEXT = {
  loadingTitle: '\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u0439',
  errorTitle: '\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f',
  pageTitle: '\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f',
  pageDescription:
    '\u0416\u0443\u0440\u043d\u0430\u043b \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u043d\u044b\u0445 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439 \u0438 \u0442\u0435\u043a\u0443\u0449\u0430\u044f \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u0434\u043e\u0441\u0442\u0430\u0432\u043a\u0438.',
  processQueue: '\u041e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u0442\u044c \u043e\u0447\u0435\u0440\u0435\u0434\u044c',
  pending: '\u0412 \u043e\u0447\u0435\u0440\u0435\u0434\u0438',
  sent: '\u041e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u043e',
  failed: '\u0421 \u043e\u0448\u0438\u0431\u043a\u043e\u0439',
  latestTitle: '\u041f\u043e\u0441\u043b\u0435\u0434\u043d\u0438\u0435 \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f',
  latestDescription:
    '\u041f\u043e\u0441\u043b\u0435\u0434\u043d\u0438\u0435 \u0441\u043e\u0431\u044b\u0442\u0438\u044f \u043f\u043e \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f\u043c \u0438 \u043d\u0430\u043f\u043e\u043c\u0438\u043d\u0430\u043d\u0438\u044f\u043c \u0432 \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u043c \u043a\u043e\u043d\u0442\u0443\u0440\u0435.',
  recipient: '\u041f\u043e\u043b\u0443\u0447\u0430\u0442\u0435\u043b\u044c',
  source: '\u0418\u0441\u0442\u043e\u0447\u043d\u0438\u043a',
  notSpecified: '\u043d\u0435 \u0443\u043a\u0430\u0437\u0430\u043d',
  read: '\u041f\u0440\u043e\u0447\u0438\u0442\u0430\u043d\u043e',
  unread: '\u041d\u0435 \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u043d\u043e',
  notification: '\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u0435',
  assignmentToEmployee: '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u0441\u043e\u0442\u0440\u0443\u0434\u043d\u0438\u043a\u0443',
  deadlineReminder: '\u041d\u0430\u043f\u043e\u043c\u0438\u043d\u0430\u043d\u0438\u0435 \u043e \u0441\u0440\u043e\u043a\u0435 \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f',
  assignmentOverdue: '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043f\u0440\u043e\u0441\u0440\u043e\u0447\u0435\u043d\u043e',
  assignmentCampaign: '\u041a\u0430\u043c\u043f\u0430\u043d\u0438\u044f \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0439',
  assignmentCancelled: '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043e\u0442\u043c\u0435\u043d\u0435\u043d\u043e',
  assignmentExtended: '\u0421\u0440\u043e\u043a \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f \u043f\u0440\u043e\u0434\u043b\u0451\u043d',
  assignmentReplaced: '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u0437\u0430\u043c\u0435\u043d\u0435\u043d\u043e',
  assignment: '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435',
  sourceFallback: '\u0418\u0441\u0442\u043e\u0447\u043d\u0438\u043a',
};

export function AdminNotificationsPage() {
  const { data: actor } = useCurrentActor();
  const notificationsQuery = useAdminNotifications(Boolean(actor));
  const usersQuery = useAdminUsers(undefined, Boolean(actor));
  const dispatchMutation = useDispatchPendingNotifications();

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }
  if (notificationsQuery.isLoading) {
    return <LoadingView title={UI_TEXT.loadingTitle} />;
  }
  if (usersQuery.isLoading) {
    return <LoadingView title={UI_TEXT.loadingTitle} />;
  }
  if (notificationsQuery.isError) {
    return <ErrorView title={UI_TEXT.errorTitle} error={notificationsQuery.error} />;
  }
  if (usersQuery.isError) {
    return <ErrorView title={UI_TEXT.errorTitle} error={usersQuery.error} />;
  }

  const notifications = notificationsQuery.data ?? [];
  const users = usersQuery.data ?? [];
  const usersById = new Map(users.map((user) => [user.id, user] as const));
  const pendingCount = notifications.filter((item) => item.status === 'PENDING').length;
  const sentCount = notifications.filter((item) => item.status === 'SENT').length;
  const failedCount = notifications.filter((item) => item.status === 'FAILED').length;

  return (
    <Space direction="vertical" size={18} style={{ width: '100%' }} className="admin-notifications-page">
      <PageIntro
        title={UI_TEXT.pageTitle}
        description={UI_TEXT.pageDescription}
        extra={
          <Button type="primary" onClick={() => dispatchMutation.mutate(100)} loading={dispatchMutation.isPending}>
            {UI_TEXT.processQueue}
          </Button>
        }
      />

      <section className="admin-notifications-summary">
        <NotificationMetricCard label={UI_TEXT.pending} value={pendingCount} tone="queue" />
        <NotificationMetricCard label={UI_TEXT.sent} value={sentCount} tone="sent" />
        <NotificationMetricCard label={UI_TEXT.failed} value={failedCount} tone="failed" />
      </section>

      <SectionCard title={UI_TEXT.latestTitle} description={UI_TEXT.latestDescription}>
        <div className="admin-notifications-feed">
          {notifications.map((notification) => (
            <Link
              key={notification.id}
              to={`/admin/notifications/${notification.id}`}
              className="admin-notifications-item"
            >
              <div className="admin-notifications-item-main">
                <div className="admin-notifications-item-topline">
                  <Typography.Text strong>{localizeNotificationType(notification.notificationType)}</Typography.Text>
                  <Tag color={resolveNotificationTagColor(notification.status)} bordered={false}>
                    {localizeNotificationStatus(notification.status ?? undefined)}
                  </Tag>
                </div>
                <Typography.Paragraph className="admin-notifications-item-meta">
                  {`${UI_TEXT.recipient}: ${formatNotificationRecipient(notification.recipientUserId, usersById)}`}
                </Typography.Paragraph>
                <Typography.Paragraph className="admin-notifications-item-meta">
                  {`${UI_TEXT.source}: ${localizeNotificationSourceType(notification.sourceEntityType)} / ${notification.sourceEntityId ?? UI_TEXT.notSpecified}`}
                </Typography.Paragraph>
              </div>

              <div className="admin-notifications-item-side">
                <Typography.Text className="admin-notifications-item-date">
                  {formatUiDate(notification.createdAt ?? undefined)}
                </Typography.Text>
                <Typography.Text type="secondary" className="admin-notifications-item-read">
                  {notification.readAt ? `${UI_TEXT.read} ${formatUiDate(notification.readAt)}` : UI_TEXT.unread}
                </Typography.Text>
              </div>
            </Link>
          ))}
        </div>
      </SectionCard>
    </Space>
  );
}

function formatNotificationRecipient(recipientUserId: number | null | undefined, usersById: Map<number, AdminUser>): string {
  if (!recipientUserId) {
    return UI_TEXT.notSpecified;
  }

  const user = usersById.get(recipientUserId);
  if (!user) {
    return `ID ${recipientUserId}`;
  }

  const fullName = [user.lastName, user.firstName, user.middleName].filter(Boolean).join(' ').trim();
  if (fullName && user.employeeNumber) {
    return `${fullName} • ${user.employeeNumber}`;
  }

  return fullName || user.employeeNumber || `ID ${recipientUserId}`;
}

function NotificationMetricCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: 'queue' | 'sent' | 'failed';
}) {
  return (
    <div className={`admin-notifications-metric admin-notifications-metric-${tone}`}>
      <span className="admin-notifications-metric-label">{label}</span>
      <strong className="admin-notifications-metric-value">{value}</strong>
    </div>
  );
}

function localizeNotificationType(notificationType?: string | null): string {
  switch (notificationType) {
    case 'assignment_assigned_manager_notice':
      return UI_TEXT.assignmentToEmployee;
    case 'assignment_deadline_manager_notice':
    case 'assignment_deadline_reminder_7d_manager_notice':
      return UI_TEXT.deadlineReminder;
    case 'assignment_overdue_manager_notice':
      return UI_TEXT.assignmentOverdue;
    case 'assignment_campaign_manager_notice':
      return UI_TEXT.assignmentCampaign;
    case 'assignment_cancelled_manager_notice':
      return UI_TEXT.assignmentCancelled;
    case 'assignment_deadline_extended_manager_notice':
      return UI_TEXT.assignmentExtended;
    case 'assignment_replaced_manager_notice':
      return UI_TEXT.assignmentReplaced;
    default:
      return notificationType?.trim() || UI_TEXT.notification;
  }
}

function localizeNotificationSourceType(sourceType?: string | null): string {
  switch (sourceType?.trim().toUpperCase()) {
    case 'ASSIGNMENT':
      return UI_TEXT.assignment;
    case 'ASSIGNMENT_CAMPAIGN':
      return UI_TEXT.assignmentCampaign;
    default:
      return sourceType?.trim() || UI_TEXT.sourceFallback;
  }
}

function resolveNotificationTagColor(status?: string | null): string {
  switch (status) {
    case 'SENT':
      return 'success';
    case 'FAILED':
      return 'error';
    case 'PENDING':
      return 'gold';
    default:
      return 'default';
  }
}
