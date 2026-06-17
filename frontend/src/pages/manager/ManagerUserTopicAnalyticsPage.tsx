import { Col, Row, Select, Typography } from 'antd';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import {
  useManagerialCurrentSupervision,
  useManagerialUserTopicAnalytics,
} from '../../features/managerial/model/useManagerial';
import { ManagerialAnalyticsToolbar } from '../../features/managerial/ui/ManagerialAnalyticsToolbar';
import { ManagerialCurrentSupervisionTable } from '../../features/managerial/ui/ManagerialCurrentSupervisionTable';
import { ManagerialUserTopicAnalyticsTable } from '../../features/managerial/ui/ManagerialUserTopicAnalyticsTable';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { formatPercent } from '../../shared/ui/presentation';

export function ManagerUserTopicAnalyticsPage() {
  const { data: actor } = useCurrentActor();
  const [selectedUserId, setSelectedUserId] = useState<number | undefined>(undefined);
  const [period, setPeriod] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().startOf('year'),
    dayjs().endOf('year'),
  ]);

  const analyticsQuery = useManagerialUserTopicAnalytics(
    period[0].toISOString(),
    period[1].toISOString(),
    Boolean(actor?.roles.includes('MANAGER')),
  );
  const supervisionQuery = useManagerialCurrentSupervision(Boolean(actor?.roles.includes('MANAGER')));
  const items = analyticsQuery.data ?? [];
  const userOptions = useMemo(
    () =>
      Array.from(
        new Map(
          items
            .filter((item) => item.userId != null)
            .map((item) => [
              item.userId as number,
              {
                value: item.userId as number,
                label: item.userDisplayName ?? 'Сотрудник без имени',
              },
            ]),
        ).values(),
      ).sort((left, right) => left.label.localeCompare(right.label)),
    [items],
  );
  const filteredItems = selectedUserId != null ? items.filter((item) => item.userId === selectedUserId) : items;
  const currentAssignments = (supervisionQuery.data ?? []).filter((item) =>
    selectedUserId != null ? item.userId === selectedUserId : true,
  );
  const selectedUserLabel = userOptions.find((option) => option.value === selectedUserId)?.label;
  const averageScore =
    filteredItems.length > 0
      ? filteredItems.reduce((sum, item) => sum + Number(item.averageScorePercent ?? 0), 0) / filteredItems.length
      : 0;

  if (!actor || !actor.roles.includes('MANAGER')) {
    return <ForbiddenView />;
  }

  if (analyticsQuery.isLoading || supervisionQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка аналитики по сотрудникам"
        description="Подготавливаем данные по сотрудникам, темам и текущим назначениям."
      />
    );
  }

  if (analyticsQuery.isError) {
    return (
      <ErrorView
        title="Не удалось загрузить аналитику по сотрудникам"
        description="Попробуйте открыть экран ещё раз немного позже."
        error={analyticsQuery.error}
      />
    );
  }

  if (supervisionQuery.isError) {
    return (
      <ErrorView
        title="Не удалось загрузить текущие назначения сотрудников"
        description="Попробуйте открыть экран ещё раз немного позже."
        error={supervisionQuery.error}
      />
    );
  }

  return (
    <div className="home-page home-page-compact">
      <section className="self-hero">
        <div className="self-hero-copy">
          <Typography.Title level={2} className="self-hero-title">
            Аналитика по сотрудникам
          </Typography.Title>
          <Typography.Paragraph className="self-hero-text">
            Выберите сотрудника, чтобы посмотреть его текущие назначения и результаты по темам за выбранный период.
          </Typography.Paragraph>
        </div>
      </section>

      <ManagerialAnalyticsToolbar
        title="Период анализа"
        description="Выберите период, за который нужно показать статистику по сотрудникам и темам."
        value={period}
        onChange={setPeriod}
      />

      <div className="home-summary-card">
        <span className="home-summary-label">Сотрудник</span>
        <Select
          allowClear
          showSearch
          placeholder="Выберите сотрудника"
          value={selectedUserId}
          onChange={(value) => setSelectedUserId(value)}
          options={userOptions}
          optionFilterProp="label"
          style={{ width: '100%' }}
        />
      </div>

      {items.length === 0 ? (
        <EmptyState
          title="Нет данных за выбранный период"
          description="Попробуйте расширить интервал и повторить запрос."
        />
      ) : filteredItems.length === 0 ? (
        <EmptyState
          title="По выбранному сотруднику нет данных"
          description="Попробуйте выбрать другого сотрудника или расширить период анализа."
        />
      ) : (
        <>
          {selectedUserLabel ? (
            <section className="self-hero">
              <div className="self-hero-copy">
                <Typography.Title level={3} className="self-hero-title">
                  {selectedUserLabel}
                </Typography.Title>
                <Typography.Paragraph className="self-hero-text">
                  Ниже показаны текущие назначения сотрудника и его результаты по темам за выбранный период.
                </Typography.Paragraph>
              </div>
            </section>
          ) : null}

          <Row gutter={[16, 16]}>
            <Col xs={24} md={8}>
              <MetricCard label="Записей в срезе" value={String(filteredItems.length)} />
            </Col>
            <Col xs={24} md={8}>
              <MetricCard label="Средний результат" value={formatPercent(averageScore)} />
            </Col>
            <Col xs={24} md={8}>
              <MetricCard
                label="Суммарно попыток"
                value={String(filteredItems.reduce((sum, item) => sum + (item.attemptCount ?? 0), 0))}
              />
            </Col>
          </Row>

          {selectedUserId != null ? (
            currentAssignments.length > 0 ? (
              <>
                <Typography.Title level={4} style={{ marginBottom: 0 }}>
                  Текущие назначения сотрудника
                </Typography.Title>
                <ManagerialCurrentSupervisionTable items={currentAssignments} />
              </>
            ) : (
              <EmptyState
                title="У сотрудника нет активных назначений"
                description="Ниже при этом может быть доступна история результатов по темам."
              />
            )
          ) : null}

          <Typography.Title level={4} style={{ marginBottom: 0 }}>
            История и результаты по темам
          </Typography.Title>
          <ManagerialUserTopicAnalyticsTable items={filteredItems} />
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
