import { Typography } from 'antd';
import { SelfResultHistoryList } from '../../features/self-results/ui/SelfResultHistoryList';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useSelfResultHistory } from '../../features/self-results/model/useSelfResults';
import { ErrorView } from '../../shared/ui/ErrorView';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function SelfResultsPage() {
  const { data: actor } = useCurrentActor();
  const historyQuery = useSelfResultHistory();

  if (!actor || !hasSection(actor, 'SELF_RESULTS')) {
    return <ForbiddenView />;
  }

  if (historyQuery.isLoading) {
    return <LoadingView title="Загрузка результатов" description="Подготавливаем историю прохождения тестов." />;
  }

  if (historyQuery.isError) {
    return (
      <ErrorView
        title="Не удалось загрузить результаты"
        description="Попробуйте открыть раздел ещё раз немного позже."
        error={historyQuery.error}
      />
    );
  }

  if (!historyQuery.data || historyQuery.data.length === 0) {
    return (
      <>
        <Typography.Title level={2}>Мои результаты</Typography.Title>
        <EmptyState
          title="Результатов пока нет"
          description="После прохождения тестов история появится в этом разделе."
        />
      </>
    );
  }

  return (
    <>
      <div className="page-header page-header-tight">
        <div>
          <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 8 }}>
            Мои результаты
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Здесь собраны результаты уже завершённых тестов.
          </Typography.Paragraph>
        </div>
      </div>
      <SelfResultHistoryList results={historyQuery.data} />
    </>
  );
}
