import { apiClient } from '../../../shared/api/apiClient';
import {
  mapAssignedAttemptAnswerMutationResult,
  mapAssignedAttemptEntry,
  mapAssignedAttemptSubmitResult,
  mapCurrentAssignedAttempt,
  type AssignedAttempt,
  type AssignedAttemptAnswerMutationRequest,
  type AssignedAttemptAnswerMutationRequestDto,
  type AssignedAttemptAnswerMutationResponseDto,
  type AssignedAttemptAnswerMutationResult,
  type AssignedAttemptEntryDto,
  type AssignedAttemptSubmitResponseDto,
  type AssignedAttemptSubmitResult,
  type CurrentAssignedAttemptDto,
} from '../model/assignedAttempt';

export async function getCurrentAssignedAttempt(
  assignmentId: string | number,
  assignmentTestId: string | number,
): Promise<AssignedAttempt> {
  const response = await apiClient.get<CurrentAssignedAttemptDto>(
    `/api/v1/current-attempts/assigned/assignments/${assignmentId}/assignment-tests/${assignmentTestId}`,
  );

  return mapCurrentAssignedAttempt(response);
}

export async function startOrContinueAssignedAttempt(
  assignmentId: string | number,
  assignmentTestId: string | number,
): Promise<AssignedAttempt> {
  const response = await apiClient.post<AssignedAttemptEntryDto>(
    `/api/v1/assigned-attempt-entries/assignments/${assignmentId}/assignment-tests/${assignmentTestId}`,
  );

  return mapAssignedAttemptEntry(response);
}

function toAssignedAttemptAnswerMutationRequestDto(
  request: AssignedAttemptAnswerMutationRequest,
): AssignedAttemptAnswerMutationRequestDto {
  return {
    answerItems: request.answerItems.map((item) => ({
      answerOptionId: item.answerOptionId ?? null,
      leftAnswerOptionId: item.leftAnswerOptionId ?? null,
      rightAnswerOptionId: item.rightAnswerOptionId ?? null,
      userOrderPosition: item.userOrderPosition ?? null,
    })),
  };
}

export async function saveAssignedAttemptAnswer(
  testAttemptId: string | number,
  questionId: string | number,
  request: AssignedAttemptAnswerMutationRequest,
): Promise<AssignedAttemptAnswerMutationResult> {
  const response = await apiClient.put<AssignedAttemptAnswerMutationResponseDto>(
    `/api/v1/assigned-attempt-answers/attempts/${testAttemptId}/questions/${questionId}`,
    toAssignedAttemptAnswerMutationRequestDto(request),
  );

  return mapAssignedAttemptAnswerMutationResult(response);
}

export async function deleteAssignedAttemptAnswer(
  testAttemptId: string | number,
  questionId: string | number,
): Promise<AssignedAttemptAnswerMutationResult> {
  const response = await apiClient.delete<AssignedAttemptAnswerMutationResponseDto>(
    `/api/v1/assigned-attempt-answers/attempts/${testAttemptId}/questions/${questionId}`,
  );

  return mapAssignedAttemptAnswerMutationResult(response);
}

export async function submitAssignedAttempt(
  testAttemptId: string | number,
): Promise<AssignedAttemptSubmitResult> {
  const response = await apiClient.post<AssignedAttemptSubmitResponseDto>(
    `/api/v1/assigned-attempt-submissions/attempts/${testAttemptId}`,
  );

  return mapAssignedAttemptSubmitResult(response);
}
