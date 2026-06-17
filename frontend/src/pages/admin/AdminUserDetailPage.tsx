import {
  Alert,
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useParams } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type {
  AdminUserOrganizationAssignment,
  AdminUserRole,
  AssignOrganizationUnitRequest,
  AssignRoleRequest,
  CloseOrganizationAssignmentRequest,
  CloseRoleRequest,
  ReplacePrimaryHomeUnitRequest,
  UpdateUserRequest,
} from '../../features/admin-users/model/adminUsers';
import {
  useAdminRoles,
  useAdminUser,
  useAdminUserOrganizationAssignments,
  useAdminUserRoles,
  useAssignAdminUserOrganizationMutation,
  useAssignAdminUserRoleMutation,
  useCloseAdminUserOrganizationMutation,
  useCloseAdminUserRoleMutation,
  useDeactivateAdminUserMutation,
  useReplaceAdminUserPrimaryHomeUnitMutation,
  useUpdateAdminUserMutation,
} from '../../features/admin-users/model/useAdminUsers';
import { useOrganizationalUnits } from '../../features/admin-organization/model/useAdminOrganization';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { humanizeOrganizationalPath, humanizeOrganizationalUnit } from '../../shared/ui/organizationalUnits';
import {
  formatUiDate,
  localizeOrganizationAssignmentType,
  localizeRole,
  localizeUserStatus,
} from '../../shared/ui/presentation';

type UserModalState = 'edit' | 'assign-role' | 'assign-unit' | 'replace-home' | null;

function formatUserFullName(user: { lastName: string; firstName: string; middleName?: string | null }) {
  return `${user.lastName} ${user.firstName} ${user.middleName ?? ''}`.trim();
}

