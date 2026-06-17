import { Alert, Button, Form, Input, Select, Space, Table, Tabs, Tag, Typography } from 'antd';
import { useState } from 'react';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type {
  AccessFilter,
  AssignManagementRelationRequest,
  AssignTemporaryAccessAreaRequest,
  AssignTemporaryManagementDelegationRequest,
  AssignTemporaryRoleRequest,
  AssignUserAccessAreaRequest,
} from '../../features/access-management/model/accessManagement';
import {
  useAccessAreas,
  useCloseAccessAreaMutation,
  useCloseManagementRelationMutation,
  useCloseTemporaryAccessAreaMutation,
  useCloseTemporaryManagementDelegationMutation,
  useCloseTemporaryRoleMutation,
  useCreateAccessAreaMutation,
  useCreateManagementRelationMutation,
  useCreateTemporaryAccessAreaMutation,
  useCreateTemporaryManagementDelegationMutation,
  useCreateTemporaryRoleMutation,
  useManagementRelations,
  useManagementRelationTypes,
  useTemporaryAccessAreas,
  useTemporaryManagementDelegations,
  useTemporaryRoleAssignments,
} from '../../features/access-management/model/useAccessManagement';
import { useAdminRoles, useAdminUsers } from '../../features/admin-users/model/useAdminUsers';
import { useOrganizationalUnits } from '../../features/admin-organization/model/useAdminOrganization';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import {
  humanizeOrganizationalPath,
  humanizeOrganizationalUnit,
} from '../../shared/ui/organizationalUnits';
import {
  formatUiDate,
  localizeAccessScopeType,
  localizeRole,
} from '../../shared/ui/presentation';

function isActive(validTo?: string | null) {
  return !validTo;
}

function formatUserName(user?: { lastName: string; firstName: string; middleName?: string | null; employeeNumber: string }) {
  if (!user) {
    return 'Пользователь не найден';
  }

  return `${user.lastName} ${user.firstName} ${user.middleName ?? ''}`.trim();
}

function formatRoleName(role?: { code?: string | null; name?: string | null }) {
  if (!role) {
    return 'Роль не найдена';
  }

  return localizeRole(role.code ?? undefined, role.name || 'Роль не указана');
}

function formatRelationType(type?: { code: string; name: string }) {
  if (!type) {
    return 'Тип связи не найден';
  }

  if (type.code === 'SUPERVISOR') {
    return 'Руководитель подразделения';
  }

  return type.name || type.code;
}

function renderState(validTo?: string | null) {
  return <Tag color={isActive(validTo) ? 'green' : 'default'}>{isActive(validTo) ? 'Активно' : 'Закрыто'}</Tag>;
}

