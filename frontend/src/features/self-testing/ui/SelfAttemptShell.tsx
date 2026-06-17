import { useState } from 'react';
import { Button, Card, List, Modal, Space, Typography, notification } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { getErrorMessage, hasErrorStatus } from '../../../shared/api/apiError';
import { selfResultsQueryKeys } from '../../self-results/model/useSelfResults';
import type {
  SelfAttempt,
  SelfAttemptAnswerMutationRequest,
  SelfVisibleTest,
} from '../model/selfTesting';
import {
  selfTestingQueryKeys,
  useAbandonSelfAttempt,
  useDeleteSelfAttemptAnswer,
  useSaveSelfAttemptAnswer,
  useSubmitSelfAttempt,
} from '../model/useSelfTesting';
import { SelfQuestionAnswerCard } from './SelfQuestionAnswerCard';

type SelfAttemptShellProps = {
  testId: string;
  test: SelfVisibleTest;
  currentAttempt?: SelfAttempt;
};

function buildSaveRequest(
  questionType: string | undefined,
  request: SelfAttemptAnswerMutationRequest,
): boolean {
  const answerItems = request.answerItems ?? [];

  if (questionType === 'SINGLE_CHOICE') {
    return answerItems.length === 1 && answerItems[0]?.answerOptionId != null;
  }

  if (questionType === 'MULTIPLE_CHOICE') {
    return answerItems.length > 0 && answerItems.every((item) => item.answerOptionId != null);
  }

  if (questionType === 'MATCHING') {
    return (
      answerItems.length > 0 &&
      answerItems.every(
        (item) => item.leftAnswerOptionId != null && item.rightAnswerOptionId != null,
      )
    );
  }

  if (questionType === 'ORDERING') {
    return (
      answerItems.length > 0 &&
      answerItems.every(
        (item) => item.answerOptionId != null && item.userOrderPosition != null,
      )
    );
  }

  return false;
}

function getAttemptMutationErrorMessage(error: unknown): string {
  if (hasErrorStatus(error, 403)) {
    return 'Недостаточно прав для выполнения действия.';
  }

  if (hasErrorStatus(error, 404)) {
    return 'Не удалось найти вопрос или текущую попытку.';
  }

  if (hasErrorStatus(error, 409)) {
    return 'Сейчас этот ответ нельзя изменить.';
  }

  return getErrorMessage(error);
}

