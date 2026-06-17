import { Space, Typography } from 'antd';
import type { ReactNode } from 'react';

type PageIntroProps = {
  title: string;
  description?: string;
  extra?: ReactNode;
};

export function PageIntro({ title, description, extra }: PageIntroProps) {
  return (
    <div className="page-header">
      <Space direction="vertical" size={6} style={{ maxWidth: 860 }}>
        <Typography.Title level={2} style={{ margin: 0 }}>
          {title}
        </Typography.Title>
        {description ? (
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            {description}
          </Typography.Paragraph>
        ) : null}
      </Space>
      {extra ? <div className="page-header-actions">{extra}</div> : null}
    </div>
  );
}
