import { Alert, Button, Descriptions, Form, Input, Space, Table } from 'antd';
import { Link, useParams } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type { CreateTopicRequest, SaveCourseRequest } from '../../features/expert-content/model/expertContent';
import {
  useArchiveCourseMutation,
  useCourse,
  useCreateTopicMutation,
  useLifecycleCourse,
  usePublishCourseMutation,
  useTopicsByCourse,
  useUpdateCourseMutation,
} from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeContentStatus } from '../../shared/ui/presentation';

export function ExpertCourseDetailPage() {
  const { courseId: courseIdParam } = useParams();
  const courseId = Number(courseIdParam);
  const { data: actor } = useCurrentActor();
  const courseQuery = useCourse(courseId, Boolean(actor));
  const lifecycleQuery = useLifecycleCourse(courseId, Boolean(actor));
  const topicsQuery = useTopicsByCourse(courseId, Boolean(actor));
  const updateMutation = useUpdateCourseMutation(courseId);
  const publishMutation = usePublishCourseMutation(courseId);
  const archiveMutation = useArchiveCourseMutation(courseId);
  const createTopicMutation = useCreateTopicMutation();
  const [courseForm] = Form.useForm<SaveCourseRequest>();
  const [topicForm] = Form.useForm<CreateTopicRequest>();

  if (!actor || !canAccessExpertArea(actor)) {
    return <ForbiddenView />;
  }

  if (courseQuery.isLoading || topicsQuery.isLoading || lifecycleQuery.isLoading) {
    return <LoadingView title="Загрузка курса" description="Подготавливаем данные курса и список тем." />;
  }

  if (courseQuery.isError || topicsQuery.isError) {
    return <ErrorView title="Не удалось загрузить курс" error={courseQuery.error ?? topicsQuery.error} />;
  }

  const course = courseQuery.data;
  if (!course) {
    return <ErrorView title="Курс не найден" />;
  }

  const lifecycle = lifecycleQuery.data;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title={course.name}
        extra={
          <Space>
            <Button onClick={() => publishMutation.mutate()} loading={publishMutation.isPending}>
              Опубликовать
            </Button>
            <Button onClick={() => archiveMutation.mutate()} loading={archiveMutation.isPending}>
              В архив
            </Button>
          </Space>
        }
      />

      {updateMutation.error || createTopicMutation.error || publishMutation.error || archiveMutation.error ? (
        <Alert
          type="error"
          showIcon
          message="Не удалось выполнить действие с курсом"
          description={String(
            updateMutation.error ?? createTopicMutation.error ?? publishMutation.error ?? archiveMutation.error,
          )}
        />
      ) : null}

      <SectionCard title="О курсе">
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Статус">
            {localizeContentStatus(course.status ?? undefined)}
          </Descriptions.Item>
          <Descriptions.Item label="Обновлено">
            {formatUiDate(course.updatedAt ?? undefined)}
          </Descriptions.Item>
          <Descriptions.Item label="Описание" span={2}>
            {course.description || 'Описание пока не заполнено.'}
          </Descriptions.Item>
          <Descriptions.Item label="Создан">
            {formatUiDate(lifecycle?.createdAt ?? undefined)}
          </Descriptions.Item>
        </Descriptions>
      </SectionCard>

      <SectionCard title="Редактирование курса">
        <Form<SaveCourseRequest>
          layout="vertical"
          form={courseForm}
          initialValues={{
            name: course.name,
            description: course.description || undefined,
          }}
          onFinish={(values) => updateMutation.mutate(values)}
        >
          <div className="admin-form-grid">
            <Form.Item name="name" label="Название" rules={[{ required: true, message: 'Укажите название курса' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="description" label="Описание">
              <Input.TextArea rows={3} placeholder="Кратко опишите назначение курса" />
            </Form.Item>
          </div>
          <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>
            Сохранить изменения
          </Button>
        </Form>
      </SectionCard>

      <SectionCard title="Новая тема">
        <Form<CreateTopicRequest>
          layout="vertical"
          form={topicForm}
          initialValues={{ courseId }}
          onFinish={(values) => createTopicMutation.mutate({ ...values, courseId })}
        >
          <div className="admin-form-grid">
            <Form.Item name="name" label="Название" rules={[{ required: true, message: 'Укажите название темы' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="description" label="Описание">
              <Input.TextArea rows={3} placeholder="Кратко опишите содержание темы" />
            </Form.Item>
          </div>
          <Button htmlType="submit" loading={createTopicMutation.isPending}>
            Создать тему
          </Button>
        </Form>
      </SectionCard>

      <SectionCard title="Темы курса">
        <Table
          rowKey="id"
          dataSource={topicsQuery.data ?? []}
          pagination={false}
          columns={[
            {
              title: 'Тема',
              render: (_, topic) => <Link to={`/expert/content/topics/${topic.id}`}>{topic.name}</Link>,
            },
            {
              title: 'Статус',
              dataIndex: 'status',
              render: (value) => localizeContentStatus(value ?? undefined),
            },
            {
              title: 'Обновлено',
              dataIndex: 'updatedAt',
              render: (value) => formatUiDate(value ?? undefined),
            },
            {
              title: 'Материалы',
              render: (_, topic) => <Link to={`/expert/content/topics/${topic.id}/materials`}>Открыть</Link>,
            },
            {
              title: 'Вопросы',
              render: (_, topic) => <Link to={`/expert/content/topics/${topic.id}/questions`}>Открыть</Link>,
            },
            {
              title: 'Тесты',
              render: (_, topic) => <Link to={`/expert/content/topics/${topic.id}/tests`}>Открыть</Link>,
            },
          ]}
        />
      </SectionCard>
    </Space>
  );
}
