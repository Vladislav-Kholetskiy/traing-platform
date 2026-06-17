import { Col, Row, Typography } from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useManagerialDepartmentTopicAnalytics } from '../../features/managerial/model/useManagerial';
import { ManagerialAnalyticsToolbar } from '../../features/managerial/ui/ManagerialAnalyticsToolbar';
import { ManagerialDepartmentTopicAnalyticsTable } from '../../features/managerial/ui/ManagerialDepartmentTopicAnalyticsTable';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { formatPercent } from '../../shared/ui/presentation';

export function ManagerDepartmentTopicAnalyticsPage() {
  const { data: actor } = useCurrentActor();
  const [period, setPeriod] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().startOf('year'),
    dayjs().endOf('year'),
  ]);
  const analyticsQuery = useManagerialDepartmentTopicAnalytics(
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
        title="Загрузка аналитики по подразделениям"
        description="Подготавливаем сводные данные по подразделениям и темам."
      />
    );
  }

  if (analyticsQuery.isError) {
    return (
      <ErrorView
        title="Не удалось загрузить аналитику по подразделениям"
        description="Попробуйте открыть экран ещё раз немного позже."
        error={analyticsQuery.error}
      />
    );
  }

  const items = analyticsQuery.data ?? [];
  const passRate =
    items.length > 0
      ? items.reduce((sum, item) => sum + Number(item.passRatePercent ?? 0), 0) / items.length
      : 0;

  return (
    <div className="home-page home-page-compact">
      <section className="self-hero">
        <div className="self-hero-copy">
          <Typography.Title level={2} className="self-hero-title">
            Аналитика по подразделениям
          </Typography.Title>
          <Typography.Paragraph className="self-hero-text">
            Сводная статистика по подразделениям за выбранный период.
          </Typography.Paragraph>
        </div>
      </section>

      <ManagerialAnalyticsToolbar
        title="Период анализа"
        description="Выберите период, за который нужно показать статистику по подразделениям."
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
              <MetricCard label="Подразделений в срезе" value={String(items.length)} />
            </Col>
            <Col xs={24} md={8}>
              <MetricCard label="Средняя доля успешных прохождений" value={formatPercent(passRate)} />
            </Col>
            <Col xs={24} md={8}>
              <MetricCard
                label="Суммарно ошибок"
                value={String(items.reduce((sum, item) => sum + (item.errorCount ?? 0), 0))}
              />
            </Col>
          </Row>
          <ManagerialDepartmentTopicAnalyticsTable items={items} />
        </>
      )}
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="home-summary-card">
      <span className="home-summary-label">{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
