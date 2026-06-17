import { Alert, Button, Descriptions, Form, Input, InputNumber, Space, Table, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type {
  SaveTestQuestionRequest,
  TestQuestionBinding,
  UpdateTestRequest,
} from '../../features/expert-content/model/expertContent';
import {
  useArchiveTestMutation,
  useCreateTestQuestionMutation,
  useDeleteTestQuestionMutation,
  useLifecycleTest,
  usePublishTestMutation,
  useTest,
  useTestQuestions,
  useUpdateTestMutation,
  useUpdateTestQuestionMutation,
} from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatPercent, formatUiDate, localizeContentStatus, localizeTestType } from '../../shared/ui/presentation';

export function ExpertTestDetailPage() {
  const { testId: testIdParam } = useParams();
  const testId = Number(testIdParam);
  const { data: actor } = useCurrentActor();
  const testQuery = useTest(testId, Boolean(actor));
  const lifecycleQuery = useLifecycleTest(testId, Boolean(actor));
  const bindingsQuery = useTestQuestions(testId, Boolean(actor));
  const updateMutation = useUpdateTestMutation(testId);
  const publishMutation = usePublishTestMutation(testId);
  const archiveMutation = useArchiveTestMutation(testId);
  const createBindingMutation = useCreateTestQuestionMutation(testId);
  const deleteBindingMutation = useDeleteTestQuestionMutation(testId);
  const [editingBindingId, setEditingBindingId] = useState<number | null>(null);
  const updateBindingMutation = useUpdateTestQuestionMutation(testId, editingBindingId ?? undefined);
  const [testForm] = Form.useForm<UpdateTestRequest>();
  const [bindingForm] = Form.useForm<SaveTestQuestionRequest>();
  const [editBindingForm] = Form.useForm<SaveTestQuestionRequest>();

  const editingBinding = (bindingsQuery.data ?? []).find((binding) => binding.id === editingBindingId) ?? null;

  useEffect(() => {
    if (!editingBinding) {
      editBindingForm.resetFields();
      return;
    }

    editBindingForm.setFieldsValue({
      questionId: editingBinding.questionId,
      displayOrder: editingBinding.displayOrder ?? 0,
      weight: editingBinding.weight ?? 1,
    });
  }, [editBindingForm, editingBinding]);

  if (!actor || !canAccessExpertArea(actor)) return <ForbiddenView />;
  if (testQuery.isLoading || bindingsQuery.isLoading || lifecycleQuery.isLoading) return <LoadingView title="Загрузка теста" />;
  if (testQuery.isError || bindingsQuery.isError) return <ErrorView title="Не удалось загрузить тест" error={testQuery.error ?? bindingsQuery.error} />;

  const test = testQuery.data;
  if (!test) return <ErrorView title="Тест не найден" />;

  const commandError =
    updateMutation.error ??
    publishMutation.error ??
    archiveMutation.error ??
    createBindingMutation.error ??
    updateBindingMutation.error ??
    deleteBindingMutation.error;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title={test.name}
        description="Карточка теста, его состояние и состав вопросов."
        extra={
          <Space>
            <Button onClick={() => publishMutation.mutate()} loading={publishMutation.isPending}>
              Опубликовать
            </Button>
            <Button onClick={() => archiveMutation.mutate()} loading={archiveMutation.isPending}>
              Архивировать
            </Button>
          </Space>
        }
      />

      {commandError ? (
        <Alert type="error" showIcon message="Команда по тесту завершилась ошибкой" description={String(commandError)} />
      ) : null}

      {updateBindingMutation.isSuccess && editingBindingId ? (
        <Alert
          type="success"
          showIcon
          message="Привязка вопроса обновлена"
          description="Изменения сохранены успешно."
        />
      ) : null}

      <SectionCard title="Карточка теста">
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Тип">{localizeTestType(test.testType ?? undefined)}</Descriptions.Item>
          <Descriptions.Item label="Статус">{localizeContentStatus(test.status ?? undefined)}</Descriptions.Item>
          <Descriptions.Item label="Порог">{formatPercent(test.thresholdPercent)}</Descriptions.Item>
          <Descriptions.Item label="Правило оценки">{test.scoringPolicyCode || 'Не указано'}</Descriptions.Item>
          <Descriptions.Item label="Описание" span={2}>{test.description || 'Не указано'}</Descriptions.Item>
        </Descriptions>
      </SectionCard>

      <SectionCard title="Текущее состояние">
        {lifecycleQuery.isError ? (
          <Alert type="error" showIcon message="Не удалось загрузить состояние теста" description={String(lifecycleQuery.error)} />
        ) : lifecycleQuery.data ? (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="Статус">
              {localizeContentStatus(lifecycleQuery.data.status ?? undefined)}
            </Descriptions.Item>
            <Descriptions.Item label="Обновлён">
              {formatUiDate(lifecycleQuery.data.updatedAt ?? undefined)}
            </Descriptions.Item>
            <Descriptions.Item label="Порог">
              {formatPercent(lifecycleQuery.data.thresholdPercent)}
            </Descriptions.Item>
            <Descriptions.Item label="Тип теста">
              {localizeTestType(lifecycleQuery.data.testType ?? undefined)}
            </Descriptions.Item>
          </Descriptions>
        ) : null}
      </SectionCard>

      <SectionCard title="Редактирование теста">
        <Form<UpdateTestRequest>
          layout="vertical"
          form={testForm}
          initialValues={{
            name: test.name,
            description: test.description || undefined,
            testType: test.testType ?? 'CONTROL',
            thresholdPercent: test.thresholdPercent ?? undefined,
            scoringPolicyCode: test.scoringPolicyCode || undefined,
            sortOrder: test.sortOrder ?? undefined,
          }}
          onFinish={(values) => updateMutation.mutate(values)}
        >
          <div className="admin-form-grid">
            <Form.Item name="name" label="Название" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item name="description" label="Описание"><Input /></Form.Item>
            <Form.Item name="testType" label="Тип" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item name="thresholdPercent" label="Порог"><Input /></Form.Item>
            <Form.Item name="scoringPolicyCode" label="Правило оценки"><Input /></Form.Item>
            <Form.Item name="sortOrder" label="Порядок сортировки"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
          </div>
          <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>Сохранить</Button>
        </Form>
      </SectionCard>

      <SectionCard title="Добавление вопроса в тест">
        <Form<SaveTestQuestionRequest>
          layout="vertical"
          form={bindingForm}
          initialValues={{ displayOrder: 0, weight: 1 }}
          onFinish={async (values) => {
            await createBindingMutation.mutateAsync(values);
            bindingForm.resetFields();
            bindingForm.setFieldsValue({ displayOrder: 0, weight: 1 });
          }}
        >
          <div className="admin-form-grid">
            <Form.Item name="questionId" label="ID вопроса" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={1} /></Form.Item>
            <Form.Item name="displayOrder" label="Порядок показа" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
            <Form.Item name="weight" label="Вес" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={0.000001} /></Form.Item>
          </div>
          <Button htmlType="submit" loading={createBindingMutation.isPending}>Добавить вопрос</Button>
        </Form>
      </SectionCard>

      <SectionCard title="Состав теста">
        <Table
          rowKey="id"
          dataSource={bindingsQuery.data ?? []}
          pagination={false}
          columns={[
            { title: 'ID привязки', dataIndex: 'id' },
            { title: 'ID вопроса', dataIndex: 'questionId' },
            { title: 'Порядок показа', dataIndex: 'displayOrder' },
            { title: 'Вес', dataIndex: 'weight' },
            {
              title: 'Действие',
              render: (_: unknown, binding: TestQuestionBinding) => (
                <Space wrap>
                  <Button size="small" onClick={() => setEditingBindingId(binding.id)}>
                    Редактировать
                  </Button>
                  <Button size="small" onClick={() => deleteBindingMutation.mutate(binding.id)}>
                    Удалить
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </SectionCard>

      {editingBinding ? (
        <SectionCard title={`Редактирование привязки #${editingBinding.id}`}>
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              Для обновления привязки можно изменить вопрос, порядок показа и вес.
            </Typography.Text>
            <Form<SaveTestQuestionRequest>
              layout="vertical"
              form={editBindingForm}
              onFinish={(values) => updateBindingMutation.mutate(values)}
            >
              <div className="admin-form-grid">
                <Form.Item name="questionId" label="ID вопроса" rules={[{ required: true }]}>
                  <InputNumber style={{ width: '100%' }} min={1} />
                </Form.Item>
                <Form.Item name="displayOrder" label="Порядок показа" rules={[{ required: true }]}>
                  <InputNumber style={{ width: '100%' }} min={0} />
                </Form.Item>
                <Form.Item name="weight" label="Вес" rules={[{ required: true }]}>
                  <InputNumber style={{ width: '100%' }} min={0.000001} />
                </Form.Item>
              </div>
              <Space wrap>
                <Button type="primary" htmlType="submit" loading={updateBindingMutation.isPending}>
                  Сохранить изменения
                </Button>
                <Button onClick={() => setEditingBindingId(null)} disabled={updateBindingMutation.isPending}>
                  Закрыть форму
                </Button>
              </Space>
            </Form>
          </Space>
        </SectionCard>
      ) : null}
    </Space>
  );
}
