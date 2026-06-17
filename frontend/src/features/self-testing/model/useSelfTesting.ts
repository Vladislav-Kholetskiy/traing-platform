import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  abandonSelfAttempt,
  deleteSelfAttemptAnswer,
  getCurrentSelfAttempt,
  getSelfTestingCatalog,
  getSelfVisibleTest,
  getSelfVisibleTopic,
  saveSelfAttemptAnswer,
  startOrContinueSelfAttempt,
  submitSelfAttempt,
} from '../api/selfTestingApi';
import type { SelfAttemptAnswerMutationRequest } from './selfTesting';

export const selfTestingQueryKeys = {
  all: ['self-testing'] as const,
  catalog: () => [...selfTestingQueryKeys.all, 'catalog'] as const,
  detail: (testId: string | number) => [...selfTestingQueryKeys.all, 'detail', String(testId)] as const,
  topic: (topicId: string | number) => [...selfTestingQueryKeys.all, 'topic', String(topicId)] as const,
  currentAttempt: (testId: string | number) =>
    [...selfTestingQueryKeys.all, 'current-attempt', String(testId)] as const,
  entry: (testId: string | number) => [...selfTestingQueryKeys.all, 'entry', String(testId)] as const,
  submit: (testAttemptId: string | number) =>
    [...selfTestingQueryKeys.all, 'submit', String(testAttemptId)] as const,
  abandon: (testAttemptId: string | number) =>
    [...selfTestingQueryKeys.all, 'abandon', String(testAttemptId)] as const,
};

export function useSelfTestingCatalog(enabled = true) {
  return useQuery({
    queryKey: selfTestingQueryKeys.catalog(),
    queryFn: getSelfTestingCatalog,
    enabled,
  });
}

export function useSelfVisibleTest(testId?: string) {
  return useQuery({
    queryKey: selfTestingQueryKeys.detail(testId ?? 'unknown'),
    queryFn: () => getSelfVisibleTest(testId as string),
    enabled: Boolean(testId),
  });
}

export function useSelfVisibleTopic(topicId?: string) {
  return useQuery({
    queryKey: selfTestingQueryKeys.topic(topicId ?? 'unknown'),
    queryFn: () => getSelfVisibleTopic(topicId as string),
    enabled: Boolean(topicId),
  });
}

export function useCurrentSelfAttempt(testId?: string) {
  return useQuery({
    queryKey: selfTestingQueryKeys.currentAttempt(testId ?? 'unknown'),
    queryFn: () => getCurrentSelfAttempt(testId as string),
    enabled: Boolean(testId),
    retry: false,
  });
}

export function useStartOrContinueSelfAttempt(testId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: selfTestingQueryKeys.entry(testId ?? 'unknown'),
    mutationFn: () => startOrContinueSelfAttempt(testId as string),
    onSuccess: (attempt) => {
      queryClient.setQueryData(selfTestingQueryKeys.currentAttempt(testId ?? 'unknown'), attempt);
    },
  });
}

type AnswerMutationVariables = {
  questionId: string | number;
  request: SelfAttemptAnswerMutationRequest;
  testAttemptId: string | number;
};

type DeleteAnswerMutationVariables = {
  questionId: string | number;
  testAttemptId: string | number;
};

export function useSaveSelfAttemptAnswer(testId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ testAttemptId, questionId, request }: AnswerMutationVariables) =>
      saveSelfAttemptAnswer(testAttemptId, questionId, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: selfTestingQueryKeys.currentAttempt(testId ?? 'unknown'),
      });
    },
  });
}

export function useDeleteSelfAttemptAnswer(testId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ testAttemptId, questionId }: DeleteAnswerMutationVariables) =>
      deleteSelfAttemptAnswer(testAttemptId, questionId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: selfTestingQueryKeys.currentAttempt(testId ?? 'unknown'),
      });
    },
  });
}

export function useSubmitSelfAttempt() {
  return useMutation({
    mutationFn: (testAttemptId: string | number) => submitSelfAttempt(testAttemptId),
    mutationKey: selfTestingQueryKeys.submit('unknown'),
  });
}

export function useAbandonSelfAttempt() {
  return useMutation({
    mutationFn: (testAttemptId: string | number) => abandonSelfAttempt(testAttemptId),
    mutationKey: selfTestingQueryKeys.abandon('unknown'),
  });
}
