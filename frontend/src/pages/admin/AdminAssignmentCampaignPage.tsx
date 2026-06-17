import { Alert, Button, Collapse, DatePicker, Form, Input, Select, Space, Typography } from 'antd';
import type { Dayjs } from 'dayjs';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { useAdminUsers } from '../../features/admin-users/model/useAdminUsers';
import type {
  CancelAssignmentRequest,
  LaunchAssignmentCampaignRequest,
  LaunchIndividualAssignmentRequest,
} from '../../features/assignment-admin/model/assignmentAdmin';
import {
  useAssignmentCampaigns,
  useAssignmentCampaignTargetUnits,
  useAssignments,
  useCancelAssignmentMutation,
  useExtendAssignmentDeadlineMutation,
  useLaunchAssignmentCampaignMutation,
  useLaunchIndividualAssignmentMutation,
  useReplaceAssignmentWithNewMutation,
} from '../../features/assignment-admin/model/useAssignmentAdmin';
import { canAccessAssignmentCampaignArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useCourses } from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { humanizeOrganizationalUnit } from '../../shared/ui/organizationalUnits';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';

type LaunchAssignmentCampaignFormValues = {
  name: string;
  description?: string;
  courseIds: number[];
  targetUnitRefs: string[];
  deadlineAt: Dayjs;
};

type LaunchIndividualAssignmentFormValues = {
  name: string;
  description?: string;
  userId: number;
  courseIds: number[];
  deadlineAt: Dayjs;
};

type CancelAssignmentFormValues = CancelAssignmentRequest & { assignmentId: number };
type ExtendAssignmentDeadlineFormValues = { assignmentId: number; newDeadlineAt: Dayjs; note: string };
type ReplaceAssignmentWithNewFormValues = { assignmentId: number; campaignId: number; newCycleDeadlineAt: Dayjs; note: string };
type QuickAccessSection = 'campaigns' | 'individual' | 'service';

function resolveQuickAccessSection(pathname: string): QuickAccessSection {
  if (pathname.startsWith('/admin/assignment-individual')) {
    return 'individual';
  }
  if (pathname.startsWith('/admin/assignment-service')) {
    return 'service';
  }
  return 'campaigns';
}

function routeBySection(section: QuickAccessSection): string {
  switch (section) {
    case 'individual':
      return '/admin/assignment-individual';
    case 'service':
      return '/admin/assignment-service';
    default:
      return '/admin/assignment-campaigns';
  }
}

function formatTargetUnitLabel(unitName?: string | null, unitPath?: string | null): string {
  const humanLabel = humanizeOrganizationalUnit(unitName, unitPath, 'Подразделение');
  const shortCode = unitPath?.split('/').filter(Boolean).at(-1)?.trim().toUpperCase();
  if (!shortCode) {
    return humanLabel;
  }

  return humanLabel.toUpperCase().includes(shortCode) ? humanLabel : `${humanLabel} (${shortCode})`;
}

function formatUserLabel(
  lastName?: string | null,
  firstName?: string | null,
  middleName?: string | null,
  employeeNumber?: string | null,
): string {
  const fullName = [lastName, firstName, middleName].filter(Boolean).join(' ').trim();
  return employeeNumber ? `${fullName || 'Сотрудник'} • ${employeeNumber}` : fullName || 'Сотрудник';
}

