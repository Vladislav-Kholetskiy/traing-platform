import { Alert, Button, Space, Table, Tag, Typography } from 'antd';
import { useRef, useState } from 'react';
import { Link } from 'react-router';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type {
  ImportJob,
  PersonnelApplyResponse,
  PersonnelApplyRow,
  PersonnelDryRunResponse,
  PersonnelDryRunRow,
} from '../../features/imports/model/imports';
import {
  useApplyPersonnelExcelMutation,
  useDryRunPersonnelExcelMutation,
  useImportJobs,
} from '../../features/imports/model/useImports';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeImportJobStatus } from '../../shared/ui/presentation';

export function AdminImportJobsPage() {
  const { data: actor } = useCurrentActor();
  const jobsQuery = useImportJobs(undefined, Boolean(actor));
  const dryRunMutation = useDryRunPersonnelExcelMutation();
  const applyMutation = useApplyPersonnelExcelMutation();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const jobs = jobsQuery.data ?? [];
  const commandError = dryRunMutation.error ?? applyMutation.error;

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }
  if (jobsQuery.isLoading) {
    return <LoadingView title="Загрузка импорта" />;
  }
  if (jobsQuery.isError) {
    return <ErrorView title="Не удалось открыть экран импорта" error={jobsQuery.error} />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title="Импорт сотрудников"
        description="Загрузите Excel-файл, сначала проверьте его, затем выполните импорт."
      />

      {commandError ? (
        <Alert
          type="error"
          showIcon
          message="Импорт завершился ошибкой"
          description={extractErrorMessage(commandError)}
        />
      ) : null}

      <SectionCard
        title="Загрузка файла"
        description="Проверка файла не вносит изменения в систему. Импорт выполняет фактическое обновление данных."
      >
        <div className="import-simple-upload">
          <input
            ref={fileInputRef}
            hidden
            type="file"
            accept=".xlsx,.xls"
            onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
          />

          <button
            type="button"
            className="import-simple-dropzone"
            onClick={() => fileInputRef.current?.click()}
          >
            <span className="import-simple-dropzone-title">
              {selectedFile ? selectedFile.name : 'Выберите Excel-файл'}
            </span>
            <span className="import-simple-dropzone-note">
              {selectedFile
                ? `Размер файла: ${formatFileSize(selectedFile.size)}`
                : 'Поддерживаются форматы .xlsx и .xls'}
            </span>
          </button>

          <div className="import-simple-actions">
            <Button
              size="large"
              disabled={!selectedFile}
              loading={dryRunMutation.isPending}
              onClick={() => selectedFile && dryRunMutation.mutate(selectedFile)}
            >
              Проверить файл
            </Button>
            <Button
              type="primary"
              size="large"
              disabled={!selectedFile}
              loading={applyMutation.isPending}
              onClick={() => selectedFile && applyMutation.mutate(selectedFile)}
            >
              Импортировать
            </Button>
          </div>

          <div className="import-simple-result-row">
            <ResultInlineCard
              title="Проверка"
              rows={dryRunMutation.data?.rows}
              emptyText="После проверки здесь появится краткая сводка."
              successCount={countDryRunApproved(dryRunMutation.data)}
              warningCount={countDryRunWarnings(dryRunMutation.data)}
            />
            <ResultInlineCard
              title="Импорт"
              rows={applyMutation.data?.rows}
              emptyText="После импорта здесь появится результат обработки файла."
              successCount={countApplySuccess(applyMutation.data)}
              warningCount={countApplyWarnings(applyMutation.data)}
            />
          </div>

          {dryRunMutation.data?.rows?.length ? (
            <div className="import-simple-details">
              <Typography.Title level={5} style={{ margin: 0 }}>
                Что покажет проверка файла
              </Typography.Title>
              <Table<PersonnelDryRunRow>
                rowKey={(row) => `${row.rowNumber ?? 'row'}-${row.employeeNumber ?? 'employee'}-dry`}
                size="small"
                pagination={{ pageSize: 8 }}
                dataSource={dryRunMutation.data.rows}
                columns={[
                  {
                    title: 'Строка',
                    dataIndex: 'rowNumber',
                    width: 90,
                  },
                  {
                    title: 'Табельный номер',
                    dataIndex: 'employeeNumber',
                    render: (value?: string | null) => value || 'Не указан',
                  },
                  {
                    title: 'Результат проверки',
                    render: (_, row) => (
                      <Space direction="vertical" size={0}>
                        <Typography.Text>{localizeImportOutcome(row.outcomeCode, row.decision)}</Typography.Text>
                        {row.plannedMutations?.length ? (
                          <Typography.Text type="secondary">
                            {`Планируется: ${row.plannedMutations.map((item) => localizeMutationType(item.mutationType)).join(', ')}`}
                          </Typography.Text>
                        ) : null}
                      </Space>
                    ),
                  },
                  {
                    title: 'Замечания',
                    render: (_, row) =>
                      row.issues?.length ? (
                        <Typography.Text type="danger">{row.issues.join('; ')}</Typography.Text>
                      ) : (
                        <Typography.Text type="secondary">Нет</Typography.Text>
                      ),
                  },
                ]}
              />
            </div>
          ) : null}

          {applyMutation.data?.rows?.length ? (
            <div className="import-simple-details">
              <Typography.Title level={5} style={{ margin: 0 }}>
                Что произошло после импорта
              </Typography.Title>
              <Table<PersonnelApplyRow>
                rowKey={(row) => `${row.rowNumber ?? 'row'}-${row.employeeNumber ?? 'employee'}-apply`}
                size="small"
                pagination={{ pageSize: 8 }}
                dataSource={applyMutation.data.rows}
                columns={[
                  {
                    title: 'Строка',
                    dataIndex: 'rowNumber',
                    width: 90,
                  },
                  {
                    title: 'Табельный номер',
                    dataIndex: 'employeeNumber',
                    render: (value?: string | null) => value || 'Не указан',
                  },
                  {
                    title: 'Итог',
                    render: (_, row) => (
                      <Space direction="vertical" size={0}>
                        <Typography.Text>{localizeImportOutcome(row.outcomeCode, row.decision)}</Typography.Text>
                        {row.appliedMutationTypes?.length ? (
                          <Typography.Text type="secondary">
                            {`Применено: ${row.appliedMutationTypes.map((item) => localizeMutationType(item)).join(', ')}`}
                          </Typography.Text>
                        ) : null}
                        {row.createdUserId ? (
                          <Typography.Text type="secondary">{`Создан пользователь ID ${row.createdUserId}`}</Typography.Text>
                        ) : null}
                      </Space>
                    ),
                  },
                  {
                    title: 'Замечания',
                    render: (_, row) =>
                      row.issues?.length ? (
                        <Typography.Text type="danger">{row.issues.join('; ')}</Typography.Text>
                      ) : (
                        <Typography.Text type="secondary">Нет</Typography.Text>
                      ),
                  },
                ]}
              />
            </div>
          ) : null}
        </div>
      </SectionCard>

      <SectionCard
        title="Последние запуски"
        description="Нижний список показывает ранее созданные задачи импорта."
      >
        <Table<ImportJob>
          rowKey="id"
          dataSource={jobs}
          pagination={{ pageSize: 8 }}
          columns={[
            {
              title: 'ID',
              dataIndex: 'id',
              width: 88,
              render: (value) => <Typography.Text code>{value}</Typography.Text>,
            },
            {
              title: 'Запуск',
              render: (_, job) => (
                <Space direction="vertical" size={0}>
                  <Link to={`/admin/import/jobs/${job.id}`}>{localizeSourceType(job.sourceType)}</Link>
                  <Typography.Text type="secondary">{job.sourceRef || 'Источник не указан'}</Typography.Text>
                </Space>
              ),
            },
            {
              title: 'Статус',
              dataIndex: 'status',
              render: (value) => localizeImportJobStatus(value ?? undefined),
            },
            {
              title: 'Прогресс',
              render: (_, job) => `${job.processedItemCount ?? 0}/${job.totalItemCount ?? 0}`,
            },
            {
              title: 'Создан',
              dataIndex: 'createdAt',
              render: (value) => formatUiDate(value ?? undefined),
            },
          ]}
        />
      </SectionCard>
    </Space>
  );
}

