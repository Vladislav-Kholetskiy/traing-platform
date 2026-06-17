import { Col, Row, Typography } from 'antd';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useManagerialCurrentSupervision } from '../../features/managerial/model/useManagerial';
import { ManagerialCurrentSupervisionTable } from '../../features/managerial/ui/ManagerialCurrentSupervisionTable';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function ManagerCurrentSupervisionPage() {
  const { data: actor } = useCurrentActor();
  const supervisionQuery = useManagerialCurrentSupervision(Boolean(actor?.roles.includes('MANAGER')));

  if (!actor || !actor.roles.includes('MANAGER')) {
    return <ForbiddenView />;
  }

  if (supervisionQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка текущего контроля"
        description="Собираем активные назначения сотрудников под вашим контуром управления."
      />
    );
  }

  if (supervisionQuery.isError) {
    return (
      <ErrorView
        title="Не удалось загрузить текущий контроль"
        description="Попробуйте открыть экран ещё раз немного позже."
        error={supervisionQuery.error}
      />
    );
  }

  const items = supervisionQuery.data ?? [];

  return (
    <div className="home-page home-page-compact">
      <section className="self-hero">
        <div className="self-hero-copy">
          <Typography.Title level={2} className="self-hero-title">
            Контроль обучения
          </Typography.Title>
          <Typography.Paragraph className="self-hero-text">
            Операционный экран для быстрого обзора активных назначений, сроков и статусов в вашем управленческом периметре.
          </Typography.Paragraph>
        </div>
      </section>

      {items.length === 0 ? (
        <EmptyState
          title="Нет активных назначений в текущем периметре"
          description="Когда появятся подчинённые или активные задания, они будут показаны здесь."
        />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            <Col xs={24} md={8}>
              <CardMetric label="Назначений в контроле" value={String(items.length)} />
            </Col>
            <Col xs={24} md={8}>
              <CardMetric
                label="Просрочено"
                value={String(items.filter((item) => item.assignmentStatus === 'OVERDUE').length)}
              />
            </Col>
            <Col xs={24} md={8}>
              <CardMetric
                label="Активно"
                value={String(
                  items.filter(
                    (item) =>
                      item.assignmentStatus === 'ASSIGNED' ||
                      item.assignmentStatus === 'ACTIVE' ||
                      item.assignmentStatus === 'IN_PROGRESS',
                  ).length,
                )}
              />
            </Col>
          </Row>
          <ManagerialCurrentSupervisionTable items={items} />
        </>
      )}
    </div>
  );
}

function CardMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="home-summary-card">
      <span className="home-summary-label">{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
