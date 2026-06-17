import { Alert, Button, DatePicker, Form, Space, Statistic, Typography } from 'antd';
import dayjs from 'dayjs';
import { canAccessAdministrationArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAnalyticsRebuildMutation } from '../../features/expert-analytics/model/useExpertAnalytics';
import type { AnalyticsRebuildRequest } from '../../features/expert-analytics/model/expertAnalytics';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { JsonBlock } from '../../shared/ui/JsonBlock';
import { PageIntro } from '../../shared/ui/PageIntro';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate } from '../../shared/ui/presentation';

export function AdminAnalyticsRebuildPage() {
  const { data: actor } = useCurrentActor();
  const [form] = Form.useForm<AnalyticsRebuildRequest>();
  const rebuildMutation = useAnalyticsRebuildMutation();
  const periodStart = form.getFieldValue('periodStart') as string | undefined;
  const periodEnd = form.getFieldValue('periodEnd') as string | undefined;
  const rangeValue = resolveRangeValue(periodStart, periodEnd);
  const rangeIsValid = Boolean(periodStart && periodEnd && dayjs(periodEnd).isAfter(dayjs(periodStart)));

  if (!actor || !canAccessAdministrationArea(actor)) {
    return <ForbiddenView />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageIntro
        title="Analytics rebuild"
        description="Служебный экран для ручного пересчёта result-based аналитики за выбранный период. После успешного запуска manager и expert analytics автоматически запросят обновлённые агрегаты."
      />

      {rebuildMutation.isError ? (
        <Alert
          type="error"
          showIcon
          message="Rebuild завершился ошибкой"
          description={String(rebuildMutation.error)}
        />
      ) : null}

      {rebuildMutation.data ? (
        <Alert
          type="success"
          showIcon
          message="Rebuild завершён"
          description={`Период пересчёта: ${formatUiDate(rebuildMutation.data.periodStart ?? undefined)} - ${formatUiDate(rebuildMutation.data.periodEnd ?? undefined)}`}
        />
      ) : null}

      <SectionCard
        title="Запуск rebuild"
        description="Пересборка обновляет user-topic, department-topic и question aggregates внутри выбранного окна."
      >
        <Form<AnalyticsRebuildRequest>
          layout="vertical"
          form={form}
          initialValues={{
            periodStart: dayjs().subtract(30, 'day').startOf('day').toISOString(),
            periodEnd: dayjs().endOf('day').toISOString(),
          }}
          onFinish={(values) => rebuildMutation.mutate(values)}
        >
          <Form.Item
            label="Период rebuild"
            required
            validateStatus={resolveRangeValidationStatus(periodStart, periodEnd)}
            help={resolveRangeValidationHelp(periodStart, periodEnd)}
          >
            <DatePicker.RangePicker
              showTime
              style={{ width: '100%' }}
              value={rangeValue}
              onChange={(value) => {
                form.setFieldsValue({
                  periodStart: value?.[0] ? value[0].toISOString() : undefined,
                  periodEnd: value?.[1] ? value[1].toISOString() : undefined,
                });
              }}
            />
          </Form.Item>

          <Form.Item name="periodStart" hidden rules={[{ required: true }]}>
            <input />
          </Form.Item>
          <Form.Item name="periodEnd" hidden rules={[{ required: true }]}>
            <input />
          </Form.Item>

          <Button type="primary" htmlType="submit" loading={rebuildMutation.isPending} disabled={!rangeIsValid}>
            Запустить rebuild
          </Button>
        </Form>
      </SectionCard>

      {rebuildMutation.data ? (
        <SectionCard
          title="Сводка пересчёта"
          description="Ключевые счётчики последнего завершённого rebuild по выбранному периоду."
        >
          <div className="admin-form-grid">
            <Statistic title="Source rows" value={rebuildMutation.data.sourceRowCount ?? 0} />
            <Statistic title="Supported topic rows" value={rebuildMutation.data.supportedTopicRowCount ?? 0} />
            <Statistic title="Unsupported topic rows" value={rebuildMutation.data.unsupportedTopicRowCount ?? 0} />
            <Statistic title="User-topic rows" value={rebuildMutation.data.userTopicAggregateRowCount ?? 0} />
            <Statistic title="Department-topic rows" value={rebuildMutation.data.departmentTopicAggregateRowCount ?? 0} />
            <Statistic title="Question rows" value={rebuildMutation.data.questionAggregateRowCount ?? 0} />
          </div>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            После завершения rebuild аналитические страницы менеджера и эксперта уже работают с обновлёнными данными.
          </Typography.Paragraph>
        </SectionCard>
      ) : null}

      {rebuildMutation.data ? <JsonBlock title="Rebuild result" value={rebuildMutation.data} /> : null}
    </Space>
  );
}

function resolveRangeValue(periodStart?: string, periodEnd?: string) {
  if (!periodStart || !periodEnd) {
    return null;
  }

  const start = dayjs(periodStart);
  const end = dayjs(periodEnd);
  if (!start.isValid() || !end.isValid()) {
    return null;
  }

  return [start, end] as [dayjs.Dayjs, dayjs.Dayjs];
}

function resolveRangeValidationStatus(periodStart?: string, periodEnd?: string) {
  if (!periodStart || !periodEnd) {
    return undefined;
  }

  return dayjs(periodEnd).isAfter(dayjs(periodStart)) ? undefined : 'error';
}

function resolveRangeValidationHelp(periodStart?: string, periodEnd?: string) {
  if (!periodStart || !periodEnd) {
    return 'Укажите начало и конец периода.';
  }

  return dayjs(periodEnd).isAfter(dayjs(periodStart))
    ? 'Rebuild будет выполнен строго внутри выбранного окна.'
    : 'Конец периода должен быть позже начала.';
}
