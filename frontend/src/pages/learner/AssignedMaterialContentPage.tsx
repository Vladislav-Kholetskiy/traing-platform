import { Typography } from 'antd';
import { useParams } from 'react-router';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAssignedMaterialContent } from '../../features/assigned-learning/model/useAssignedLearning';
import { AssignedMaterialContentView } from '../../features/assigned-learning/ui/AssignedMaterialContentView';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function AssignedMaterialContentPage() {
  const { assignmentId, materialId } = useParams();
  const { data: actor } = useCurrentActor();
  const materialQuery = useAssignedMaterialContent(assignmentId, materialId);

  if (!actor || !hasSection(actor, 'ASSIGNED_LEARNING')) {
    return <ForbiddenView />;
  }

  if (!assignmentId || !materialId) {
    return (
      <EmptyState
        title="Материал не выбран"
        description="Вернитесь к назначению и откройте нужный материал из списка темы."
      />
    );
  }

  if (materialQuery.isLoading) {
    return (
      <LoadingView
        title="Открываем материал"
        description="Подтягиваем содержимое, тему и контекст назначения."
      />
    );
  }

  if (materialQuery.isError) {
    if (hasErrorStatus(materialQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Материал недоступен"
          description="Сейчас открыть этот материал в вашем контуре не удалось."
        />
      );
    }

    if (hasErrorStatus(materialQuery.error, 404)) {
      return (
        <>
          <Typography.Title level={2}>Материал обучения</Typography.Title>
          <EmptyState
            title="Материал не найден"
            description="Внутри этого назначения такой материал не опубликован или уже недоступен."
          />
        </>
      );
    }

    return (
      <ErrorView
        title="Не удалось открыть материал"
        description="Попробуйте открыть материал ещё раз немного позже."
        error={materialQuery.error}
      />
    );
  }

  if (!materialQuery.data) {
    return (
      <>
        <Typography.Title level={2}>Материал обучения</Typography.Title>
        <EmptyState
          title="Содержимое пока недоступно"
          description="Backend не вернул содержимое материала для этого назначения."
        />
      </>
    );
  }

  return <AssignedMaterialContentView assignmentId={assignmentId} content={materialQuery.data} />;
}
