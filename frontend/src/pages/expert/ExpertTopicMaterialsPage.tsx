import { Alert, Button, Descriptions, Form, Input, InputNumber, Select, Space, Table, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type {
  CreateMaterialRequest,
  Material,
  UpdateMaterialRequest,
} from '../../features/expert-content/model/expertContent';
import {
  useArchiveMaterialMutation,
  useCreateMaterialMutation,
  useLifecycleMaterial,
  useMaterial,
  useMaterialsByTopic,
  usePublishMaterialMutation,
  useUpdateMaterialMutation,
} from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeContentStatus, localizeMaterialType } from '../../shared/ui/presentation';

export function ExpertTopicMaterialsPage() {
  const { topicId: topicIdParam } = useParams();
  const topicId = Number(topicIdParam);
  const { data: actor } = useCurrentActor();
  const materialsQuery = useMaterialsByTopic(topicId, Boolean(actor));
  const createMutation = useCreateMaterialMutation();
  const [editingMaterialId, setEditingMaterialId] = useState<number | null>(null);
  const materialQuery = useMaterial(editingMaterialId ?? undefined, Boolean(actor) && Boolean(editingMaterialId));
  const lifecycleQuery = useLifecycleMaterial(editingMaterialId ?? undefined, Boolean(actor) && Boolean(editingMaterialId));
  const updateMutation = useUpdateMaterialMutation(editingMaterialId ?? undefined);
  const publishMutation = usePublishMaterialMutation(editingMaterialId ?? undefined);
  const archiveMutation = useArchiveMaterialMutation(editingMaterialId ?? undefined);
  const [createForm] = Form.useForm<CreateMaterialRequest>();
  const [editForm] = Form.useForm<UpdateMaterialRequest>();

  const selectedMaterial = materialQuery.data;

  useEffect(() => {
    if (!selectedMaterial) {
      editForm.resetFields();
      return;
    }

    editForm.setFieldsValue({
      name: selectedMaterial.name,
      description: selectedMaterial.description ?? undefined,
      body: selectedMaterial.body ?? undefined,
      videoUrl: selectedMaterial.videoUrl ?? undefined,
      materialType: selectedMaterial.materialType ?? 'TEXT',
      sortOrder: selectedMaterial.sortOrder ?? 0,
    });
  }, [editForm, selectedMaterial]);

  if (!actor || !canAccessExpertArea(actor)) return <ForbiddenView />;
  if (materialsQuery.isLoading) return <LoadingView title="Загрузка материалов" />;
  if (materialsQuery.isError) return <ErrorView title="Не удалось загрузить материалы" error={materialsQuery.error} />;

  const commandError =
    createMutation.error ??
    updateMutation.error ??
    publishMutation.error ??
    archiveMutation.error;

  const materialColumns = [
    { title: 'ID', dataIndex: 'id' },
    { title: 'Название', dataIndex: 'name' },
    {
      title: 'Тип',
      dataIndex: 'materialType',
      render: (value: string | null | undefined) => localizeMaterialType(value ?? undefined),
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      render: (value: string | null | undefined) => localizeContentStatus(value ?? undefined),
    },
    {
      title: 'Обновлён',
      dataIndex: 'updatedAt',
      render: (value: string | null | undefined) => formatUiDate(value ?? undefined),
    },
    {
      title: 'Действие',
      render: (_: unknown, material: Material) => (
        <Button size="small" onClick={() => setEditingMaterialId(material.id)}>
          Редактировать
        </Button>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro title={`Материалы темы #${topicId}`} description="Создание, просмотр и редактирование материалов темы." />

      {commandError ? (
        <Alert type="error" showIcon message="Команда по материалу завершилась ошибкой" description={String(commandError)} />
      ) : null}

      {updateMutation.isSuccess && editingMaterialId ? (
        <Alert
          type="success"
          showIcon
          message="Материал обновлён"
          description="Изменения сохранены успешно."
        />
      ) : null}

      <SectionCard title="Создание материала">
        <Form<CreateMaterialRequest>
          layout="vertical"
          form={createForm}
          initialValues={{ topicId, materialType: 'TEXT', sortOrder: 0 }}
          onFinish={async (values) => {
            await createMutation.mutateAsync({ ...values, topicId });
            createForm.resetFields();
            createForm.setFieldsValue({ topicId, materialType: 'TEXT', sortOrder: 0 });
          }}
        >
          <div className="admin-form-grid">
            <Form.Item name="name" label="Название" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item name="description" label="Описание">
              <Input />
            </Form.Item>
            <Form.Item name="materialType" label="Тип" rules={[{ required: true }]}>
              <Select
                options={[
                  { label: 'TEXT', value: 'TEXT' },
                  { label: 'PDF', value: 'PDF' },
                  { label: 'DOCX', value: 'DOCX' },
                  { label: 'VIDEO', value: 'VIDEO' },
                ]}
              />
            </Form.Item>
            <Form.Item name="sortOrder" label="Порядок сортировки" rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} min={0} />
            </Form.Item>
            <Form.Item shouldUpdate={(prev, curr) => prev.materialType !== curr.materialType} noStyle>
              {({ getFieldValue }) =>
                getFieldValue('materialType') === 'VIDEO' ? (
                  <Form.Item
                    name="videoUrl"
                    label="Ссылка на видео"
                    rules={[{ required: true, message: 'Укажите ссылку на видео' }]}
                  >
                    <Input placeholder="https://... или ссылка на mp4/embed" />
                  </Form.Item>
                ) : null
              }
            </Form.Item>
          </div>
          <Form.Item name="body" label="Содержимое">
            <Input.TextArea rows={6} />
          </Form.Item>
          <Button htmlType="submit" loading={createMutation.isPending}>
            Создать материал
          </Button>
        </Form>
      </SectionCard>

      <SectionCard title="Материалы">
        <Table rowKey="id" dataSource={materialsQuery.data ?? []} pagination={{ pageSize: 10 }} columns={materialColumns} />
      </SectionCard>

      {editingMaterialId ? (
        <SectionCard title={`Редактирование материала #${editingMaterialId}`}>
          {materialQuery.isLoading ? <LoadingView title="Загрузка материала" /> : null}
          {materialQuery.isError ? <ErrorView title="Не удалось загрузить материал" error={materialQuery.error} /> : null}
          {selectedMaterial ? (
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Descriptions bordered column={2}>
                <Descriptions.Item label="Название">{selectedMaterial.name}</Descriptions.Item>
                <Descriptions.Item label="Тип">
                  {localizeMaterialType(selectedMaterial.materialType ?? undefined)}
                </Descriptions.Item>
                <Descriptions.Item label="Статус">
                  {localizeContentStatus(selectedMaterial.status ?? undefined)}
                </Descriptions.Item>
                <Descriptions.Item label="Обновлён">
                  {formatUiDate(selectedMaterial.updatedAt ?? undefined)}
                </Descriptions.Item>
                <Descriptions.Item label="Описание" span={2}>
                  {selectedMaterial.description || 'Не указано'}
                </Descriptions.Item>
                <Descriptions.Item label="Ссылка на видео" span={2}>
                  {selectedMaterial.videoUrl || 'Не указано'}
                </Descriptions.Item>
              </Descriptions>

              <SectionCard title="Текущее состояние">
                {lifecycleQuery.isLoading ? <Typography.Text type="secondary">Загружаем состояние материала.</Typography.Text> : null}
                {lifecycleQuery.isError ? (
                  <Alert
                    type="error"
                    showIcon
                    message="Не удалось загрузить состояние материала"
                    description={String(lifecycleQuery.error)}
                  />
                ) : null}
                {lifecycleQuery.data ? (
                  <Descriptions bordered column={2}>
                    <Descriptions.Item label="Статус">
                      {localizeContentStatus(lifecycleQuery.data.status ?? undefined)}
                    </Descriptions.Item>
                    <Descriptions.Item label="Обновлён">
                      {formatUiDate(lifecycleQuery.data.updatedAt ?? undefined)}
                    </Descriptions.Item>
                    <Descriptions.Item label="Порядок сортировки">
                      {lifecycleQuery.data.sortOrder ?? 0}
                    </Descriptions.Item>
                    <Descriptions.Item label="Тип">
                      {localizeMaterialType(lifecycleQuery.data.materialType ?? undefined)}
                    </Descriptions.Item>
                  </Descriptions>
                ) : null}
              </SectionCard>

              <Space wrap>
                <Button onClick={() => publishMutation.mutate()} loading={publishMutation.isPending}>
                  Опубликовать
                </Button>
                <Button onClick={() => archiveMutation.mutate()} loading={archiveMutation.isPending}>
                  Архивировать
                </Button>
              </Space>

              <Form<UpdateMaterialRequest>
                layout="vertical"
                form={editForm}
                onFinish={async (values) => {
                  await updateMutation.mutateAsync(values);
                }}
              >
                <div className="admin-form-grid">
                  <Form.Item name="name" label="Название" rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                  <Form.Item name="description" label="Описание">
                    <Input />
                  </Form.Item>
                  <Form.Item name="materialType" label="Тип" rules={[{ required: true }]}>
                    <Select
                      options={[
                        { label: 'TEXT', value: 'TEXT' },
                        { label: 'PDF', value: 'PDF' },
                        { label: 'DOCX', value: 'DOCX' },
                        { label: 'VIDEO', value: 'VIDEO' },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item name="sortOrder" label="Порядок сортировки" rules={[{ required: true }]}>
                    <InputNumber style={{ width: '100%' }} min={0} />
                  </Form.Item>
                  <Form.Item shouldUpdate={(prev, curr) => prev.materialType !== curr.materialType} noStyle>
                    {({ getFieldValue }) =>
                      getFieldValue('materialType') === 'VIDEO' ? (
                        <Form.Item
                          name="videoUrl"
                          label="Ссылка на видео"
                          rules={[{ required: true, message: 'Укажите ссылку на видео' }]}
                        >
                          <Input placeholder="https://... или ссылка на mp4/embed" />
                        </Form.Item>
                      ) : null
                    }
                  </Form.Item>
                </div>
                <Form.Item name="body" label="Содержимое">
                  <Input.TextArea rows={8} />
                </Form.Item>
                <Space wrap>
                  <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>
                    Сохранить изменения
                  </Button>
                  <Button onClick={() => setEditingMaterialId(null)} disabled={updateMutation.isPending}>
                    Закрыть форму
                  </Button>
                </Space>
              </Form>
            </Space>
          ) : null}
        </SectionCard>
      ) : null}
    </Space>
  );
}
