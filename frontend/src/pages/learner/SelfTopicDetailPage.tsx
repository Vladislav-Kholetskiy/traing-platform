import { Typography } from 'antd';
import { useParams } from 'react-router';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useSelfVisibleTopic } from '../../features/self-testing/model/useSelfTesting';
import { SelfTopicContentView } from '../../features/self-testing/ui/SelfTopicContentView';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function SelfTopicDetailPage() {
  const { topicId } = useParams();
  const { data: actor } = useCurrentActor();
  const topicQuery = useSelfVisibleTopic(topicId);

  if (!actor || !hasSection(actor, 'SELF_TESTING')) {
    return <ForbiddenView />;
  }

  if (!topicId) {
    return (
      <EmptyState
        title="Тема не выбрана"
        description="Вернитесь в каталог самостоятельного обучения и откройте нужную тему."
      />
    );
  }

  if (topicQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка темы"
        description="Подготавливаем материалы и описание темы для самостоятельного обучения."
      />
    );
  }

  if (topicQuery.isError) {
    if (hasErrorStatus(topicQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Тема недоступна"
          description="Сейчас открыть эту тему самостоятельного обучения не удалось."
        />
      );
    }

    if (hasErrorStatus(topicQuery.error, 404)) {
      return (
        <>
          <Typography.Title level={2}>Самостоятельное обучение</Typography.Title>
          <EmptyState
            title="Тема не найдена"
            description="Возможно, она больше не опубликована для самостоятельного изучения."
          />
        </>
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить тему"
        description="Попробуйте открыть материалы темы ещё раз немного позже."
        error={topicQuery.error}
      />
    );
  }

  if (!topicQuery.data) {
    return (
      <>
        <Typography.Title level={2}>Самостоятельное обучение</Typography.Title>
        <EmptyState
          title="Тема пока недоступна"
          description="Не удалось получить материалы для этой темы."
        />
      </>
    );
  }

  return <SelfTopicContentView topic={topicQuery.data} />;
}
