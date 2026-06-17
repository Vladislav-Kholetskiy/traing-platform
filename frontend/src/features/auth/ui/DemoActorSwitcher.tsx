import { DownOutlined, UpOutlined } from '@ant-design/icons';
import { Button, Select, Tag, Typography } from 'antd';
import { useState } from 'react';
import type { CurrentActor } from '../model/currentActor';
import { demoActorPresets, getEffectiveDemoActorId, resetDemoActorId, setDemoActorId } from '../model/demoActor';

type DemoActorSwitcherProps = {
  actor: CurrentActor;
};

function reloadApp(): void {
  window.location.assign('/');
}

export function DemoActorSwitcher({ actor }: DemoActorSwitcherProps) {
  const selectedActorId = getEffectiveDemoActorId();
  const [expanded, setExpanded] = useState(false);

  const options = demoActorPresets.map((preset) => ({
    value: preset.id,
    label: `${preset.label} • ${preset.username}`,
  }));

  const handleChange = (value: string) => {
    setDemoActorId(value);
    reloadApp();
  };

  const handleReset = () => {
    resetDemoActorId();
    reloadApp();
  };

  return (
    <div className={expanded ? 'sidebar-actor-switcher is-expanded' : 'sidebar-actor-switcher'}>
      <button
        type="button"
        className="sidebar-actor-toggle"
        onClick={() => setExpanded((current) => !current)}
        aria-label={expanded ? 'Свернуть смену аккаунта' : 'Развернуть смену аккаунта'}
        aria-expanded={expanded}
      >
        {expanded ? <DownOutlined /> : <UpOutlined />}
      </button>

      {expanded ? (
        <div className="sidebar-actor-panel">
          <div className="sidebar-actor-switcher-head">
            <Typography.Text strong>Смена аккаунта</Typography.Text>
            <Tag bordered={false}>demo</Tag>
          </div>

          <Typography.Text type="secondary" className="sidebar-actor-switcher-note">
            Текущий: {actor.username ?? 'не задан'}
          </Typography.Text>

          <Select
            value={selectedActorId}
            onChange={handleChange}
            options={options}
            placeholder="Выберите demo-аккаунт"
            popupMatchSelectWidth={false}
            className="sidebar-actor-select"
          />

          <Button block onClick={handleReset} className="sidebar-actor-reset">
            Вернуться к текущему профилю
          </Button>
        </div>
      ) : null}
    </div>
  );
}
