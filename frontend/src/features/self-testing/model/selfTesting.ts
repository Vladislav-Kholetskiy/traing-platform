import type { AssignedAttemptAnswerMutationRequest } from '../../assigned-attempt/model/assignedAttempt';

export type SelfVisibleTestCatalogEntryDto = {
  id?: number | null;
  courseId?: number | null;
  courseName?: string | null;
  topicId?: number | null;
  topicName?: string | null;
  name?: string | null;
  description?: string | null;
  testType?: string | null;
};

export type SelfVisibleTopicMaterialDto = {
  materialId?: number | null;
  name?: string | null;
  description?: string | null;
  body?: string | null;
  videoUrl?: string | null;
  materialType?: string | null;
  sortOrder?: number | null;
  updatedAt?: string | null;
};

export type SelfVisibleTopicDto = {
  topicId?: number | null;
  topicName?: string | null;
  topicDescription?: string | null;
  courseId?: number | null;
  courseName?: string | null;
  materials?: SelfVisibleTopicMaterialDto[] | null;
};

export type SelfVisibleTestAnswerOptionDto = {
  id?: number | null;
  body?: string | null;
  answerOptionRole?: string | null;
  displayOrder?: number | null;
};

export type SelfVisibleTestQuestionDto = {
  id?: number | null;
  body?: string | null;
  questionType?: string | null;
  displayOrder?: number | null;
  weight?: string | number | null;
  answerOptions?: SelfVisibleTestAnswerOptionDto[] | null;
};

export type SelfVisibleTestDto = {
  id?: number | null;
  topicId?: number | null;
  name?: string | null;
  description?: string | null;
  testType?: string | null;
  questions?: SelfVisibleTestQuestionDto[] | null;
};

