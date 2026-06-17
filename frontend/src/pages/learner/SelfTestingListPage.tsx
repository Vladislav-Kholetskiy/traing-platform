import { Space, Typography } from 'antd';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useSelfTestingCatalog } from '../../features/self-testing/model/useSelfTesting';
import { SelfTestingCatalog } from '../../features/self-testing/ui/SelfTestingCatalog';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

export function SelfTestingListPage() {
  const { data: actor } = useCurrentActor();
  const catalogQuery = useSelfTestingCatalog();

  if (!actor || !hasSection(actor, 'SELF_TESTING')) {
    return <ForbiddenView />;
  }

  if (catalogQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка самостоятельного обучения"
        description="Подбираем каталог модулей, доступных для самостоятельного прохождения."
      />
    );
  }

  if (catalogQuery.isError) {
    if (hasErrorStatus(catalogQuery.error, 403)) {
      return (
        <ForbiddenView
          title="Раздел самостоятельного обучения недоступен"
          description="Сейчас открыть каталог самостоятельного обучения не удалось."
        />
      );
    }

    return (
      <ErrorView
        title="Не удалось загрузить каталог самостоятельного обучения"
        description="Попробуйте открыть раздел еще раз немного позже."
        error={catalogQuery.error}
      />
    );
  }

  const tests = catalogQuery.data ?? [];

  if (tests.length === 0) {
    return (
      <>
        <Typography.Title level={2}>Самостоятельное обучение</Typography.Title>
        <EmptyState
          title="Пока нет доступных модулей"
          description="Когда в каталоге появятся элементы самостоятельного обучения, они отобразятся в этом разделе."
        />
      </>
    );
  }

  return (
    <>
      <section className="self-hero">
        <div className="self-hero-copy">
          <Typography.Title level={2} className="self-hero-title">
            Самостоятельное обучение
          </Typography.Title>
          <Typography.Paragraph className="self-hero-text">
            Выбирайте модули для самостоятельного прохождения, двигайтесь в удобном ритме и отслеживайте результаты в общей истории.
          </Typography.Paragraph>
          <Space wrap>
            <span className="stat-pill">Доступно модулей: {tests.length}</span>
            <span className="stat-pill">Формат: без назначения</span>
          </Space>
        </div>
      </section>

      <div className="page-header page-header-tight">
        <div>
          <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 8 }}>
            Каталог самостоятельного обучения
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Сначала обзор каталога, затем карточка модуля и только потом явный старт попытки.
          </Typography.Paragraph>
        </div>
      </div>

      <SelfTestingCatalog tests={tests} />
    </>
  );
}
