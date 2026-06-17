import { Alert, Button, Form, Input, Modal, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router';
import {
  canAccessAdministrationArea,
} from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type { AdminUser, CreateUserRequest, UserStatus } from '../../features/admin-users/model/adminUsers';
import {
  useAdminRoles,
  useAdminUsers,
  useCreateAdminUserMutation,
} from '../../features/admin-users/model/useAdminUsers';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeRole, localizeUserStatus } from '../../shared/ui/presentation';
import { useState } from 'react';

const userStatusOptions = [
  { label: 'Активные', value: 'ACTIVE' },
  { label: 'Неактивные', value: 'INACTIVE' },
];

export function AdminUsersPage() {
  const { data: actor } = useCurrentActor();
  const [status, setStatus] = useState<UserStatus | undefined>();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [form] = Form.useForm<CreateUserRequest>();
  const usersQuery = useAdminUsers({ status }, Boolean(actor));
  const rolesQuery = useAdminRoles(Boolean(actor));
  const createMutation = useCreateAdminUserMutation();

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }

  if (usersQuery.isLoading || rolesQuery.isLoading) {
    return <LoadingView title="Загрузка пользователей" description="Получаем список пользователей и справочник ролей." />;
  }

  if (usersQuery.isError) {
    return <ErrorView title="Не удалось загрузить пользователей" error={usersQuery.error} />;
  }

  if (rolesQuery.isError) {
    return <ErrorView title="Не удалось загрузить роли" error={rolesQuery.error} />;
  }

  const columns: ColumnsType<AdminUser> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 88,
      render: (value: number) => <Typography.Text code>{value}</Typography.Text>,
    },
    {
      title: 'Сотрудник',
      key: 'person',
      render: (_, user) => (
        <Space direction="vertical" size={0}>
          <Link to={`/admin/users/${user.id}`}>{`${user.lastName} ${user.firstName}`}</Link>
          <Typography.Text type="secondary">{user.middleName || user.employeeNumber}</Typography.Text>
        </Space>
      ),
    },
    {
      title: 'Табельный номер',
      dataIndex: 'employeeNumber',
    },
    {
      title: 'Внешний ID',
      dataIndex: 'externalId',
      render: (value?: string | null) => value || <Typography.Text type="secondary">Не указан</Typography.Text>,
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      render: (value?: string | null) => <Tag>{localizeUserStatus(value ?? undefined)}</Tag>,
    },
    {
      title: 'Обновлён',
      dataIndex: 'updatedAt',
      render: (value?: string | null) => formatUiDate(value ?? undefined),
    },
  ];

  const visibleRoleCodes = ['ADMIN', 'EXPERT', 'MANAGER', 'OPERATOR'];
  const roleSummary = (rolesQuery.data ?? [])
    .map((role) => (role.code ?? role.name ?? String(role.id)).toUpperCase())
    .map((code) => {
      switch (code) {
        case 'ROLE_MANAGER':
          return 'MANAGER';
        case 'ROLE_OPERATIONS':
        case 'ROLE_OPERATOR':
          return 'OPERATOR';
        case 'ROLE_ADMIN':
          return 'ADMIN';
        case 'ROLE_EXPERT':
          return 'EXPERT';
        default:
          return code;
      }
    })
    .filter((code) => visibleRoleCodes.includes(code))
    .filter((code, index, array) => array.indexOf(code) === index)
    .map((code) => ({
      code,
      label: localizeRole(code),
    }));

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title="Администрирование пользователей"
        description="Раздел используется для просмотра сотрудников, создания учетных записей и перехода к управлению ролями и организационными привязками."
        extra={
          <>
            <Select
              allowClear
              placeholder="Фильтр по статусу"
              style={{ minWidth: 220 }}
              options={userStatusOptions}
              value={status}
              onChange={(value) => setStatus(value)}
            />
            <Button type="primary" onClick={() => setIsCreateOpen(true)}>
              Создать пользователя
            </Button>
          </>
        }
      />

      <SectionCard
        title="Справочник ролей"
        description="Используется для назначения ролей в карточке пользователя."
      >
        <Space wrap>
          {roleSummary.length > 0 ? (
            roleSummary.map((role) => (
              <Tag key={role.code} color="blue">
                {role.label}
              </Tag>
            ))
          ) : (
            <Typography.Text type="secondary">Роли не найдены.</Typography.Text>
          )}
        </Space>
      </SectionCard>

      {createMutation.isError ? (
        <Alert type="error" showIcon message="Создание пользователя завершилось ошибкой" description={String(createMutation.error instanceof Error ? createMutation.error.message : createMutation.error)} />
      ) : null}

      <SectionCard title="Список пользователей">
        <Table rowKey="id" columns={columns} dataSource={usersQuery.data ?? []} pagination={{ pageSize: 10 }} />
      </SectionCard>

      <Modal
        open={isCreateOpen}
        title="Создание пользователя"
        confirmLoading={createMutation.isPending}
        onCancel={() => setIsCreateOpen(false)}
        onOk={() => form.submit()}
      >
        <Form<CreateUserRequest>
          layout="vertical"
          form={form}
          initialValues={{ status: 'ACTIVE' }}
          onFinish={async (values) => {
            await createMutation.mutateAsync(values);
            setIsCreateOpen(false);
            form.resetFields();
          }}
        >
          <Form.Item name="employeeNumber" label="Табельный номер" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="externalId" label="Внешний ID">
            <Input />
          </Form.Item>
          <Form.Item name="lastName" label="Фамилия" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="firstName" label="Имя" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="middleName" label="Отчество">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="Статус" rules={[{ required: true }]}>
            <Select options={userStatusOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
