export type CurrentAssignedAttemptDto = {
  id?: number | null;
  userId?: number | null;
  testId?: number | null;
  assignmentTestId?: number | null;
  attemptMode?: string | null;
  status?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  expiredAt?: string | null;
  abandonedAt?: string | null;
  lastActivityAt?: string | null;
};

export type AssignedAttemptEntryDto = {
  testAttemptId?: number | null;
  assignmentTestId?: number | null;
  testId?: number | null;
  attemptMode?: string | null;
  status?: string | null;
  startedAt?: string | null;
  lastActivityAt?: string | null;
};

export type AssignedAttemptAnswerItemRequestDto = {
  answerOptionId?: number | null;
  leftAnswerOptionId?: number | null;
  rightAnswerOptionId?: number | null;
  userOrderPosition?: number | null;
};

export type AssignedAttemptAnswerMutationRequestDto = {
  answerItems: AssignedAttemptAnswerItemRequestDto[];
};

export type AssignedAttemptAnswerMutationResponseDto = {
  testAttemptId?: number | null;
  questionId?: number | null;
  status?: string | null;
  lastActivityAt?: string | null;
};

export type AssignedAttemptSubmitResponseDto = {
  testAttemptId?: number | null;
  status?: string | null;
  resultId?: number | null;
};

export type AssignedAttempt = {
  testAttemptId?: number;
  userId?: number;
  testId?: number;
  assignmentTestId?: number;
  attemptMode?: string;
  status?: string;
  startedAt?: string;
  completedAt?: string;
  expiredAt?: string;
  abandonedAt?: string;
  lastActivityAt?: string;
};

export type AssignedAttemptAnswerItemRequest = {
  answerOptionId?: number;
  leftAnswerOptionId?: number;
  rightAnswerOptionId?: number;
  userOrderPosition?: number;
};

export type AssignedAttemptAnswerMutationRequest = {
  answerItems: AssignedAttemptAnswerItemRequest[];
};

export type AssignedAttemptAnswerMutationResult = {
  testAttemptId?: number;
  questionId?: number;
  status?: string;
  lastActivityAt?: string;
};

export type AssignedAttemptSubmitResult = {
  testAttemptId?: number;
  status?: string;
  resultId?: number;
};

export function mapCurrentAssignedAttempt(dto: CurrentAssignedAttemptDto): AssignedAttempt {
  return {
    testAttemptId: dto.id ?? undefined,
    userId: dto.userId ?? undefined,
    testId: dto.testId ?? undefined,
    assignmentTestId: dto.assignmentTestId ?? undefined,
    attemptMode: dto.attemptMode ?? undefined,
    status: dto.status ?? undefined,
    startedAt: dto.startedAt ?? undefined,
    completedAt: dto.completedAt ?? undefined,
    expiredAt: dto.expiredAt ?? undefined,
    abandonedAt: dto.abandonedAt ?? undefined,
    lastActivityAt: dto.lastActivityAt ?? undefined,
  };
}

export function mapAssignedAttemptEntry(dto: AssignedAttemptEntryDto): AssignedAttempt {
  return {
    testAttemptId: dto.testAttemptId ?? undefined,
    assignmentTestId: dto.assignmentTestId ?? undefined,
    testId: dto.testId ?? undefined,
    attemptMode: dto.attemptMode ?? undefined,
    status: dto.status ?? undefined,
    startedAt: dto.startedAt ?? undefined,
    lastActivityAt: dto.lastActivityAt ?? undefined,
  };
}

export function mapAssignedAttemptAnswerMutationResult(
  dto: AssignedAttemptAnswerMutationResponseDto,
): AssignedAttemptAnswerMutationResult {
  return {
    testAttemptId: dto.testAttemptId ?? undefined,
    questionId: dto.questionId ?? undefined,
    status: dto.status ?? undefined,
    lastActivityAt: dto.lastActivityAt ?? undefined,
  };
}

export function mapAssignedAttemptSubmitResult(
  dto: AssignedAttemptSubmitResponseDto,
): AssignedAttemptSubmitResult {
  return {
    testAttemptId: dto.testAttemptId ?? undefined,
    status: dto.status ?? undefined,
    resultId: dto.resultId ?? undefined,
  };
}
