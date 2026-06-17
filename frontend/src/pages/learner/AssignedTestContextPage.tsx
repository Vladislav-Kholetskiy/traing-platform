import { Typography } from 'antd';
import { useParams } from 'react-router';
import { AssignedTestContextView } from '../../features/assigned-learning/ui/AssignedTestContextView';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAssignedTestContext } from '../../features/assigned-learning/model/useAssignedLearning';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function AssignedTestContextPage() {
  const { assignmentId, assignmentTestId } = useParams();
  const { data: actor } = useCurrentActor();
  const testContextQuery = useAssignedTestContext(assignmentId, assignmentTestId);

  if (!actor || !hasSection(actor, 'ASSIGNED_LEARNING')) {
    return <ForbiddenView />;
  }

  if (!assignmentId || !assignmentTestId) {
    return (
      <EmptyState
        title="Тест не выбран"
        description="Вернитесь к материалам и откройте нужный тест."
      />
    );
  }

  if (testContextQuery.isLoading) {
    return <LoadingView title="Загрузка теста" description="Подготавливаем вопросы и варианты ответа." />;
  }

  if (testContextQuery.isError) {
    if (hasErrorStatus(testContextQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Тест недоступен"
          description="Сейчас открыть этот тест не удалось."
        />
      );
    }

    if (hasErrorStatus(testContextQuery.error, 404)) {
      return (
        <>
          <Typography.Title level={2}>Тест по назначению</Typography.Title>
          <EmptyState
            title="Тест не найден"
            description="Возможно, он больше недоступен для прохождения."
          />
        </>
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить тест"
        description="Попробуйте открыть тест ещё раз немного позже."
        error={testContextQuery.error}
      />
    );
  }

  const context = testContextQuery.data;
  if (!context) {
    return (
      <>
        <Typography.Title level={2}>Тест по назначению</Typography.Title>
        <EmptyState
          title="Тест пока недоступен"
          description="Не удалось получить структуру вопросов."
        />
      </>
    );
  }

  return (
    <AssignedTestContextView
      assignmentId={assignmentId}
      assignmentTestId={assignmentTestId}
      context={context}
    />
  );
}
