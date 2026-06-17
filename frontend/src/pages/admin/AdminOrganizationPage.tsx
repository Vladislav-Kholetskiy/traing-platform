import {
  Alert,
  Button,
  Checkbox,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Tabs,
  Tree,
  Typography,
} from 'antd';
import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type {
  CreateOrganizationalUnitRequest,
  CreateOrganizationalUnitTypeRequest,
  OrganizationalNodeKind,
  OrganizationalUnit,
  OrganizationalUnitType,
  UpdateOrganizationalUnitTypeRequest,
} from '../../features/admin-organization/model/adminOrganization';
import {
  useCreateOrganizationalUnitMutation,
  useCreateOrganizationalUnitTypeMutation,
  useOrganizationalUnits,
  useOrganizationalUnitsTree,
  useOrganizationalUnitTypes,
  useUpdateOrganizationalUnitTypeMutation,
} from '../../features/admin-organization/model/useAdminOrganization';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import {
  humanizeOrganizationalPath,
  humanizeOrganizationalUnit,
  humanizeOrganizationalUnitType,
} from '../../shared/ui/organizationalUnits';
import {
  formatBoolean,
  formatUiDate,
  localizeOrganizationalNodeKind,
  localizeOrganizationalUnitStatus,
} from '../../shared/ui/presentation';

function mapTreeData(units: OrganizationalUnit[]): Array<{
  key: number;
  title: ReactNode;
  children: ReturnType<typeof mapTreeData>;
}> {
  return units.map((unit) => ({
    key: unit.id,
    title: (
      <Space direction="vertical" size={0}>
        <Link to={`/admin/organization/${unit.id}`}>
          {humanizeOrganizationalUnit(unit.name, unit.path)}
        </Link>
        <Typography.Text type="secondary">{humanizeOrganizationalPath(unit.path)}</Typography.Text>
      </Space>
    ),
    children: mapTreeData(unit.children ?? []),
  }));
}

