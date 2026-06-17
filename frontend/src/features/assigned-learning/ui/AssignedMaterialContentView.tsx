import { Button, Card, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import {
  formatAssignmentDate,
  type AssignedMaterialContent,
} from '../model/assignedLearning';
import { localizeMaterialType } from '../../../shared/ui/presentation';

type AssignedMaterialContentViewProps = {
  assignmentId: string;
  content: AssignedMaterialContent;
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

function EmptyMaterialBody() {
  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        Полное содержимое ещё не добавлено
      </Typography.Title>
      <Typography.Paragraph style={{ marginBottom: 0 }}>
        Для этого материала уже сохранены тип, тема и описание, но текст или сценарий просмотра
        пока не заполнен в backend.
      </Typography.Paragraph>
    </Space>
  );
}

export function AssignedMaterialContentView({
  assignmentId,
  content,
}: AssignedMaterialContentViewProps) {
  const material = content.publishedMaterial;
  const course = content.publishedCourse;
  const topic = content.publishedTopic;
  const bodyBlocks = splitMaterialBody(material?.body);

  return (
    <div className="home-page home-page-compact material-viewer-page">
      <section className="self-hero material-viewer-hero">
        <div className="self-hero-copy">
          <Link to={`/learner/assigned-learning/${assignmentId}/learning-context`}>
            <Button type="link" style={{ paddingInline: 0 }}>
              Назад к материалам
            </Button>
          </Link>
          <Typography.Title level={2} className="self-hero-title">
            {material?.name ?? 'Материал'}
          </Typography.Title>
          <Typography.Paragraph className="self-hero-text">
            {material?.description ?? 'Материал открыт из назначенного учебного контура.'}
          </Typography.Paragraph>
        </div>
        <Card className="self-hero-panel material-viewer-panel">
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <div>
              <Typography.Text type="secondary">Курс</Typography.Text>
              <Typography.Paragraph className="self-panel-text">
                {course?.name ?? 'Курс не указан'}
              </Typography.Paragraph>
            </div>
            <div>
              <Typography.Text type="secondary">Тема</Typography.Text>
              <Typography.Paragraph className="self-panel-text">
                {topic?.name ?? 'Тема не указана'}
              </Typography.Paragraph>
            </div>
            <Space wrap>
              <Tag color="gold">{localizeMaterialType(material?.materialType)}</Tag>
              {material?.updatedAt ? (
                <span className="stat-pill">Обновлено: {formatAssignmentDate(material.updatedAt)}</span>
              ) : null}
            </Space>
          </Space>
        </Card>
      </section>

      <Card className="soft-card material-viewer-card">
        {material?.materialType === 'VIDEO' && material.videoUrl ? (
          <div style={{ marginBottom: 24 }}>
            {isDirectVideoUrl(material.videoUrl) ? (
              <video controls style={{ width: '100%', borderRadius: 16 }} src={material.videoUrl} />
            ) : (
              <iframe
                title={material.name ?? 'Видео'}
                src={toEmbedUrl(material.videoUrl)}
                style={{ width: '100%', aspectRatio: '16 / 9', border: 0, borderRadius: 16 }}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowFullScreen
              />
            )}
          </div>
        ) : null}
        {bodyBlocks.length > 0 ? (
          <div className="material-viewer-body">{bodyBlocks.map(renderBodyBlock)}</div>
        ) : (
          <EmptyMaterialBody />
        )}
      </Card>
    </div>
  );
}
