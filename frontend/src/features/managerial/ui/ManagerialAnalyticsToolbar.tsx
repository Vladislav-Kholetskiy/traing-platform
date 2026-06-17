import { Button, Card, DatePicker, Space, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';

const { RangePicker } = DatePicker;

type ManagerialAnalyticsToolbarProps = {
  description: string;
  title: string;
  value: [Dayjs, Dayjs];
  onChange: (value: [Dayjs, Dayjs]) => void;
};

export function ManagerialAnalyticsToolbar({
  title,
  description,
  value,
  onChange,
}: ManagerialAnalyticsToolbarProps) {
  const currentYearRange: [Dayjs, Dayjs] = [dayjs().startOf('year'), dayjs().endOf('year')];
  const surroundingYearRange: [Dayjs, Dayjs] = [dayjs().subtract(1, 'year').startOf('year'), dayjs().endOf('year')];

  return (
    <Card className="soft-card hero-card">
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 8 }}>
            {title}
          </Typography.Title>
          <Typography.Paragraph className="self-test-description">{description}</Typography.Paragraph>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Если за выбранный период данных нет, попробуйте расширить диапазон дат.
          </Typography.Paragraph>
        </div>

        <Space wrap>
          <RangePicker
            showTime
            value={value}
            onChange={(range) => {
              if (range?.[0] && range?.[1]) {
                onChange([range[0], range[1]]);
              }
            }}
          />
          <Button onClick={() => onChange(currentYearRange)}>Текущий год</Button>
          <Button onClick={() => onChange(surroundingYearRange)}>Прошлый + текущий год</Button>
        </Space>
      </Space>
    </Card>
  );
}
