import { Card, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { formatAssignmentDate } from '../../assigned-learning/model/assignedLearning';
import { formatPercent, formatText } from '../../../shared/ui/presentation';

export type ManagerialTopicAnalyticsItem = {
  topicId?: number;
  topicName?: string;
  averageScorePercent: number;
  passRatePercent: number;
  attemptCount: number;
  errorCount: number;
  refreshedAt?: string;
};

type ManagerialTopicAnalyticsTableProps = {
  items: ManagerialTopicAnalyticsItem[];
};

export function ManagerialTopicAnalyticsTable({
  items,
}: ManagerialTopicAnalyticsTableProps) {
  const columns: ColumnsType<ManagerialTopicAnalyticsItem> = [
    {
      title: 'Тема',
      key: 'topic',
      render: (_, item) => <Typography.Text strong>{item.topicName ?? 'Тема не указана'}</Typography.Text>,
    },
    {
      title: 'Средний результат',
      dataIndex: 'averageScorePercent',
      key: 'averageScorePercent',
      render: (value: number) => formatPercent(value),
    },
    {
      title: 'Процент прохождения',
      dataIndex: 'passRatePercent',
      key: 'passRatePercent',
      render: (value: number) => formatPercent(value),
    },
    {
      title: 'Попытки',
      dataIndex: 'attemptCount',
      key: 'attemptCount',
      render: (value: number) => formatText(value),
    },
    {
      title: 'Ошибки',
      dataIndex: 'errorCount',
      key: 'errorCount',
      render: (value: number) => formatText(value),
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
        rowKey={(item) => `${item.topicId ?? item.topicName ?? 'topic'}-${item.refreshedAt ?? 'refresh'}`}
        columns={columns}
        dataSource={items}
        pagination={{ pageSize: 8, hideOnSinglePage: true }}
      />
    </Card>
  );
}
