import { Button, Card, Collapse, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import { localizeMaterialType } from '../../../shared/ui/presentation';
import type { SelfVisibleTopic } from '../model/selfTesting';

type SelfTopicContentViewProps = {
  topic: SelfVisibleTopic;
};

function splitMaterialBody(body?: string): string[] {
  if (!body) {
    return [];
  }

  return body
    .split(/\r?\n\r?\n/)
    .map((chunk) => chunk.trim())
    .filter(Boolean);
}

function renderBodyBlock(block: string, index: number) {
  const lines = block.split(/\r?\n/).map((line) => line.trimEnd());
  const nonEmptyLines = lines.filter((line) => line.trim().length > 0);
  const isBulletBlock =
    nonEmptyLines.length > 0 && nonEmptyLines.every((line) => line.trim().startsWith('- '));

  if (isBulletBlock) {
    return (
      <ul key={index} className="material-viewer-list">
        {nonEmptyLines.map((line) => (
          <li key={`${index}-${line}`}>{line.replace(/^- /, '').trim()}</li>
        ))}
      </ul>
    );
  }

  return (
    <Typography.Paragraph key={index} className="material-viewer-paragraph">
      {lines.join('\n')}
    </Typography.Paragraph>
  );
}

function isDirectVideoUrl(url: string) {
  return /\.(mp4|webm|ogg)(\?.*)?$/i.test(url);
}

function toEmbedUrl(url: string) {
  const youtubeWatchMatch = url.match(/^https?:\/\/(?:www\.)?youtube\.com\/watch\?v=([^&]+)/i);
  if (youtubeWatchMatch) {
    return `https://www.youtube.com/embed/${youtubeWatchMatch[1]}`;
  }

  const youtubeShortMatch = url.match(/^https?:\/\/youtu\.be\/([^?&]+)/i);
  if (youtubeShortMatch) {
    return `https://www.youtube.com/embed/${youtubeShortMatch[1]}`;
  }

  return url;
}

export function SelfTopicContentView({ topic }: SelfTopicContentViewProps) {
  return (
    <div className="home-page home-page-compact material-viewer-page">
      <section className="self-hero material-viewer-hero">
        <div className="self-hero-copy">
          <Link to="/learner/self-testing">
            <Button type="link" style={{ paddingInline: 0 }}>
              Назад к каталогу
            </Button>
          </Link>
          <Typography.Title level={2} className="self-hero-title">
            {topic.topicName ?? 'Тема самостоятельного обучения'}
          </Typography.Title>
          <Typography.Paragraph className="self-hero-text">
            {topic.topicDescription ?? 'Изучите материалы темы, а затем переходите к тесту в удобный для вас момент.'}
          </Typography.Paragraph>
        </div>
        <Card className="self-hero-panel material-viewer-panel">
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <div>
              <Typography.Text type="secondary">Курс</Typography.Text>
              <Typography.Paragraph className="self-panel-text">
                {topic.courseName ?? 'Курс не указан'}
              </Typography.Paragraph>
            </div>
            <div>
              <Typography.Text type="secondary">Материалы</Typography.Text>
              <Typography.Paragraph className="self-panel-text">
                {topic.materials.length > 0 ? `${topic.materials.length} доступно для изучения` : 'Пока без материалов'}
              </Typography.Paragraph>
            </div>
            <Tag color="gold">Самостоятельное обучение</Tag>
          </Space>
        </Card>
      </section>

      <Card className="soft-card" title={`Материалы темы (${topic.materials.length})`}>
        {topic.materials.length > 0 ? (
          <Collapse
            items={topic.materials.map((material, index) => ({
              key: String(material.materialId ?? index),
              label: material.name ?? `Материал ${index + 1}`,
              children: (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Space wrap>
                    <Tag color="blue">{localizeMaterialType(material.materialType)}</Tag>
                  </Space>
                  {material.description ? (
                    <Typography.Paragraph style={{ marginBottom: 0 }}>
                      {material.description}
                    </Typography.Paragraph>
                  ) : null}
                  {material.materialType === 'VIDEO' && material.videoUrl ? (
                    isDirectVideoUrl(material.videoUrl) ? (
                      <video controls style={{ width: '100%', borderRadius: 16 }} src={material.videoUrl} />
                    ) : (
                      <iframe
                        title={material.name ?? `Видео ${index + 1}`}
                        src={toEmbedUrl(material.videoUrl)}
                        style={{ width: '100%', aspectRatio: '16 / 9', border: 0, borderRadius: 16 }}
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowFullScreen
                      />
                    )
                  ) : null}
                  {splitMaterialBody(material.body).length > 0 ? (
                    <div className="material-viewer-body">
                      {splitMaterialBody(material.body).map(renderBodyBlock)}
                    </div>
                  ) : (
                    <Typography.Text type="secondary">
                      Полное содержимое этого материала пока не заполнено.
                    </Typography.Text>
                  )}
                </Space>
              ),
            }))}
          />
        ) : (
          <Typography.Text type="secondary">
            Для этой темы пока не опубликованы материалы.
          </Typography.Text>
        )}
      </Card>
    </div>
  );
}
