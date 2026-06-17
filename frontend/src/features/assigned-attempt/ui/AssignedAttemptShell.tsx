import { useState } from 'react';
import { Button, Card, List, Modal, Space, Typography, notification } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { getErrorMessage, hasErrorStatus } from '../../../shared/api/apiError';
import type { AssignedTestContext } from '../../assigned-learning/model/assignedLearning';
import type {
  AssignedAttempt,
  AssignedAttemptAnswerMutationRequest,
} from '../model/assignedAttempt';
import {
  assignedAttemptQueryKeys,
  useDeleteAssignedAttemptAnswer,
  useSaveAssignedAttemptAnswer,
  useSubmitAssignedAttempt,
} from '../model/useAssignedAttempt';
import { AssignedQuestionAnswerCard } from './AssignedQuestionAnswerCard';
import { assignedLearningQueryKeys } from '../../assigned-learning/model/useAssignedLearning';
import { selfResultsQueryKeys } from '../../self-results/model/useSelfResults';

type AssignedAttemptShellProps = {
  assignmentId: string;
  assignmentTestId: string;
  testContext: AssignedTestContext;
  currentAttempt?: AssignedAttempt;
};

function canSaveRequest(
  questionType: string | undefined,
  request: AssignedAttemptAnswerMutationRequest,
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

export function AssignedAttemptShell({
  assignmentId,
  assignmentTestId,
  testContext,
  currentAttempt,
}: AssignedAttemptShellProps) {
  const [selectedAnswers, setSelectedAnswers] = useState<
    Record<string, AssignedAttemptAnswerMutationRequest>
  >({});
  const [notificationApi, contextHolder] = notification.useNotification();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const saveAnswerMutation = useSaveAssignedAttemptAnswer(assignmentId, assignmentTestId);
  const deleteAnswerMutation = useDeleteAssignedAttemptAnswer(assignmentId, assignmentTestId);
  const submitAttemptMutation = useSubmitAssignedAttempt();

  function getAnswerRequest(questionId?: number): AssignedAttemptAnswerMutationRequest {
    if (questionId == null) {
      return { answerItems: [] };
    }

    return selectedAnswers[String(questionId)] ?? { answerItems: [] };
  }

  function setQuestionSelection(
    questionId: number | undefined,
    nextRequest: AssignedAttemptAnswerMutationRequest,
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
    if (!canSaveRequest(questionType, request)) {
      return;
    }

    try {
      await saveAnswerMutation.mutateAsync({
        testAttemptId: currentAttempt.testAttemptId,
        questionId,
        request,
      });

      notificationApi.success({
        message: 'Ответ сохранён',
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

          await Promise.all([
            queryClient.invalidateQueries({
              queryKey: assignedAttemptQueryKeys.current(assignmentId, assignmentTestId),
            }),
            queryClient.invalidateQueries({
              queryKey: assignedLearningQueryKeys.list(),
            }),
            queryClient.invalidateQueries({
              queryKey: assignedLearningQueryKeys.detail(assignmentId),
            }),
            queryClient.invalidateQueries({
              queryKey: assignedLearningQueryKeys.learningContext(assignmentId),
            }),
            queryClient.invalidateQueries({
              queryKey: selfResultsQueryKeys.history(),
            }),
          ]);

          notificationApi.success({
            message: 'Попытка завершена',
            description: 'Результаты отправлены и будут доступны в разделе результатов.',
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

  return (
    <>
      {contextHolder}
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <div className="page-header page-header-tight">
          <div>
            <Link to={`/learner/assigned-learning/${assignmentId}/tests/${assignmentTestId}`}>
              <Button type="link" style={{ paddingInline: 0 }}>
                Назад
              </Button>
            </Link>
            <Typography.Title level={2} style={{ margin: 0 }}>
              {testContext.testName ?? 'Попытка прохождения теста'}
            </Typography.Title>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
              Отвечайте на вопросы по порядку и сохраняйте выбранные варианты.
            </Typography.Paragraph>
          </div>
        </div>

        <Card className="soft-card" title={`Вопросы (${testContext.questions.length})`}>
          {testContext.questions.length > 0 ? (
            <List
              dataSource={testContext.questions}
              renderItem={(question, index) => (
                <List.Item>
                  <AssignedQuestionAnswerCard
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
                Завершение теста
              </Typography.Title>
              <Typography.Text type="secondary">
                После завершения попытки результаты появятся в разделе «Мои результаты».
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
            </Space>
          </Space>
        </div>
      </Space>
    </>
  );
}
