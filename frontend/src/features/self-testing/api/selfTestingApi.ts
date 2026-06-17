import { apiClient } from '../../../shared/api/apiClient';
import {
  mapCurrentSelfAttempt,
  mapSelfAttemptAbandonResult,
  mapSelfAttemptEntry,
  mapSelfAttemptSubmitResult,
  mapSelfVisibleTest,
  mapSelfVisibleTestCatalogEntry,
  mapSelfVisibleTopic,
  type SelfAttempt,
  type SelfAttemptAbandonResponseDto,
  type SelfAttemptAnswerMutationRequest,
  type SelfAttemptEntryDto,
  type SelfAttemptSubmitResponseDto,
  type SelfCurrentAttemptDto,
  type SelfVisibleTest,
  type SelfVisibleTestCatalogEntry,
  type SelfVisibleTestCatalogEntryDto,
  type SelfVisibleTestDto,
  type SelfVisibleTopic,
  type SelfVisibleTopicDto,
} from '../model/selfTesting';
import {
  mapAssignedAttemptAnswerMutationResult,
  type AssignedAttemptAnswerMutationResult,
  type AssignedAttemptAnswerMutationResponseDto,
} from '../../assigned-attempt/model/assignedAttempt';

export async function getSelfTestingCatalog(): Promise<SelfVisibleTestCatalogEntry[]> {
  const response = await apiClient.get<SelfVisibleTestCatalogEntryDto[]>('/api/v1/self-testing/tests');
  return Array.isArray(response) ? response.map(mapSelfVisibleTestCatalogEntry) : [];
}

export async function getSelfVisibleTest(testId: string | number): Promise<SelfVisibleTest> {
  const response = await apiClient.get<SelfVisibleTestDto>(`/api/v1/self-testing/tests/${testId}`);
  return mapSelfVisibleTest(response);
}

export async function getSelfVisibleTopic(topicId: string | number): Promise<SelfVisibleTopic> {
  const response = await apiClient.get<SelfVisibleTopicDto>(`/api/v1/self-testing/tests/topics/${topicId}`);
  return mapSelfVisibleTopic(response);
}

export async function getCurrentSelfAttempt(testId: string | number): Promise<SelfAttempt> {
  const response = await apiClient.get<SelfCurrentAttemptDto>(`/api/v1/current-attempts/self/tests/${testId}`);
  return mapCurrentSelfAttempt(response);
}

export async function startOrContinueSelfAttempt(testId: string | number): Promise<SelfAttempt> {
  const response = await apiClient.post<SelfAttemptEntryDto>(`/api/v1/self-attempt-entries/tests/${testId}`);
  return mapSelfAttemptEntry(response);
}

export async function saveSelfAttemptAnswer(
  testAttemptId: string | number,
  questionId: string | number,
  request: SelfAttemptAnswerMutationRequest,
): Promise<AssignedAttemptAnswerMutationResult> {
  const response = await apiClient.put<AssignedAttemptAnswerMutationResponseDto>(
    `/api/v1/self-attempt-answers/attempts/${testAttemptId}/questions/${questionId}`,
    request,
  );

  return mapAssignedAttemptAnswerMutationResult(response);
}

export async function deleteSelfAttemptAnswer(
  testAttemptId: string | number,
  questionId: string | number,
): Promise<AssignedAttemptAnswerMutationResult> {
  const response = await apiClient.delete<AssignedAttemptAnswerMutationResponseDto>(
    `/api/v1/self-attempt-answers/attempts/${testAttemptId}/questions/${questionId}`,
  );

  return mapAssignedAttemptAnswerMutationResult(response);
}

export async function submitSelfAttempt(testAttemptId: string | number) {
  const response = await apiClient.post<SelfAttemptSubmitResponseDto>(
    `/api/v1/self-attempt-submissions/attempts/${testAttemptId}`,
  );

  return mapSelfAttemptSubmitResult(response);
}

export async function abandonSelfAttempt(testAttemptId: string | number) {
  const response = await apiClient.post<SelfAttemptAbandonResponseDto>(
    `/api/v1/self-attempt-abandonments/attempts/${testAttemptId}`,
  );

  return mapSelfAttemptAbandonResult(response);
}