function ResultInlineCard({
  title,
  rows,
  emptyText,
  successCount,
  warningCount,
}: {
  title: string;
  rows?: unknown[] | null;
  emptyText: string;
  successCount: number;
  warningCount: number;
}) {
  const total = rows?.length ?? 0;

  return (
    <div className="import-simple-result-card">
      <div className="import-simple-result-head">
        <Typography.Text strong>{title}</Typography.Text>
        <Tag bordered={false}>{total > 0 ? `${total} строк` : 'Нет данных'}</Tag>
      </div>
      {total > 0 ? (
        <Typography.Paragraph className="muted-note" style={{ marginBottom: 0 }}>
          {`Успешно: ${successCount}. С замечаниями или ошибками: ${warningCount}.`}
        </Typography.Paragraph>
      ) : (
        <Typography.Paragraph className="muted-note" style={{ marginBottom: 0 }}>
          {emptyText}
        </Typography.Paragraph>
      )}
    </div>
  );
}

function countDryRunApproved(response?: PersonnelDryRunResponse | null): number {
  return (
    response?.rows?.filter((row) => {
      const issuesCount = row.issues?.length ?? 0;
      return issuesCount === 0;
    }).length ?? 0
  );
}

function countDryRunWarnings(response?: PersonnelDryRunResponse | null): number {
  return (
    response?.rows?.filter((row) => {
      const issuesCount = row.issues?.length ?? 0;
      return issuesCount > 0;
    }).length ?? 0
  );
}