export type SelfCurrentAttemptDto = {
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

export type SelfAttemptEntryDto = {
  testAttemptId?: number | null;
  testId?: number | null;
  attemptMode?: string | null;
  status?: string | null;
  startedAt?: string | null;
  lastActivityAt?: string | null;
};

export type SelfAttemptSubmitResponseDto = {
  testAttemptId?: number | null;
  resultId?: number | null;
};

export type SelfAttemptAbandonResponseDto = {
  testAttemptId?: number | null;
};

export type SelfVisibleTestCatalogEntry = {
  testId?: number;
  courseId?: number;
  courseName?: string;
  topicId?: number;
  topicName?: string;
  name?: string;
  description?: string;
  testType?: string;
};

export type SelfVisibleTopicMaterial = {
  materialId?: number;
  name?: string;
  description?: string;
  body?: string;
  videoUrl?: string;
  materialType?: string;
  sortOrder?: number;
  updatedAt?: string;
};

export type SelfVisibleTopic = {
  topicId?: number;
  topicName?: string;
  topicDescription?: string;
  courseId?: number;
  courseName?: string;
  materials: SelfVisibleTopicMaterial[];
};

export type SelfVisibleTestAnswerOption = {
  answerOptionId?: number;
  body?: string;
  answerOptionRole?: string;
  displayOrder?: number;
};

export type SelfVisibleTestQuestion = {
  questionId?: number;
  body?: string;
  questionType?: string;
  displayOrder?: number;
  weight?: string | number;
  answerOptions: SelfVisibleTestAnswerOption[];
};

export type SelfVisibleTest = {
  testId?: number;
  topicId?: number;
  name?: string;
  description?: string;
  testType?: string;
  questions: SelfVisibleTestQuestion[];
};

export type SelfAttempt = {
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

export type SelfAttemptSubmitResult = {
  testAttemptId?: number;
  resultId?: number;
};

export type SelfAttemptAbandonResult = {
  testAttemptId?: number;
};

export type SelfAttemptAnswerMutationRequest = AssignedAttemptAnswerMutationRequest;

export function mapSelfVisibleTestCatalogEntry(
  dto: SelfVisibleTestCatalogEntryDto,
): SelfVisibleTestCatalogEntry {
  return {
    testId: dto.id ?? undefined,
    courseId: dto.courseId ?? undefined,
    courseName: dto.courseName ?? undefined,
    topicId: dto.topicId ?? undefined,
    topicName: dto.topicName ?? undefined,
    name: dto.name ?? undefined,
    description: dto.description ?? undefined,
    testType: dto.testType ?? undefined,
  };
}

export function mapSelfVisibleTopicMaterial(
  dto: SelfVisibleTopicMaterialDto,
): SelfVisibleTopicMaterial {
  return {
    materialId: dto.materialId ?? undefined,
    name: dto.name ?? undefined,
    description: dto.description ?? undefined,
    body: dto.body ?? undefined,
    videoUrl: dto.videoUrl ?? undefined,
    materialType: dto.materialType ?? undefined,
    sortOrder: dto.sortOrder ?? undefined,
    updatedAt: dto.updatedAt ?? undefined,
  };
}

export function mapSelfVisibleTopic(dto: SelfVisibleTopicDto): SelfVisibleTopic {
  return {
    topicId: dto.topicId ?? undefined,
    topicName: dto.topicName ?? undefined,
    topicDescription: dto.topicDescription ?? undefined,
    courseId: dto.courseId ?? undefined,
    courseName: dto.courseName ?? undefined,
    materials: Array.isArray(dto.materials) ? dto.materials.map(mapSelfVisibleTopicMaterial) : [],
  };
}

export function mapSelfVisibleTestAnswerOption(
  dto: SelfVisibleTestAnswerOptionDto,
): SelfVisibleTestAnswerOption {
  return {
    answerOptionId: dto.id ?? undefined,
    body: dto.body ?? undefined,
    answerOptionRole: dto.answerOptionRole ?? undefined,
    displayOrder: dto.displayOrder ?? undefined,
  };
}

export function mapSelfVisibleTestQuestion(dto: SelfVisibleTestQuestionDto): SelfVisibleTestQuestion {
  return {
    questionId: dto.id ?? undefined,
    body: dto.body ?? undefined,
    questionType: dto.questionType ?? undefined,
    displayOrder: dto.displayOrder ?? undefined,
    weight: dto.weight ?? undefined,
    answerOptions: Array.isArray(dto.answerOptions)
      ? dto.answerOptions.map(mapSelfVisibleTestAnswerOption)
      : [],
  };
}

export function mapSelfVisibleTest(dto: SelfVisibleTestDto): SelfVisibleTest {
  return {
    testId: dto.id ?? undefined,
    topicId: dto.topicId ?? undefined,
    name: dto.name ?? undefined,
    description: dto.description ?? undefined,
    testType: dto.testType ?? undefined,
    questions: Array.isArray(dto.questions) ? dto.questions.map(mapSelfVisibleTestQuestion) : [],
  };
}

export function mapCurrentSelfAttempt(dto: SelfCurrentAttemptDto): SelfAttempt {
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

export function mapSelfAttemptEntry(dto: SelfAttemptEntryDto): SelfAttempt {
  return {
    testAttemptId: dto.testAttemptId ?? undefined,
    testId: dto.testId ?? undefined,
    attemptMode: dto.attemptMode ?? undefined,
    status: dto.status ?? undefined,
    startedAt: dto.startedAt ?? undefined,
    lastActivityAt: dto.lastActivityAt ?? undefined,
  };
}

export function mapSelfAttemptSubmitResult(
  dto: SelfAttemptSubmitResponseDto,
): SelfAttemptSubmitResult {
  return {
    testAttemptId: dto.testAttemptId ?? undefined,
    resultId: dto.resultId ?? undefined,
  };
}

export function mapSelfAttemptAbandonResult(
  dto: SelfAttemptAbandonResponseDto,
): SelfAttemptAbandonResult {
  return {
    testAttemptId: dto.testAttemptId ?? undefined,
  };
}
