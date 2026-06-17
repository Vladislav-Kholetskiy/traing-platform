import { Typography } from 'antd';
import { useParams } from 'react-router';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useSelfVisibleTest } from '../../features/self-testing/model/useSelfTesting';
import { SelfTestContextView } from '../../features/self-testing/ui/SelfTestContextView';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function SelfTestDetailPage() {
  const { testId } = useParams();
  const { data: actor } = useCurrentActor();
  const testQuery = useSelfVisibleTest(testId);

  if (!actor || !hasSection(actor, 'SELF_TESTING')) {
    return <ForbiddenView />;
  }

  if (!testId) {
    return (
      <EmptyState
        title="Модуль не выбран"
        description="Вернитесь в каталог и откройте нужный модуль самостоятельного обучения."
      />
    );
  }

  if (testQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка модуля"
        description="Подготавливаем состав вопросов и обзор модуля самостоятельного обучения."
      />
    );
  }

  if (testQuery.isError) {
    if (hasErrorStatus(testQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Модуль недоступен"
          description="Сейчас открыть этот модуль самостоятельного обучения не удалось."
        />
      );
    }

    if (hasErrorStatus(testQuery.error, 404)) {
      return (
        <>
          <Typography.Title level={2}>Самостоятельное обучение</Typography.Title>
          <EmptyState
            title="Модуль не найден"
            description="Возможно, он больше недоступен для самостоятельного прохождения."
          />
        </>
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить модуль"
        description="Попробуйте открыть карточку модуля еще раз немного позже."
        error={testQuery.error}
      />
    );
  }

  if (!testQuery.data) {
    return (
      <>
        <Typography.Title level={2}>Самостоятельное обучение</Typography.Title>
        <EmptyState
          title="Модуль пока недоступен"
          description="Не удалось получить структуру вопросов."
        />
      </>
    );
  }

  return <SelfTestContextView testId={testId} test={testQuery.data} />;
}
