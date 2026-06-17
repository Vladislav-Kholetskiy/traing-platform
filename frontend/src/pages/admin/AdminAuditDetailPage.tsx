import { Descriptions } from 'antd';
import { useParams } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAuditEvent } from '../../features/audit/model/useAudit';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatText, formatUiDate } from '../../shared/ui/presentation';

export function AdminAuditDetailPage() {
  const { auditEventId: auditEventIdParam } = useParams();
  const auditEventId = Number(auditEventIdParam);
  const { data: actor } = useCurrentActor();
  const auditQuery = useAuditEvent(auditEventId, Boolean(actor));

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }
  if (auditQuery.isLoading) {
    return <LoadingView title="Загрузка записи аудита" />;
  }
  if (auditQuery.isError) {
    return <ErrorView title="Не удалось загрузить запись аудита" error={auditQuery.error} />;
  }

  const event = auditQuery.data;
  if (!event) {
    return <ErrorView title="Запись аудита не найдена" />;
  }

  return (
    <>
      <PageIntro
        title={`Событие аудита #${event.id}`}
        description="Подробная карточка системного события без возможности редактирования."
      />
      <SectionCard title="Карточка события">
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Тип события">{localizeAuditEventType(event.eventType)}</Descriptions.Item>
          <Descriptions.Item label="Пользователь">{formatText(event.actorUserId)}</Descriptions.Item>
          <Descriptions.Item label="Тип объекта">{localizeAuditEntityType(event.entityType)}</Descriptions.Item>
          <Descriptions.Item label="ID объекта">{formatText(event.entityId)}</Descriptions.Item>
          <Descriptions.Item label="Время события">{formatUiDate(event.occurredAt ?? undefined)}</Descriptions.Item>
          <Descriptions.Item label="Создано">{formatUiDate(event.createdAt ?? undefined)}</Descriptions.Item>
          <Descriptions.Item label="Correlation ID">{formatText(event.correlationId)}</Descriptions.Item>
          <Descriptions.Item label="Request ID">{formatText(event.requestId)}</Descriptions.Item>
        </Descriptions>
      </SectionCard>
    </>
  );
}

function localizeAuditEventType(eventType?: string | null): string {
  switch (eventType) {
    case 'RESULT_RECORDED':
      return 'Зафиксирован результат';
    case 'TESTING_SELF_ATTEMPT_SUBMITTED':
      return 'Самостоятельный тест отправлен';
    case 'TESTING_SELF_ANSWER_MUTATED':
      return 'Изменён ответ в самостоятельном тесте';
    case 'TESTING_SELF_ATTEMPT_STARTED':
      return 'Начата попытка самостоятельного теста';
    default:
      return eventType?.trim() || 'Событие аудита';
  }
}

function localizeAuditEntityType(entityType?: string | null): string {
  switch (entityType?.trim().toLowerCase()) {
    case 'result':
      return 'Результат';
    case 'test_attempt':
      return 'Попытка теста';
    default:
      return entityType?.trim() || 'Объект';
  }
}