export function AdminOrganizationPage() {
  const { data: actor } = useCurrentActor();
  const unitTypesQuery = useOrganizationalUnitTypes(undefined, Boolean(actor));
  const treeQuery = useOrganizationalUnitsTree(undefined, Boolean(actor));
  const unitsQuery = useOrganizationalUnits(undefined, Boolean(actor));
  const createTypeMutation = useCreateOrganizationalUnitTypeMutation();
  const createUnitMutation = useCreateOrganizationalUnitMutation();
  const [editingTypeId, setEditingTypeId] = useState<number | null>(null);
  const updateTypeMutation = useUpdateOrganizationalUnitTypeMutation(editingTypeId ?? undefined);
  const [typeForm] = Form.useForm<CreateOrganizationalUnitTypeRequest>();
  const [editTypeForm] = Form.useForm<UpdateOrganizationalUnitTypeRequest>();
  const [unitForm] = Form.useForm<CreateOrganizationalUnitRequest>();
  const [activeTab, setActiveTab] = useState('tree');

  const editingType = (unitTypesQuery.data ?? []).find((type) => type.id === editingTypeId) ?? null;

  useEffect(() => {
    if (!editingType) {
      editTypeForm.resetFields();
      return;
    }

    editTypeForm.setFieldsValue({
      name: editingType.name,
      description: editingType.description ?? undefined,
      nodeKind: editingType.nodeKind,
      canBeOperatorHomeUnit: editingType.canBeOperatorHomeUnit,
      canBeCampaignTarget: editingType.canBeCampaignTarget,
      participatesInSubtreeScope: editingType.participatesInSubtreeScope,
      canHaveManagementRelation: editingType.canHaveManagementRelation,
      canHaveAccessArea: editingType.canHaveAccessArea,
    });
  }, [editingType, editTypeForm]);

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }

  if (unitTypesQuery.isLoading || treeQuery.isLoading || unitsQuery.isLoading) {
    return (
      <LoadingView
        title="Загрузка оргструктуры"
        description="Получаем дерево оргузлов, список типов и каталог узлов."
      />
    );
  }

  if (unitTypesQuery.isError || treeQuery.isError || unitsQuery.isError) {
    return (
      <ErrorView
        title="Не удалось загрузить оргструктуру"
        error={unitTypesQuery.error ?? treeQuery.error ?? unitsQuery.error}
      />
    );
  }

  const unitTypesById = new Map((unitTypesQuery.data ?? []).map((type) => [type.id, type]));

  const unitTypeColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      render: (value: number) => <Typography.Text code>{value}</Typography.Text>,
    },
    {
      title: 'Тип',
      render: (_: unknown, type: OrganizationalUnitType) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{humanizeOrganizationalUnitType(type.name, type.code)}</Typography.Text>
          <Typography.Text type="secondary">{type.code}</Typography.Text>
        </Space>
      ),
    },
    {
      title: 'Характер',
      dataIndex: 'nodeKind',
      render: (value: OrganizationalNodeKind) => localizeOrganizationalNodeKind(value),
    },
    {
      title: 'Цель кампаний',
      dataIndex: 'canBeCampaignTarget',
      render: (value: boolean) => formatBoolean(value),
    },
    {
      title: 'Обновлён',
      dataIndex: 'updatedAt',
      render: (value?: string | null) => formatUiDate(value ?? undefined),
    },
    {
      title: 'Действие',
      render: (_: unknown, type: OrganizationalUnitType) => (
        <Button size="small" onClick={() => setEditingTypeId(type.id)}>
          Редактировать
        </Button>
      ),
    },
  ];

  const unitColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      render: (value: number) => <Typography.Text code>{value}</Typography.Text>,
    },
    {
      title: 'Узел',
      render: (_: unknown, unit: OrganizationalUnit) => (
        <Space direction="vertical" size={0}>
          <Link to={`/admin/organization/${unit.id}`}>{humanizeOrganizationalUnit(unit.name, unit.path)}</Link>
          <Typography.Text type="secondary">{humanizeOrganizationalPath(unit.path)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      render: (value: string) => localizeOrganizationalUnitStatus(value),
    },
    {
      title: 'Тип',
      dataIndex: 'organizationalUnitTypeId',
      render: (value: number) => {
        const type = unitTypesById.get(value);
        return humanizeOrganizationalUnitType(type?.name, type?.code, `Тип #${value}`);
      },
    },
    {
      title: 'Внешний ID',
      dataIndex: 'externalId',
      render: (value?: string | null) => value || 'Не указан',
    },
  ];

  const commandError = createTypeMutation.error ?? createUnitMutation.error ?? updateTypeMutation.error;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title="Администрирование оргструктуры"
        description="Раздел показывает дерево подразделений, справочник типов узлов и каталог организационных единиц."
      />

      {commandError ? (
        <Alert
          type="error"
          showIcon
          message="Команда по оргструктуре завершилась ошибкой"
          description={String(commandError)}
        />
      ) : null}

      {updateTypeMutation.isSuccess && editingType ? (
        <Alert
          type="success"
          showIcon
          message="Тип оргузла обновлён"
          description={`Изменения для типа «${humanizeOrganizationalUnitType(editingType.name, editingType.code)}» сохранены.`}
        />
      ) : null}

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'tree',
            label: 'Дерево',
            children: (
              <SectionCard title="Дерево оргструктуры">
                <Tree treeData={mapTreeData(treeQuery.data ?? [])} defaultExpandAll />
              </SectionCard>
            ),
          },
          {
            key: 'types',
            label: 'Типы узлов',
            children: (
              <SectionCard title="Типы оргузлов">
                <Form<CreateOrganizationalUnitTypeRequest>
                  layout="vertical"
                  form={typeForm}
                  initialValues={{
                    nodeKind: 'LINEAR',
                    canBeOperatorHomeUnit: false,
                    canBeCampaignTarget: false,
                    participatesInSubtreeScope: false,
                    canHaveManagementRelation: false,
                    canHaveAccessArea: false,
                  }}
                  onFinish={async (values) => {
                    await createTypeMutation.mutateAsync(values);
                    typeForm.resetFields();
                  }}
                >
                  <div className="admin-form-grid">
                    <Form.Item name="code" label="Код типа" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                    <Form.Item name="name" label="Название" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                    <Form.Item name="nodeKind" label="Характер узла" rules={[{ required: true }]}>
                      <Select
                        options={[
                          { label: 'Линейный', value: 'LINEAR' },
                          { label: 'Функциональный', value: 'FUNCTIONAL' },
                        ]}
                      />
                    </Form.Item>
                    <Form.Item name="description" label="Описание">
                      <Input />
                    </Form.Item>
                    <Form.Item name="canBeOperatorHomeUnit" label="Может быть основным подразделением работника" valuePropName="checked">
                      <Checkbox />
                    </Form.Item>
                    <Form.Item name="canBeCampaignTarget" label="Может быть целью кампании обучения" valuePropName="checked">
                      <Checkbox />
                    </Form.Item>
                    <Form.Item name="participatesInSubtreeScope" label="Участвует в доступе по поддереву" valuePropName="checked">
                      <Checkbox />
                    </Form.Item>
                    <Form.Item name="canHaveManagementRelation" label="Поддерживает управленческие связи" valuePropName="checked">
                      <Checkbox />
                    </Form.Item>
                    <Form.Item name="canHaveAccessArea" label="Поддерживает области доступа" valuePropName="checked">
                      <Checkbox />
                    </Form.Item>
                  </div>
                  <Button type="primary" htmlType="submit" loading={createTypeMutation.isPending}>
                    Создать тип узла
                  </Button>
                </Form>

                <Table rowKey="id" dataSource={unitTypesQuery.data ?? []} columns={unitTypeColumns} pagination={false} />

                {editingType ? (
                  <SectionCard title={`Редактирование типа узла #${editingType.id}`}>
                    <Space direction="vertical" size={12} style={{ width: '100%' }}>
                      <Typography.Text type="secondary">
                        Код типа используется как стабильный идентификатор справочника: {editingType.code}
                      </Typography.Text>
                      <Form<UpdateOrganizationalUnitTypeRequest>
                        layout="vertical"
                        form={editTypeForm}
                        onFinish={async (values) => {
                          await updateTypeMutation.mutateAsync(values);
                        }}
                      >
                        <div className="admin-form-grid">
                          <Form.Item name="name" label="Название" rules={[{ required: true }]}>
                            <Input />
                          </Form.Item>
                          <Form.Item name="nodeKind" label="Характер узла">
                            <Select
                              options={[
                                { label: 'Линейный', value: 'LINEAR' },
                                { label: 'Функциональный', value: 'FUNCTIONAL' },
                              ]}
                            />
                          </Form.Item>
                          <Form.Item name="description" label="Описание">
                            <Input />
                          </Form.Item>
                          <Form.Item name="canBeOperatorHomeUnit" label="Может быть основным подразделением работника" valuePropName="checked">
                            <Checkbox />
                          </Form.Item>
                          <Form.Item name="canBeCampaignTarget" label="Может быть целью кампании обучения" valuePropName="checked">
                            <Checkbox />
                          </Form.Item>
                          <Form.Item name="participatesInSubtreeScope" label="Участвует в доступе по поддереву" valuePropName="checked">
                            <Checkbox />
                          </Form.Item>
                          <Form.Item name="canHaveManagementRelation" label="Поддерживает управленческие связи" valuePropName="checked">
                            <Checkbox />
                          </Form.Item>
                          <Form.Item name="canHaveAccessArea" label="Поддерживает области доступа" valuePropName="checked">
                            <Checkbox />
                          </Form.Item>
                        </div>
                        <Space wrap>
                          <Button type="primary" htmlType="submit" loading={updateTypeMutation.isPending}>
                            Сохранить изменения
                          </Button>
                          <Button onClick={() => setEditingTypeId(null)} disabled={updateTypeMutation.isPending}>
                            Закрыть форму
                          </Button>
                        </Space>
                      </Form>
                    </Space>
                  </SectionCard>
                ) : null}
              </SectionCard>
            ),
          },
          {
            key: 'units',
            label: 'Каталог узлов',
            children: (
              <SectionCard title="Оргузлы">
                <Form<CreateOrganizationalUnitRequest>
                  layout="vertical"
                  form={unitForm}
                  onFinish={async (values) => {
                    await createUnitMutation.mutateAsync(values);
                    unitForm.resetFields();
                  }}
                >
                  <div className="admin-form-grid">
                    <Form.Item name="parentId" label="Родительский узел">
                      <InputNumber style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="organizationalUnitTypeId" label="Тип узла" rules={[{ required: true }]}>
                      <Select
                        options={(unitTypesQuery.data ?? []).map((type) => ({
                          label: `${humanizeOrganizationalUnitType(type.name, type.code)} (${type.code})`,
                          value: type.id,
                        }))}
                      />
                    </Form.Item>
                    <Form.Item name="name" label="Название" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                    <Form.Item name="externalId" label="Внешний ID">
                      <Input />
                    </Form.Item>
                  </div>
                  <Button type="primary" htmlType="submit" loading={createUnitMutation.isPending}>
                    Создать оргузел
                  </Button>
                </Form>
                <Table
                  rowKey="id"
                  dataSource={unitsQuery.data ?? []}
                  columns={unitColumns}
                  pagination={{ pageSize: 10 }}
                  expandable={{ showExpandColumn: false }}
                />
              </SectionCard>
            ),
          },
        ]}
      />
    </Space>
  );
}
