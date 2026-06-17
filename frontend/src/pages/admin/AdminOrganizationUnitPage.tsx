import { Alert, Button, Descriptions, Form, Input, Select, Space } from 'antd';
import { useParams } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type {
  MoveOrganizationalUnitRequest,
  UpdateOrganizationalUnitRequest,
} from '../../features/admin-organization/model/adminOrganization';
import {
  useArchiveOrganizationalUnitMutation,
  useMoveOrganizationalUnitMutation,
  useOrganizationalUnit,
  useOrganizationalUnits,
  useOrganizationalUnitTypes,
  useUpdateOrganizationalUnitMutation,
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
import { formatUiDate, localizeOrganizationalUnitStatus } from '../../shared/ui/presentation';

export function AdminOrganizationUnitPage() {
  const { unitId: unitIdParam } = useParams();
  const unitId = Number(unitIdParam);
  const { data: actor } = useCurrentActor();
  const unitQuery = useOrganizationalUnit(unitId, Boolean(actor));
  const unitTypesQuery = useOrganizationalUnitTypes(undefined, Boolean(actor));
  const unitsQuery = useOrganizationalUnits(undefined, Boolean(actor));
  const updateMutation = useUpdateOrganizationalUnitMutation(unitId);
  const moveMutation = useMoveOrganizationalUnitMutation(unitId);
  const archiveMutation = useArchiveOrganizationalUnitMutation(unitId);
  const [editForm] = Form.useForm<UpdateOrganizationalUnitRequest>();
  const [moveForm] = Form.useForm<MoveOrganizationalUnitRequest>();

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }

  if (unitQuery.isLoading || unitTypesQuery.isLoading || unitsQuery.isLoading) {
    return <LoadingView title="Загрузка оргузла" description="Получаем карточку узла и справочник типов." />;
  }

  if (unitQuery.isError || unitTypesQuery.isError || unitsQuery.isError) {
    return <ErrorView title="Не удалось загрузить оргузел" error={unitQuery.error ?? unitTypesQuery.error ?? unitsQuery.error} />;
  }

  const unit = unitQuery.data;
  if (!unit) {
    return <ErrorView title="Оргузел не найден" />;
  }

  const unitTypesById = new Map((unitTypesQuery.data ?? []).map((type) => [type.id, type]));
  const unitsById = new Map((unitsQuery.data ?? []).map((item) => [item.id, item]));
  const unitType = unitTypesById.get(unit.organizationalUnitTypeId);
  const parentUnit = unit.parentId ? unitsById.get(unit.parentId) : undefined;
  const unitName = humanizeOrganizationalUnit(unit.name, unit.path);

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title={unitName}
        description="Карточка подразделения показывает место узла в организационной структуре и доступные административные действия."
        extra={
          <Button danger loading={archiveMutation.isPending} onClick={() => archiveMutation.mutate()}>
            Перевести в архив
          </Button>
        }
      />

      {(updateMutation.isError || moveMutation.isError || archiveMutation.isError) ? (
        <Alert type="error" showIcon message="Команда по оргузлу завершилась ошибкой" description={String(updateMutation.error ?? moveMutation.error ?? archiveMutation.error)} />
      ) : null}

      <SectionCard title="Сведения о подразделении">
        <Descriptions bordered column={1}>
          <Descriptions.Item label="Название">{unitName}</Descriptions.Item>
          <Descriptions.Item label="Статус">{localizeOrganizationalUnitStatus(unit.status)}</Descriptions.Item>
          <Descriptions.Item label="Путь в структуре">{humanizeOrganizationalPath(unit.path)}</Descriptions.Item>
          <Descriptions.Item label="Тип подразделения">
            {humanizeOrganizationalUnitType(unitType?.name, unitType?.code, `Тип #${unit.organizationalUnitTypeId}`)}
          </Descriptions.Item>
          <Descriptions.Item label="Родительское подразделение">
            {parentUnit ? humanizeOrganizationalUnit(parentUnit.name, parentUnit.path) : 'Корневой узел'}
          </Descriptions.Item>
          <Descriptions.Item label="Уровень иерархии">{unit.depth ?? 0}</Descriptions.Item>
          <Descriptions.Item label="Внешний ID">{unit.externalId || 'Не указан'}</Descriptions.Item>
          <Descriptions.Item label="Обновлён">{formatUiDate(unit.updatedAt ?? undefined)}</Descriptions.Item>
        </Descriptions>
      </SectionCard>

      <SectionCard title="Редактирование подразделения">
        <Form<UpdateOrganizationalUnitRequest>
          layout="vertical"
          form={editForm}
          initialValues={{
            name: unit.name,
            externalId: unit.externalId || undefined,
            organizationalUnitTypeId: unit.organizationalUnitTypeId,
          }}
          onFinish={(values) => updateMutation.mutate(values)}
        >
          <div className="admin-form-grid">
            <Form.Item name="name" label="Название" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item name="externalId" label="Внешний ID">
              <Input />
            </Form.Item>
            <Form.Item name="organizationalUnitTypeId" label="Тип узла" rules={[{ required: true }]}>
              <Select
                options={(unitTypesQuery.data ?? []).map((type) => ({
                  label: `${humanizeOrganizationalUnitType(type.name, type.code)} (${type.code})`,
                  value: type.id,
                }))}
              />
            </Form.Item>
          </div>
          <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>
            Сохранить
          </Button>
        </Form>
      </SectionCard>

      <SectionCard title="Перемещение в структуре">
        <Form<MoveOrganizationalUnitRequest>
          layout="vertical"
          form={moveForm}
          onFinish={(values) => moveMutation.mutate(values)}
        >
          <Form.Item name="newParentOrganizationalUnitId" label="Новое родительское подразделение" rules={[{ required: true }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={(unitsQuery.data ?? [])
                .filter((item) => item.id !== unit.id)
                .map((item) => ({
                  label: humanizeOrganizationalPath(item.path, humanizeOrganizationalUnit(item.name)),
                  value: item.id,
                }))}
            />
          </Form.Item>
          <Button htmlType="submit" loading={moveMutation.isPending}>
            Переместить
          </Button>
        </Form>
      </SectionCard>
    </Space>
  );
}
