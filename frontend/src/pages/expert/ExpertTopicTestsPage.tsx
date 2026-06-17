import { Alert, Button, Form, Input, InputNumber, Select, Space, Table } from 'antd';
import { Link, useParams } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type { CreateTestRequest } from '../../features/expert-content/model/expertContent';
import { useCreateTestMutation, useTestsByTopic } from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatPercent, formatUiDate, localizeContentStatus, localizeTestType } from '../../shared/ui/presentation';

export function ExpertTopicTestsPage() {
  const { topicId: topicIdParam } = useParams();
  const topicId = Number(topicIdParam);
  const { data: actor } = useCurrentActor();
  const testsQuery = useTestsByTopic(topicId, Boolean(actor));
  const createMutation = useCreateTestMutation();
  const [form] = Form.useForm<CreateTestRequest>();

  if (!actor || !canAccessExpertArea(actor)) return <ForbiddenView />;
  if (testsQuery.isLoading) return <LoadingView title="Загрузка тестов" />;
  if (testsQuery.isError) return <ErrorView title="Не удалось загрузить тесты" error={testsQuery.error} />;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro title={`Тесты темы #${topicId}`} description="Создание тестов и переход к составу теста." />
      {createMutation.isError ? <Alert type="error" showIcon message="Создание теста завершилось ошибкой" description={String(createMutation.error)} /> : null}
      <SectionCard title="Создание теста">
        <Form<CreateTestRequest> layout="vertical" form={form} initialValues={{ topicId, testType: 'CONTROL' }} onFinish={(values) => createMutation.mutate({ ...values, topicId })}>
          <div className="admin-form-grid">
            <Form.Item name="name" label="Название" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item name="description" label="Описание"><Input /></Form.Item>
            <Form.Item name="testType" label="Тип" rules={[{ required: true }]}><Select options={[{ label: 'CONTROL', value: 'CONTROL' }, { label: 'TRAINING', value: 'TRAINING' }, { label: 'ENTRANCE', value: 'ENTRANCE' }, { label: 'AUXILIARY', value: 'AUXILIARY' }, { label: 'ALL_QUESTIONS', value: 'ALL_QUESTIONS' }]} /></Form.Item>
            <Form.Item name="thresholdPercent" label="Порог прохождения, %"><Input /></Form.Item>
            <Form.Item name="scoringPolicyCode" label="Правило оценки"><Input /></Form.Item>
            <Form.Item name="sortOrder" label="Порядок сортировки"><InputNumber style={{ width: '100%' }} /></Form.Item>
          </div>
          <Button htmlType="submit" loading={createMutation.isPending}>Создать тест</Button>
        </Form>
      </SectionCard>
      <SectionCard title="Тесты">
        <Table rowKey="id" dataSource={testsQuery.data ?? []} pagination={{ pageSize: 10 }} columns={[
          { title: 'ID', dataIndex: 'id' },
          { title: 'Тест', render: (_, test) => <Link to={`/expert/content/tests/${test.id}`}>{test.name}</Link> },
          { title: 'Тип', dataIndex: 'testType', render: (value) => localizeTestType(value ?? undefined) },
          { title: 'Порог', dataIndex: 'thresholdPercent', render: (value) => formatPercent(value) },
          { title: 'Статус', dataIndex: 'status', render: (value) => localizeContentStatus(value ?? undefined) },
          { title: 'Обновлён', dataIndex: 'updatedAt', render: (value) => formatUiDate(value ?? undefined) },
        ]} />
      </SectionCard>
    </Space>
  );
}
