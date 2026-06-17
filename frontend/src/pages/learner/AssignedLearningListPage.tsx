import { Typography } from 'antd';
import { AssignedLearningList } from '../../features/assigned-learning/ui/AssignedLearningList';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAssignedLearningList } from '../../features/assigned-learning/model/useAssignedLearning';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function AssignedLearningListPage() {
  const { data: actor } = useCurrentActor();
  const listQuery = useAssignedLearningList();

  if (!actor || !hasSection(actor, 'ASSIGNED_LEARNING')) {
    return <ForbiddenView />;
  }

  if (listQuery.isLoading) {
    return <LoadingView title="Загрузка назначенного обучения" description="Получаем список доступных курсов." />;
  }

  if (listQuery.isError) {
    if (hasErrorStatus(listQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Раздел обучения недоступен"
          description="Сейчас открыть список назначений не удалось."
        />
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить назначенное обучение"
        description="Попробуйте обновить страницу немного позже."
        error={listQuery.error}
      />
    );
  }

  const assignments = listQuery.data ?? [];

  if (assignments.length === 0) {
    return (
      <>
        <Typography.Title level={2}>Назначенное обучение</Typography.Title>
        <EmptyState
          title="Пока нет назначенных курсов"
          description="Когда обучение будет назначено, оно появится в этом разделе."
        />
      </>
    );
  }

  return (
    <>
      <div className="page-header page-header-tight">
        <div>
          <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 8 }}>
            Назначенное обучение
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Откройте нужное назначение, чтобы перейти к материалам и тестированию.
          </Typography.Paragraph>
        </div>
      </div>
      <AssignedLearningList assignments={assignments} />
    </>
  );
}
