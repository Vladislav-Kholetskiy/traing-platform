import { Card, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { formatAssignmentDate } from '../../assigned-learning/model/assignedLearning';
import type { ManagerialCurrentSupervisionItem } from '../model/managerialSupervision';
import { getStatusColor, localizeStatus } from '../../../shared/ui/presentation';

type ManagerialCurrentSupervisionTableProps = {
  items: ManagerialCurrentSupervisionItem[];
};

export function ManagerialCurrentSupervisionTable({
  items,
}: ManagerialCurrentSupervisionTableProps) {
  const columns: ColumnsType<ManagerialCurrentSupervisionItem> = [
    {
      title: 'Сотрудник',
      key: 'user',
      render: (_, item) => (
        <>
          <Typography.Text strong>{item.userDisplayName ?? 'Сотрудник не указан'}</Typography.Text>
          <br />
          <Typography.Text type="secondary">{item.courseName ?? 'Курс не указан'}</Typography.Text>
          {item.assignmentTestCount != null ? (
            <>
              <br />
              <Typography.Text type="secondary">{`Тестов в курсе: ${item.assignmentTestCount}`}</Typography.Text>
            </>
          ) : null}
        </>
      ),
    },
    {
      title: 'Назначено',
      dataIndex: 'assignedAt',
      key: 'assignedAt',
      render: (value?: string) => formatAssignmentDate(value),
    },
    {
      title: 'Срок',
      dataIndex: 'deadlineAt',
      key: 'deadlineAt',
      render: (value?: string) => formatAssignmentDate(value),
    },
    {
      title: 'Статус',
      dataIndex: 'assignmentStatus',
      key: 'assignmentStatus',
      render: (status?: string) => (
        <Tag color={getStatusColor(status)}>
          {localizeStatus(status, 'Статус не указан')}
        </Tag>
      ),
    },
  ];

  return (
    <Card className="soft-card" styles={{ body: { padding: 0 } }}>
      <Table
        rowKey={(item) => `${item.assignmentId ?? 'assignment'}-${item.userId ?? 'user'}`}
        columns={columns}
        dataSource={items}
        pagination={{ pageSize: 8, hideOnSinglePage: true }}
      />
    </Card>
  );
}
