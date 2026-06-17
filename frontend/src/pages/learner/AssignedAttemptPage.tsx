import { useEffect, useRef } from 'react';
import { Typography } from 'antd';
import { useParams } from 'react-router';
import { AssignedAttemptShell } from '../../features/assigned-attempt/ui/AssignedAttemptShell';
import {
  useCurrentAssignedAttempt,
  useStartOrContinueAssignedAttempt,
} from '../../features/assigned-attempt/model/useAssignedAttempt';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAssignedTestContext } from '../../features/assigned-learning/model/useAssignedLearning';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function AssignedAttemptPage() {
  const { assignmentId, assignmentTestId } = useParams();
  const { data: actor } = useCurrentActor();
  const testContextQuery = useAssignedTestContext(assignmentId, assignmentTestId);
  const currentAttemptQuery = useCurrentAssignedAttempt(assignmentId, assignmentTestId);
  const startAttemptMutation = useStartOrContinueAssignedAttempt(assignmentId, assignmentTestId);
  const autoStartRequestedRef = useRef(false);
  const currentAttempt = startAttemptMutation.data ?? currentAttemptQuery.data;
  const canStartAttempt =
    Boolean(assignmentId && assignmentTestId) &&
    (hasErrorStatus(currentAttemptQuery.error, 404) || !currentAttempt);

  useEffect(() => {
    if (!assignmentId || !assignmentTestId || !testContextQuery.data || currentAttempt || !canStartAttempt) {
      return;
    }

    if (startAttemptMutation.isPending || autoStartRequestedRef.current) {
      return;
    }

    autoStartRequestedRef.current = true;
    startAttemptMutation.mutate(undefined, {
      onSettled: () => {
        autoStartRequestedRef.current = false;
      },
    });
  }, [
    assignmentId,
    assignmentTestId,
    canStartAttempt,
    currentAttempt,
    startAttemptMutation,
    testContextQuery.data,
  ]);

  if (!actor || !hasSection(actor, 'ASSIGNED_LEARNING')) {
    return <ForbiddenView />;
  }

  if (!assignmentId || !assignmentTestId) {
    return (
      <EmptyState
        title="Попытка недоступна"
        description="Вернитесь к тесту и начните прохождение заново."
      />
    );
  }

  if (testContextQuery.isLoading || currentAttemptQuery.isLoading) {
    return <LoadingView title="Загрузка попытки" description="Подготавливаем тест и текущее состояние прохождения." />;
  }

  if (testContextQuery.isError) {
    if (hasErrorStatus(testContextQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Тест недоступен"
          description="Сейчас открыть тест для прохождения не удалось."
        />
      );
    }

    if (hasErrorStatus(testContextQuery.error, 404)) {
      return (
        <>
          <Typography.Title level={2}>Попытка прохождения</Typography.Title>
          <EmptyState
            title="Тест не найден"
            description="Не удалось подготовить вопросы для прохождения."
          />
        </>
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить тест"
        description="Попробуйте открыть попытку ещё раз немного позже."
        error={testContextQuery.error}
      />
    );
  }

  if (currentAttemptQuery.isError && !hasErrorStatus(currentAttemptQuery.error, 404)) {
    if (hasErrorStatus(currentAttemptQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Попытка недоступна"
          description="Сейчас открыть текущее прохождение не удалось."
        />
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить попытку"
        description="Попробуйте открыть этот экран ещё раз немного позже."
        error={currentAttemptQuery.error}
      />
    );
  }

  if (!testContextQuery.data) {
    return (
      <>
        <Typography.Title level={2}>Попытка прохождения</Typography.Title>
        <EmptyState
          title="Попытка пока недоступна"
          description="Не удалось получить вопросы для этого теста."
        />
      </>
    );
  }

  return (
    <AssignedAttemptShell
      assignmentId={assignmentId}
      assignmentTestId={assignmentTestId}
      testContext={testContextQuery.data}
      currentAttempt={currentAttempt}
    />
  );
}
