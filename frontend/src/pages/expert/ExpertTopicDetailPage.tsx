import { Alert, Button, Descriptions, Form, Input, InputNumber, Space } from 'antd';
import { Link, useParams } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type { UpdateTopicRequest } from '../../features/expert-content/model/expertContent';
import {
  useArchiveTopicMutation,
  useLifecycleTopic,
  usePublishTopicMutation,
  useTopic,
  useUpdateTopicMutation,
} from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeContentStatus } from '../../shared/ui/presentation';

export function ExpertTopicDetailPage() {
  const { topicId: topicIdParam } = useParams();
  const topicId = Number(topicIdParam);
  const { data: actor } = useCurrentActor();
  const topicQuery = useTopic(topicId, Boolean(actor));
  const lifecycleQuery = useLifecycleTopic(topicId, Boolean(actor));
  const updateMutation = useUpdateTopicMutation(topicId);
  const publishMutation = usePublishTopicMutation(topicId);
  const archiveMutation = useArchiveTopicMutation(topicId);
  const [form] = Form.useForm<UpdateTopicRequest>();

  if (!actor || !canAccessExpertArea(actor)) return <ForbiddenView />;
  if (topicQuery.isLoading || lifecycleQuery.isLoading) return <LoadingView title="Загрузка темы" />;
  if (topicQuery.isError) return <ErrorView title="Не удалось загрузить тему" error={topicQuery.error} />;

  const topic = topicQuery.data;
  if (!topic) return <ErrorView title="Тема не найдена" />;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title={topic.name}
        description="Карточка темы и её текущее состояние."
        extra={
          <Space>
            <Button onClick={() => publishMutation.mutate()} loading={publishMutation.isPending}>Опубликовать</Button>
            <Button onClick={() => archiveMutation.mutate()} loading={archiveMutation.isPending}>Архивировать</Button>
          </Space>
        }
      />
      {(updateMutation.error || publishMutation.error || archiveMutation.error) ? <Alert type="error" showIcon message="Команда по теме завершилась ошибкой" description={String(updateMutation.error ?? publishMutation.error ?? archiveMutation.error)} /> : null}
      <SectionCard title="Карточка темы">
        <Descriptions bordered column={2}>
          <Descriptions.Item label="ID">{topic.id}</Descriptions.Item>
          <Descriptions.Item label="Статус">{localizeContentStatus(topic.status ?? undefined)}</Descriptions.Item>
          <Descriptions.Item label="Описание" span={2}>{topic.description || 'Не указано'}</Descriptions.Item>
          <Descriptions.Item label="Курс">{topic.courseId}</Descriptions.Item>
          <Descriptions.Item label="Обновлена">{formatUiDate(topic.updatedAt ?? undefined)}</Descriptions.Item>
        </Descriptions>
      </SectionCard>
      <SectionCard title="Текущее состояние">
        {lifecycleQuery.isError ? (
          <Alert type="error" showIcon message="Не удалось загрузить состояние темы" description={String(lifecycleQuery.error)} />
        ) : lifecycleQuery.data ? (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="Статус">{localizeContentStatus(lifecycleQuery.data.status ?? undefined)}</Descriptions.Item>
            <Descriptions.Item label="Обновлена">{formatUiDate(lifecycleQuery.data.updatedAt ?? undefined)}</Descriptions.Item>
            <Descriptions.Item label="Порядок сортировки">{lifecycleQuery.data.sortOrder ?? 0}</Descriptions.Item>
            <Descriptions.Item label="Создана">{formatUiDate(lifecycleQuery.data.createdAt ?? undefined)}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </SectionCard>
      <SectionCard title="Редактирование темы">
        <Form<UpdateTopicRequest> layout="vertical" form={form} initialValues={{ name: topic.name, description: topic.description || undefined, sortOrder: topic.sortOrder ?? undefined }} onFinish={(values) => updateMutation.mutate(values)}>
          <div className="admin-form-grid">
            <Form.Item name="name" label="Название" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item name="description" label="Описание"><Input.TextArea rows={3} /></Form.Item>
            <Form.Item name="sortOrder" label="Порядок сортировки"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
          </div>
          <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>Сохранить</Button>
        </Form>
      </SectionCard>
      <SectionCard title="Разделы темы">
        <Space>
          <Link to={`/expert/content/topics/${topic.id}/materials`}>Материалы</Link>
          <Link to={`/expert/content/topics/${topic.id}/questions`}>Вопросы</Link>
          <Link to={`/expert/content/topics/${topic.id}/tests`}>Тесты</Link>
          <Link to={`/expert/content/topics/${topic.id}/final-control`}>Итоговый контроль</Link>
        </Space>
      </SectionCard>
    </Space>
  );
}
