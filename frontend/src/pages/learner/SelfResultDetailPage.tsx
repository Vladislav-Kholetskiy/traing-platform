import { Button, Card, List, Space, Tag, Typography } from 'antd';
import { Link, useParams } from 'react-router';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useSelfResultReview } from '../../features/self-results/model/useSelfResults';
import { formatAssignmentDate } from '../../features/assigned-learning/model/assignedLearning';
import {
  formatPassedLabel,
  formatPercent,
  formatText,
  localizeQuestionType,
} from '../../shared/ui/presentation';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

function formatResultTestName(testName?: string, fallbackId?: number | null) {
  if (testName?.trim()) {
    return testName.replace(/^Итоговый контроль:/u, 'Итоговое тестирование:');
  }

  return fallbackId != null ? `Результат тестирования #${fallbackId}` : 'Результат тестирования';
}

function localizeAttemptMode(mode?: string, assignmentId?: number | null) {
  if (mode === 'ASSIGNED' || assignmentId != null) {
    return 'Назначенное обучение';
  }

  if (mode === 'SELF') {
    return 'Самостоятельное обучение';
  }

  return 'Формат не указан';
}

function formatSnapshotText(value?: string) {
  if (!value?.trim()) {
    return 'Не указано';
  }

  return value;
}

export function SelfResultDetailPage() {
  const { resultId } = useParams();
  const { data: actor } = useCurrentActor();
  const numericResultId = resultId ? Number(resultId) : undefined;
  const resultQuery = useSelfResultReview(numericResultId, Boolean(actor && hasSection(actor, 'SELF_RESULTS')));

  if (!actor || !hasSection(actor, 'SELF_RESULTS')) {
    return <ForbiddenView />;
  }

  if (!resultId) {
    return (
      <EmptyState
        title="Результат не выбран"
        description="Вернитесь в историю и откройте нужную карточку результата."
      />
    );
  }

  if (resultQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка результата"
        description="Подготавливаем разбор завершённого тестирования."
      />
    );
  }

  if (resultQuery.isError) {
    return (
      <ErrorView
        title="Не удалось загрузить результат"
        description="Попробуйте открыть карточку результата ещё раз немного позже."
        error={resultQuery.error}
      />
    );
  }

  const result = resultQuery.data;

  if (!result) {
    return (
      <>
        <Typography.Title level={2}>Детали результата</Typography.Title>
        <EmptyState
          title="Результат не найден"
          description="В текущей истории нет карточки с таким идентификатором."
        />
      </>
    );
  }

  const title = formatResultTestName(result.testName, result.resultId);

  return (
    <div className="home-page home-page-compact">
      <section className="self-hero">
        <div className="self-hero-copy">
          <Link to="/learner/self-results">
            <Button type="link" style={{ paddingInline: 0 }}>
              Назад
            </Button>
          </Link>
          <Typography.Title level={2} className="self-hero-title">
            {title}
          </Typography.Title>
          <Space wrap>
            <Tag color={result.passed ? 'success' : 'error'}>
              {formatPassedLabel(result.passed)}
            </Tag>
            <span className="stat-pill">{`Дата: ${formatAssignmentDate(result.recordedAt)}`}</span>
          </Space>
        </div>
      </section>

      <Card className="soft-card hero-card">
        <div className="summary-grid">
          <div className="summary-item">
            <span className="summary-label">Баллы</span>
            <Typography.Text strong>{formatText(result.score)}</Typography.Text>
          </div>
          <div className="summary-item">
            <span className="summary-label">Процент</span>
            <Typography.Text strong>{formatPercent(result.scorePercent)}</Typography.Text>
          </div>
          <div className="summary-item">
            <span className="summary-label">Формат прохождения</span>
            <Typography.Text strong>{localizeAttemptMode(result.attemptMode, result.assignmentId)}</Typography.Text>
          </div>
          <div className="summary-item">
            <span className="summary-label">Статус</span>
            <Typography.Text strong>{formatPassedLabel(result.passed)}</Typography.Text>
          </div>
          <div className="summary-item">
            <span className="summary-label">Дата прохождения</span>
            <Typography.Text strong>{formatAssignmentDate(result.recordedAt)}</Typography.Text>
          </div>
        </div>
      </Card>

      <Card className="soft-card" title={`Разбор ответов (${result.questions.length})`}>
        {result.questions.length > 0 ? (
          <List
            dataSource={result.questions}
            renderItem={(question, index) => (
              <List.Item>
                <Card className="soft-card question-card" style={{ width: '100%' }}>
                  <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    <Space wrap>
                      <Typography.Title level={5} style={{ margin: 0 }}>
                        {`Вопрос ${index + 1}`}
                      </Typography.Title>
                      <Tag color={question.correct ? 'success' : 'error'}>
                        {question.correct ? 'Верно' : 'Неверно'}
                      </Tag>
                      {question.earnedScore != null && question.maxScore != null ? (
                        <span className="stat-pill">{`Баллы: ${question.earnedScore} / ${question.maxScore}`}</span>
                      ) : null}
                    </Space>

                    <Typography.Text strong>
                      {question.body ?? 'Текст вопроса недоступен.'}
                    </Typography.Text>
                    <Typography.Text type="secondary">
                      {localizeQuestionType(question.questionType, 'Тип вопроса не указан')}
                    </Typography.Text>

                    {question.answerOptions.length > 0 ? (
                      <Space direction="vertical" size="small" style={{ width: '100%' }}>
                        <Typography.Text strong>Варианты ответа</Typography.Text>
                        <List
                          bordered
                          dataSource={question.answerOptions}
                          renderItem={(option) => (
                            <List.Item>
                              <Space direction="vertical" size={6} style={{ width: '100%' }}>
                                <Typography.Text>{option.body ?? 'Вариант ответа без текста'}</Typography.Text>
                                <Space wrap>
                                  {option.selectedByUser ? <Tag color="blue">Ваш выбор</Tag> : null}
                                  {option.correctAtSnapshot ? <Tag color="success">Правильный ответ</Tag> : null}
                                </Space>
                              </Space>
                            </List.Item>
                          )}
                        />
                      </Space>
                    ) : (
                      <Space direction="vertical" size="small" style={{ width: '100%' }}>
                        <Typography.Text strong>Ответ пользователя</Typography.Text>
                        <Typography.Paragraph style={{ marginBottom: 0 }}>
                          {formatSnapshotText(question.userAnswerSnapshot)}
                        </Typography.Paragraph>
                        <Typography.Text strong>Правильный ответ</Typography.Text>
                        <Typography.Paragraph style={{ marginBottom: 0 }}>
                          {formatSnapshotText(question.correctAnswerSnapshot)}
                        </Typography.Paragraph>
                      </Space>
                    )}

                    {!question.correct && question.evaluationNote?.trim() ? (
                      <Typography.Text type="secondary">
                        {question.evaluationNote.trim()}
                      </Typography.Text>
                    ) : null}
                  </Space>
                </Card>
              </List.Item>
            )}
          />
        ) : (
          <Typography.Text type="secondary">
            Детальный разбор ответов для этого результата пока недоступен.
          </Typography.Text>
        )}
      </Card>
    </div>
  );
}
