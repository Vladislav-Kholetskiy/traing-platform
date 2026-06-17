import { Alert, Button, Form, Input, InputNumber, Select, Space, Table, Tag, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { canAccessExpertArea } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import type {
  AnswerOption,
  SaveAnswerOptionRequest,
  UpdateQuestionRequest,
} from '../../features/expert-content/model/expertContent';
import {
  useArchiveQuestionMutation,
  useCreateAnswerOptionMutation,
  useDeleteAnswerOptionMutation,
  useLifecycleQuestion,
  usePublishQuestionMutation,
  useQuestion,
  useQuestionAnswerOptions,
  useUpdateAnswerOptionMutation,
  useUpdateQuestionMutation,
} from '../../features/expert-content/model/useExpertContent';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';
import { SectionCard } from '../../shared/ui/SectionCard';
import {
  formatUiDate,
  localizeAnswerOptionRole,
  localizeContentStatus,
  localizeQuestionType,
} from '../../shared/ui/presentation';

export function ExpertQuestionDetailPage() {
  const { questionId: questionIdParam } = useParams();
  const questionId = Number(questionIdParam);
  const { data: actor } = useCurrentActor();
  const questionQuery = useQuestion(questionId, Boolean(actor));
  const lifecycleQuery = useLifecycleQuestion(questionId, Boolean(actor));
  const optionsQuery = useQuestionAnswerOptions(questionId, Boolean(actor));
  const updateMutation = useUpdateQuestionMutation(questionId);
  const publishMutation = usePublishQuestionMutation(questionId);
  const archiveMutation = useArchiveQuestionMutation(questionId);
  const createOptionMutation = useCreateAnswerOptionMutation(questionId);
  const deleteOptionMutation = useDeleteAnswerOptionMutation(questionId);
  const [editingAnswerOptionId, setEditingAnswerOptionId] = useState<number | null>(null);
  const updateAnswerOptionMutation = useUpdateAnswerOptionMutation(questionId, editingAnswerOptionId ?? undefined);
  const [questionForm] = Form.useForm<UpdateQuestionRequest>();
  const [optionForm] = Form.useForm<SaveAnswerOptionRequest>();
  const [editOptionForm] = Form.useForm<SaveAnswerOptionRequest>();

  const editingOption = (optionsQuery.data ?? []).find((option) => option.id === editingAnswerOptionId) ?? null;

  useEffect(() => {
    if (!editingOption) {
      editOptionForm.resetFields();
      return;
    }

    editOptionForm.setFieldsValue({
      body: editingOption.body ?? '',
      answerOptionRole: editingOption.answerOptionRole ?? 'CHOICE_OPTION',
      isCorrect: editingOption.isCorrect ?? false,
      displayOrder: editingOption.displayOrder ?? 0,
      pairingKey: editingOption.pairingKey ?? undefined,
      canonicalOrderPosition: editingOption.canonicalOrderPosition ?? undefined,
    });
  }, [editOptionForm, editingOption]);

  if (!actor || !canAccessExpertArea(actor)) return <ForbiddenView />;
  if (questionQuery.isLoading || optionsQuery.isLoading || lifecycleQuery.isLoading) {
    return <LoadingView title="Загрузка вопроса" />;
  }
  if (questionQuery.isError || optionsQuery.isError) {
    return <ErrorView title="Не удалось загрузить вопрос" error={questionQuery.error ?? optionsQuery.error} />;
  }

  const question = questionQuery.data;
  if (!question) return <ErrorView title="Вопрос не найден" />;

  const lifecycle = lifecycleQuery.data;
  const options = optionsQuery.data ?? [];
  const commandError =
    updateMutation.error ??
    publishMutation.error ??
    archiveMutation.error ??
    createOptionMutation.error ??
    updateAnswerOptionMutation.error ??
    deleteOptionMutation.error;

  return (
    <div className="expert-question-page">
      <section className="expert-question-hero">
        <div className="expert-question-hero-copy">
          <span className="expert-question-kicker">Банк вопросов</span>
          <Typography.Title level={2} className="expert-question-title">
            Вопрос #{question.id}
          </Typography.Title>
          <Typography.Paragraph className="expert-question-body">
            {question.body}
          </Typography.Paragraph>
          <div className="expert-question-meta-row">
            <Tag color="blue">{localizeQuestionType(question.questionType ?? undefined)}</Tag>
            <Tag color="gold">{localizeContentStatus(question.status ?? undefined)}</Tag>
            <Tag bordered={false}>Обновлён {formatUiDate(question.updatedAt ?? undefined)}</Tag>
          </div>
        </div>

        <div className="expert-question-hero-panel">
          <div className="expert-question-summary-grid">
            <SummaryTile label="Статус" value={localizeContentStatus(lifecycle?.status ?? question.status ?? undefined)} />
            <SummaryTile label="Порядок" value={String(lifecycle?.sortOrder ?? question.sortOrder ?? 0)} />
            <SummaryTile label="Вариантов" value={String(options.length)} />
            <SummaryTile
              label="Правильных"
              value={String(options.filter((option) => option.isCorrect).length)}
            />
          </div>
          <div className="expert-question-hero-actions">
            <Button type="primary" onClick={() => publishMutation.mutate()} loading={publishMutation.isPending}>
              Публиковать
            </Button>
            <Button onClick={() => archiveMutation.mutate()} loading={archiveMutation.isPending}>
              В архив
            </Button>
          </div>
        </div>
      </section>

      {commandError ? (
        <Alert
          type="error"
          showIcon
          message="Не удалось выполнить действие"
          description={String(commandError)}
        />
      ) : null}

      {updateAnswerOptionMutation.isSuccess && editingAnswerOptionId ? (
        <Alert
          type="success"
          showIcon
          message="Вариант ответа обновлён"
          description="Изменения сохранены и уже участвуют в рабочей карточке вопроса."
        />
      ) : null}

      <div className="expert-question-layout">
        <div className="expert-question-main">
          <SectionCard
            title="Содержание вопроса"
            description="Здесь редактируется формулировка, тип вопроса и место в последовательности."
          >
            <Form<UpdateQuestionRequest>
              layout="vertical"
              form={questionForm}
              initialValues={{
                body: question.body,
                questionType: question.questionType ?? 'SINGLE_CHOICE',
                sortOrder: question.sortOrder ?? 0,
              }}
              onFinish={(values) => updateMutation.mutate(values)}
            >
              <div className="admin-form-grid">
                <Form.Item name="questionType" label="Тип вопроса" rules={[{ required: true }]}>
                  <Select
                    options={[
                      { label: 'SINGLE_CHOICE', value: 'SINGLE_CHOICE' },
                      { label: 'MULTIPLE_CHOICE', value: 'MULTIPLE_CHOICE' },
                      { label: 'MATCHING', value: 'MATCHING' },
                      { label: 'ORDERING', value: 'ORDERING' },
                    ]}
                  />
                </Form.Item>
                <Form.Item name="sortOrder" label="Позиция в списке" rules={[{ required: true }]}>
                  <InputNumber style={{ width: '100%' }} min={0} />
                </Form.Item>
              </div>
              <Form.Item name="body" label="Текст вопроса" rules={[{ required: true }]}>
                <Input.TextArea rows={5} />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>
                Сохранить изменения
              </Button>
            </Form>
          </SectionCard>

          <SectionCard
            title="Варианты ответа"
            description="Ниже можно быстро просмотреть текущие варианты и при необходимости открыть редактирование."
            extra={<Tag color="blue">Всего: {options.length}</Tag>}
          >
            <Table
              className="expert-question-options-table"
              rowKey="id"
              dataSource={options}
              pagination={false}
              columns={[
                {
                  title: 'Вариант',
                  render: (_: unknown, option: AnswerOption) => (
                    <div className="expert-question-option-cell">
                      <Typography.Text strong>#{option.id}</Typography.Text>
                      <Typography.Text>{option.body}</Typography.Text>
                    </div>
                  ),
                },
                {
                  title: 'Роль',
                  dataIndex: 'answerOptionRole',
                  render: (value) => localizeAnswerOptionRole(value ?? undefined),
                },
                {
                  title: 'Статус',
                  dataIndex: 'isCorrect',
                  render: (value) => (
                    <Tag color={value ? 'green' : 'default'}>{value ? 'Правильный' : 'Обычный'}</Tag>
                  ),
                },
                {
                  title: 'Порядок',
                  dataIndex: 'displayOrder',
                },
                {
                  title: 'Действия',
                  render: (_: unknown, option: AnswerOption) => (
                    <Space wrap>
                      <Button size="small" onClick={() => setEditingAnswerOptionId(option.id)}>
                        Редактировать
                      </Button>
                      <Button size="small" danger onClick={() => deleteOptionMutation.mutate(option.id)}>
                        Удалить
                      </Button>
                    </Space>
                  ),
                },
              ]}
            />
          </SectionCard>

          {editingOption ? (
            <SectionCard
              title={`Редактирование варианта #${editingOption.id}`}
              description="Изменения применяются сразу к выбранному варианту ответа."
            >
              <Form<SaveAnswerOptionRequest>
                layout="vertical"
                form={editOptionForm}
                onFinish={(values) => updateAnswerOptionMutation.mutate(values)}
              >
                <div className="admin-form-grid">
                  <Form.Item name="answerOptionRole" label="Роль варианта" rules={[{ required: true }]}>
                    <Select
                      options={[
                        { label: 'CHOICE_OPTION', value: 'CHOICE_OPTION' },
                        { label: 'MATCH_LEFT', value: 'MATCH_LEFT' },
                        { label: 'MATCH_RIGHT', value: 'MATCH_RIGHT' },
                        { label: 'ORDER_ITEM', value: 'ORDER_ITEM' },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item name="displayOrder" label="Порядок показа" rules={[{ required: true }]}>
                    <InputNumber style={{ width: '100%' }} min={0} />
                  </Form.Item>
                  <Form.Item name="canonicalOrderPosition" label="Позиция в эталоне">
                    <InputNumber style={{ width: '100%' }} min={0} />
                  </Form.Item>
                  <Form.Item name="pairingKey" label="Ключ пары">
                    <Input />
                  </Form.Item>
                  <Form.Item name="isCorrect" label="Это правильный ответ">
                    <Select options={[{ label: 'Да', value: true }, { label: 'Нет', value: false }]} />
                  </Form.Item>
                </div>
                <Form.Item name="body" label="Текст варианта" rules={[{ required: true }]}>
                  <Input.TextArea rows={4} />
                </Form.Item>
                <Space wrap>
                  <Button type="primary" htmlType="submit" loading={updateAnswerOptionMutation.isPending}>
                    Сохранить вариант
                  </Button>
                  <Button onClick={() => setEditingAnswerOptionId(null)} disabled={updateAnswerOptionMutation.isPending}>
                    Закрыть
                  </Button>
                </Space>
              </Form>
            </SectionCard>
          ) : null}
        </div>

        <div className="expert-question-side">
          <SectionCard
            title="Публикация"
            description="Сводка по состоянию вопроса без технических дублирований."
          >
            <div className="expert-question-side-list">
              <SideFact label="Тип вопроса" value={localizeQuestionType(lifecycle?.questionType ?? question.questionType ?? undefined)} />
              <SideFact label="Статус" value={localizeContentStatus(lifecycle?.status ?? question.status ?? undefined)} />
              <SideFact label="Обновлён" value={formatUiDate(lifecycle?.updatedAt ?? question.updatedAt ?? undefined)} />
              <SideFact label="Позиция" value={String(lifecycle?.sortOrder ?? question.sortOrder ?? 0)} />
            </div>
          </SectionCard>

          <SectionCard
            title="Новый вариант ответа"
            description="Используйте этот блок, когда нужно быстро добавить ещё один вариант."
          >
            <Form<SaveAnswerOptionRequest>
              layout="vertical"
              form={optionForm}
              initialValues={{ answerOptionRole: 'CHOICE_OPTION', isCorrect: false, displayOrder: 0 }}
              onFinish={async (values) => {
                await createOptionMutation.mutateAsync(values);
                optionForm.resetFields();
                optionForm.setFieldsValue({ answerOptionRole: 'CHOICE_OPTION', isCorrect: false, displayOrder: 0 });
              }}
            >
              <Form.Item name="answerOptionRole" label="Роль варианта" rules={[{ required: true }]}>
                <Select
                  options={[
                    { label: 'CHOICE_OPTION', value: 'CHOICE_OPTION' },
                    { label: 'MATCH_LEFT', value: 'MATCH_LEFT' },
                    { label: 'MATCH_RIGHT', value: 'MATCH_RIGHT' },
                    { label: 'ORDER_ITEM', value: 'ORDER_ITEM' },
                  ]}
                />
              </Form.Item>
              <Form.Item name="displayOrder" label="Порядок показа" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
              <Form.Item name="canonicalOrderPosition" label="Позиция в эталоне">
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
              <Form.Item name="pairingKey" label="Ключ пары">
                <Input />
              </Form.Item>
              <Form.Item name="isCorrect" label="Это правильный ответ">
                <Select options={[{ label: 'Да', value: true }, { label: 'Нет', value: false }]} />
              </Form.Item>
              <Form.Item name="body" label="Текст варианта" rules={[{ required: true }]}>
                <Input.TextArea rows={4} />
              </Form.Item>
              <Button htmlType="submit" loading={createOptionMutation.isPending}>
                Добавить вариант
              </Button>
            </Form>
          </SectionCard>
        </div>
      </div>
    </div>
  );
}

function SummaryTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="expert-question-summary-tile">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function SideFact({ label, value }: { label: string; value: string }) {
  return (
    <div className="expert-question-side-fact">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
