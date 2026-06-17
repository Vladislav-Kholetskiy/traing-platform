import { Button, Card, Checkbox, Radio, Select, Space, Typography } from 'antd';
import type { SelfAttemptAnswerMutationRequest, SelfVisibleTestQuestion } from '../model/selfTesting';
import { localizeQuestionType } from '../../../shared/ui/presentation';

type SelfQuestionAnswerCardProps = {
  index: number;
  question: SelfVisibleTestQuestion;
  answerRequest: SelfAttemptAnswerMutationRequest;
  isSavePending: boolean;
  isDeletePending: boolean;
  onChange: (nextRequest: SelfAttemptAnswerMutationRequest) => void;
  onSave: () => void;
  onDelete: () => void;
};

function hasUniqueDefinedValues(values: Array<number | undefined>) {
  const normalized = values.filter((value): value is number => value != null);
  return normalized.length === new Set(normalized).size;
}

export function SelfQuestionAnswerCard({
  index,
  question,
  answerRequest,
  isSavePending,
  isDeletePending,
  onChange,
  onSave,
  onDelete,
}: SelfQuestionAnswerCardProps) {
  const answerItems = answerRequest.answerItems ?? [];
  const optionNodes = question.answerOptions.map((option) => ({
    label: option.body ?? 'Вариант ответа без текста',
    value: option.answerOptionId ?? -1,
    role: option.answerOptionRole,
    displayOrder: option.displayOrder ?? 0,
  }));
  const leftOptions = optionNodes.filter((option) => option.role === 'MATCH_LEFT');
  const rightOptions = optionNodes.filter((option) => option.role === 'MATCH_RIGHT');
  const orderingOptions = optionNodes.filter((option) => option.role === 'ORDER_ITEM');
  const selectedOptionIds = answerItems
    .map((item) => item.answerOptionId)
    .filter((value): value is number => value != null);
  const matchingAnswerByLeftId = new Map(
    answerItems
      .filter((item) => item.leftAnswerOptionId != null)
      .map((item) => [item.leftAnswerOptionId as number, item.rightAnswerOptionId]),
  );
  const orderingAnswerByOptionId = new Map(
    answerItems
      .filter((item) => item.answerOptionId != null)
      .map((item) => [item.answerOptionId as number, item.userOrderPosition]),
  );

  const supportedQuestion =
    question.questionType === 'SINGLE_CHOICE' ||
    question.questionType === 'MULTIPLE_CHOICE' ||
    question.questionType === 'MATCHING' ||
    question.questionType === 'ORDERING';

  const canSave =
    question.questionType === 'SINGLE_CHOICE'
      ? selectedOptionIds.length === 1
      : question.questionType === 'MULTIPLE_CHOICE'
        ? selectedOptionIds.length > 0
        : question.questionType === 'MATCHING'
          ? leftOptions.length > 0 &&
            rightOptions.length > 0 &&
            leftOptions.every((option) => matchingAnswerByLeftId.get(option.value) != null) &&
            hasUniqueDefinedValues(leftOptions.map((option) => matchingAnswerByLeftId.get(option.value)))
          : question.questionType === 'ORDERING'
            ? orderingOptions.length > 0 &&
              orderingOptions.every((option) => orderingAnswerByOptionId.get(option.value) != null) &&
              hasUniqueDefinedValues(orderingOptions.map((option) => orderingAnswerByOptionId.get(option.value)))
            : false;

  function handleSingleChoiceChange(nextValue: number) {
    onChange({
      answerItems: [{ answerOptionId: nextValue }],
    });
  }

  function handleMultipleChoiceChange(nextValues: number[]) {
    onChange({
      answerItems: nextValues.map((answerOptionId) => ({ answerOptionId })),
    });
  }

  function handleMatchingChange(leftAnswerOptionId: number, rightAnswerOptionId?: number) {
    const remainingItems = answerItems.filter((item) => item.leftAnswerOptionId !== leftAnswerOptionId);
    const nextItems = rightAnswerOptionId == null
      ? remainingItems
      : [...remainingItems, { leftAnswerOptionId, rightAnswerOptionId }];

    onChange({ answerItems: nextItems });
  }

  function handleOrderingChange(answerOptionId: number, userOrderPosition?: number) {
    const remainingItems = answerItems.filter((item) => item.answerOptionId !== answerOptionId);
    const nextItems = userOrderPosition == null
      ? remainingItems
      : [...remainingItems, { answerOptionId, userOrderPosition }];

    onChange({ answerItems: nextItems });
  }

  return (
    <Card className="soft-card question-card self-question-card" style={{ width: '100%' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space direction="vertical" size="small" style={{ width: '100%' }}>
          <Typography.Title level={5} style={{ margin: 0 }}>
            {`Вопрос ${index + 1}`}
          </Typography.Title>
          <Typography.Text strong>{question.body ?? 'Текст вопроса скоро появится.'}</Typography.Text>
          <Typography.Text type="secondary">
            {localizeQuestionType(question.questionType, 'Тип вопроса не указан')}
          </Typography.Text>
        </Space>

        {question.questionType === 'SINGLE_CHOICE' ? (
          <Radio.Group value={selectedOptionIds[0]} onChange={(event) => handleSingleChoiceChange(event.target.value)}>
            <Space direction="vertical" className="option-list">
              {optionNodes.map((option) => (
                <div key={option.value} className="option-item">
                  <Radio value={option.value}>{option.label}</Radio>
                </div>
              ))}
            </Space>
          </Radio.Group>
        ) : null}

        {question.questionType === 'MULTIPLE_CHOICE' ? (
          <Checkbox.Group value={selectedOptionIds} onChange={(values) => handleMultipleChoiceChange(values as number[])}>
            <Space direction="vertical" className="option-list">
              {optionNodes.map((option) => (
                <div key={option.value} className="option-item">
                  <Checkbox value={option.value}>{option.label}</Checkbox>
                </div>
              ))}
            </Space>
          </Checkbox.Group>
        ) : null}

        {question.questionType === 'MATCHING' ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            {leftOptions.map((leftOption) => (
              <div key={leftOption.value} className="option-item">
                <Typography.Text strong>{leftOption.label}</Typography.Text>
                <Select
                  allowClear
                  style={{ width: '100%', marginTop: 8 }}
                  placeholder="Выберите соответствие"
                  value={matchingAnswerByLeftId.get(leftOption.value)}
                  options={rightOptions.map((rightOption) => ({
                    label: rightOption.label,
                    value: rightOption.value,
                  }))}
                  onChange={(value) => handleMatchingChange(leftOption.value, value)}
                />
              </div>
            ))}
          </Space>
        ) : null}

        {question.questionType === 'ORDERING' ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            {orderingOptions
              .slice()
              .sort((left, right) => left.displayOrder - right.displayOrder)
              .map((option) => (
                <div key={option.value} className="option-item">
                  <Typography.Text strong>{option.label}</Typography.Text>
                  <Select
                    allowClear
                    style={{ width: '100%', marginTop: 8 }}
                    placeholder="Выберите позицию"
                    value={orderingAnswerByOptionId.get(option.value)}
                    options={orderingOptions.map((_, indexPosition) => ({
                      label: `Позиция ${indexPosition + 1}`,
                      value: indexPosition + 1,
                    }))}
                    onChange={(value) => handleOrderingChange(option.value, value)}
                  />
                </div>
              ))}
          </Space>
        ) : null}

        {!supportedQuestion ? (
          <Typography.Text type="secondary">
            Frontend не распознал runtime question type `{question.questionType ?? 'UNKNOWN'}`. Ответ не будет отправлен, пока для него нет явной формы.
          </Typography.Text>
        ) : null}

        <Space wrap>
          <Button
            type="primary"
            loading={isSavePending}
            disabled={isDeletePending || !canSave}
            onClick={onSave}
          >
            Сохранить ответ
          </Button>
          <Button loading={isDeletePending} disabled={isSavePending} onClick={onDelete}>
            Удалить ответ
          </Button>
        </Space>
      </Space>
    </Card>
  );
}
