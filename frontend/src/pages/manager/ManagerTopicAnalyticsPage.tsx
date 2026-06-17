import { Col, Row, Typography } from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useManagerialUserTopicAnalytics } from '../../features/managerial/model/useManagerial';
import { ManagerialAnalyticsToolbar } from '../../features/managerial/ui/ManagerialAnalyticsToolbar';
import {
  ManagerialTopicAnalyticsTable,
  type ManagerialTopicAnalyticsItem,
} from '../../features/managerial/ui/ManagerialTopicAnalyticsTable';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { formatPercent } from '../../shared/ui/presentation';

type SourceTopicRow = {
  topicId?: number;
  topicName?: string;
  averageScorePercent?: string;
  passRatePercent?: string;
  attemptCount?: number;
  errorCount?: number;
  refreshedAt?: string;
};

type TopicAccumulator = {
  topicId?: number;
  topicName?: string;
  weightedAverageScoreSum: number;
  weightedPassRateSum: number;
  weightSum: number;
  rowCount: number;
  averageScoreFallbackSum: number;
  passRateFallbackSum: number;
  attemptCount: number;
  errorCount: number;
  refreshedAt?: string;
};

export function ManagerTopicAnalyticsPage() {
  const { data: actor } = useCurrentActor();
  const [period, setPeriod] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().startOf('year'),
    dayjs().endOf('year'),
  ]);
  const analyticsQuery = useManagerialUserTopicAnalytics(
    period[0].toISOString(),
    period[1].toISOString(),
    Boolean(actor?.roles.includes('MANAGER')),
  );

  if (!actor || !actor.roles.includes('MANAGER')) {
    return <ForbiddenView />;
  }

  if (analyticsQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка аналитики по темам"
        description="Подготавливаем сводные данные по темам обучения."
      />
    );
  }

  if (analyticsQuery.isError) {
    return (
      <ErrorView
        title="Не удалось загрузить аналитику по темам"
        description="Попробуйте открыть экран ещё раз немного позже."
        error={analyticsQuery.error}
      />
    );
  }

  const items = buildTopicAnalyticsItems(analyticsQuery.data ?? []);
  const averageScore =
    items.length > 0
      ? items.reduce((sum, item) => sum + item.averageScorePercent, 0) / items.length
      : 0;

  return (
    <div className="home-page home-page-compact">
      <section className="self-hero">
        <div className="self-hero-copy">
          <Typography.Title level={2} className="self-hero-title">
            Аналитика по темам
          </Typography.Title>
          <Typography.Paragraph className="self-hero-text">
            Сводная статистика по темам обучения за выбранный период.
          </Typography.Paragraph>
        </div>
      </section>

      <ManagerialAnalyticsToolbar
        title="Период анализа"
        description="Выберите период, за который нужно показать статистику по темам."
        value={period}
        onChange={setPeriod}
      />

      {items.length === 0 ? (
        <EmptyState
          title="Нет данных за выбранный период"
          description="Попробуйте расширить интервал и повторить запрос."
        />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            <Col xs={24} md={8}>
              <MetricCard label="Тем в срезе" value={String(items.length)} />
            </Col>
            <Col xs={24} md={8}>
              <MetricCard label="Средний результат" value={formatPercent(averageScore)} />
            </Col>
            <Col xs={24} md={8}>
              <MetricCard
                label="Суммарно попыток"
                value={String(items.reduce((sum, item) => sum + item.attemptCount, 0))}
              />
            </Col>
          </Row>
          <ManagerialTopicAnalyticsTable items={items} />
        </>
      )}
    </div>
  );
}

function buildTopicAnalyticsItems(items: SourceTopicRow[]): ManagerialTopicAnalyticsItem[] {
  const grouped = new Map<string, TopicAccumulator>();

  for (const item of items) {
    const key = String(item.topicId ?? item.topicName ?? 'topic');
    const attemptCount = item.attemptCount ?? 0;
    const averageScorePercent = Number(item.averageScorePercent ?? 0);
    const passRatePercent = Number(item.passRatePercent ?? 0);
    const weight = attemptCount > 0 ? attemptCount : 1;

    const current = grouped.get(key) ?? {
      topicId: item.topicId,
      topicName: item.topicName,
      weightedAverageScoreSum: 0,
      weightedPassRateSum: 0,
      weightSum: 0,
      rowCount: 0,
      averageScoreFallbackSum: 0,
      passRateFallbackSum: 0,
      attemptCount: 0,
      errorCount: 0,
      refreshedAt: item.refreshedAt,
    };

    current.topicId ??= item.topicId;
    current.topicName ??= item.topicName;
    current.weightedAverageScoreSum += averageScorePercent * weight;
    current.weightedPassRateSum += passRatePercent * weight;
    current.weightSum += weight;
    current.rowCount += 1;
    current.averageScoreFallbackSum += averageScorePercent;
    current.passRateFallbackSum += passRatePercent;
    current.attemptCount += attemptCount;
    current.errorCount += item.errorCount ?? 0;
    if ((item.refreshedAt ?? '') > (current.refreshedAt ?? '')) {
      current.refreshedAt = item.refreshedAt;
    }

    grouped.set(key, current);
  }

  return Array.from(grouped.values())
    .map((item) => ({
      topicId: item.topicId,
      topicName: item.topicName,
      averageScorePercent:
        item.weightSum > 0
          ? item.weightedAverageScoreSum / item.weightSum
          : item.averageScoreFallbackSum / item.rowCount,
      passRatePercent:
        item.weightSum > 0
          ? item.weightedPassRateSum / item.weightSum
          : item.passRateFallbackSum / item.rowCount,
      attemptCount: item.attemptCount,
      errorCount: item.errorCount,
      refreshedAt: item.refreshedAt,
    }))
    .sort((left, right) => (left.topicName ?? '').localeCompare(right.topicName ?? ''));
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="home-summary-card">
      <span className="home-summary-label">{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
