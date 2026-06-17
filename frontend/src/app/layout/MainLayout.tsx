import { Layout } from 'antd';
import { Outlet } from 'react-router';
import { getEffectiveDemoActorId } from '../../features/auth/model/demoActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { ActorBootstrapView } from '../../features/auth/ui/ActorBootstrapView';
import { ErrorView } from '../../shared/ui/ErrorView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { isInteractiveActorResolutionError } from '../../shared/api/apiError';
import { HeaderBar } from './HeaderBar';
import { Sidebar } from './Sidebar';

const { Header, Sider, Content } = Layout;

export function MainLayout() {
  const currentActorQuery = useCurrentActor();
  const selectedDemoActorId = getEffectiveDemoActorId();

  if (currentActorQuery.isLoading) {
    return <LoadingView title="Загрузка приложения" description="Подготавливаем рабочее пространство." />;
  }

  if (
    currentActorQuery.isError &&
    !selectedDemoActorId &&
    isInteractiveActorResolutionError(currentActorQuery.error)
  ) {
    return <ActorBootstrapView />;
  }

  if (currentActorQuery.isError) {
    return (
      <ErrorView
        title="Не удалось открыть приложение"
        error={currentActorQuery.error}
        description="Не получилось загрузить данные текущего пользователя."
      />
    );
  }

  const actor = currentActorQuery.data;

  if (!actor) {
    return <LoadingView title="Загрузка приложения" description="Ожидаем данные пользователя." />;
  }

  return (
    <Layout className="app-shell">
      <Sider width={320} breakpoint="lg" collapsedWidth={0} theme="light" className="app-sider">
        <Sidebar actor={actor} />
      </Sider>
      <Layout className="app-main">
        <Header className="app-header">
          <HeaderBar actor={actor} />
        </Header>
        <Content className="app-content">
          <div className="page-surface">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
