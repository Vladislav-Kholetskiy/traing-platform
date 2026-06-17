import { Button, Card, Col, Row, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import type { SelfResultHistoryItem } from '../model/selfResult';
import {
  formatPassedLabel,
  formatPercent,
  formatText,
  formatUiDate,
} from '../../../shared/ui/presentation';

type SelfResultHistoryListProps = {
  results: SelfResultHistoryItem[];
};

export function SelfResultHistoryList({ results }: SelfResultHistoryListProps) {
  return (
    <Row gutter={[16, 16]}>
      {results.map((result, index) => (
        <Col key={`${result.recordedAt ?? 'result'}-${index}`} xs={24} lg={12}>
          <Card className="soft-card result-card" title={result.testName ?? `Тест ${index + 1}`}>
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Space wrap>
                <Tag color={result.passed ? 'success' : 'error'}>
                  {formatPassedLabel(result.passed)}
                </Tag>
                <span className="stat-pill">Дата: {formatUiDate(result.recordedAt)}</span>
              </Space>

              <div className="summary-grid">
                <div className="summary-item">
                  <span className="summary-label">Баллы</span>
                  <Typography.Text strong>{formatText(result.score)}</Typography.Text>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Процент</span>
                  <Typography.Text strong>{formatPercent(result.scorePercent)}</Typography.Text>
                </div>
              </div>

              {result.resultId != null ? (
                <Link to={`/learner/self-results/${result.resultId}`}>
                  <Button type="primary">Открыть детали</Button>
                </Link>
              ) : null}
            </Space>
          </Card>
        </Col>
      ))}
    </Row>
  );
}
