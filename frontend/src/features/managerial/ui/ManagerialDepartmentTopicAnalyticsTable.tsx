import { Card, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { formatAssignmentDate } from '../../assigned-learning/model/assignedLearning';
import { formatPercent, formatText } from '../../../shared/ui/presentation';
import type { ManagerialDepartmentTopicAnalyticsItem } from '../model/managerialAnalytics';

const departmentDisplayNames: Record<string, string> = {
  dept: 'Департамент',
  npz: 'Нефтеперерабатывающий завод',
  hq: 'Заводоуправление',
  production: 'Производство',
  cck: 'Комплекс каталитического крекинга',
  ukk: 'Установка каталитического крекинга',
  upv: 'Установка производства водорода',
  komt: 'Комплекс облагораживания моторных топлив',
  mtbeo: 'Установка по производству МТБЭ и олигомеризата',
  gobkk: 'Установка гидрооблагораживания бензина каталитического крекинга',
  tame: 'Установка по производству ТАМЭ',
  edu: 'Отдел по работе с учебными материалами',
  hse: 'Отдел охраны труда и промышленной безопасности',
};

type ManagerialDepartmentTopicAnalyticsTableProps = {
  items: ManagerialDepartmentTopicAnalyticsItem[];
};

export function ManagerialDepartmentTopicAnalyticsTable({
  items,
}: ManagerialDepartmentTopicAnalyticsTableProps) {
  const columns: ColumnsType<ManagerialDepartmentTopicAnalyticsItem> = [
    {
      title: 'Подразделение',
      key: 'department',
      render: (_, item) => (
        <>
          <Typography.Text strong>{resolveDepartmentDisplayName(item)}</Typography.Text>
          <br />
          <Typography.Text type="secondary">{item.topicName ?? 'Тема не указана'}</Typography.Text>
        </>
      ),
    },
    {
      title: 'Средний результат',
      dataIndex: 'averageScorePercent',
      key: 'averageScorePercent',
      render: (value?: string) => formatPercent(value),
    },
    {
      title: 'Процент прохождения',
      dataIndex: 'passRatePercent',
      key: 'passRatePercent',
      render: (value?: string) => formatPercent(value),
    },
    {
      title: 'Попытки',
      dataIndex: 'attemptCount',
      key: 'attemptCount',
      render: (value?: number) => formatText(value),
    },
    {
      title: 'Ошибки',
      dataIndex: 'errorCount',
      key: 'errorCount',
      render: (value?: number) => formatText(value),
    },
    {
      title: 'Обновлено',
      dataIndex: 'refreshedAt',
      key: 'refreshedAt',
      render: (value?: string) => formatAssignmentDate(value),
    },
  ];

  return (
    <Card className="soft-card" styles={{ body: { padding: 0 } }}>
      <Table
        rowKey={(item) =>
          `${item.organizationalUnitIdSnapshot ?? 'unit'}-${item.topicId ?? 'topic'}-${item.periodStart ?? 'start'}`
        }
        columns={columns}
        dataSource={items}
        pagination={{ pageSize: 8, hideOnSinglePage: true }}
      />
    </Card>
  );
}

function resolveDepartmentDisplayName(item: ManagerialDepartmentTopicAnalyticsItem): string {
  const runtimeCode = (
    item.organizationalPathSnapshot?.split('/').filter(Boolean).at(-1)
    ?? item.organizationalUnitName
  )?.trim().toLowerCase();

  if (runtimeCode) {
    const displayName = departmentDisplayNames[runtimeCode];
    if (displayName) {
      return displayName;
    }
  }

  return item.organizationalUnitName ?? item.organizationalPathSnapshot ?? 'Структура не указана';
}
