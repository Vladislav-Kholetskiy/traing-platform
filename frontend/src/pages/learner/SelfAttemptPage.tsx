import { useEffect, useRef } from 'react';
import { Typography } from 'antd';
import { useParams } from 'react-router';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import {
  useCurrentSelfAttempt,
  useSelfVisibleTest,
  useStartOrContinueSelfAttempt,
} from '../../features/self-testing/model/useSelfTesting';
import { SelfAttemptShell } from '../../features/self-testing/ui/SelfAttemptShell';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function SelfAttemptPage() {
  const { testId } = useParams();
  const { data: actor } = useCurrentActor();
  const testQuery = useSelfVisibleTest(testId);
  const currentAttemptQuery = useCurrentSelfAttempt(testId);
  const startAttemptMutation = useStartOrContinueSelfAttempt(testId);
  const autoStartRequestedRef = useRef(false);
  const currentAttempt = startAttemptMutation.data ?? currentAttemptQuery.data;
  const canStartAttempt = Boolean(testId) && (hasErrorStatus(currentAttemptQuery.error, 404) || !currentAttempt);

  useEffect(() => {
    if (!testId || !testQuery.data || currentAttempt || !canStartAttempt) {
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
  }, [canStartAttempt, currentAttempt, startAttemptMutation, testId, testQuery.data]);

  if (!actor || !hasSection(actor, 'SELF_TESTING')) {
    return <ForbiddenView />;
  }

  if (!testId) {
    return (
      <EmptyState
        title="Попытка недоступна"
        description="Вернитесь к модулю и начните прохождение заново."
      />
    );
  }

  if (testQuery.isLoading || currentAttemptQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка попытки"
        description="Подготавливаем модуль и текущее состояние самостоятельного прохождения."
      />
    );
  }

  if (testQuery.isError) {
    if (hasErrorStatus(testQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Модуль недоступен"
          description="Сейчас открыть модуль для прохождения не удалось."
        />
      );
    }

    if (hasErrorStatus(testQuery.error, 404)) {
      return (
        <>
          <Typography.Title level={2}>Попытка самостоятельного обучения</Typography.Title>
          <EmptyState
            title="Модуль не найден"
            description="Не удалось подготовить вопросы для прохождения."
          />
        </>
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить модуль"
        description="Попробуйте открыть попытку еще раз немного позже."
        error={testQuery.error}
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
        description="Попробуйте открыть этот экран еще раз немного позже."
        error={currentAttemptQuery.error}
      />
    );
  }

  if (!testQuery.data) {
    return (
      <>
        <Typography.Title level={2}>Попытка самостоятельного обучения</Typography.Title>
        <EmptyState
          title="Попытка пока недоступна"
          description="Не удалось получить вопросы для этого модуля."
        />
      </>
    );
  }

  return (
    <SelfAttemptShell
      testId={testId}
      test={testQuery.data}
      currentAttempt={currentAttempt}
    />
  );
}
