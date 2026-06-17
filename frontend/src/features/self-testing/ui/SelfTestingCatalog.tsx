import { Button, Card, Col, Row, Space, Typography } from 'antd';
import { Link } from 'react-router';
import type { SelfVisibleTestCatalogEntry } from '../model/selfTesting';

type SelfTestingCatalogProps = {
  tests: SelfVisibleTestCatalogEntry[];
};

function formatSelfTestTitle(test: SelfVisibleTestCatalogEntry, index: number) {
  if (test.topicName?.trim()) {
    return test.topicName;
  }

  if (test.name?.trim()) {
    return test.name.replace(/^(Самопроверка|Тренировочный тест)\s*:\s*/u, '');
  }

  return `Модуль самостоятельного обучения ${index + 1}`;
}

export function SelfTestingCatalog({ tests }: SelfTestingCatalogProps) {
  return (
    <Row gutter={[16, 16]}>
      {tests.map((test, index) => (
        <Col key={test.testId ?? `self-test-${index}`} xs={24} lg={12}>
          <Card className="soft-card self-test-card">
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Space wrap>
                {test.courseName ? <span className="stat-pill">{test.courseName}</span> : null}
              </Space>

              <div>
                <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 0 }}>
                  {formatSelfTestTitle(test, index)}
                </Typography.Title>
              </div>

              <div className="summary-grid">
                <div className="summary-item">
                  <span className="summary-label">Курс</span>
                  <Typography.Text strong>{test.courseName ?? 'Не указан'}</Typography.Text>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Тема</span>
                  <Typography.Text strong>{test.topicName ?? 'Не указана'}</Typography.Text>
                </div>
              </div>

              <Space wrap>
                {test.topicId != null ? (
                  <Link to={`/learner/self-testing/topics/${test.topicId}`}>
                    <Button size="large">Изучить тему</Button>
                  </Link>
                ) : null}
                <Link to={`/learner/self-testing/${test.testId}`}>
                  <Button type="primary" size="large">
                    Открыть тест
                  </Button>
                </Link>
              </Space>
            </Space>
          </Card>
        </Col>
      ))}
    </Row>
  );
}
