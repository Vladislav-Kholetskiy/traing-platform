import { Button, Card, List, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import { localizeQuestionType } from '../../../shared/ui/presentation';
import type { SelfVisibleTest } from '../model/selfTesting';

type SelfTestContextViewProps = {
  testId: string;
  test: SelfVisibleTest;
};

export function SelfTestContextView({ testId, test }: SelfTestContextViewProps) {
  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <section className="self-hero">
        <div className="self-hero-copy">
          <Tag className="home-badge home-badge-accent" bordered={false}>
            Самостоятельное обучение
          </Tag>
          <Typography.Title level={2} className="self-hero-title">
            {test.name ?? 'Модуль самостоятельного обучения'}
          </Typography.Title>
          <Typography.Paragraph className="self-hero-text">
            {test.description ??
              'Ознакомьтесь со структурой модуля, изучите тему и только потом начните прохождение без привязки к назначению.'}
          </Typography.Paragraph>
          <Space wrap>
            <span className="stat-pill">Вопросов: {test.questions.length}</span>
            <span className="stat-pill">Режим: самостоятельный</span>
            {test.testType ? <span className="stat-pill">Тип: {test.testType}</span> : null}
            {test.topicId != null ? (
              <Link to={`/learner/self-testing/topics/${test.topicId}`}>
                <Button>Изучить тему</Button>
              </Link>
            ) : null}
          </Space>
        </div>

        <Card className="self-hero-panel">
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <div className="home-profile-topline">Готовность</div>
            <Typography.Title level={4} style={{ margin: 0 }}>
              Превью теста
            </Typography.Title>
            <Typography.Paragraph className="self-panel-text">
              Экран ниже показывает состав вопросов и форматы ответов. Попытка создается только после явного нажатия кнопки старта.
            </Typography.Paragraph>
            <Link to={`/learner/self-testing/${testId}/attempt`}>
              <Button type="primary" size="large" block>
                Начать прохождение
              </Button>
            </Link>
          </Space>
        </Card>
      </section>

      <div className="page-header page-header-tight">
        <div>
          <Link to="/learner/self-testing">
            <Button type="link" style={{ paddingInline: 0 }}>
              Назад
            </Button>
          </Link>
          <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 8 }}>
            Состав теста
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Перед началом можно просмотреть вопросы и варианты ответов без создания попытки.
          </Typography.Paragraph>
        </div>
      </div>

      <Card className="soft-card" title={`Вопросы (${test.questions.length})`}>
        {test.questions.length > 0 ? (
          <List
            dataSource={test.questions}
            renderItem={(question, index) => (
              <List.Item>
                <Card className="soft-card question-card self-question-preview" style={{ width: '100%' }}>
                  <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    <div>
                      <Typography.Title level={5} style={{ marginTop: 0, marginBottom: 8 }}>
                        {`Вопрос ${index + 1}`}
                      </Typography.Title>
                      <Typography.Text strong>
                        {question.body ?? 'Текст вопроса скоро появится.'}
                      </Typography.Text>
                    </div>

                    <Space wrap>
                      <Tag color="blue">{localizeQuestionType(question.questionType, 'Формат вопроса')}</Tag>
                      {question.weight != null ? <span className="stat-pill">Вес: {question.weight}</span> : null}
                    </Space>

                    <List
                      bordered
                      dataSource={question.answerOptions}
                      locale={{ emptyText: 'Для этого вопроса пока нет вариантов ответа.' }}
                      renderItem={(option) => (
                        <List.Item>
                          <Typography.Text>{option.body ?? 'Вариант ответа без текста'}</Typography.Text>
                        </List.Item>
                      )}
                    />
                  </Space>
                </Card>
              </List.Item>
            )}
          />
        ) : (
          <Typography.Text type="secondary">Вопросы для этого модуля пока недоступны.</Typography.Text>
        )}
      </Card>
    </Space>
  );
}