export function AdminAccessManagementPage() {
  const { data: actor } = useCurrentActor();
  const [filter] = useState<AccessFilter>({ activeOnly: false });
  const accessAreasQuery = useAccessAreas(filter, Boolean(actor));
  const relationsQuery = useManagementRelations(filter, Boolean(actor));
  const tempRolesQuery = useTemporaryRoleAssignments(filter, Boolean(actor));
  const tempAreasQuery = useTemporaryAccessAreas(filter, Boolean(actor));
  const tempDelegationsQuery = useTemporaryManagementDelegations(filter, Boolean(actor));
  const relationTypesQuery = useManagementRelationTypes(Boolean(actor));
  const usersQuery = useAdminUsers(undefined, Boolean(actor));
  const rolesQuery = useAdminRoles(Boolean(actor));
  const unitsQuery = useOrganizationalUnits(undefined, Boolean(actor));

  const createAccessAreaMutation = useCreateAccessAreaMutation();
  const closeAccessAreaMutation = useCloseAccessAreaMutation();
  const createRelationMutation = useCreateManagementRelationMutation();
  const closeRelationMutation = useCloseManagementRelationMutation();
  const createTempRoleMutation = useCreateTemporaryRoleMutation();
  const closeTempRoleMutation = useCloseTemporaryRoleMutation();
  const createTempAreaMutation = useCreateTemporaryAccessAreaMutation();
  const closeTempAreaMutation = useCloseTemporaryAccessAreaMutation();
  const createTempDelegationMutation = useCreateTemporaryManagementDelegationMutation();
  const closeTempDelegationMutation = useCloseTemporaryManagementDelegationMutation();

  const [accessAreaForm] = Form.useForm<AssignUserAccessAreaRequest>();
  const [relationForm] = Form.useForm<AssignManagementRelationRequest>();
  const [tempRoleForm] = Form.useForm<AssignTemporaryRoleRequest>();
  const [tempAreaForm] = Form.useForm<AssignTemporaryAccessAreaRequest>();
  const [tempDelegationForm] = Form.useForm<AssignTemporaryManagementDelegationRequest>();

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }

  if (
    accessAreasQuery.isLoading ||
    relationsQuery.isLoading ||
    tempRolesQuery.isLoading ||
    tempAreasQuery.isLoading ||
    tempDelegationsQuery.isLoading ||
    relationTypesQuery.isLoading ||
    usersQuery.isLoading ||
    rolesQuery.isLoading ||
    unitsQuery.isLoading
  ) {
    return <LoadingView title="Загрузка доступов" description="Получаем действующие роли, области доступа и управленческие связи." />;
  }

  if (
    accessAreasQuery.isError ||
    relationsQuery.isError ||
    tempRolesQuery.isError ||
    tempAreasQuery.isError ||
    tempDelegationsQuery.isError ||
    relationTypesQuery.isError ||
    usersQuery.isError ||
    rolesQuery.isError ||
    unitsQuery.isError
  ) {
    return <ErrorView title="Не удалось загрузить доступы" error={accessAreasQuery.error ?? relationsQuery.error ?? tempRolesQuery.error ?? tempAreasQuery.error ?? tempDelegationsQuery.error ?? relationTypesQuery.error ?? usersQuery.error ?? rolesQuery.error ?? unitsQuery.error} />;
  }

  const usersById = new Map((usersQuery.data ?? []).map((user) => [user.id, user]));
  const rolesById = new Map((rolesQuery.data ?? []).map((role) => [role.id, role]));
  const unitsById = new Map((unitsQuery.data ?? []).map((unit) => [unit.id, unit]));
  const relationTypesById = new Map((relationTypesQuery.data ?? []).map((type) => [type.id, type]));

  const userOptions = (usersQuery.data ?? []).map((user) => ({
    label: `${formatUserName(user)} (${user.employeeNumber})`,
    value: user.id,
  }));
  const roleOptions = (rolesQuery.data ?? []).map((role) => ({
    label: formatRoleName(role),
    value: role.id,
  }));
  const unitOptions = (unitsQuery.data ?? []).map((unit) => ({
    label: humanizeOrganizationalPath(unit.path, humanizeOrganizationalUnit(unit.name)),
    value: unit.id,
  }));
  const relationTypeOptions = (relationTypesQuery.data ?? []).map((type) => ({
    label: formatRelationType(type),
    value: type.id,
  }));
  const accessScopeOptions = [
    { label: localizeAccessScopeType('GLOBAL'), value: 'GLOBAL' },
    { label: localizeAccessScopeType('UNIT_ONLY'), value: 'UNIT_ONLY' },
    { label: localizeAccessScopeType('UNIT_SUBTREE'), value: 'UNIT_SUBTREE' },
  ];

  const mutationError =
    createAccessAreaMutation.error ??
    closeAccessAreaMutation.error ??
    createRelationMutation.error ??
    closeRelationMutation.error ??
    createTempRoleMutation.error ??
    closeTempRoleMutation.error ??
    createTempAreaMutation.error ??
    closeTempAreaMutation.error ??
    createTempDelegationMutation.error ??
    closeTempDelegationMutation.error;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title="Администрирование доступов и связей"
        description="Раздел показывает области доступа, управленческие связи и временные полномочия пользователей."
      />

      {mutationError ? <Alert type="error" showIcon message="Команда завершилась ошибкой" description={String(mutationError)} /> : null}

      <Tabs
        items={[
          {
            key: 'access-areas',
            label: 'Области доступа',
            children: (
              <SectionCard
                title="Области доступа"
                description="Определяют, с какими подразделениями пользователь может работать в административных и управленческих сценариях."
              >
                <Form<AssignUserAccessAreaRequest>
                  layout="vertical"
                  form={accessAreaForm}
                  initialValues={{ accessScopeType: 'UNIT_ONLY' }}
                  onFinish={(values) => createAccessAreaMutation.mutate(values)}
                >
                  <div className="admin-form-grid">
                    <Form.Item name="userId" label="Пользователь" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={userOptions} />
                    </Form.Item>
                    <Form.Item name="organizationalUnitId" label="Подразделение" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={unitOptions} />
                    </Form.Item>
                    <Form.Item name="accessScopeType" label="Область действия" rules={[{ required: true }]}>
                      <Select options={accessScopeOptions} />
                    </Form.Item>
                    <Form.Item name="validFrom" label="Действует с (ISO)">
                      <Input />
                    </Form.Item>
                  </div>
                  <Button type="primary" htmlType="submit" loading={createAccessAreaMutation.isPending}>
                    Создать запись
                  </Button>
                </Form>
                <Table
                  rowKey="id"
                  pagination={{ pageSize: 10 }}
                  dataSource={accessAreasQuery.data ?? []}
                  columns={[
                    { title: 'ID', dataIndex: 'id' },
                    {
                      title: 'Пользователь',
                      dataIndex: 'userId',
                      render: (value) => (
                        <Space direction="vertical" size={0}>
                          <Typography.Text strong>{formatUserName(usersById.get(value))}</Typography.Text>
                          <Typography.Text type="secondary">{usersById.get(value)?.employeeNumber ?? `ID ${value}`}</Typography.Text>
                        </Space>
                      ),
                    },
                    {
                      title: 'Подразделение',
                      dataIndex: 'organizationalUnitId',
                      render: (value) => humanizeOrganizationalUnit(unitsById.get(value)?.name, unitsById.get(value)?.path, `Узел #${value}`),
                    },
                    { title: 'Область', dataIndex: 'accessScopeType', render: (value) => localizeAccessScopeType(value) },
                    { title: 'Действует с', dataIndex: 'validFrom', render: (value) => formatUiDate(value) },
                    { title: 'Действует до', dataIndex: 'validTo', render: (value) => formatUiDate(value, 'Открыто') },
                    {
                      title: 'Состояние',
                      render: (_, record) => renderState(record.validTo),
                    },
                    {
                      title: 'Действие',
                      render: (_, record) =>
                        isActive(record.validTo) ? (
                          <Button size="small" onClick={() => closeAccessAreaMutation.mutate({ id: record.id, values: {} })}>
                            Закрыть
                          </Button>
                        ) : null,
                    },
                  ]}
                />
              </SectionCard>
            ),
          },
          {
            key: 'relations',
            label: 'Управленческие связи',
            children: (
              <SectionCard
                title="Управленческие связи"
                description="Связывают руководителя с подразделением, за которое он отвечает."
              >
                <Form<AssignManagementRelationRequest>
                  layout="vertical"
                  form={relationForm}
                  onFinish={(values) => createRelationMutation.mutate(values)}
                >
                  <div className="admin-form-grid">
                    <Form.Item name="userId" label="Пользователь" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={userOptions} />
                    </Form.Item>
                    <Form.Item name="organizationalUnitId" label="Подразделение" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={unitOptions} />
                    </Form.Item>
                    <Form.Item name="managementRelationTypeId" label="Тип связи" rules={[{ required: true }]}>
                      <Select options={relationTypeOptions} />
                    </Form.Item>
                    <Form.Item name="validFrom" label="Действует с (ISO)">
                      <Input />
                    </Form.Item>
                  </div>
                  <Button type="primary" htmlType="submit" loading={createRelationMutation.isPending}>
                    Создать связь
                  </Button>
                </Form>
                <Table
                  rowKey="id"
                  pagination={{ pageSize: 10 }}
                  dataSource={relationsQuery.data ?? []}
                  columns={[
                    { title: 'ID', dataIndex: 'id' },
                    { title: 'Пользователь', dataIndex: 'userId', render: (value) => formatUserName(usersById.get(value)) },
                    {
                      title: 'Подразделение',
                      dataIndex: 'organizationalUnitId',
                      render: (value) => humanizeOrganizationalUnit(unitsById.get(value)?.name, unitsById.get(value)?.path, `Узел #${value}`),
                    },
                    {
                      title: 'Тип связи',
                      dataIndex: 'managementRelationTypeId',
                      render: (value) => formatRelationType(relationTypesById.get(value)),
                    },
                    { title: 'Действует с', dataIndex: 'validFrom', render: (value) => formatUiDate(value) },
                    { title: 'Действует до', dataIndex: 'validTo', render: (value) => formatUiDate(value, 'Открыто') },
                    {
                      title: 'Действие',
                      render: (_, record) =>
                        isActive(record.validTo) ? (
                          <Button size="small" onClick={() => closeRelationMutation.mutate({ id: record.id, values: {} })}>
                            Закрыть
                          </Button>
                        ) : null,
                    },
                  ]}
                />
              </SectionCard>
            ),
          },
          {
            key: 'temp-roles',
            label: 'Временные роли',
            children: (
              <SectionCard
                title="Временные роли"
                description="Позволяют временно расширить полномочия пользователя без изменения его основной роли."
              >
                <Form<AssignTemporaryRoleRequest> layout="vertical" form={tempRoleForm} onFinish={(values) => createTempRoleMutation.mutate(values)}>
                  <div className="admin-form-grid">
                    <Form.Item name="userId" label="Пользователь" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={userOptions} />
                    </Form.Item>
                    <Form.Item name="roleId" label="Роль" rules={[{ required: true }]}>
                      <Select options={roleOptions} />
                    </Form.Item>
                    <Form.Item name="validFrom" label="Действует с (ISO)">
                      <Input />
                    </Form.Item>
                  </div>
                  <Button type="primary" htmlType="submit" loading={createTempRoleMutation.isPending}>
                    Назначить временную роль
                  </Button>
                </Form>
                <Table
                  rowKey="id"
                  pagination={{ pageSize: 10 }}
                  dataSource={tempRolesQuery.data ?? []}
                  columns={[
                    { title: 'ID', dataIndex: 'id' },
                    { title: 'Пользователь', dataIndex: 'userId', render: (value) => formatUserName(usersById.get(value)) },
                    { title: 'Роль', dataIndex: 'roleId', render: (value) => formatRoleName(rolesById.get(value)) },
                    { title: 'Действует с', dataIndex: 'validFrom', render: (value) => formatUiDate(value) },
                    { title: 'Действует до', dataIndex: 'validTo', render: (value) => formatUiDate(value, 'Открыто') },
                    {
                      title: 'Действие',
                      render: (_, record) =>
                        isActive(record.validTo) ? (
                          <Button size="small" onClick={() => closeTempRoleMutation.mutate({ id: record.id, values: {} })}>
                            Закрыть
                          </Button>
                        ) : null,
                    },
                  ]}
                />
              </SectionCard>
            ),
          },
          {
            key: 'temp-areas',
            label: 'Временные доступы',
            children: (
              <SectionCard
                title="Временные области доступа"
                description="Используются для ограниченного по времени доступа к подразделению или его поддереву."
              >
                <Form<AssignTemporaryAccessAreaRequest> layout="vertical" form={tempAreaForm} initialValues={{ accessScopeType: 'UNIT_ONLY' }} onFinish={(values) => createTempAreaMutation.mutate(values)}>
                  <div className="admin-form-grid">
                    <Form.Item name="userId" label="Пользователь" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={userOptions} />
                    </Form.Item>
                    <Form.Item name="organizationalUnitId" label="Подразделение" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={unitOptions} />
                    </Form.Item>
                    <Form.Item name="accessScopeType" label="Область действия" rules={[{ required: true }]}>
                      <Select options={accessScopeOptions} />
                    </Form.Item>
                    <Form.Item name="validFrom" label="Действует с (ISO)">
                      <Input />
                    </Form.Item>
                  </div>
                  <Button type="primary" htmlType="submit" loading={createTempAreaMutation.isPending}>
                    Назначить временную область
                  </Button>
                </Form>
                <Table
                  rowKey="id"
                  pagination={{ pageSize: 10 }}
                  dataSource={tempAreasQuery.data ?? []}
                  columns={[
                    { title: 'ID', dataIndex: 'id' },
                    { title: 'Пользователь', dataIndex: 'userId', render: (value) => formatUserName(usersById.get(value)) },
                    {
                      title: 'Подразделение',
                      dataIndex: 'organizationalUnitId',
                      render: (value) => humanizeOrganizationalUnit(unitsById.get(value)?.name, unitsById.get(value)?.path, `Узел #${value}`),
                    },
                    { title: 'Область', dataIndex: 'accessScopeType', render: (value) => localizeAccessScopeType(value) },
                    { title: 'Действует с', dataIndex: 'validFrom', render: (value) => formatUiDate(value) },
                    { title: 'Действует до', dataIndex: 'validTo', render: (value) => formatUiDate(value, 'Открыто') },
                    {
                      title: 'Действие',
                      render: (_, record) =>
                        isActive(record.validTo) ? (
                          <Button size="small" onClick={() => closeTempAreaMutation.mutate({ id: record.id, values: {} })}>
                            Закрыть
                          </Button>
                        ) : null,
                    },
                  ]}
                />
              </SectionCard>
            ),
          },
          {
            key: 'temp-delegations',
            label: 'Временные делегирования',
            children: (
              <SectionCard
                title="Временные управленческие делегирования"
                description="Фиксируют временную передачу управленческой ответственности по подразделению."
              >
                <Form<AssignTemporaryManagementDelegationRequest> layout="vertical" form={tempDelegationForm} onFinish={(values) => createTempDelegationMutation.mutate(values)}>
                  <div className="admin-form-grid">
                    <Form.Item name="userId" label="Пользователь" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={userOptions} />
                    </Form.Item>
                    <Form.Item name="organizationalUnitId" label="Подразделение" rules={[{ required: true }]}>
                      <Select showSearch optionFilterProp="label" options={unitOptions} />
                    </Form.Item>
                    <Form.Item name="managementRelationTypeId" label="Тип связи" rules={[{ required: true }]}>
                      <Select options={relationTypeOptions} />
                    </Form.Item>
                    <Form.Item name="validFrom" label="Действует с (ISO)">
                      <Input />
                    </Form.Item>
                  </div>
                  <Button type="primary" htmlType="submit" loading={createTempDelegationMutation.isPending}>
                    Назначить делегирование
                  </Button>
                </Form>
                <Table
                  rowKey="id"
                  pagination={{ pageSize: 10 }}
                  dataSource={tempDelegationsQuery.data ?? []}
                  columns={[
                    { title: 'ID', dataIndex: 'id' },
                    { title: 'Пользователь', dataIndex: 'userId', render: (value) => formatUserName(usersById.get(value)) },
                    {
                      title: 'Подразделение',
                      dataIndex: 'organizationalUnitId',
                      render: (value) => humanizeOrganizationalUnit(unitsById.get(value)?.name, unitsById.get(value)?.path, `Узел #${value}`),
                    },
                    {
                      title: 'Тип связи',
                      dataIndex: 'managementRelationTypeId',
                      render: (value) => formatRelationType(relationTypesById.get(value)),
                    },
                    { title: 'Действует с', dataIndex: 'validFrom', render: (value) => formatUiDate(value) },
                    { title: 'Действует до', dataIndex: 'validTo', render: (value) => formatUiDate(value, 'Открыто') },
                    {
                      title: 'Действие',
                      render: (_, record) =>
                        isActive(record.validTo) ? (
                          <Button size="small" onClick={() => closeTempDelegationMutation.mutate({ id: record.id, values: {} })}>
                            Закрыть
                          </Button>
                        ) : null,
                    },
                  ]}
                />
              </SectionCard>
            ),
          },
        ]}
      />
    </Space>
  );
}
