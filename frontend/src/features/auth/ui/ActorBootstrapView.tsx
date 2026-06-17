import { Button, Card, Space, Tag, Typography } from 'antd';
import { demoActorPresets, resetDemoActorId, setDemoActorId } from '../model/demoActor';

function reloadApp(): void {
  window.location.assign('/');
}

export function ActorBootstrapView() {
  const handleSelect = (actorId: string) => {
    setDemoActorId(actorId);
    reloadApp();
  };

  const handleRetry = () => {
    resetDemoActorId();
    reloadApp();
  };

  return (
    <div className="actor-bootstrap-shell">
      <Card className="actor-bootstrap-card">
        <Space direction="vertical" size={20} style={{ width: '100%' }}>
          <div>
            <Tag bordered={false} className="actor-bootstrap-tag">
              Локальный вход
            </Tag>
            <Typography.Title level={2} className="actor-bootstrap-title">
              Выберите профиль для входа
            </Typography.Title>
            <Typography.Paragraph className="actor-bootstrap-text">
              Система сейчас не видит интерактивную пользовательскую сессию. Для локальной разработки можно
              продолжить через один из доступных демонстрационных профилей или повторить вход.
            </Typography.Paragraph>
          </div>

          <div className="actor-bootstrap-grid">
            {demoActorPresets.map((preset) => (
              <button
                key={preset.id}
                type="button"
                className="actor-bootstrap-option"
                onClick={() => handleSelect(preset.id)}
              >
                <span className="actor-bootstrap-option-label">{preset.label}</span>
                <span className="actor-bootstrap-option-note">{preset.note}</span>
              </button>
            ))}
          </div>

          <div className="actor-bootstrap-actions">
            <Button onClick={handleRetry}>Повторить вход</Button>
          </div>
        </Space>
      </Card>
    </div>
  );
}
