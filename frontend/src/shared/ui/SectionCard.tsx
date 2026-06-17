import { Card, Space, Typography } from 'antd';
import type { ReactNode } from 'react';

type SectionCardProps = {
  title: string;
  description?: string;
  extra?: ReactNode;
  children: ReactNode;
};

export function SectionCard({ title, description, extra, children }: SectionCardProps) {
  return (
    <Card className="soft-card section-card" extra={extra}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div>
          <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 8 }}>
            {title}
          </Typography.Title>
          {description ? (
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
              {description}
            </Typography.Paragraph>
          ) : null}
        </div>
        {children}
      </Space>
    </Card>
  );
}
