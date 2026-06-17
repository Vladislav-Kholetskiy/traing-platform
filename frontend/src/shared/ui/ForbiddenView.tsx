import { Result } from 'antd';

type ForbiddenViewProps = {
  title?: string;
  description?: string;
};

export function ForbiddenView({
  title = 'Раздел сейчас недоступен',
  description = 'У текущего пользователя пока нет доступа к этому разделу.',
}: ForbiddenViewProps) {
  return <Result status="403" title={title} subTitle={description} />;
}
