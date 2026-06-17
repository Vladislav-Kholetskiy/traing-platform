import { Button, Result, Typography } from 'antd';
import { getErrorMessage } from '../api/apiError';

type ErrorViewProps = {
  title?: string;
  description?: string;
  error?: unknown;
};

export function ErrorView({ title = 'Что-то пошло не так', description, error }: ErrorViewProps) {
  return (
    <Result
      status="error"
      title={title}
      subTitle={description ?? getErrorMessage(error)}
      extra={
        error ? (
          <Typography.Text type="secondary">{getErrorMessage(error)}</Typography.Text>
        ) : (
          <Button onClick={() => window.location.reload()}>Обновить страницу</Button>
        )
      }
    />
  );
}
