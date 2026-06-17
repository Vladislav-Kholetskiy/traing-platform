import { Button, DatePicker, Progress, Table, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';
import { Link } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type { ExpertQuestionAnalyticsItem } from '../../features/expert-analytics/model/expertAnalytics';
import { useExpertQuestionAnalytics } from '../../features/expert-analytics/model/useExpertAnalytics';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatPercent, formatUiDate } from '../../shared/ui/presentation';

export function ExpertQuestionAnalyticsPage() {
  const { data: actor } = useCurrentActor();
  const [period, setPeriod] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([dayjs().subtract(30, 'day'), dayjs()]);
  const analyticsQuery = useExpertQuestionAnalytics(period[0].toISOString(), period[1].toISOString(), Boolean(actor));

  if (!actor || !canAccessExpertArea(actor)) {
    return <ForbiddenView />;
  }
  if (analyticsQuery.isLoading) {
    return <LoadingView title="Загрузка аналитики по вопросам" />;
  }
  if (analyticsQuery.isError) {
    return <ErrorView title="Не удалось загрузить аналитику по вопросам" error={analyticsQuery.error} />;
  }

  const items = analyticsQuery.data ?? [];
  const totalAttempts = items.reduce((sum, item) => sum + (item.attemptCount ?? 0), 0);
  const totalCorrect = items.reduce((sum, item) => sum + (item.correctCount ?? 0), 0);
  const totalIncorrect = items.reduce((sum, item) => sum + (item.incorrectCount ?? 0), 0);
  const totalAnswers = totalCorrect + totalIncorrect;
  const weightedAverageScore =
    totalAttempts > 0
      ? items.reduce((sum, item) => sum + normalizePercentValue(item.averageEarnedScore) * (item.attemptCount ?? 0), 0) /
        totalAttempts
      : 0;
  const accuracyPercent = totalAnswers > 0 ? (totalCorrect / totalAnswers) * 100 : 0;
  const topQuestion = [...items].sort(
    (left, right) => normalizePercentValue(right.averageEarnedScore) - normalizePercentValue(left.averageEarnedScore),
  )[0];
  const weakestQuestion = [...items].sort(
    (left, right) => normalizePercentValue(left.averageEarnedScore) - normalizePercentValue(right.averageEarnedScore),
  )[0];
  const freshestUpdate = [...items]
    .map((item) => item.refreshedAt ?? item.calculatedAt)
    .filter((value): value is string => Boolean(value))
    .sort()
    .at(-1);

  return (
    <div className="expert-analytics-page">
      <section className="expert-analytics-hero">
        <div className="expert-analytics-hero-copy">
          <span className="expert-analytics-kicker">Экспертный обзор</span>
          <Typography.Title level={2} className="expert-analytics-title">
            Аналитика по вопросам
          </Typography.Title>
          <Typography.Paragraph className="expert-analytics-text">
            Экран показывает, какие вопросы действительно помогают обучению, а какие стоит пересмотреть. Здесь удобнее
            быстро заметить слабые места банка вопросов, а не просто смотреть на сухой список агрегатов.
          </Typography.Paragraph>
          <div className="expert-analytics-inline-stats">
            <InlineStat label="Вопросов в анализе" value={String(items.length)} />
            <InlineStat label="Средний результат" value={formatPercent(weightedAverageScore)} />
            <InlineStat label="Последнее обновление" value={formatUiDate(freshestUpdate, 'Нет расчёта')} />
          </div>
        </div>

        <div className="expert-analytics-hero-panel">
          <div className="expert-analytics-panel-card expert-analytics-panel-card-strong">
            <span className="expert-analytics-panel-label">Точность ответов</span>
            <strong>{formatPercent(accuracyPercent)}</strong>
            <span className="expert-analytics-panel-note">
              {totalCorrect} верных против {totalIncorrect} неверных ответов за период.
            </span>
          </div>
          <div className="expert-analytics-panel-grid">
            <InsightCard
              tone="positive"
              label="Лучший вопрос"
              value={topQuestion ? `#${topQuestion.questionId}` : 'Нет данных'}
              note={topQuestion ? `Средний результат ${formatPercent(topQuestion.averageEarnedScore)}` : 'Расчёты появятся после попыток'}
            />
            <InsightCard
              tone="warning"
              label="Требует внимания"
              value={weakestQuestion ? `#${weakestQuestion.questionId}` : 'Нет данных'}
              note={
                weakestQuestion
                  ? `Средний результат ${formatPercent(weakestQuestion.averageEarnedScore)}`
                  : 'Пока нечего сравнивать'
              }
            />
          </div>
        </div>
      </section>

      <SectionCard
        title="Период анализа"
        description="Выберите интервал, чтобы пересобрать картину по попыткам и понять, как меняется качество вопросов во времени."
        extra={
          <Button type="primary" onClick={() => analyticsQuery.refetch()}>
            Обновить
          </Button>
        }
      >
        <div className="expert-analytics-toolbar">
          <DatePicker.RangePicker
            showTime
            value={period}
            onChange={(value) => {
              if (value?.[0] && value?.[1]) {
                setPeriod([value[0], value[1]]);
              }
            }}
          />
          <div className="expert-analytics-toolbar-meta">
            <Tag bordered={false}>Попыток: {totalAttempts}</Tag>
            <Tag bordered={false}>Ответов: {totalAnswers}</Tag>
          </div>
        </div>
      </SectionCard>

      {items.length === 0 ? (
        <SectionCard title="Срез по вопросам" description="Когда за выбранный период появятся данные, здесь будет сводка по качеству вопросов.">
          <EmptyState
            title="За выбранный период нет данных"
            description="Попробуйте расширить период анализа или дождаться новых прохождений."
          />
        </SectionCard>
      ) : (
        <>
          <div className="expert-analytics-metric-grid">
            <MetricCard label="Всего попыток" value={String(totalAttempts)} detail="Количество прохождений, вошедших в расчёт." />
            <MetricCard label="Верных ответов" value={String(totalCorrect)} detail="Суммарный объём корректных ответов по всем вопросам." />
            <MetricCard label="Неверных ответов" value={String(totalIncorrect)} detail="Сигнал о вопросах, которые могут быть неочевидными." />
            <MetricCard
              label="Средний балл"
              value={formatPercent(weightedAverageScore)}
              detail="Взвешен по числу попыток, чтобы активные вопросы влияли сильнее."
            />
          </div>

          <SectionCard
            title="Срез по вопросам"
            description="Список отсортирован как аналитическая витрина: сначала легко увидеть интенсивность использования, затем качество ответа и свежесть расчёта."
            extra={<Tag color="gold">Позиций: {items.length}</Tag>}
          >
            <Table
              className="expert-analytics-table"
              rowKey="questionId"
              dataSource={items}
              pagination={{ pageSize: 10 }}
              columns={[
                {
                  title: 'Вопрос',
                  dataIndex: 'questionId',
                  render: (_, record) => (
                    <div className="expert-analytics-question-cell">
                      <Link to={`/expert/content/questions/${record.questionId}`} className="expert-analytics-question-link">
                        <Typography.Text className="expert-analytics-question-id">#{record.questionId}</Typography.Text>
                      </Link>
                      <Typography.Text type="secondary">Открыть карточку вопроса</Typography.Text>
                    </div>
                  ),
                },
                {
                  title: 'Интенсивность',
                  dataIndex: 'attemptCount',
                  render: (value: number | null | undefined) => (
                    <div className="expert-analytics-number-cell">
                      <strong>{value ?? 0}</strong>
                      <span>попыток</span>
                    </div>
                  ),
                },
                {
                  title: 'Верные / неверные',
                  render: (_, record) => (
                    <div className="expert-analytics-balance">
                      <span className="expert-analytics-balance-good">{record.correctCount ?? 0}</span>
                      <span className="expert-analytics-balance-separator">/</span>
                      <span className="expert-analytics-balance-bad">{record.incorrectCount ?? 0}</span>
                    </div>
                  ),
                },
                {
                  title: 'Средний результат',
                  dataIndex: 'averageEarnedScore',
                  render: (value: ExpertQuestionAnalyticsItem['averageEarnedScore']) => {
                    const numericValue = normalizePercentValue(value);
                    return (
                      <div className="expert-analytics-score-cell">
                        <Progress
                          percent={Number(numericValue.toFixed(2))}
                          size="small"
                          showInfo={false}
                          strokeColor={resolveScoreColor(numericValue)}
                          trailColor="rgba(170, 184, 198, 0.22)"
                        />
                        <Typography.Text strong>{formatPercent(value)}</Typography.Text>
                      </div>
                    );
                  },
                },
                {
                  title: 'Статус качества',
                  render: (_, record) => {
                    const numericValue = normalizePercentValue(record.averageEarnedScore);
                    const quality = resolveQualityLabel(numericValue);
                    return <Tag color={quality.color}>{quality.label}</Tag>;
                  },
                },
                {
                  title: 'Действие',
                  render: (_, record) => (
                    <Link to={`/expert/content/questions/${record.questionId}`} className="expert-analytics-open-link">
                      Открыть
                    </Link>
                  ),
                },
                {
                  title: 'Дата расчёта',
                  dataIndex: 'calculatedAt',
                  render: (value: string | null | undefined, record: ExpertQuestionAnalyticsItem) => (
                    <div className="expert-analytics-date-cell">
                      <Typography.Text>{formatUiDate(value ?? undefined)}</Typography.Text>
                      <Typography.Text type="secondary">
                        Обновлено {formatUiDate(record.refreshedAt ?? value ?? undefined)}
                      </Typography.Text>
                    </div>
                  ),
                },
              ]}
            />
          </SectionCard>
        </>
      )}
    </div>
  );
}

function InlineStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="expert-analytics-inline-stat">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function InsightCard({
  label,
  value,
  note,
  tone,
}: {
  label: string;
  value: string;
  note: string;
  tone: 'positive' | 'warning';
}) {
  return (
    <div className={`expert-analytics-panel-card expert-analytics-panel-card-${tone}`}>
      <span className="expert-analytics-panel-label">{label}</span>
      <strong>{value}</strong>
      <span className="expert-analytics-panel-note">{note}</span>
    </div>
  );
}

function MetricCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div className="expert-analytics-metric-card">
      <span className="expert-analytics-metric-label">{label}</span>
      <strong>{value}</strong>
      <span className="expert-analytics-metric-detail">{detail}</span>
    </div>
  );
}

function normalizePercentValue(value?: string | number | null): number {
  if (value == null || value === '') {
    return 0;
  }

  const normalized = String(value).replace('%', '').trim();
  const numericValue = Number(normalized);
  return Number.isFinite(numericValue) ? numericValue : 0;
}

function resolveScoreColor(value: number): string {
  if (value >= 75) {
    return '#1d7a57';
  }
  if (value >= 45) {
    return '#d38a1f';
  }
  return '#b74b4b';
}

function resolveQualityLabel(value: number): { label: string; color: string } {
  if (value >= 75) {
    return { label: 'Сильный вопрос', color: 'green' };
  }
  if (value >= 45) {
    return { label: 'Нужно наблюдать', color: 'gold' };
  }
  return { label: 'Нужна проверка', color: 'red' };
}
