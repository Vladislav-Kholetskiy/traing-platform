import { Empty, Typography } from 'antd';

type EmptyStateProps = {
  title: string;
  description: string;
};

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <Empty
      description={
        <>
          <Typography.Text strong>{title}</Typography.Text>
          <br />
          <Typography.Text type="secondary">{description}</Typography.Text>
        </>
      }
    />
  );
}
