import { Button, Card, List, Space, Typography } from 'antd';
import { Link } from 'react-router';
import type { AssignedTestContext } from '../model/assignedLearning';

type AssignedTestContextViewProps = {
  assignmentId: string;
  assignmentTestId: string;
  context: AssignedTestContext;
};

function formatAssignedTestName(testName?: string) {
  if (!testName?.trim()) {
    return 'Тест по назначению';
  }

  return testName.replace(/^Итоговый контроль:/u, 'Итоговое тестирование:');
}

export function AssignedTestContextView({
  assignmentId,
  assignmentTestId,
  context,
}: AssignedTestContextViewProps) {
  const title = formatAssignedTestName(context.testName);
  const startAttemptPath = `/learner/assigned-learning/${assignmentId}/tests/${assignmentTestId}/attempt`;

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <div className="page-header page-header-tight">
        <div>
          <Link to={`/learner/assigned-learning/${assignmentId}/learning-context`}>
            <Button type="link" style={{ paddingInline: 0 }}>
              Назад
            </Button>
          </Link>
          <Typography.Title level={2} style={{ margin: 0 }}>
            {title}
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Ознакомьтесь со структурой вопросов перед началом прохождения.
          </Typography.Paragraph>
        </div>
      </div>

      <Card className="soft-card hero-card">
        <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {title}
          </Typography.Title>
          <Space wrap>
            <span className="stat-pill">Вопросов: {context.questions.length}</span>
            <span className="stat-pill">Формат: выбор ответа</span>
          </Space>
          <div>
            <Link to={startAttemptPath}>
              <Button type="primary" size="large">
                Начать прохождение
              </Button>
            </Link>
          </div>
        </Space>
      </Card>

      <Card className="soft-card" title={`Вопросы (${context.questions.length})`}>
        {context.questions.length > 0 ? (
          <List
            dataSource={context.questions}
            renderItem={(question, index) => (
              <List.Item>
                <Card className="soft-card question-card" style={{ width: '100%' }}>
                  <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                    <Typography.Title level={5} style={{ margin: 0 }}>
                      {`Вопрос ${index + 1}`}
                    </Typography.Title>
                    <Typography.Text strong>
                      {question.body ?? 'Текст вопроса скоро появится.'}
                    </Typography.Text>

                    <div>
                      <Typography.Text strong>Варианты ответа</Typography.Text>
                      <List
                        style={{ marginTop: 12 }}
                        bordered
                        dataSource={question.answerOptions}
                        locale={{ emptyText: 'Для этого вопроса пока нет вариантов ответа.' }}
                        renderItem={(option) => (
                          <List.Item>
                            <Typography.Text>
                              {option.body ?? 'Вариант ответа без текста'}
                            </Typography.Text>
                          </List.Item>
                        )}
                      />
                    </div>
                  </Space>
                </Card>
              </List.Item>
            )}
          />
        ) : (
          <Typography.Text type="secondary">Вопросы для этого теста пока недоступны.</Typography.Text>
        )}
      </Card>

    </Space>
  );
}
