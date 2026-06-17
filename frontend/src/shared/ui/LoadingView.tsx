import { Flex, Spin, Typography } from 'antd';

type LoadingViewProps = {
  title?: string;
  description?: string;
};

export function LoadingView({ title = 'Загрузка', description }: LoadingViewProps) {
  return (
    <Flex vertical align="center" justify="center" gap="middle" style={{ minHeight: '60vh' }}>
      <Spin size="large" />
      <div style={{ textAlign: 'center' }}>
        <Typography.Title level={4}>{title}</Typography.Title>
        {description ? <Typography.Text type="secondary">{description}</Typography.Text> : null}
      </div>
    </Flex>
  );
}
