import { Empty, Space, Table, Typography } from 'antd';
import { Link } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAuditEvents } from '../../features/audit/model/useAudit';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate } from '../../shared/ui/presentation';

export function AdminAuditPage() {
  const { data: actor } = useCurrentActor();
  const auditQuery = useAuditEvents(undefined, Boolean(actor));

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }
  if (auditQuery.isLoading) {
    return <LoadingView title="Загрузка журнала аудита" />;
  }
  if (auditQuery.isError) {
    return <ErrorView title="Не удалось загрузить журнал аудита" error={auditQuery.error} />;
  }

  const filteredEvents = (auditQuery.data ?? []).filter((event) => isSignificantAuditEvent(event.eventType));

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <div className="page-header">
        <Typography.Title level={1} style={{ margin: 0, fontSize: '2.25rem' }}>
          Аудит
        </Typography.Title>
      </div>

      <SectionCard title="Журнал событий">
        {filteredEvents.length > 0 ? (
          <Table
            rowKey="id"
            dataSource={filteredEvents}
            pagination={{ pageSize: 10 }}
            columns={[
              {
                title: 'ID',
                dataIndex: 'id',
                render: (value) => <Typography.Text code>{value}</Typography.Text>,
              },
              {
                title: 'Событие',
                render: (_, item) => (
                  <Link to={`/admin/audit/${item.id}`}>{localizeAuditEventType(item.eventType)}</Link>
                ),
              },
              {
                title: 'Объект',
                render: (_, item) =>
                  `${localizeAuditEntityType(item.entityType)} / ${item.entityId || 'не указан'}`,
              },
              {
                title: 'Пользователь',
                dataIndex: 'actorUserId',
              },
              {
                title: 'Время',
                dataIndex: 'occurredAt',
                render: (value) => formatUiDate(value ?? undefined),
              },
            ]}
          />
        ) : (
          <Empty description="Значимых административных событий пока нет" />
        )}
      </SectionCard>
    </Space>
  );
}

function isSignificantAuditEvent(eventType?: string | null): boolean {
  const value = eventType?.trim().toUpperCase();
  if (!value) {
    return false;
  }

  return (
    value.startsWith('USERORG_') ||
    value.startsWith('ACCESS_') ||
    value.startsWith('CONTENT_') ||
    value.startsWith('ASSIGNMENT_') ||
    value.startsWith('NOTIFICATION_RULE_') ||
    value.startsWith('IMPORT_')
  );
}

