import { Button, Card, Col, Row, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import {
  formatAssignmentDate,
  getAssignmentTitle,
  type AssignedLearningAssignment,
} from '../model/assignedLearning';
import { getStatusColor, localizeStatus } from '../../../shared/ui/presentation';

type AssignmentDetailCardProps = {
  assignment: AssignedLearningAssignment;
};

export function AssignmentDetailCard({ assignment }: AssignmentDetailCardProps) {
  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <div className="page-header page-header-tight">
        <div>
          <Link to="/learner/assigned-learning">
            <Button type="link" style={{ paddingInline: 0 }}>
              Назад
            </Button>
          </Link>
          <Typography.Title level={2} style={{ margin: 0 }}>
            {getAssignmentTitle(assignment)}
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Просмотрите сроки обучения и переходите к материалам, когда будете готовы.
          </Typography.Paragraph>
        </div>
      </div>

      <Card className="soft-card hero-card">
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} lg={16}>
            <Space orientation="vertical" size={8}>
              <Tag color={getStatusColor(assignment.status)}>
                {localizeStatus(assignment.status, 'Статус не указан')}
              </Tag>
              <Typography.Title level={3} style={{ margin: 0 }}>
                Назначенное обучение
              </Typography.Title>
              <Typography.Text className="muted-note">
                В этом разделе собраны материалы курса и связанные тесты.
              </Typography.Text>
            </Space>
          </Col>
          <Col xs={24} lg={8}>
            {assignment.assignmentId != null ? (
              <Link to={`/learner/assigned-learning/${assignment.assignmentId}/learning-context`}>
                <Button type="primary" size="large" block>
                  Открыть материалы
                </Button>
              </Link>
            ) : null}
          </Col>
        </Row>
      </Card>

      <Card className="soft-card" title="Краткая информация">
        <div className="summary-grid">
          <div className="summary-item">
            <span className="summary-label">Статус</span>
            <Typography.Text strong>
              {localizeStatus(assignment.status, 'Статус не указан')}
            </Typography.Text>
          </div>
          <div className="summary-item">
            <span className="summary-label">Дата назначения</span>
            <Typography.Text strong>{formatAssignmentDate(assignment.assignedAt)}</Typography.Text>
          </div>
          <div className="summary-item">
            <span className="summary-label">Срок выполнения</span>
            <Typography.Text strong>{formatAssignmentDate(assignment.deadlineAt)}</Typography.Text>
          </div>
          <div className="summary-item">
            <span className="summary-label">Дата завершения</span>
            <Typography.Text strong>{formatAssignmentDate(assignment.closedAt)}</Typography.Text>
          </div>
        </div>
      </Card>
    </Space>
  );
}