export function AdminAssignmentCampaignPage() {
  const { data: actor } = useCurrentActor();
  const location = useLocation();
  const navigate = useNavigate();
  const activeSection = resolveQuickAccessSection(location.pathname);

  const coursesQuery = useCourses(Boolean(actor));
  const targetUnitsQuery = useAssignmentCampaignTargetUnits(Boolean(actor));
  const activeUsersQuery = useAdminUsers({ status: 'ACTIVE' }, Boolean(actor) && activeSection === 'individual');
  const adminCampaignsQuery = useAssignmentCampaigns(undefined, Boolean(actor) && activeSection === 'service');
  const adminAssignmentsQuery = useAssignments(undefined, Boolean(actor) && activeSection === 'service');

  const [campaignSuccessMessage, setCampaignSuccessMessage] = useState<string | null>(null);
  const [individualSuccessMessage, setIndividualSuccessMessage] = useState<string | null>(null);
  const [isLaunchingCampaigns, setIsLaunchingCampaigns] = useState(false);

  const [launchForm] = Form.useForm<LaunchAssignmentCampaignFormValues>();
  const [individualForm] = Form.useForm<LaunchIndividualAssignmentFormValues>();
  const [cancelForm] = Form.useForm<CancelAssignmentFormValues>();
  const [extendForm] = Form.useForm<ExtendAssignmentDeadlineFormValues>();
  const [replaceForm] = Form.useForm<ReplaceAssignmentWithNewFormValues>();

  const launchMutation = useLaunchAssignmentCampaignMutation();
  const individualLaunchMutation = useLaunchIndividualAssignmentMutation();
  const cancelMutation = useCancelAssignmentMutation();
  const extendMutation = useExtendAssignmentDeadlineMutation();
  const replaceMutation = useReplaceAssignmentWithNewMutation();

  if (!actor || !canAccessAssignmentCampaignArea(actor)) {
    return <ForbiddenView />;
  }

  if (
    coursesQuery.isLoading
    || targetUnitsQuery.isLoading
    || (activeSection === 'individual' && activeUsersQuery.isLoading)
    || (activeSection === 'service' && (adminCampaignsQuery.isLoading || adminAssignmentsQuery.isLoading))
  ) {
    return (
      <LoadingView
        title="Загрузка экрана назначений"
        description="Подготавливаем курсы, подразделения и доступные сценарии запуска."
      />
    );
  }

  if (
    coursesQuery.isError
    || targetUnitsQuery.isError
    || (activeSection === 'individual' && activeUsersQuery.isError)
    || (activeSection === 'service' && (adminCampaignsQuery.isError || adminAssignmentsQuery.isError))
  ) {
    return (
      <ErrorView
        title="Не удалось открыть экран назначений"
        error={
          coursesQuery.error
          ?? targetUnitsQuery.error
          ?? activeUsersQuery.error
          ?? adminCampaignsQuery.error
          ?? adminAssignmentsQuery.error
        }
      />
    );
  }

  const commandError =
    launchMutation.error ??
    individualLaunchMutation.error ??
    cancelMutation.error ??
    extendMutation.error ??
    replaceMutation.error;

  const availableCourses = (coursesQuery.data ?? []).filter((course) => course.status !== 'ARCHIVED');
  const courseOptions = availableCourses.map((course) => ({
    value: course.id,
    label: `${course.name}${course.status === 'PUBLISHED' ? '' : ' • черновик'}`,
  }));

  const actorUnitPath = actor.primaryOrganizationalUnitPath?.trim() || undefined;
  const actorUnitName = actor.primaryOrganizationalUnitName?.trim() || actorUnitPath;
  const targetUnits = targetUnitsQuery.data ?? [];
  const targetUnitOptions = targetUnits.map((unit) => ({
    value: unit.path || String(unit.id),
    label: formatTargetUnitLabel(unit.name, unit.path),
    searchText: `${formatTargetUnitLabel(unit.name, unit.path)} ${unit.name ?? ''} ${unit.path ?? ''}`,
  }));
  const resolvedDefaultTargetUnit =
    targetUnits.find((unit) => unit.path === actorUnitPath)?.path || targetUnitOptions[0]?.value;

  const activeUsers = activeUsersQuery.data ?? [];
  const activeUserOptions = activeUsers.map((user) => ({
    value: user.id,
    label: formatUserLabel(user.lastName, user.firstName, user.middleName, user.employeeNumber),
    searchText: `${formatUserLabel(user.lastName, user.firstName, user.middleName, user.employeeNumber)} ${user.employeeNumber}`,
  }));
  const adminCampaigns = adminCampaignsQuery.data ?? [];
  const adminAssignments = adminAssignmentsQuery.data ?? [];
  const adminCampaignOptions = adminCampaigns.map((campaign) => ({
    value: campaign.id,
    label: `#${campaign.id} ${campaign.name}`,
    searchText: `${campaign.id} ${campaign.name} ${campaign.sourceNameSnapshot ?? ''}`,
  }));
  const adminAssignmentOptions = adminAssignments.map((assignment) => ({
    value: assignment.id,
    label: `#${assignment.id} • ${assignment.courseName ?? `Курс ${assignment.courseId ?? ''}`} • ${assignment.status ?? 'Статус не указан'}`,
    searchText: `${assignment.id} ${assignment.courseName ?? ''} ${assignment.courseId ?? ''} ${assignment.status ?? ''} ${assignment.userId ?? ''}`,
  }));

  const renderQuickAccessButton = (key: QuickAccessSection, title: string, note?: string) => (
    <button
      type="button"
      key={key}
      className={`assignment-admin-quick-link${activeSection === key ? ' assignment-admin-quick-link-active' : ''}`}
      onClick={() => navigate(routeBySection(key))}
    >
      <span className="assignment-admin-quick-link-title">{title}</span>
      {note ? <span className="assignment-admin-quick-link-note">{note}</span> : null}
    </button>
  );

  const campaignSection = (
    <SectionCard title="Кампании назначений">
      <Form<LaunchAssignmentCampaignFormValues>
        layout="vertical"
        form={launchForm}
        initialValues={{
          targetUnitRefs: resolvedDefaultTargetUnit ? [resolvedDefaultTargetUnit] : [],
        }}
        onFinish={async (values) => {
          setCampaignSuccessMessage(null);
          setIndividualSuccessMessage(null);
          setIsLaunchingCampaigns(true);

          const successfulLaunches: string[] = [];
          try {
            for (const targetUnitRef of values.targetUnitRefs) {
              const selectedUnit = targetUnits.find((unit) => (unit.path || String(unit.id)) === targetUnitRef);
              const targetUnitLabel = formatTargetUnitLabel(selectedUnit?.name, selectedUnit?.path ?? targetUnitRef);
              const payload: LaunchAssignmentCampaignRequest = {
                name: values.name.trim(),
                description: values.description?.trim() || undefined,
                sourceType: 'ORG_UNIT',
                sourceRef: targetUnitRef,
                sourceNameSnapshot: selectedUnit?.name ?? actorUnitName ?? targetUnitLabel,
                courseIds: values.courseIds,
                targeting: {
                  basisType: 'ORG_UNIT',
                  basisRef: targetUnitRef,
                },
                deadlinePolicy: {
                  deadlineAt: values.deadlineAt.toISOString(),
                },
              };

              const launchedCampaign = await launchMutation.mutateAsync(payload);
              successfulLaunches.push(
                `#${launchedCampaign.id}${launchedCampaign.name ? ` • ${launchedCampaign.name}` : ''} • ${targetUnitLabel}`,
              );
            }

            setCampaignSuccessMessage(
              successfulLaunches.length === 1
                ? `Кампания создана: ${successfulLaunches[0]}`
                : `Запущено кампаний: ${successfulLaunches.length}. ${successfulLaunches.join('; ')}`,
            );
          } catch {
            if (successfulLaunches.length > 0) {
              setCampaignSuccessMessage(`Часть запусков выполнена успешно: ${successfulLaunches.join('; ')}`);
            }
          } finally {
            setIsLaunchingCampaigns(false);
          }
        }}
      >
        <div className="assignment-admin-steps">
          <div className="assignment-admin-step">1. Выберите курсы</div>
          <div className="assignment-admin-step">2. Укажите подразделения</div>
          <div className="assignment-admin-step">3. Задайте срок и запустите</div>
        </div>

        <div className="admin-form-grid">
          <Form.Item
            name="name"
            label="Название кампании"
            rules={[{ required: true, message: 'Укажите название кампании' }]}
          >
            <Input placeholder="Например, Промышленная безопасность для смены" />
          </Form.Item>

          <Form.Item name="description" label="Комментарий" className="assignment-admin-span-2">
            <Input.TextArea rows={3} placeholder="Необязательно. Помогает понять цель назначения." />
          </Form.Item>

          <Form.Item
            name="courseIds"
            label="Курсы"
            rules={[{ required: true, message: 'Выберите хотя бы один курс' }]}
          >
            <Select
              mode="multiple"
              showSearch
              optionFilterProp="label"
              options={courseOptions}
              maxTagCount="responsive"
              placeholder="Выберите один или несколько курсов"
            />
          </Form.Item>

          <Form.Item
            name="targetUnitRefs"
            label="Подразделения"
            rules={[{ required: true, message: 'Выберите хотя бы одно подразделение' }]}
          >
            <Select
              mode="multiple"
              showSearch
              optionFilterProp="searchText"
              options={targetUnitOptions}
              maxTagCount="responsive"
              placeholder="Выберите одно или несколько подразделений"
            />
          </Form.Item>

          <Form.Item
            name="deadlineAt"
            label="Срок прохождения"
            rules={[{ required: true, message: 'Укажите срок прохождения' }]}
          >
            <DatePicker showTime format="DD.MM.YYYY HH:mm" style={{ width: '100%' }} placeholder="Выберите дату и время" />
          </Form.Item>
        </div>

        <div className="assignment-admin-submit-row">
          <Button
            type="primary"
            size="large"
            htmlType="submit"
            loading={isLaunchingCampaigns || launchMutation.isPending}
            disabled={!targetUnitOptions.length}
          >
            Запустить кампанию
          </Button>
        </div>
      </Form>
    </SectionCard>
  );

  const individualSection = (
    <SectionCard
      title="Индивидуальные назначения"
      description="Точечное назначение конкретному сотруднику без массовой кампании."
    >
      <Form<LaunchIndividualAssignmentFormValues>
        layout="vertical"
        form={individualForm}
        onFinish={async (values) => {
          setCampaignSuccessMessage(null);
          setIndividualSuccessMessage(null);

          const selectedUser = activeUsers.find((user) => user.id === values.userId);
          const payload: LaunchIndividualAssignmentRequest = {
            name: values.name.trim(),
            description: values.description?.trim() || undefined,
            userId: values.userId,
            courseIds: values.courseIds,
            deadlinePolicy: {
              deadlineAt: values.deadlineAt.toISOString(),
            },
          };

          const launchedCampaign = await individualLaunchMutation.mutateAsync(payload);
          setIndividualSuccessMessage(
            `Индивидуальное назначение создано: #${launchedCampaign.id}${launchedCampaign.name ? ` • ${launchedCampaign.name}` : ''}${selectedUser ? ` • ${formatUserLabel(selectedUser.lastName, selectedUser.firstName, selectedUser.middleName, selectedUser.employeeNumber)}` : ''}`,
          );
        }}
      >
        <div className="admin-form-grid">
          <Form.Item
            name="name"
            label="Название назначения"
            rules={[{ required: true, message: 'Укажите название назначения' }]}
          >
            <Input placeholder="Например, Персональное назначение по промбезопасности" />
          </Form.Item>

          <Form.Item name="description" label="Комментарий" className="assignment-admin-span-2">
            <Input.TextArea rows={3} placeholder="Необязательно. Для внутреннего пояснения." />
          </Form.Item>

          <Form.Item name="userId" label="Сотрудник" rules={[{ required: true, message: 'Укажите сотрудника' }]}>
            <Select
              showSearch
              optionFilterProp="searchText"
              options={activeUserOptions}
              placeholder="Выберите сотрудника"
            />
          </Form.Item>

          <Form.Item
            name="courseIds"
            label="Курсы"
            rules={[{ required: true, message: 'Выберите хотя бы один курс' }]}
          >
            <Select
              mode="multiple"
              showSearch
              optionFilterProp="label"
              options={courseOptions}
              maxTagCount="responsive"
              placeholder="Выберите один или несколько курсов"
            />
          </Form.Item>

          <Form.Item
            name="deadlineAt"
            label="Срок прохождения"
            rules={[{ required: true, message: 'Укажите срок прохождения' }]}
          >
            <DatePicker showTime format="DD.MM.YYYY HH:mm" style={{ width: '100%' }} placeholder="Выберите дату и время" />
          </Form.Item>
        </div>

        <div className="assignment-admin-submit-row">
          <Typography.Text type="secondary">
            Назначение будет создано только для выбранного сотрудника и не затронет остальных работников его подразделения.
          </Typography.Text>
          <Button type="primary" size="large" htmlType="submit" loading={individualLaunchMutation.isPending}>
            Назначить сотруднику
          </Button>
        </div>
      </Form>
    </SectionCard>
  );

  const serviceSection = (
    <SectionCard
      title="Служебные действия"
      description="Редкие операции по уже созданным назначениям."
    >
      {!adminCampaignOptions.length ? (
        <Alert
          type="info"
          showIcon
          className="assignment-admin-alert"
          message="Кампании пока не найдены"
          description="Когда в системе появятся запущенные кампании, их можно будет выбрать здесь для служебных действий."
        />
      ) : null}
      {!adminAssignmentOptions.length ? (
        <Alert
          type="info"
          showIcon
          className="assignment-admin-alert"
          message="Назначения пока не найдены"
          description="Список назначений загружается из административного раздела. Пока данных нет, служебные действия недоступны."
        />
      ) : null}
      <Collapse
        ghost
        items={[
          {
            key: 'cancel',
            label: 'Отменить назначение',
            children: (
              <Form
                form={cancelForm}
                layout="vertical"
                onFinish={async (values) => {
                  setCampaignSuccessMessage(null);
                  setIndividualSuccessMessage(null);
                  await cancelMutation.mutateAsync({
                    assignmentId: values.assignmentId,
                    values: { note: values.note.trim() },
                  });
                  cancelForm.resetFields();
                }}
              >
                <div className="admin-form-grid">
                  <Form.Item name="assignmentId" label="Назначение" rules={[{ required: true, message: 'Выберите назначение' }]}>
                    <Select showSearch optionFilterProp="searchText" options={adminAssignmentOptions} />
                  </Form.Item>
                  <Form.Item name="note" label="Комментарий" rules={[{ required: true, message: 'Добавьте комментарий' }]}>
                    <Input.TextArea rows={3} placeholder="Почему назначение отменяется" />
                  </Form.Item>
                </div>
                <Button type="primary" ghost htmlType="submit" loading={cancelMutation.isPending}>
                  Отменить назначение
                </Button>
              </Form>
            ),
          },
          {
            key: 'extend',
            label: 'Продлить срок',
            children: (
              <Form
                form={extendForm}
                layout="vertical"
                onFinish={async (values) => {
                  setCampaignSuccessMessage(null);
                  setIndividualSuccessMessage(null);
                  await extendMutation.mutateAsync({
                    assignmentId: values.assignmentId,
                    values: {
                      newDeadlineAt: values.newDeadlineAt.toISOString(),
                      note: values.note.trim(),
                    },
                  });
                  extendForm.resetFields();
                }}
              >
                <div className="admin-form-grid">
                  <Form.Item name="assignmentId" label="Назначение" rules={[{ required: true, message: 'Выберите назначение' }]}>
                    <Select showSearch optionFilterProp="searchText" options={adminAssignmentOptions} />
                  </Form.Item>
                  <Form.Item
                    name="newDeadlineAt"
                    label="Новый срок"
                    rules={[{ required: true, message: 'Укажите новый срок' }]}
                  >
                    <DatePicker showTime format="DD.MM.YYYY HH:mm" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="note" label="Комментарий" rules={[{ required: true, message: 'Добавьте комментарий' }]}>
                    <Input.TextArea rows={3} placeholder="Почему срок продлевается" />
                  </Form.Item>
                </div>
                <Button type="primary" ghost htmlType="submit" loading={extendMutation.isPending}>
                  Продлить срок
                </Button>
              </Form>
            ),
          },
          {
            key: 'replace',
            label: 'Заменить новым циклом',
            children: (
              <Form
                form={replaceForm}
                layout="vertical"
                onFinish={async (values) => {
                  setCampaignSuccessMessage(null);
                  setIndividualSuccessMessage(null);
                  await replaceMutation.mutateAsync({
                    assignmentId: values.assignmentId,
                    values: {
                      campaignId: values.campaignId,
                      newCycleDeadlineAt: values.newCycleDeadlineAt.toISOString(),
                      note: values.note.trim(),
                    },
                  });
                  replaceForm.resetFields();
                }}
              >
                <div className="admin-form-grid">
                  <Form.Item name="assignmentId" label="Назначение" rules={[{ required: true, message: 'Выберите назначение' }]}>
                    <Select showSearch optionFilterProp="searchText" options={adminAssignmentOptions} />
                  </Form.Item>
                  <Form.Item name="campaignId" label="Новая кампания" rules={[{ required: true, message: 'Выберите кампанию' }]}>
                    <Select showSearch optionFilterProp="searchText" options={adminCampaignOptions} />
                  </Form.Item>
                  <Form.Item
                    name="newCycleDeadlineAt"
                    label="Новый срок"
                    rules={[{ required: true, message: 'Укажите срок нового цикла' }]}
                  >
                    <DatePicker showTime format="DD.MM.YYYY HH:mm" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="note" label="Комментарий" rules={[{ required: true, message: 'Добавьте комментарий' }]}>
                    <Input.TextArea rows={3} placeholder="Почему создаётся новый цикл" />
                  </Form.Item>
                </div>
                <Button type="primary" ghost htmlType="submit" loading={replaceMutation.isPending}>
                  Создать новый цикл
                </Button>
              </Form>
            ),
          },
        ]}
      />
    </SectionCard>
  );

  const activeSectionContent =
    activeSection === 'campaigns'
      ? campaignSection
      : activeSection === 'individual'
        ? individualSection
        : serviceSection;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }} className="assignment-admin-page">
      <PageIntro title="Назначить обучение" />

      {commandError ? (
        <Alert
          type="error"
          showIcon
          className="assignment-admin-alert"
          message="Команда не выполнена"
          description={String(commandError)}
        />
      ) : null}

      {campaignSuccessMessage ? (
        <Alert
          type="success"
          showIcon
          className="assignment-admin-alert"
          message="Кампания запущена"
          description={campaignSuccessMessage}
        />
      ) : null}

      {individualSuccessMessage ? (
        <Alert
          type="success"
          showIcon
          className="assignment-admin-alert"
          message="Индивидуальное назначение создано"
          description={individualSuccessMessage}
        />
      ) : null}

      {!targetUnitOptions.length && activeSection === 'campaigns' ? (
        <Alert
          type="warning"
          showIcon
          className="assignment-admin-alert"
          message="Нет доступных подразделений"
          description="Система не вернула подразделения, доступные для назначения. Пока список пуст, кампанию запустить нельзя."
        />
      ) : null}

      <div className="assignment-admin-quick-panel">
        {renderQuickAccessButton('campaigns', 'Кампании назначений')}
        {renderQuickAccessButton('individual', 'Индивидуальные назначения')}
        {renderQuickAccessButton('service', 'Служебные действия')}
      </div>

      {activeSectionContent}
    </Space>
  );
}
