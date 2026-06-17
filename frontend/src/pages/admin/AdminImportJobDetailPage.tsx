import { Alert, Button, Form, Input, InputNumber, Space, Table } from 'antd';
import { Link, useParams } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import {
  useApplyImportReviewMutation,
  useImportJob,
  useImportJobItems,
  useRejectImportReviewMutation,
} from '../../features/imports/model/useImports';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeImportItemStatus, localizeImportJobStatus } from '../../shared/ui/presentation';

export function AdminImportJobDetailPage() {
  const { importJobId: importJobIdParam } = useParams();
  const importJobId = Number(importJobIdParam);
  const { data: actor } = useCurrentActor();
  const jobQuery = useImportJob(importJobId, Boolean(actor));
  const itemsQuery = useImportJobItems(importJobId, undefined, Boolean(actor));
  const applyReviewMutation = useApplyImportReviewMutation();
  const rejectReviewMutation = useRejectImportReviewMutation();
  const [applyForm] = Form.useForm<{ itemId: number; matchedUserId: number }>();
  const [rejectForm] = Form.useForm<{ itemId: number; reason: string }>();

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }
  if (jobQuery.isLoading || itemsQuery.isLoading) {
    return <LoadingView title="Загрузка задачи импорта" />;
  }
  if (jobQuery.isError || itemsQuery.isError) {
    return <ErrorView title="Не удалось загрузить задачу импорта" error={jobQuery.error ?? itemsQuery.error} />;
  }

  const job = jobQuery.data;
  if (!job) {
    return <ErrorView title="Задача импорта не найдена" />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title={`Задача импорта #${job.id}`}
        description={`Статус: ${localizeImportJobStatus(job.status ?? undefined)}. Здесь доступны карточка задачи и строки на проверке.`}
      />
      {(applyReviewMutation.error || rejectReviewMutation.error) ? (
        <Alert
          type="error"
          showIcon
          message="Проверка строки завершилась ошибкой"
          description={String(applyReviewMutation.error ?? rejectReviewMutation.error)}
        />
      ) : null}
      <SectionCard title="Карточка задачи">
        <Space direction="vertical" size={6}>
          <span>{`Источник: ${job.sourceType || 'не указан'} / ${job.sourceRef || 'не указан'}`}</span>
          <span>{`Создана: ${formatUiDate(job.createdAt ?? undefined)}`}</span>
          <span>{`Завершена: ${formatUiDate(job.completedAt ?? undefined, 'Не завершена')}`}</span>
        </Space>
      </SectionCard>
      <SectionCard title="Действия по проверке">
        <Form
          layout="vertical"
          form={applyForm}
          onFinish={(values) => applyReviewMutation.mutate({ itemId: values.itemId, values: { matchedUserId: values.matchedUserId } })}
        >
          <div className="admin-form-grid">
            <Form.Item name="itemId" label="ID строки" rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="matchedUserId" label="ID найденного сотрудника" rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Button htmlType="submit" loading={applyReviewMutation.isPending}>Принять строку</Button>
        </Form>
        <Form
          layout="vertical"
          form={rejectForm}
          onFinish={(values) => rejectReviewMutation.mutate({ itemId: values.itemId, values: { reason: values.reason } })}
        >
          <div className="admin-form-grid">
            <Form.Item name="itemId" label="ID строки" rules={[{ required: true }]}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="reason" label="Причина отклонения" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
          </div>
          <Button htmlType="submit" loading={rejectReviewMutation.isPending}>Отклонить строку</Button>
        </Form>
      </SectionCard>
      <SectionCard title="Строки задачи">
        <Table
          rowKey="id"
          dataSource={itemsQuery.data ?? []}
          pagination={{ pageSize: 10 }}
          columns={[
            { title: 'ID', dataIndex: 'id' },
            { title: 'Строка', render: (_, item) => <Link to={`/admin/import/items/${item.id}`}>{item.targetEntityType || 'Элемент'}</Link> },
            { title: 'Статус', dataIndex: 'status', render: (value) => localizeImportItemStatus(value ?? undefined) },
            { title: 'Табельный номер', dataIndex: 'employeeNumber' },
            { title: 'Внешний ID', dataIndex: 'externalId' },
            { title: 'Обработан', dataIndex: 'processedAt', render: (value) => formatUiDate(value ?? undefined, 'Не обработан') },
          ]}
        />
      </SectionCard>
    </Space>
  );
}