export function SelfAttemptShell({
  testId,
  test,
  currentAttempt,
}: SelfAttemptShellProps) {
  const [selectedAnswers, setSelectedAnswers] = useState<
    Record<string, SelfAttemptAnswerMutationRequest>
  >({});
  const [notificationApi, contextHolder] = notification.useNotification();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const saveAnswerMutation = useSaveSelfAttemptAnswer(testId);
  const deleteAnswerMutation = useDeleteSelfAttemptAnswer(testId);
  const submitAttemptMutation = useSubmitSelfAttempt();
  const abandonAttemptMutation = useAbandonSelfAttempt();

  function getAnswerRequest(questionId?: number): SelfAttemptAnswerMutationRequest {
    if (questionId == null) {
      return { answerItems: [] };
    }

    return selectedAnswers[String(questionId)] ?? { answerItems: [] };
  }

  function setQuestionSelection(
    questionId: number | undefined,
    nextRequest: SelfAttemptAnswerMutationRequest,
  ) {
    if (questionId == null) {
      return;
    }

    setSelectedAnswers((previous) => ({
      ...previous,
      [String(questionId)]: nextRequest,
    }));
  }

  async function handleSaveAnswer(
    questionId: number | undefined,
    questionType: string | undefined,
  ) {
    if (!currentAttempt?.testAttemptId || questionId == null) {
      return;
    }

    const request = getAnswerRequest(questionId);
    if (!buildSaveRequest(questionType, request)) {
      return;
    }

    try {
      await saveAnswerMutation.mutateAsync({
        testAttemptId: currentAttempt.testAttemptId,
        questionId,
        request,
      });

      notificationApi.success({
        message: 'Ответ сохранен',
        description: 'Ваш выбор успешно записан.',
      });
    } catch (error) {
      notificationApi.error({
        message: 'Не удалось сохранить ответ',
        description: getAttemptMutationErrorMessage(error),
      });
    }
  }

  async function handleDeleteAnswer(questionId: number | undefined) {
    if (!currentAttempt?.testAttemptId || questionId == null) {
      return;
    }

    try {
      await deleteAnswerMutation.mutateAsync({
        testAttemptId: currentAttempt.testAttemptId,
        questionId,
      });

      setSelectedAnswers((previous) => ({
        ...previous,
        [String(questionId)]: { answerItems: [] },
      }));

      notificationApi.success({
        message: 'Ответ очищен',
        description: 'Выбор для этого вопроса сброшен.',
      });
    } catch (error) {
      notificationApi.error({
        message: 'Не удалось удалить ответ',
        description: getAttemptMutationErrorMessage(error),
      });
    }
  }

  async function invalidateSelfTestingState() {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: selfTestingQueryKeys.catalog(),
      }),
      queryClient.invalidateQueries({
        queryKey: selfTestingQueryKeys.detail(testId),
      }),
      queryClient.invalidateQueries({
        queryKey: selfTestingQueryKeys.currentAttempt(testId),
      }),
      queryClient.invalidateQueries({
        queryKey: selfResultsQueryKeys.history(),
      }),
    ]);
  }

  async function handleSubmitAttempt() {
    if (!currentAttempt?.testAttemptId) {
      return;
    }

    Modal.confirm({
      title: 'Завершить попытку',
      content:
        'После подтверждения ответы будут отправлены на проверку. Продолжить?',
      okText: 'Завершить попытку',
      cancelText: 'Отмена',
      onOk: async () => {
        try {
          await submitAttemptMutation.mutateAsync(currentAttempt.testAttemptId as number);
          await invalidateSelfTestingState();

          notificationApi.success({
            message: 'Попытка завершена',
            description: 'Результат сохранен и появится в разделе ваших результатов.',
          });

          navigate('/learner/self-results');
        } catch (error) {
          notificationApi.error({
            message: 'Не удалось завершить попытку',
            description: hasErrorStatus(error, 409)
              ? 'Попытка уже завершена или недоступна для повторной отправки.'
              : getErrorMessage(error),
          });
        }
      },
    });
  }

  async function handleAbandonAttempt() {
    if (!currentAttempt?.testAttemptId) {
      return;
    }

    Modal.confirm({
      title: 'Прервать попытку',
      content:
        'Текущая попытка будет закрыта. После этого можно будет вернуться к каталогу модулей.',
      okText: 'Прервать попытку',
      cancelText: 'Отмена',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await abandonAttemptMutation.mutateAsync(currentAttempt.testAttemptId as number);
          await invalidateSelfTestingState();

          notificationApi.success({
            message: 'Попытка прервана',
            description:
              'Модуль можно будет открыть заново из каталога самостоятельного обучения.',
          });

          navigate('/learner/self-testing');
        } catch (error) {
          notificationApi.error({
            message: 'Не удалось прервать попытку',
            description: hasErrorStatus(error, 409)
              ? 'Попытка уже закрыта и не может быть прервана повторно.'
              : getErrorMessage(error),
          });
        }
      },
    });
  }

  return (
    <>
      {contextHolder}
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <section className="self-hero self-hero-attempt">
          <div className="self-hero-copy">
            <Link to={`/learner/self-testing/${testId}`}>
              <Button type="link" style={{ paddingInline: 0 }}>
                Назад
              </Button>
            </Link>
            <Typography.Title level={2} className="self-hero-title">
              {test.name ?? 'Попытка самостоятельного обучения'}
            </Typography.Title>
            <Typography.Paragraph className="self-hero-text">
              Отвечайте на вопросы по порядку, сохраняйте выбранные варианты и
              завершайте попытку, когда будете готовы.
            </Typography.Paragraph>
            <Space wrap>
              <span className="stat-pill">Вопросов: {test.questions.length}</span>
              <span className="stat-pill">Контур: самостоятельное обучение</span>
            </Space>
          </div>
        </section>

        <Card className="soft-card" title={`Вопросы (${test.questions.length})`}>
          {test.questions.length > 0 ? (
            <List
              dataSource={test.questions}
              renderItem={(question, index) => (
                <List.Item>
                  <SelfQuestionAnswerCard
                    index={index}
                    question={question}
                    answerRequest={getAnswerRequest(question.questionId)}
                    isSavePending={
                      saveAnswerMutation.isPending &&
                      Number(saveAnswerMutation.variables?.questionId) === question.questionId
                    }
                    isDeletePending={
                      deleteAnswerMutation.isPending &&
                      Number(deleteAnswerMutation.variables?.questionId) === question.questionId
                    }
                    onChange={(nextRequest) =>
                      setQuestionSelection(question.questionId, nextRequest)
                    }
                    onSave={() => handleSaveAnswer(question.questionId, question.questionType)}
                    onDelete={() => handleDeleteAnswer(question.questionId)}
                  />
                </List.Item>
              )}
            />
          ) : (
            <Typography.Text type="secondary">
              Вопросы для прохождения пока недоступны.
            </Typography.Text>
          )}
        </Card>

        <div className="submit-panel">
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <div>
              <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 8 }}>
                Действия с попыткой
              </Typography.Title>
              <Typography.Text type="secondary">
                После завершения результат появится в разделе «Мои результаты».
                Если нужно выйти без отправки, можно прервать текущую попытку.
              </Typography.Text>
            </div>
            <Space wrap>
              <Button
                type="primary"
                loading={submitAttemptMutation.isPending}
                disabled={
                  !currentAttempt ||
                  saveAnswerMutation.isPending ||
                  deleteAnswerMutation.isPending
                }
                onClick={handleSubmitAttempt}
              >
                Завершить попытку
              </Button>
              <Button
                danger
                loading={abandonAttemptMutation.isPending}
                disabled={!currentAttempt || submitAttemptMutation.isPending}
                onClick={handleAbandonAttempt}
              >
                Прервать попытку
              </Button>
            </Space>
          </Space>
        </div>
      </Space>
    </>
  );
}
