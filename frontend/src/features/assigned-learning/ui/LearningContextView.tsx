import { Button, Card, Collapse, List, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import {
  formatAssignmentDate,
  type AssignedLearningContext,
  type AssignedLearningMaterial,
} from '../model/assignedLearning';
import {
  getStatusColor,
  localizeMaterialType,
  localizeStatus,
} from '../../../shared/ui/presentation';

type LearningContextViewProps = {
  assignmentId: string;
  context: AssignedLearningContext;
};

function buildMaterialsByTopic(materials: AssignedLearningMaterial[]) {
  return materials.reduce<Record<string, AssignedLearningMaterial[]>>((accumulator, material) => {
    const key = String(material.topicId ?? 'unassigned');
    accumulator[key] = accumulator[key] ?? [];
    accumulator[key].push(material);
    return accumulator;
  }, {});
}

function buildMaterialOpenPath(assignmentId: string, materialId?: number) {
  if (materialId == null) {
    return `/learner/assigned-learning/${assignmentId}/learning-context`;
  }

  return `/learner/assigned-learning/${assignmentId}/materials/${materialId}`;
}

function localizeAssignmentTestRole(role?: string) {
  if (role === 'FINAL_TOPIC_CONTROL') {
    return 'Итоговое тестирование';
  }

  if (role === 'NON_FINAL') {
    return 'Промежуточный тест по назначению';
  }

  return 'Тест по назначению';
}

export function LearningContextView({ assignmentId, context }: LearningContextViewProps) {
  const assignment = context.assignment;
  const course = context.publishedCourse;
  const materialsByTopic = buildMaterialsByTopic(context.publishedMaterials);
  const extraMaterials = materialsByTopic.unassigned ?? [];

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <div className="page-header page-header-tight">
        <div>
          <Link to={`/learner/assigned-learning/${assignmentId}`}>
            <Button type="link" style={{ paddingInline: 0 }}>
              Назад
            </Button>
          </Link>
          <Typography.Title level={2} style={{ margin: 0 }}>
            {course?.name ?? 'Материалы обучения'}
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Изучите темы, материалы и доступные тесты по текущему назначению.
          </Typography.Paragraph>
        </div>
      </div>

      <Card className="soft-card hero-card">
        <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
          <Space wrap>
            {assignment?.status ? (
              <Tag color={getStatusColor(assignment.status)}>
                {localizeStatus(assignment.status, 'Статус не указан')}
              </Tag>
            ) : null}
            <span className="stat-pill">Назначено: {formatAssignmentDate(assignment?.assignedAt)}</span>
            <span className="stat-pill">Срок: {formatAssignmentDate(assignment?.deadlineAt)}</span>
          </Space>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {course?.name ?? 'Учебный курс'}
          </Typography.Title>
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            {course?.description ?? 'Описание курса пока не указано.'}
          </Typography.Paragraph>
        </Space>
      </Card>

      <Card className="soft-card" title={`Темы курса (${context.publishedTopics.length})`}>
        {context.publishedTopics.length > 0 ? (
          <Collapse
            items={context.publishedTopics.map((topic, index) => ({
              key: String(topic.topicId ?? index),
              label: topic.name ?? `Тема ${index + 1}`,
              children: (
                <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                  <Typography.Paragraph style={{ marginBottom: 0 }}>
                    {topic.description ?? 'Описание темы пока не указано.'}
                  </Typography.Paragraph>
                  <div>
                    <Typography.Text strong>Материалы</Typography.Text>
                    <List
                      style={{ marginTop: 12 }}
                      dataSource={materialsByTopic[String(topic.topicId ?? 'missing')] ?? []}
                      locale={{ emptyText: 'Для этой темы материалы пока не добавлены.' }}
                      renderItem={(material) => (
                        <List.Item
                          className="topic-card"
                          actions={[
                            <Link
                              key="open-material"
                              to={buildMaterialOpenPath(assignmentId, material.materialId)}
                            >
                              <Button type="primary">Открыть материал</Button>
                            </Link>,
                          ]}
                        >
                          <List.Item.Meta
                            title={material.name ?? 'Материал'}
                            description={
                              <Space orientation="vertical" size={6}>
                                <Typography.Text>{material.description ?? 'Без описания.'}</Typography.Text>
                                <Typography.Text type="secondary">
                                  Формат: {localizeMaterialType(material.materialType)}
                                </Typography.Text>
                              </Space>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  </div>
                </Space>
              ),
            }))}
          />
        ) : (
          <Typography.Text type="secondary">Темы пока не опубликованы.</Typography.Text>
        )}
      </Card>

      {extraMaterials.length > 0 ? (
        <Card className="soft-card" title="Дополнительные материалы">
          <List
            dataSource={extraMaterials}
            renderItem={(material) => (
              <List.Item
                actions={[
                  <Link
                    key="open-material"
                    to={buildMaterialOpenPath(assignmentId, material.materialId)}
                  >
                    <Button type="primary">Открыть материал</Button>
                  </Link>,
                ]}
              >
                <List.Item.Meta
                  title={material.name ?? 'Материал'}
                  description={
                    <Space orientation="vertical" size={6}>
                      <Typography.Text>{material.description ?? 'Без описания.'}</Typography.Text>
                      <Typography.Text type="secondary">
                        Формат: {localizeMaterialType(material.materialType)}
                      </Typography.Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        </Card>
      ) : null}

      <Card className="soft-card" title={`Тесты по назначению (${context.assignmentTests.length})`}>
        {context.assignmentTests.length > 0 ? (
          <List
            dataSource={context.assignmentTests}
            renderItem={(assignmentTest, index) => (
              <List.Item
                actions={[
                  assignmentTest.assignmentTestId != null ? (
                    <Link
                      key="open-test"
                      to={`/learner/assigned-learning/${assignmentId}/tests/${assignmentTest.assignmentTestId}`}
                    >
                      <Button type="primary">Открыть тест</Button>
                    </Link>
                  ) : null,
                ]}
              >
                <List.Item.Meta
                  title={
                    assignmentTest.testName?.trim()
                      || `${localizeAssignmentTestRole(assignmentTest.assignmentTestRole)} ${index + 1}`
                  }
                  description={
                    <Space orientation="vertical" size={6}>
                      <Typography.Text>
                        {localizeAssignmentTestRole(assignmentTest.assignmentTestRole)}
                        {assignmentTest.topicName?.trim() ? ` · Тема: ${assignmentTest.topicName.trim()}` : ''}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        {assignmentTest.isClosed
                          ? 'Тест уже завершён.'
                          : 'Тест доступен для прохождения.'}
                      </Typography.Text>
                      {assignmentTest.closedAt ? (
                        <Typography.Text type="secondary">
                          Завершён: {formatAssignmentDate(assignmentTest.closedAt)}
                        </Typography.Text>
                      ) : null}
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        ) : (
          <Typography.Text type="secondary">Доступные тесты пока не опубликованы.</Typography.Text>
        )}
      </Card>
    </Space>
  );
}
