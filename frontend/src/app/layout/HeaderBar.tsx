import { Avatar, Flex, Space, Typography } from 'antd';
import dayjs from 'dayjs';
import type { CurrentActor } from '../../features/auth/model/currentActor';
import { humanizeOrganizationalUnit } from '../../shared/ui/organizationalUnits';

type HeaderBarProps = {
  actor: CurrentActor;
};

export function HeaderBar({ actor }: HeaderBarProps) {
  const title = actor.displayName ?? 'Пользователь';
  const installation = humanizeOrganizationalUnit(
    actor.primaryOrganizationalUnitName,
    actor.primaryOrganizationalUnitPath,
    'Установка не указана',
  );
  const subtitle = `Табельный номер: ${actor.employeeNumber ?? 'не указан'} • ${installation}`;

  return (
    <Flex align="center" justify="space-between" style={{ width: '100%' }} gap={16}>
      <Typography.Text type="secondary">{dayjs().format('DD.MM.YYYY HH:mm')}</Typography.Text>
      <Space size="middle" wrap>
        <Space size="small">
          <Avatar>{title.charAt(0).toUpperCase()}</Avatar>
          <div>
            <Typography.Text strong>{title}</Typography.Text>
            <br />
            <Typography.Text type="secondary">{subtitle}</Typography.Text>
          </div>
        </Space>
      </Space>
    </Flex>
  );
}
