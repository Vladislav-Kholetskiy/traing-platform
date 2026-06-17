import { Alert, Button, Form, Input, Space, Table } from 'antd';
import { Link } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type { Course, SaveCourseRequest } from '../../features/expert-content/model/expertContent';
import { useCourses, useCreateCourseMutation } from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeContentStatus } from '../../shared/ui/presentation';

export function ExpertCoursesPage() {
  const { data: actor } = useCurrentActor();
  const coursesQuery = useCourses(Boolean(actor));
  const createMutation = useCreateCourseMutation();
  const [form] = Form.useForm<SaveCourseRequest>();

  if (!actor || !canAccessExpertArea(actor)) {
    return <ForbiddenView />;
  }

  if (coursesQuery.isLoading) {
    return <LoadingView title="Загрузка курсов" description="Подготавливаем список курсов." />;
  }

  if (coursesQuery.isError) {
    return <ErrorView title="Не удалось загрузить курсы" error={coursesQuery.error} />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro title="Курсы" />

      {createMutation.isError ? (
        <Alert
          type="error"
          showIcon
          message="Не удалось создать курс"
          description={String(createMutation.error)}
        />
      ) : null}

      <SectionCard title="Новый курс">
        <Form<SaveCourseRequest> layout="vertical" form={form} onFinish={(values) => createMutation.mutate(values)}>
          <div className="admin-form-grid">
            <Form.Item name="name" label="Название" rules={[{ required: true, message: 'Укажите название курса' }]}>
              <Input placeholder="Например, Промышленная безопасность оператора НПЗ" />
            </Form.Item>
            <Form.Item name="description" label="Краткое описание">
              <Input.TextArea rows={3} placeholder="Кому предназначен курс и что в него входит" />
            </Form.Item>
          </div>
          <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
            Создать курс
          </Button>
        </Form>
      </SectionCard>

      <SectionCard title="Список курсов">
        <Table<Course>
          rowKey="id"
          dataSource={coursesQuery.data ?? []}
          pagination={{ pageSize: 10 }}
          columns={[
            {
              title: 'Курс',
              render: (_, course) => <Link to={`/expert/content/courses/${course.id}`}>{course.name}</Link>,
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
              title: 'Темы',
              render: (_, course) => (
                <Link to={`/expert/content/courses/${course.id}/topics`}>Перейти к темам</Link>
              ),
            },
          ]}
        />
      </SectionCard>
    </Space>
  );
}