function localizeAuditEventType(eventType?: string | null): string {
  switch (eventType?.trim().toUpperCase()) {
    case 'USERORG_USER_CREATE':
      return 'Создан пользователь';
    case 'USERORG_USER_UPDATE':
      return 'Обновлён пользователь';
    case 'USERORG_USER_DEACTIVATE':
      return 'Пользователь деактивирован';
    case 'USERORG_USER_ROLE_ASSIGN':
      return 'Назначена роль пользователю';
    case 'USERORG_USER_ROLE_CLOSE':
      return 'Роль пользователя закрыта';
    case 'USERORG_USER_ORGANIZATION_ASSIGN':
      return 'Назначено подразделение пользователю';
    case 'USERORG_USER_ORGANIZATION_CLOSE':
      return 'Привязка пользователя к подразделению закрыта';
    case 'USERORG_USER_PRIMARY_HOME_REPLACE':
      return 'Заменено основное подразделение пользователя';
    case 'ACCESS_USER_ACCESS_AREA_ASSIGN':
      return 'Назначена зона доступа пользователю';
    case 'ACCESS_USER_ACCESS_AREA_CLOSE':
      return 'Зона доступа пользователя закрыта';
    case 'ACCESS_MANAGEMENT_RELATION_ASSIGN':
      return 'Назначена управленческая связь';
    case 'ACCESS_MANAGEMENT_RELATION_CLOSE':
      return 'Управленческая связь закрыта';
    case 'ACCESS_TEMPORARY_ROLE_ASSIGN':
      return 'Назначена временная роль';
    case 'ACCESS_TEMPORARY_ROLE_CLOSE':
      return 'Временная роль закрыта';
    case 'ACCESS_TEMPORARY_ACCESS_ASSIGN':
      return 'Назначен временный доступ';
    case 'ACCESS_TEMPORARY_ACCESS_CLOSE':
      return 'Временный доступ закрыт';
    case 'ACCESS_TEMPORARY_MANAGEMENT_ASSIGN':
      return 'Назначено временное управление';
    case 'ACCESS_TEMPORARY_MANAGEMENT_CLOSE':
      return 'Временное управление закрыто';
    case 'CONTENT_COURSE_DRAFT_CREATED':
      return 'Создан черновик курса';
    case 'CONTENT_COURSE_DRAFT_UPDATED':
      return 'Обновлён черновик курса';
    case 'CONTENT_COURSE_PUBLISHED':
      return 'Курс опубликован';
    case 'CONTENT_COURSE_ARCHIVED':
      return 'Курс архивирован';
    case 'CONTENT_TOPIC_DRAFT_CREATED':
      return 'Создан черновик темы';
    case 'CONTENT_TOPIC_DRAFT_UPDATED':
      return 'Обновлён черновик темы';
    case 'CONTENT_TOPIC_PUBLISHED':
      return 'Тема опубликована';
    case 'CONTENT_TOPIC_ARCHIVED':
      return 'Тема архивирована';
    case 'CONTENT_MATERIAL_DRAFT_CREATED':
      return 'Создан черновик материала';
    case 'CONTENT_MATERIAL_DRAFT_UPDATED':
      return 'Обновлён черновик материала';
    case 'CONTENT_MATERIAL_PUBLISHED':
      return 'Материал опубликован';
    case 'CONTENT_MATERIAL_ARCHIVED':
      return 'Материал архивирован';
    case 'CONTENT_QUESTION_DRAFT_CREATED':
      return 'Создан черновик вопроса';
    case 'CONTENT_QUESTION_DRAFT_UPDATED':
      return 'Обновлён черновик вопроса';
    case 'CONTENT_QUESTION_DRAFT_COMPOSITION_UPDATED':
      return 'Обновлён состав вопроса';
    case 'CONTENT_QUESTION_PUBLISHED':
      return 'Вопрос опубликован';
    case 'CONTENT_QUESTION_ARCHIVED':
      return 'Вопрос архивирован';
    case 'CONTENT_TEST_DRAFT_CREATED':
      return 'Создан черновик теста';
    case 'CONTENT_TEST_DRAFT_UPDATED':
      return 'Обновлён черновик теста';
    case 'CONTENT_TEST_DRAFT_COMPOSITION_UPDATED':
      return 'Обновлён состав теста';
    case 'CONTENT_TEST_PUBLISHED':
      return 'Тест опубликован';
    case 'CONTENT_TEST_ARCHIVED':
      return 'Тест архивирован';
    case 'CONTENT_TOPIC_ACTIVE_FINAL_ASSIGNED':
      return 'Назначен итоговый тест темы';
    case 'CONTENT_TOPIC_ACTIVE_FINAL_REPLACED':
      return 'Итоговый тест темы заменён';
    case 'CONTENT_TOPIC_ACTIVE_FINAL_CLEARED':
      return 'Итоговый тест темы снят';
    case 'ASSIGNMENT_CAMPAIGN_LAUNCH':
    case 'ASSIGNMENT_CAMPAIGN_LAUNCHED':
      return 'Запущена кампания назначений';
    case 'ASSIGNMENT_CANCEL':
      return 'Назначение отменено';
    case 'ASSIGNMENT_DEADLINE_EXTEND':
      return 'Продлён срок назначения';
    case 'ASSIGNMENT_REPLACE_WITH_NEW':
      return 'Назначение заменено новым';
    case 'IMPORT_JOB_LAUNCH':
      return 'Запущен импорт';
    case 'IMPORT_ITEM_REVIEW_APPLY':
      return 'Подтверждён элемент импорта';
    case 'IMPORT_ITEM_REVIEW_REJECT':
      return 'Отклонён элемент импорта';
    case 'NOTIFICATION_RULE_CREATE':
      return 'Создано правило уведомлений';
    case 'NOTIFICATION_RULE_UPDATE':
      return 'Обновлено правило уведомлений';
    case 'NOTIFICATION_RULE_ENABLE':
      return 'Правило уведомлений включено';
    case 'NOTIFICATION_RULE_DISABLE':
      return 'Правило уведомлений отключено';
    default:
      return eventType?.trim() || 'Событие аудита';
  }
}

function localizeAuditEntityType(entityType?: string | null): string {
  switch (entityType?.trim().toLowerCase()) {
    case 'app_user':
      return 'Пользователь';
    case 'user_role_assignment':
      return 'Назначение роли';
    case 'user_organization_assignment':
      return 'Привязка к подразделению';
    case 'organizational_unit':
      return 'Подразделение';
    case 'organizational_unit_type':
      return 'Тип подразделения';
    case 'assignment':
      return 'Назначение';
    case 'assignment_campaign':
      return 'Кампания назначений';
    case 'course':
      return 'Курс';
    case 'topic':
      return 'Тема';
    case 'material':
      return 'Материал';
    case 'question':
      return 'Вопрос';
    case 'test':
      return 'Тест';
    case 'import_job':
      return 'Импорт';
    case 'import_job_item':
      return 'Элемент импорта';
    case 'notification_rule':
      return 'Правило уведомлений';
    default:
      return entityType?.trim() || 'Объект';
  }
}
