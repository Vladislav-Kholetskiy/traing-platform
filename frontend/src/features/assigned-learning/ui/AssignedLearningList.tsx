import { Button, Card, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router';
import {
  formatAssignmentDate,
  getAssignmentTitle,
  type AssignedLearningAssignment,
} from '../model/assignedLearning';
import { getStatusColor, localizeStatus } from '../../../shared/ui/presentation';

type AssignedLearningListProps = {
  assignments: AssignedLearningAssignment[];
};

export function AssignedLearningList({ assignments }: AssignedLearningListProps) {
  const columns: ColumnsType<AssignedLearningAssignment> = [
    {
      title: 'Курс',
      key: 'course',
      render: (_, assignment) => (
        <>
          <Typography.Text strong>{getAssignmentTitle(assignment)}</Typography.Text>
          <br />
          <Typography.Text type="secondary">
            Откройте карточку, чтобы перейти к материалам и тестам.
          </Typography.Text>
        </>
      ),
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      key: 'status',
      render: (status?: string) => (
        <Tag color={getStatusColor(status)}>{localizeStatus(status, 'Статус не указан')}</Tag>
      ),
    },
    {
      title: 'Дата назначения',
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
      title: 'Действие',
      key: 'action',
      render: (_, assignment) => (
        <Link to={`/learner/assigned-learning/${assignment.assignmentId}/learning-context`}>
          <Button type="primary">Открыть</Button>
        </Link>
      ),
    },
  ];

  return (
    <Card className="soft-card" styles={{ body: { padding: 0 } }}>
      <Table
        rowKey={(assignment) => String(assignment.assignmentId)}
        columns={columns}
        dataSource={assignments}
        pagination={false}
      />
    </Card>
  );
}
