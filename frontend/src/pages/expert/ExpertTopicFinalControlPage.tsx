import { Alert, Button, Space, Table, Typography } from 'antd';
import { useParams } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import {
  useActiveFinalTest,
  useAssignActiveFinalTestMutation,
  useClearActiveFinalTestMutation,
  useEligibleFinalControlTests,
  useReplaceActiveFinalTestMutation,
} from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatPercent, localizeTestType } from '../../shared/ui/presentation';

export function ExpertTopicFinalControlPage() {
  const { topicId: topicIdParam } = useParams();
  const topicId = Number(topicIdParam);
  const { data: actor } = useCurrentActor();
  const activeTestQuery = useActiveFinalTest(topicId, Boolean(actor));
  const eligibleTestsQuery = useEligibleFinalControlTests(topicId, Boolean(actor));
  const assignMutation = useAssignActiveFinalTestMutation(topicId);
  const replaceMutation = useReplaceActiveFinalTestMutation(topicId);
  const clearMutation = useClearActiveFinalTestMutation(topicId);

  if (!actor || !canAccessExpertArea(actor)) return <ForbiddenView />;
  if (activeTestQuery.isLoading || eligibleTestsQuery.isLoading) return <LoadingView title="Загрузка итогового контроля" />;
  if (eligibleTestsQuery.isError) return <ErrorView title="Не удалось загрузить доступные тесты" error={eligibleTestsQuery.error} />;
  if (activeTestQuery.isError && String(activeTestQuery.error).includes('404') === false) {
    return <ErrorView title="Не удалось загрузить текущий итоговый тест" error={activeTestQuery.error} />;
  }

  const commandError = assignMutation.error ?? replaceMutation.error ?? clearMutation.error;
  const activeTest = activeTestQuery.data;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title={`Итоговый контроль темы #${topicId}`}
        description="Здесь можно назначить, заменить или снять итоговый тест для темы."
        extra={
          <Button danger onClick={() => clearMutation.mutate()} loading={clearMutation.isPending}>
            Снять итоговый тест
          </Button>
        }
      />
      {commandError ? <Alert type="error" showIcon message="Команда по итоговому контролю завершилась ошибкой" description={String(commandError)} /> : null}
      <SectionCard title="Текущий итоговый тест">
        {activeTest ? (
          <Space direction="vertical">
            <Typography.Text strong>{activeTest.name}</Typography.Text>
            <Typography.Text type="secondary">{`${localizeTestType(activeTest.testType ?? undefined)} / порог ${formatPercent(activeTest.thresholdPercent ?? undefined)}`}</Typography.Text>
          </Space>
        ) : (
          <Typography.Text type="secondary">Сейчас итоговый тест не назначен.</Typography.Text>
        )}
      </SectionCard>
      <SectionCard title="Доступные тесты">
        <Table rowKey="id" dataSource={eligibleTestsQuery.data ?? []} pagination={false} columns={[
          { title: 'ID', dataIndex: 'id' },
          { title: 'Тест', dataIndex: 'name' },
          { title: 'Тип', dataIndex: 'testType', render: (value) => localizeTestType(value ?? undefined) },
          { title: 'Порог', dataIndex: 'thresholdPercent', render: (value) => formatPercent(value) },
          {
            title: 'Действие',
            render: (_, test) => (
              <Space>
                <Button size="small" onClick={() => assignMutation.mutate(test.id)}>Назначить</Button>
                <Button size="small" onClick={() => replaceMutation.mutate(test.id)}>Заменить</Button>
              </Space>
            ),
          },
        ]} />
      </SectionCard>
    </Space>
  );
}