function countApplySuccess(response?: PersonnelApplyResponse | null): number {
  return (
    response?.rows?.filter((row) => {
      const issuesCount = row.issues?.length ?? 0;
      return issuesCount === 0;
    }).length ?? 0
  );
}

function countApplyWarnings(response?: PersonnelApplyResponse | null): number {
  return (
    response?.rows?.filter((row) => {
      const issuesCount = row.issues?.length ?? 0;
      return issuesCount > 0;
    }).length ?? 0
  );
}

function localizeImportOutcome(outcomeCode?: string | null, decision?: string | null): string {
  if (decision?.trim()) {
    return decision;
  }

  switch (outcomeCode?.trim().toUpperCase()) {
    case 'NO_CHANGE':
      return 'Без изменений';
    case 'UPDATED':
      return 'Обновлено';
    case 'CREATED':
      return 'Создано';
    case 'SKIPPED':
      return 'Пропущено';
    case 'FAILED':
      return 'Ошибка';
    default:
      return outcomeCode?.trim() || 'Результат не указан';
  }
}

function localizeMutationType(mutationType?: string | null): string {
  switch (mutationType?.trim().toUpperCase()) {
    case 'CREATE_USER':
      return 'Создание пользователя';
    case 'CLOSE_PRIMARY_ORG_ASSIGNMENT':
      return 'Закрытие основной оргпривязки';
    case 'OPEN_PRIMARY_ORG_ASSIGNMENT':
      return 'Открытие основной оргпривязки';
    case 'CLOSE_ROLE_ASSIGNMENT':
      return 'Закрытие роли';
    case 'OPEN_ROLE_ASSIGNMENT':
      return 'Назначение роли';
    case 'CLOSE_USER_ACCESS_AREA':
      return 'Закрытие зоны доступа';
    case 'OPEN_USER_ACCESS_AREA':
      return 'Открытие зоны доступа';
    case 'OPEN_TEMPORARY_ROLE_ASSIGNMENT':
      return 'Назначение временной роли';
    case 'CLOSE_TEMPORARY_ROLE_ASSIGNMENT':
      return 'Закрытие временной роли';
    case 'OPEN_TEMPORARY_ACCESS_AREA':
      return 'Назначение временного доступа';
    case 'CLOSE_TEMPORARY_ACCESS_AREA':
      return 'Закрытие временного доступа';
    case 'DEACTIVATE_USER_TO_INACTIVE':
      return 'Деактивация пользователя';
    default:
      return mutationType?.trim() || 'Изменение';
  }
}

function formatFileSize(size: number): string {
  if (size < 1024) {
    return `${size} Б`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} КБ`;
  }
  return `${(size / (1024 * 1024)).toFixed(1)} МБ`;
}

function localizeSourceType(sourceType?: string | null): string {
  switch (sourceType?.trim().toUpperCase()) {
    case 'PERSONNEL':
      return 'Кадровый импорт';
    default:
      return sourceType?.trim() || 'Импорт';
  }
}

function extractErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return String(error);
}
