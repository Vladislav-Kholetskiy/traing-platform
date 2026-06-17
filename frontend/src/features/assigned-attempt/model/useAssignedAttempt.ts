import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  deleteAssignedAttemptAnswer,
  getCurrentAssignedAttempt,
  saveAssignedAttemptAnswer,
  startOrContinueAssignedAttempt,
  submitAssignedAttempt,
} from '../api/assignedAttemptApi';
import type { AssignedAttemptAnswerMutationRequest } from './assignedAttempt';

export const assignedAttemptQueryKeys = {
  all: ['assigned-attempt'] as const,
  current: (assignmentId: string | number, assignmentTestId: string | number) =>
    [...assignedAttemptQueryKeys.all, 'current', String(assignmentId), String(assignmentTestId)] as const,
  entry: (assignmentId: string | number, assignmentTestId: string | number) =>
    [...assignedAttemptQueryKeys.all, 'entry', String(assignmentId), String(assignmentTestId)] as const,
  submit: (testAttemptId: string | number) =>
    [...assignedAttemptQueryKeys.all, 'submit', String(testAttemptId)] as const,
};

export function useCurrentAssignedAttempt(assignmentId?: string, assignmentTestId?: string) {
  return useQuery({
    queryKey: assignedAttemptQueryKeys.current(
      assignmentId ?? 'unknown',
      assignmentTestId ?? 'unknown',
    ),
    queryFn: () => getCurrentAssignedAttempt(assignmentId as string, assignmentTestId as string),
    enabled: Boolean(assignmentId && assignmentTestId),
    retry: false,
  });
}

export function useStartOrContinueAssignedAttempt(
  assignmentId?: string,
  assignmentTestId?: string,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: assignedAttemptQueryKeys.entry(
      assignmentId ?? 'unknown',
      assignmentTestId ?? 'unknown',
    ),
    mutationFn: () => startOrContinueAssignedAttempt(assignmentId as string, assignmentTestId as string),
    onSuccess: (attempt) => {
      queryClient.setQueryData(
        assignedAttemptQueryKeys.current(assignmentId ?? 'unknown', assignmentTestId ?? 'unknown'),
        attempt,
      );
    },
  });
}

type AnswerMutationVariables = {
  questionId: string | number;
  request: AssignedAttemptAnswerMutationRequest;
  testAttemptId: string | number;
};

type DeleteAnswerMutationVariables = {
  questionId: string | number;
  testAttemptId: string | number;
};

export function useSaveAssignedAttemptAnswer(assignmentId?: string, assignmentTestId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ testAttemptId, questionId, request }: AnswerMutationVariables) =>
      saveAssignedAttemptAnswer(testAttemptId, questionId, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: assignedAttemptQueryKeys.current(
          assignmentId ?? 'unknown',
          assignmentTestId ?? 'unknown',
        ),
      });
    },
  });
}

export function useDeleteAssignedAttemptAnswer(assignmentId?: string, assignmentTestId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ testAttemptId, questionId }: DeleteAnswerMutationVariables) =>
      deleteAssignedAttemptAnswer(testAttemptId, questionId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: assignedAttemptQueryKeys.current(
          assignmentId ?? 'unknown',
          assignmentTestId ?? 'unknown',
        ),
      });
    },
  });
}

export function useSubmitAssignedAttempt() {
  return useMutation({
    mutationFn: (testAttemptId: string | number) => submitAssignedAttempt(testAttemptId),
    mutationKey: assignedAttemptQueryKeys.submit('unknown'),
  });
}
