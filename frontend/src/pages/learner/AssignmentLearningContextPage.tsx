import { Typography } from 'antd';
import { useParams } from 'react-router';
import { LearningContextView } from '../../features/assigned-learning/ui/LearningContextView';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAssignmentLearningContext } from '../../features/assigned-learning/model/useAssignedLearning';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function AssignmentLearningContextPage() {
  const { assignmentId } = useParams();
  const { data: actor } = useCurrentActor();
  const learningContextQuery = useAssignmentLearningContext(assignmentId);

  if (!actor || !hasSection(actor, 'ASSIGNED_LEARNING')) {
    return <ForbiddenView />;
  }

  if (!assignmentId) {
    return (
      <EmptyState
        title="Назначение не выбрано"
        description="Вернитесь к списку и откройте нужный курс."
      />
    );
  }

  if (learningContextQuery.isLoading) {
    return <LoadingView title="Загрузка материалов" description="Собираем темы, материалы и тесты курса." />;
  }

  if (learningContextQuery.isError) {
    if (hasErrorStatus(learningContextQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Материалы недоступны"
          description="Сейчас открыть учебное содержание не удалось."
        />
      );
    }

    if (hasErrorStatus(learningContextQuery.error, 404)) {
      return (
        <>
          <Typography.Title level={2}>Материалы обучения</Typography.Title>
          <EmptyState
            title="Материалы не найдены"
            description="Для этого назначения материалы пока недоступны."
          />
        </>
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить материалы"
        description="Попробуйте открыть курс ещё раз немного позже."
        error={learningContextQuery.error}
      />
    );
  }

  const context = learningContextQuery.data;
  if (!context) {
    return (
      <>
        <Typography.Title level={2}>Материалы обучения</Typography.Title>
        <EmptyState
          title="Материалы пока недоступны"
          description="Не удалось получить содержимое курса."
        />
      </>
    );
  }

  return <LearningContextView assignmentId={assignmentId} context={context} />;
}
