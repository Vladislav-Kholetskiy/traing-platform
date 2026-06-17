import { Alert, Button, Form, Input, InputNumber, Select, Space, Table, Tag, Typography } from 'antd';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { createAnswerOption } from '../../features/expert-content/api/expertContentApi';
import type { CreateQuestionRequest, QuestionType, SaveAnswerOptionRequest } from '../../features/expert-content/model/expertContent';
import { useCreateQuestionMutation, useQuestionsByTopic, useTopic } from '../../features/expert-content/model/useExpertContent';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { SectionCard } from '../../shared/ui/SectionCard';
import { formatUiDate, localizeContentStatus, localizeQuestionType } from '../../shared/ui/presentation';

const questionTypeOptions = [
  { label: 'Один вариант ответа', value: 'SINGLE_CHOICE' },
  { label: 'Несколько вариантов ответа', value: 'MULTIPLE_CHOICE' },
  { label: 'Сопоставление', value: 'MATCHING' },
  { label: 'Правильный порядок', value: 'ORDERING' },
] as const;

export function ExpertTopicQuestionsPage() {
  const { topicId: topicIdParam } = useParams();
  const topicId = Number(topicIdParam);
  const { data: actor } = useCurrentActor();
  const topicQuery = useTopic(topicId, Boolean(actor));
  const questionsQuery = useQuestionsByTopic(topicId, Boolean(actor));
  const createMutation = useCreateQuestionMutation();
  const [form] = Form.useForm<CreateQuestionWithAnswersForm>();
  const [creationError, setCreationError] = useState<string | null>(null);
  const [createdQuestionId, setCreatedQuestionId] = useState<number | null>(null);
  const watchedQuestionType = Form.useWatch('questionType', form) ?? 'SINGLE_CHOICE';

  if (!actor || !canAccessExpertArea(actor)) return <ForbiddenView />;
  if (questionsQuery.isLoading || topicQuery.isLoading) return <LoadingView title="Загрузка вопросов" />;
  if (topicQuery.isError) return <ErrorView title="Не удалось загрузить тему" error={topicQuery.error} />;
  if (questionsQuery.isError) return <ErrorView title="Не удалось загрузить вопросы" error={questionsQuery.error} />;

  const questions = questionsQuery.data ?? [];
  const topicName = topicQuery.data?.name?.trim() || `Тема #${topicId}`;

  return (
    <div className="expert-question-catalog-page">
      <section className="expert-question-catalog-hero">
        <div className="expert-question-catalog-copy">
          <span className="expert-question-catalog-kicker">Конструктор темы</span>
          <Typography.Title level={2} className="expert-question-catalog-title">
            {topicName}
          </Typography.Title>
          <Typography.Paragraph className="expert-question-catalog-text">
            Здесь вы собираете банк вопросов для темы «{topicName}». Сначала создайте формулировку, затем откройте карточку вопроса и
            добавьте варианты ответа, порядок или логику сопоставления.
          </Typography.Paragraph>
        </div>

        <div className="expert-question-catalog-panel">
          <CatalogMetric label="Вопросов в теме" value={String(questions.length)} />
          <CatalogMetric
            label="Следующий шаг"
            value={questions.length > 0 ? 'Открыть карточку вопроса' : 'Создать первый вопрос'}
          />
        </div>
      </section>

      {creationError ? (
        <Alert
          type="error"
          showIcon
          message="Не удалось сохранить вопрос целиком"
          description={creationError}
        />
      ) : null}

      {createdQuestionId ? (
        <Alert
          type="success"
          showIcon
          message="Вопрос и ответы созданы"
          description={
            <Space wrap>
              <span>Можно сразу перейти к карточке и при необходимости доработать детали.</span>
              <Link to={`/expert/content/questions/${createdQuestionId}`}>Открыть вопрос</Link>
            </Space>
          }
        />
      ) : null}

      <div className="expert-question-catalog-layout">
        <SectionCard
          title="Новый вопрос"
          description="Создайте вопрос вместе с ответами в одном месте. После сохранения останется только при необходимости доработать карточку."
        >
          <Form<CreateQuestionWithAnswersForm>
            layout="vertical"
            form={form}
            initialValues={{
              topicId,
              questionType: 'SINGLE_CHOICE',
              answerOptions: [buildDefaultAnswerOption('SINGLE_CHOICE', 0), buildDefaultAnswerOption('SINGLE_CHOICE', 1)],
            }}
            onFinish={async (values) => {
              setCreationError(null);
              setCreatedQuestionId(null);

              try {
                const createdQuestion = await createMutation.mutateAsync({
                  topicId,
                  body: values.body,
                  questionType: values.questionType,
                  sortOrder: values.sortOrder,
                });

                for (const option of values.answerOptions) {
                  await createAnswerOption(createdQuestion.id, normalizeAnswerOptionPayload(values.questionType, option));
                }

                setCreatedQuestionId(createdQuestion.id);
                form.resetFields();
                form.setFieldsValue({
                  topicId,
                  questionType: 'SINGLE_CHOICE',
                  answerOptions: [buildDefaultAnswerOption('SINGLE_CHOICE', 0), buildDefaultAnswerOption('SINGLE_CHOICE', 1)],
                });
                await questionsQuery.refetch();
              } catch (error) {
                setCreationError(error instanceof Error ? error.message : String(error));
              }
            }}
          >
            <div className="admin-form-grid">
              <Form.Item name="questionType" label="Какой это вопрос?" rules={[{ required: true }]}>
                <Select options={questionTypeOptions.map((option) => ({ label: option.label, value: option.value }))} />
              </Form.Item>
              <Form.Item name="sortOrder" label="Позиция в теме">
                <InputNumber style={{ width: '100%' }} min={0} placeholder="Например, 1" />
              </Form.Item>
            </div>
            <Form.Item
              name="body"
              label="Формулировка вопроса"
              extra="Напишите сам вопрос так, как его увидит обучающийся."
              rules={[{ required: true }]}
            >
              <Input.TextArea rows={5} placeholder="Например: Какое действие нужно выполнить первым?" />
            </Form.Item>
            <SectionCard
              title="Варианты ответа"
              description="Добавьте ответы сразу здесь. Набор полей меняется в зависимости от типа вопроса."
            >
              <Form.List
                name="answerOptions"
                rules={[
                  {
                    validator: async (_, value) => {
                      if (Array.isArray(value) && value.length >= 2) {
                        return;
                      }
                      throw new Error('Добавьте минимум два варианта ответа.');
                    },
                  },
                ]}
              >
                {(fields, { add, remove }, { errors }) => (
                  <div className="expert-question-builder-list">
                    {fields.map((field, index) => (
                      <div key={field.key} className="expert-question-builder-item">
                        <div className="expert-question-builder-item-head">
                          <strong>Ответ {index + 1}</strong>
                          {fields.length > 2 ? (
                            <Button danger type="text" onClick={() => remove(field.name)}>
                              Удалить
                            </Button>
                          ) : null}
                        </div>

                        <Form.Item
                          name={[field.name, 'body']}
                          label="Текст ответа"
                          rules={[{ required: true, message: 'Введите текст ответа' }]}
                        >
                          <Input.TextArea rows={3} placeholder="Введите вариант ответа" />
                        </Form.Item>

                        <div className="admin-form-grid">
                          {watchedQuestionType === 'MATCHING' ? (
                            <>
                              <Form.Item
                                name={[field.name, 'answerOptionRole']}
                                label="Роль элемента"
                                rules={[{ required: true, message: 'Выберите роль элемента' }]}
                              >
                                <Select
                                  options={[
                                    { label: 'Левая часть пары', value: 'MATCH_LEFT' },
                                    { label: 'Правая часть пары', value: 'MATCH_RIGHT' },
                                  ]}
                                />
                              </Form.Item>
                              <Form.Item
                                name={[field.name, 'pairingKey']}
                                label="Ключ пары"
                                rules={[{ required: true, message: 'Укажите ключ пары' }]}
                              >
                                <Input placeholder="Например, A" />
                              </Form.Item>
                            </>
                          ) : null}

                          {watchedQuestionType === 'ORDERING' ? (
                            <Form.Item
                              name={[field.name, 'canonicalOrderPosition']}
                              label="Правильная позиция"
                              rules={[{ required: true, message: 'Укажите правильную позицию' }]}
                            >
                              <InputNumber style={{ width: '100%' }} min={1} placeholder="1" />
                            </Form.Item>
                          ) : null}

                          {watchedQuestionType !== 'MATCHING' ? (
                            <Form.Item name={[field.name, 'displayOrder']} label="Порядок показа">
                              <InputNumber style={{ width: '100%' }} min={0} placeholder="0" />
                            </Form.Item>
                          ) : null}

                          {watchedQuestionType === 'SINGLE_CHOICE' || watchedQuestionType === 'MULTIPLE_CHOICE' ? (
                            <Form.Item name={[field.name, 'isCorrect']} label="Это правильный ответ?">
                              <Select options={[{ label: 'Да', value: true }, { label: 'Нет', value: false }]} />
                            </Form.Item>
                          ) : null}
                        </div>
                      </div>
                    ))}

                    <Button onClick={() => add(buildDefaultAnswerOption(watchedQuestionType, fields.length))}>
                      Добавить вариант
                    </Button>
                    <Form.ErrorList errors={errors} />
                  </div>
                )}
              </Form.List>
            </SectionCard>
            <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
              Создать вопрос с ответами
            </Button>
          </Form>
        </SectionCard>

        <SectionCard
          title="Список вопросов"
          description="Каждый вопрос можно открыть и доработать: отредактировать текст, добавить варианты ответа и опубликовать."
          extra={<Tag color="blue">Всего: {questions.length}</Tag>}
        >
          {questions.length === 0 ? (
            <div className="expert-question-catalog-empty">
              <EmptyState
                title="В теме пока нет вопросов"
                description="Начните с формы выше: создайте первый вопрос, а затем откройте его карточку для настройки ответов."
              />
            </div>
          ) : (
            <Table
              className="expert-question-catalog-table"
              rowKey="id"
              dataSource={questions}
              pagination={{ pageSize: 10 }}
              columns={[
                {
                  title: 'Вопрос',
                  render: (_, question) => (
                    <div className="expert-question-catalog-cell">
                      <Link to={`/expert/content/questions/${question.id}`} className="expert-analytics-question-link">
                        <Typography.Text strong>#{question.id}</Typography.Text>
                      </Link>
                      <Typography.Text>{question.body}</Typography.Text>
                    </div>
                  ),
                },
                {
                  title: 'Тип',
                  dataIndex: 'questionType',
                  render: (value) => localizeQuestionType(value ?? undefined),
                },
                {
                  title: 'Статус',
                  dataIndex: 'status',
                  render: (value) => <Tag color="gold">{localizeContentStatus(value ?? undefined)}</Tag>,
                },
                {
                  title: 'Обновлён',
                  dataIndex: 'updatedAt',
                  render: (value) => formatUiDate(value ?? undefined),
                },
                {
                  title: 'Действие',
                  render: (_, question) => (
                    <Link to={`/expert/content/questions/${question.id}`} className="expert-analytics-open-link">
                      Открыть
                    </Link>
                  ),
                },
              ]}
            />
          )}
        </SectionCard>
      </div>
    </div>
  );
}

function CatalogMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="expert-question-catalog-metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

type CreateQuestionWithAnswersForm = CreateQuestionRequest & {
  answerOptions: CreateQuestionAnswerDraft[];
};

type CreateQuestionAnswerDraft = SaveAnswerOptionRequest & {
  body: string;
};

function buildDefaultAnswerOption(questionType: QuestionType, index: number): CreateQuestionAnswerDraft {
  if (questionType === 'MATCHING') {
    return {
      body: '',
      answerOptionRole: index % 2 === 0 ? 'MATCH_LEFT' : 'MATCH_RIGHT',
      pairingKey: '',
      displayOrder: index,
    };
  }

  if (questionType === 'ORDERING') {
    return {
      body: '',
      answerOptionRole: 'ORDER_ITEM',
      displayOrder: index,
      canonicalOrderPosition: index + 1,
    };
  }

  return {
    body: '',
    answerOptionRole: 'CHOICE_OPTION',
    isCorrect: false,
    displayOrder: index,
  };
}

function normalizeAnswerOptionPayload(
  questionType: QuestionType,
  option: CreateQuestionAnswerDraft,
): SaveAnswerOptionRequest {
  const payload: SaveAnswerOptionRequest = {
    body: option.body.trim(),
    answerOptionRole: resolveAnswerOptionRole(questionType, option.answerOptionRole),
  };

  if (option.displayOrder != null) {
    payload.displayOrder = option.displayOrder;
  }

  if (questionType === 'SINGLE_CHOICE' || questionType === 'MULTIPLE_CHOICE') {
    payload.isCorrect = Boolean(option.isCorrect);
  }

  if (questionType === 'MATCHING' && option.pairingKey?.trim()) {
    payload.pairingKey = option.pairingKey.trim();
  }

  if (questionType === 'ORDERING' && option.canonicalOrderPosition != null) {
    payload.canonicalOrderPosition = option.canonicalOrderPosition;
  }

  return payload;
}

function resolveAnswerOptionRole(questionType: QuestionType, role?: string): SaveAnswerOptionRequest['answerOptionRole'] {
  if (questionType === 'MATCHING') {
    return role === 'MATCH_RIGHT' ? 'MATCH_RIGHT' : 'MATCH_LEFT';
  }

  if (questionType === 'ORDERING') {
    return 'ORDER_ITEM';
  }

  return 'CHOICE_OPTION';
}
