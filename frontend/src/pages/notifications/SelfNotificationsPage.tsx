import { Button, List, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router';
import type {
  SelfNotification,
  SelfNotificationAssignmentRecipient,
} from '../../features/notifications/model/notifications';
import { localizeNotificationChannel } from '../../features/notifications/model/notificationPresentation';
import {
  useMarkAllSelfNotificationsRead,
  useMarkSelfNotificationRead,
  useSelfNotifications,
} from '../../features/notifications/model/useNotifications';
import { ErrorView } from '../../shared/ui/ErrorView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate } from '../../shared/ui/presentation';

type SelfNotificationTableRow = {
  key: string;
  id?: number;
  title: string;
  message?: string | null;
  channelCode?: string | null;
  createdAt?: string | null;
  readAt?: string | null;
  read?: boolean | null;
  groupCompanyName?: string | null;
  groupedAssignments?: SelfNotificationAssignmentRecipient[];
};

type ParsedManagerAssignmentNotification = {
  companyName?: string | null;
  recipient: SelfNotificationAssignmentRecipient;
};

export function SelfNotificationsPage() {
  const notificationsQuery = useSelfNotifications();
  const markReadMutation = useMarkSelfNotificationRead();
  const markAllReadMutation = useMarkAllSelfNotificationsRead();

  if (notificationsQuery.isLoading) {
    return <LoadingView title="Загрузка уведомлений" description="Подготавливаем вашу ленту уведомлений." />;
  }

  if (notificationsQuery.isError) {
    return <ErrorView title="Не удалось загрузить уведомления" error={notificationsQuery.error} />;
  }

  const notifications = notificationsQuery.data ?? [];
  const rows = buildNotificationRows(notifications);
  const unreadCount = notifications.filter((notification) => !notification.read).length;

  const columns: ColumnsType<SelfNotificationTableRow> = [
    {
      title: 'Событие',
      render: (_, item) => (
        <Space direction="vertical" size={0}>
          {item.id ? (
            <Link to={`/learner/notifications/${item.id}`}>{item.title}</Link>
          ) : (
            <Typography.Text strong>{item.title}</Typography.Text>
          )}
          {item.message ? <Typography.Text>{item.message}</Typography.Text> : null}
          <Space size={8} wrap>
            <Typography.Text type="secondary">{localizeNotificationChannel(item.channelCode)}</Typography.Text>
            <Tag color={item.read ? 'default' : 'blue'}>{item.read ? 'Прочитано' : 'Новое'}</Tag>
          </Space>
        </Space>
      ),
    },
    { title: 'Получено', dataIndex: 'createdAt', render: (value) => formatUiDate(value ?? undefined) },
    {
      title: '',
      width: 140,
      render: (_, item) =>
        item.id && !item.read ? (
          <Button
            size="small"
            onClick={() => markReadMutation.mutate(item.id as number)}
            loading={markReadMutation.isPending && markReadMutation.variables === item.id}
          >
            Прочитано
          </Button>
        ) : null,
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title="Мои уведомления"
        description="Здесь отображаются важные события: новые назначения, напоминания о сроках и другие рабочие сообщения."
      />
      <SectionCard
        title="Список уведомлений"
        extra={
          <Button
            onClick={() => markAllReadMutation.mutate()}
            disabled={unreadCount === 0}
            loading={markAllReadMutation.isPending}
          >
            Прочитать все
          </Button>
        }
      >
        <Table
          rowKey="key"
          dataSource={rows}
          columns={columns}
          pagination={{ pageSize: 10 }}
          expandable={{
            rowExpandable: (item) => Boolean(item.groupedAssignments && item.groupedAssignments.length > 1),
            expandedRowRender: (item) =>
              item.groupedAssignments && item.groupedAssignments.length > 1 ? (
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  <Typography.Text strong>
                    {item.groupCompanyName ? `Сотрудники компании ${item.groupCompanyName}` : 'Список сотрудников'}
                  </Typography.Text>
                  <List
                    dataSource={item.groupedAssignments}
                    renderItem={(assignment) => (
                      <List.Item>
                        <Space direction="vertical" size={0}>
                          <Typography.Text>{assignment.fullName ?? 'Сотрудник'}</Typography.Text>
                          {assignment.courseName ? (
                            <Typography.Text type="secondary">{assignment.courseName}</Typography.Text>
                          ) : null}
                        </Space>
                      </List.Item>
                    )}
                  />
                </Space>
              ) : null,
          }}
        />
      </SectionCard>
    </Space>
  );
}

function buildNotificationRows(notifications: SelfNotification[]): SelfNotificationTableRow[] {
  const groupedRowsByKey = new Map<string, SelfNotificationTableRow>();
  const groupedRows: SelfNotificationTableRow[] = [];
  const plainRows: SelfNotificationTableRow[] = [];

  for (const notification of notifications) {
    const groupingCandidate = getGroupingCandidate(notification);
    if (!groupingCandidate) {
      plainRows.push(toPlainRow(notification));
      continue;
    }

    const companyName = groupingCandidate.companyName ?? null;
    const groupKey = buildGroupingKey(notification, groupingCandidate.companyName);
    const existingGroup = groupedRowsByKey.get(groupKey);

    if (existingGroup) {
      const mergedAssignments = deduplicateAssignments([
        ...(existingGroup.groupedAssignments ?? []),
        groupingCandidate.recipient,
      ]);
      existingGroup.groupedAssignments = mergedAssignments;
      existingGroup.message = buildAssignmentGroupMessage(companyName, mergedAssignments);
      existingGroup.read = Boolean(existingGroup.read) && Boolean(notification.read);
      continue;
    }

    const groupRow: SelfNotificationTableRow = {
      key: groupKey,
      title: notification.title || 'Назначение сотруднику',
      message: buildAssignmentGroupMessage(companyName, [groupingCandidate.recipient]),
      channelCode: notification.channelCode,
      createdAt: notification.createdAt,
      readAt: notification.readAt,
      read: notification.read,
      groupCompanyName: groupingCandidate.companyName,
      groupedAssignments: [groupingCandidate.recipient],
    };
    groupedRowsByKey.set(groupKey, groupRow);
    groupedRows.push(groupRow);
  }

  return [...groupedRows, ...plainRows].sort(compareNotificationRows);
}

function toPlainRow(notification: SelfNotification): SelfNotificationTableRow {
  return {
    key: String(notification.id),
    id: notification.id,
    title: notification.title || 'Уведомление',
    message: notification.message,
    channelCode: notification.channelCode,
    createdAt: notification.createdAt,
    readAt: notification.readAt,
    read: notification.read,
  };
}

function getGroupingCandidate(notification: SelfNotification): ParsedManagerAssignmentNotification | null {
  if (notification.notificationType === 'assignment_campaign_manager_notice' && notification.assignmentRecipients?.length) {
    const companyName = notification.companyName ?? notification.assignmentRecipients[0]?.companyName ?? null;
    const primaryRecipient = notification.assignmentRecipients[0];
    return primaryRecipient
      ? {
          companyName,
          recipient: primaryRecipient,
        }
      : null;
  }

  if (notification.notificationType === 'assignment_assigned_manager_notice' && notification.assignmentRecipients?.length) {
    const primaryRecipient = notification.assignmentRecipients[0];
    return primaryRecipient
      ? {
          companyName: notification.companyName ?? primaryRecipient.companyName ?? null,
          recipient: primaryRecipient,
        }
      : null;
  }

  return parseManagerAssignmentNotification(notification);
}

function parseManagerAssignmentNotification(notification: SelfNotification): ParsedManagerAssignmentNotification | null {
  const title = notification.title?.trim();
  const message = notification.message?.trim();
  if (title !== 'Назначение сотруднику' || !message) {
    return null;
  }

  const parsed = message.match(/^Сотруднику\s+(.+?)\s+назначено обучение\s+"(.+)"\.$/u);
  if (!parsed) {
    return null;
  }

  return {
    companyName: notification.companyName,
    recipient: {
      fullName: parsed[1],
      courseName: parsed[2],
      companyName: notification.companyName,
    },
  };
}

function buildGroupingKey(notification: SelfNotification, companyName?: string | null): string {
  const timestampKey = normalizeCreatedAt(notification.createdAt) ?? String(notification.id);
  const companyKey = companyName?.trim() || 'companyless';
  return `manager-assignment:${timestampKey}:${companyKey}`;
}

function normalizeCreatedAt(createdAt?: string | null): string | null {
  if (!createdAt) {
    return null;
  }

  const parsedDate = new Date(createdAt);
  if (Number.isNaN(parsedDate.getTime())) {
    return createdAt;
  }

  parsedDate.setSeconds(0, 0);
  return parsedDate.toISOString();
}

function deduplicateAssignments(
  assignments: SelfNotificationAssignmentRecipient[]
): SelfNotificationAssignmentRecipient[] {
  const uniqueAssignments = new Map<string, SelfNotificationAssignmentRecipient>();

  for (const assignment of assignments) {
    const key = `${assignment.userId ?? 'nouser'}:${assignment.fullName ?? 'noname'}:${assignment.courseName ?? 'nocourse'}`;
    uniqueAssignments.set(key, assignment);
  }

  return [...uniqueAssignments.values()];
}

function buildAssignmentGroupMessage(
  companyName: string | null | undefined,
  assignments: SelfNotificationAssignmentRecipient[]
): string {
  const uniqueEmployees = new Set(assignments.map((assignment) => assignment.userId ?? assignment.fullName ?? 'unknown'));
  if (companyName && companyName.trim()) {
    return `В компании ${companyName} назначение получили ${uniqueEmployees.size} ${pluralizeEmployees(uniqueEmployees.size)}.`;
  }

  return `Назначение получили ${uniqueEmployees.size} ${pluralizeEmployees(uniqueEmployees.size)}.`;
}

function pluralizeEmployees(count: number): string {
  const remainder10 = count % 10;
  const remainder100 = count % 100;

  if (remainder10 === 1 && remainder100 !== 11) {
    return 'сотрудник';
  }
  if (remainder10 >= 2 && remainder10 <= 4 && (remainder100 < 12 || remainder100 > 14)) {
    return 'сотрудника';
  }
  return 'сотрудников';
}

function compareNotificationRows(left: SelfNotificationTableRow, right: SelfNotificationTableRow): number {
  const leftTime = left.createdAt ? Date.parse(left.createdAt) : 0;
  const rightTime = right.createdAt ? Date.parse(right.createdAt) : 0;

  if (leftTime !== rightTime) {
    return rightTime - leftTime;
  }

  return left.key.localeCompare(right.key);
}
