import { Card, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { formatAssignmentDate } from '../../assigned-learning/model/assignedLearning';
import { formatPercent, formatText } from '../../../shared/ui/presentation';
import type { ManagerialUserTopicAnalyticsItem } from '../model/managerialAnalytics';

type ManagerialUserTopicAnalyticsTableProps = {
  items: ManagerialUserTopicAnalyticsItem[];
};

export function ManagerialUserTopicAnalyticsTable({
  items,
}: ManagerialUserTopicAnalyticsTableProps) {
  const columns: ColumnsType<ManagerialUserTopicAnalyticsItem> = [
    {
      title: 'Сотрудник / тема',
      key: 'subject',
      render: (_, item) => (
        <>
          <Typography.Text strong>{item.userDisplayName ?? 'Сотрудник не указан'}</Typography.Text>
          <br />
          <Typography.Text type="secondary">{item.topicName ?? 'Тема не указана'}</Typography.Text>
        </>
      ),
    },
    {
      title: 'Средний результат',
      dataIndex: 'averageScorePercent',
      key: 'averageScorePercent',
      render: (value?: string) => formatPercent(value),
    },
    {
      title: 'Процент прохождения',
      dataIndex: 'passRatePercent',
      key: 'passRatePercent',
      render: (value?: string) => formatPercent(value),
    },
    {
      title: 'Попытки',
      dataIndex: 'attemptCount',
      key: 'attemptCount',
      render: (value?: number) => formatText(value),
    },
    {
      title: 'Ошибки',
      dataIndex: 'errorCount',
      key: 'errorCount',
      render: (value?: number) => formatText(value),
    },
    {
      title: 'Обновлено',
      dataIndex: 'refreshedAt',
      key: 'refreshedAt',
      render: (value?: string) => formatAssignmentDate(value),
    },
  ];

  return (
    <Card className="soft-card" styles={{ body: { padding: 0 } }}>
      <Table
        rowKey={(item) => `${item.userId ?? 'user'}-${item.topicId ?? 'topic'}-${item.periodStart ?? 'start'}`}
        columns={columns}
        dataSource={items}
        pagination={{ pageSize: 8, hideOnSinglePage: true }}
      />
    </Card>
  );
}
