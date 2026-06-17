import { Card, Typography } from 'antd';
import { formatJson } from './presentation';

type JsonBlockProps = {
  title?: string;
  value: unknown;
};

export function JsonBlock({ title = 'JSON payload', value }: JsonBlockProps) {
  return (
    <Card className="soft-card">
      <Typography.Title level={5}>{title}</Typography.Title>
      <pre className="json-block">{formatJson(value)}</pre>
    </Card>
  );
}
