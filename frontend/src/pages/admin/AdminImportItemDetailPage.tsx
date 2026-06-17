import { Descriptions } from 'antd';
import { useParams } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useImportJobItem } from '../../features/imports/model/useImports';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatText, formatUiDate, localizeImportItemStatus } from '../../shared/ui/presentation';

export function AdminImportItemDetailPage() {
  const { itemId: itemIdParam } = useParams();
  const itemId = Number(itemIdParam);
  const { data: actor } = useCurrentActor();
  const itemQuery = useImportJobItem(itemId, Boolean(actor));

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }
  if (itemQuery.isLoading) {
    return <LoadingView title="Загрузка элемента импорта" />;
  }
  if (itemQuery.isError) {
    return <ErrorView title="Не удалось загрузить элемент импорта" error={itemQuery.error} />;
  }

  const item = itemQuery.data;
  if (!item) {
    return <ErrorView title="Элемент импорта не найден" />;
  }

  return (
    <>
      <PageIntro
        title={`Элемент импорта #${item.id}`}
        description="Детальная карточка строки импорта и её текущего состояния."
      />
      <SectionCard title="Карточка элемента импорта">
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Задача импорта">{item.importJobId}</Descriptions.Item>
          <Descriptions.Item label="Статус">{localizeImportItemStatus(item.status ?? undefined)}</Descriptions.Item>
          <Descriptions.Item label="Целевая сущность">{formatText(item.targetEntityType)}</Descriptions.Item>
          <Descriptions.Item label="ID найденной сущности">{formatText(item.matchedEntityId)}</Descriptions.Item>
          <Descriptions.Item label="Внешний ID">{formatText(item.externalId)}</Descriptions.Item>
          <Descriptions.Item label="Табельный номер">{formatText(item.employeeNumber)}</Descriptions.Item>
          <Descriptions.Item label="Код ошибки">{formatText(item.errorCode)}</Descriptions.Item>
          <Descriptions.Item label="Текст ошибки">{formatText(item.errorMessage)}</Descriptions.Item>
          <Descriptions.Item label="Обработан">{formatUiDate(item.processedAt ?? undefined, 'Не обработан')}</Descriptions.Item>
          <Descriptions.Item label="Обновлён">{formatUiDate(item.updatedAt ?? undefined)}</Descriptions.Item>
        </Descriptions>
      </SectionCard>
    </>
  );
}