export function AdminUserDetailPage() {
  const { userId: userIdParam } = useParams();
  const userId = Number(userIdParam);
  const { data: actor } = useCurrentActor();
  const userQuery = useAdminUser(userId, Boolean(actor));
  const rolesQuery = useAdminUserRoles(userId, Boolean(actor));
  const assignmentsQuery = useAdminUserOrganizationAssignments(userId, Boolean(actor));
  const roleOptionsQuery = useAdminRoles(Boolean(actor));
  const unitsQuery = useOrganizationalUnits(undefined, Boolean(actor));
  const [modalState, setModalState] = useState<UserModalState>(null);
  const [closingRoleId, setClosingRoleId] = useState<number | null>(null);
  const [closingAssignmentId, setClosingAssignmentId] = useState<number | null>(null);
  const [editForm] = Form.useForm<UpdateUserRequest>();
  const [roleForm] = Form.useForm<AssignRoleRequest>();
  const [orgForm] = Form.useForm<AssignOrganizationUnitRequest>();
  const [homeUnitForm] = Form.useForm<ReplacePrimaryHomeUnitRequest>();
  const [closeRoleForm] = Form.useForm<CloseRoleRequest>();
  const [closeOrgForm] = Form.useForm<CloseOrganizationAssignmentRequest>();

  const updateMutation = useUpdateAdminUserMutation(userId);
  const deactivateMutation = useDeactivateAdminUserMutation(userId);
  const assignRoleMutation = useAssignAdminUserRoleMutation(userId);
  const closeRoleMutation = useCloseAdminUserRoleMutation(userId);
  const assignOrgMutation = useAssignAdminUserOrganizationMutation(userId);
  const closeOrgMutation = useCloseAdminUserOrganizationMutation(userId);
  const replaceHomeMutation = useReplaceAdminUserPrimaryHomeUnitMutation(userId);

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }

  if (
    userQuery.isLoading ||
    rolesQuery.isLoading ||
    assignmentsQuery.isLoading ||
    roleOptionsQuery.isLoading ||
    unitsQuery.isLoading
  ) {
    return <LoadingView title="Загрузка карточки пользователя" description="Получаем профиль, роли и оргпривязки." />;
  }

  if (userQuery.isError) {
    return <ErrorView title="Не удалось загрузить пользователя" error={userQuery.error} />;
  }

  if (rolesQuery.isError || assignmentsQuery.isError || roleOptionsQuery.isError || unitsQuery.isError) {
    return <ErrorView title="Не удалось загрузить связанные данные пользователя" error={rolesQuery.error ?? assignmentsQuery.error ?? roleOptionsQuery.error ?? unitsQuery.error} />;
  }

  const user = userQuery.data;
  if (!user) {
    return <ErrorView title="Пользователь не найден" />;
  }

  const userFullName = formatUserFullName(user);

  const roleColumns: ColumnsType<AdminUserRole> = [
    { title: 'ID', dataIndex: 'id', render: (value) => <Typography.Text code>{value}</Typography.Text> },
    {
      title: 'Роль',
      render: (_, role) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>
            {localizeRole(role.roleCode ?? undefined, role.roleName || 'Роль не указана')}
          </Typography.Text>
          <Typography.Text type="secondary">{role.roleCode || 'Код не указан'}</Typography.Text>
        </Space>
      ),
    },
    { title: 'Действует с', dataIndex: 'validFrom', render: (value) => formatUiDate(value ?? undefined) },
    { title: 'Действует до', dataIndex: 'validTo', render: (value) => formatUiDate(value ?? undefined, 'Открыто') },
    {
      title: 'Состояние',
      render: (_, role) => (
        <Tag color={role.validTo ? 'default' : 'green'}>{role.validTo ? 'Закрыта' : 'Активна'}</Tag>
      ),
    },
    {
      title: 'Действие',
      render: (_, role) =>
        role.validTo ? null : (
          <Button size="small" onClick={() => setClosingRoleId(role.id)}>
            Закрыть
          </Button>
        ),
    },
  ];

  const assignmentColumns: ColumnsType<AdminUserOrganizationAssignment> = [
    { title: 'ID', dataIndex: 'id', render: (value) => <Typography.Text code>{value}</Typography.Text> },
    {
      title: 'Оргпривязка',
      render: (_, assignment) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>
            {humanizeOrganizationalUnit(
              assignment.organizationalUnitName,
              assignment.organizationalUnitPath,
              'Подразделение не указано',
            )}
          </Typography.Text>
          <Typography.Text type="secondary">
            {humanizeOrganizationalPath(assignment.organizationalUnitPath, 'Путь подразделения не указан')}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: 'Тип',
      dataIndex: 'assignmentType',
      render: (value) => localizeOrganizationAssignmentType(value ?? undefined),
    },
    { title: 'Действует с', dataIndex: 'validFrom', render: (value) => formatUiDate(value ?? undefined) },
    { title: 'Действует до', dataIndex: 'validTo', render: (value) => formatUiDate(value ?? undefined, 'Открыто') },
    {
      title: 'Действие',
      render: (_, assignment) =>
        assignment.validTo ? null : (
          <Button size="small" onClick={() => setClosingAssignmentId(assignment.id)}>
            Закрыть
          </Button>
        ),
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title={userFullName}
        description="Карточка сотрудника показывает учетные данные, действующие роли и привязку к подразделению."
        extra={
          <>
            <Button onClick={() => {
              editForm.setFieldsValue({
                lastName: user.lastName,
                firstName: user.firstName,
                middleName: user.middleName || undefined,
              });
              setModalState('edit');
            }}>
              Изменить профиль
            </Button>
            <Button danger loading={deactivateMutation.isPending} onClick={() => deactivateMutation.mutate()}>
              Отключить пользователя
            </Button>
          </>
        }
      />

      {[
        updateMutation,
        deactivateMutation,
        assignRoleMutation,
        closeRoleMutation,
        assignOrgMutation,
        closeOrgMutation,
        replaceHomeMutation,
      ].some((mutation) => mutation.isError) ? (
        <Alert
          type="error"
          showIcon
          message="Одна из команд завершилась ошибкой"
          description={String(
            updateMutation.error ??
              deactivateMutation.error ??
              assignRoleMutation.error ??
              closeRoleMutation.error ??
              assignOrgMutation.error ??
              closeOrgMutation.error ??
              replaceHomeMutation.error,
          )}
        />
      ) : null}

      <SectionCard title="Профиль">
        <Descriptions bordered column={2}>
          <Descriptions.Item label="ФИО">{userFullName}</Descriptions.Item>
          <Descriptions.Item label="Статус">
            <Tag>{localizeUserStatus(user.status)}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Табельный номер">{user.employeeNumber}</Descriptions.Item>
          <Descriptions.Item label="Внешний ID">{user.externalId || 'Не указан'}</Descriptions.Item>
          <Descriptions.Item label="Создан">{formatUiDate(user.createdAt ?? undefined)}</Descriptions.Item>
          <Descriptions.Item label="Обновлён">{formatUiDate(user.updatedAt ?? undefined)}</Descriptions.Item>
        </Descriptions>
      </SectionCard>

      <SectionCard
        title="Роли пользователя"
        extra={<Button type="primary" onClick={() => setModalState('assign-role')}>Назначить роль</Button>}
      >
        <Table rowKey="id" columns={roleColumns} dataSource={rolesQuery.data ?? []} pagination={false} />
      </SectionCard>

      <SectionCard
        title="Оргпривязки пользователя"
        extra={
          <Space>
            <Button onClick={() => setModalState('replace-home')}>Сменить основное подразделение</Button>
            <Button type="primary" onClick={() => setModalState('assign-unit')}>
              Назначить оргпривязку
            </Button>
          </Space>
        }
      >
        <Table rowKey="id" columns={assignmentColumns} dataSource={assignmentsQuery.data ?? []} pagination={false} />
      </SectionCard>

      <Modal
        open={modalState === 'edit'}
        title="Редактирование пользователя"
        confirmLoading={updateMutation.isPending}
        onCancel={() => setModalState(null)}
        onOk={() => editForm.submit()}
      >
        <Form<UpdateUserRequest>
          layout="vertical"
          form={editForm}
          onFinish={async (values) => {
            await updateMutation.mutateAsync(values);
            setModalState(null);
          }}
        >
          <Form.Item name="lastName" label="Фамилия" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="firstName" label="Имя" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="middleName" label="Отчество">
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={modalState === 'assign-role'}
        title="Назначение роли"
        confirmLoading={assignRoleMutation.isPending}
        onCancel={() => setModalState(null)}
        onOk={() => roleForm.submit()}
      >
        <Form<AssignRoleRequest>
          layout="vertical"
          form={roleForm}
          onFinish={async (values) => {
            await assignRoleMutation.mutateAsync({
              ...values,
              validFrom: values.validFrom ? dayjs(values.validFrom).toISOString() : undefined,
            });
            setModalState(null);
            roleForm.resetFields();
          }}
        >
          <Form.Item name="roleId" label="Роль" rules={[{ required: true }]}>
            <Select
              options={(roleOptionsQuery.data ?? []).map((role) => ({
                label: localizeRole(role.code ?? undefined, role.name || 'Роль не указана'),
                value: role.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="validFrom" label="Начало действия (ISO)">
            <Input placeholder="2026-05-17T12:00:00Z" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={modalState === 'assign-unit'}
        title="Назначение оргпривязки"
        confirmLoading={assignOrgMutation.isPending}
        onCancel={() => setModalState(null)}
        onOk={() => orgForm.submit()}
      >
        <Form<AssignOrganizationUnitRequest>
          layout="vertical"
          form={orgForm}
          initialValues={{ assignmentType: 'PRIMARY' }}
          onFinish={async (values) => {
            await assignOrgMutation.mutateAsync(values);
            setModalState(null);
            orgForm.resetFields();
          }}
        >
          <Form.Item name="organizationalUnitId" label="Подразделение" rules={[{ required: true }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={(unitsQuery.data ?? []).map((unit) => ({
                label: humanizeOrganizationalPath(unit.path, humanizeOrganizationalUnit(unit.name)),
                value: unit.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="assignmentType" label="Тип привязки" rules={[{ required: true }]}>
            <Select
              options={[
                { label: 'Основная', value: 'PRIMARY' },
                { label: 'Дополнительная', value: 'SECONDARY' },
                { label: 'Временная', value: 'TEMPORARY' },
              ]}
            />
          </Form.Item>
          <Form.Item name="validFrom" label="Начало действия (ISO)">
            <Input placeholder="2026-05-17T12:00:00Z" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={modalState === 'replace-home'}
        title="Смена основного подразделения"
        confirmLoading={replaceHomeMutation.isPending}
        onCancel={() => setModalState(null)}
        onOk={() => homeUnitForm.submit()}
      >
        <Form<ReplacePrimaryHomeUnitRequest>
          layout="vertical"
          form={homeUnitForm}
          onFinish={async (values) => {
            await replaceHomeMutation.mutateAsync(values);
            setModalState(null);
            homeUnitForm.resetFields();
          }}
        >
          <Form.Item name="organizationalUnitId" label="Новое основное подразделение" rules={[{ required: true }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={(unitsQuery.data ?? []).map((unit) => ({
                label: humanizeOrganizationalPath(unit.path, humanizeOrganizationalUnit(unit.name)),
                value: unit.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="effectiveAt" label="Дата вступления в силу (ISO)">
            <Input placeholder="2026-05-17T12:00:00Z" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={closingRoleId != null}
        title="Закрытие роли пользователя"
        confirmLoading={closeRoleMutation.isPending}
        onCancel={() => setClosingRoleId(null)}
        onOk={() => closeRoleForm.submit()}
      >
        <Form<CloseRoleRequest>
          layout="vertical"
          form={closeRoleForm}
          onFinish={async (values) => {
            await closeRoleMutation.mutateAsync({ assignmentId: closingRoleId as number, values });
            setClosingRoleId(null);
            closeRoleForm.resetFields();
          }}
        >
          <Form.Item name="validTo" label="Дата закрытия (ISO)">
            <Input placeholder="2026-05-17T12:00:00Z" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={closingAssignmentId != null}
        title="Закрытие организационной привязки"
        confirmLoading={closeOrgMutation.isPending}
        onCancel={() => setClosingAssignmentId(null)}
        onOk={() => closeOrgForm.submit()}
      >
        <Form<CloseOrganizationAssignmentRequest>
          layout="vertical"
          form={closeOrgForm}
          onFinish={async (values) => {
            await closeOrgMutation.mutateAsync({ assignmentId: closingAssignmentId as number, values });
            setClosingAssignmentId(null);
            closeOrgForm.resetFields();
          }}
        >
          <Form.Item name="validTo" label="Дата закрытия (ISO)">
            <Input placeholder="2026-05-17T12:00:00Z" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
