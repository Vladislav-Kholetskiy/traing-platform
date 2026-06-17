import dayjs from 'dayjs';

export type AssignedLearningAssignmentDto = {
  id?: number | null;
  campaignId?: number | null;
  userId?: number | null;
  courseId?: number | null;
  courseName?: string | null;
  status?: string | null;
  assignedAt?: string | null;
  deadlineAt?: string | null;
  cancelledAt?: string | null;
  closedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AssignedLearningAssignmentTestDto = {
  id?: number | null;
  assignmentId?: number | null;
  testId?: number | null;
  testName?: string | null;
  topicName?: string | null;
  assignmentTestRole?: string | null;
  countedResultId?: number | null;
  closedAt?: string | null;
  isClosed?: boolean | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AssignedLearningCourseDto = {
  id?: number | null;
  name?: string | null;
  description?: string | null;
  status?: string | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AssignedLearningTopicDto = {
  id?: number | null;
  courseId?: number | null;
  name?: string | null;
  description?: string | null;
  status?: string | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AssignedLearningMaterialDto = {
  id?: number | null;
  topicId?: number | null;
  name?: string | null;
  description?: string | null;
  body?: string | null;
  videoUrl?: string | null;
  materialType?: string | null;
  status?: string | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AssignedMaterialContentDto = {
  assignmentId?: number | null;
  publishedCourse?: AssignedLearningCourseDto | null;
  publishedTopic?: AssignedLearningTopicDto | null;
  publishedMaterial?: AssignedLearningMaterialDto | null;
};

export type AssignedLearningContextDto = {
  assignment?: AssignedLearningAssignmentDto | null;
  assignmentTests?: AssignedLearningAssignmentTestDto[] | null;
  publishedCourse?: AssignedLearningCourseDto | null;
  publishedTopics?: AssignedLearningTopicDto[] | null;
  publishedMaterials?: AssignedLearningMaterialDto[] | null;
};

export type AssignedTestContextAnswerOptionDto = {
  answerOptionId?: number | null;
  body?: string | null;
  answerOptionRole?: string | null;
  displayOrder?: number | null;
  isCorrect?: boolean | null;
  correct?: boolean | null;
  correctAnswer?: string | null;
  scoringKey?: string | null;
  canonicalOrderPosition?: number | null;
};

export type AssignedTestContextQuestionDto = {
  questionId?: number | null;
  body?: string | null;
  questionType?: string | null;
  displayOrder?: number | null;
  answerOptions?: AssignedTestContextAnswerOptionDto[] | null;
};

export type AssignedTestContextDto = {
  assignmentId?: number | null;
  assignmentTestId?: number | null;
  testId?: number | null;
  testName?: string | null;
  questions?: AssignedTestContextQuestionDto[] | null;
};

export type AssignedLearningAssignment = {
  assignmentId?: number;
  campaignId?: number;
  userId?: number;
  courseId?: number;
  courseName?: string;
  status?: string;
  assignedAt?: string;
  deadlineAt?: string;
  cancelledAt?: string;
  closedAt?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type AssignedLearningAssignmentTest = {
  assignmentTestId?: number;
  assignmentId?: number;
  testId?: number;
  testName?: string;
  topicName?: string;
  assignmentTestRole?: string;
  countedResultId?: number;
  closedAt?: string;
  isClosed: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type AssignedLearningCourse = {
  courseId?: number;
  name?: string;
  description?: string;
  status?: string;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type AssignedLearningTopic = {
  topicId?: number;
  courseId?: number;
  name?: string;
  description?: string;
  status?: string;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type AssignedLearningMaterial = {
  materialId?: number;
  topicId?: number;
  name?: string;
  description?: string;
  body?: string;
  videoUrl?: string;
  materialType?: string;
  status?: string;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type AssignedMaterialContent = {
  assignmentId?: number;
  publishedCourse?: AssignedLearningCourse;
  publishedTopic?: AssignedLearningTopic;
  publishedMaterial?: AssignedLearningMaterial;
};

export type AssignedLearningContext = {
  assignment?: AssignedLearningAssignment;
  assignmentTests: AssignedLearningAssignmentTest[];
  publishedCourse?: AssignedLearningCourse;
  publishedTopics: AssignedLearningTopic[];
  publishedMaterials: AssignedLearningMaterial[];
};

export type AssignedTestContextAnswerOption = {
  answerOptionId?: number;
  body?: string;
  answerOptionRole?: string;
  displayOrder?: number;
};

export type AssignedTestContextQuestion = {
  questionId?: number;
  body?: string;
  questionType?: string;
  displayOrder?: number;
  answerOptions: AssignedTestContextAnswerOption[];
};

export type AssignedTestContext = {
  assignmentId?: number;
  assignmentTestId?: number;
  testId?: number;
  testName?: string;
  questions: AssignedTestContextQuestion[];
};

export function mapAssignedLearningAssignment(
  dto: AssignedLearningAssignmentDto,
): AssignedLearningAssignment {
  return {
    assignmentId: dto.id ?? undefined,
    campaignId: dto.campaignId ?? undefined,
    userId: dto.userId ?? undefined,
    courseId: dto.courseId ?? undefined,
    courseName: dto.courseName ?? undefined,
    status: dto.status ?? undefined,
    assignedAt: dto.assignedAt ?? undefined,
    deadlineAt: dto.deadlineAt ?? undefined,
    cancelledAt: dto.cancelledAt ?? undefined,
    closedAt: dto.closedAt ?? undefined,
    createdAt: dto.createdAt ?? undefined,
    updatedAt: dto.updatedAt ?? undefined,
  };
}

export function mapAssignedLearningAssignmentTest(
  dto: AssignedLearningAssignmentTestDto,
): AssignedLearningAssignmentTest {
  return {
    assignmentTestId: dto.id ?? undefined,
    assignmentId: dto.assignmentId ?? undefined,
    testId: dto.testId ?? undefined,
    testName: dto.testName ?? undefined,
    topicName: dto.topicName ?? undefined,
    assignmentTestRole: dto.assignmentTestRole ?? undefined,
    countedResultId: dto.countedResultId ?? undefined,
    closedAt: dto.closedAt ?? undefined,
    isClosed: dto.isClosed ?? false,
    createdAt: dto.createdAt ?? undefined,
    updatedAt: dto.updatedAt ?? undefined,
  };
}

export function mapAssignedLearningCourse(
  dto: AssignedLearningCourseDto,
): AssignedLearningCourse {
  return {
    courseId: dto.id ?? undefined,
    name: dto.name ?? undefined,
    description: dto.description ?? undefined,
    status: dto.status ?? undefined,
    sortOrder: dto.sortOrder ?? undefined,
    createdAt: dto.createdAt ?? undefined,
    updatedAt: dto.updatedAt ?? undefined,
  };
}

export function mapAssignedLearningTopic(dto: AssignedLearningTopicDto): AssignedLearningTopic {
  return {
    topicId: dto.id ?? undefined,
    courseId: dto.courseId ?? undefined,
    name: dto.name ?? undefined,
    description: dto.description ?? undefined,
    status: dto.status ?? undefined,
    sortOrder: dto.sortOrder ?? undefined,
    createdAt: dto.createdAt ?? undefined,
    updatedAt: dto.updatedAt ?? undefined,
  };
}

export function mapAssignedLearningMaterial(
  dto: AssignedLearningMaterialDto,
): AssignedLearningMaterial {
  return {
    materialId: dto.id ?? undefined,
    topicId: dto.topicId ?? undefined,
    name: dto.name ?? undefined,
    description: dto.description ?? undefined,
    body: dto.body ?? undefined,
    videoUrl: dto.videoUrl ?? undefined,
    materialType: dto.materialType ?? undefined,
    status: dto.status ?? undefined,
    sortOrder: dto.sortOrder ?? undefined,
    createdAt: dto.createdAt ?? undefined,
    updatedAt: dto.updatedAt ?? undefined,
  };
}

export function mapAssignedLearningContext(dto: AssignedLearningContextDto): AssignedLearningContext {
  return {
    assignment: dto.assignment ? mapAssignedLearningAssignment(dto.assignment) : undefined,
    assignmentTests: Array.isArray(dto.assignmentTests)
      ? dto.assignmentTests.map(mapAssignedLearningAssignmentTest)
      : [],
    publishedCourse: dto.publishedCourse ? mapAssignedLearningCourse(dto.publishedCourse) : undefined,
    publishedTopics: Array.isArray(dto.publishedTopics)
      ? dto.publishedTopics.map(mapAssignedLearningTopic)
      : [],
    publishedMaterials: Array.isArray(dto.publishedMaterials)
      ? dto.publishedMaterials.map(mapAssignedLearningMaterial)
      : [],
  };
}

export function mapAssignedMaterialContent(dto: AssignedMaterialContentDto): AssignedMaterialContent {
  return {
    assignmentId: dto.assignmentId ?? undefined,
    publishedCourse: dto.publishedCourse ? mapAssignedLearningCourse(dto.publishedCourse) : undefined,
    publishedTopic: dto.publishedTopic ? mapAssignedLearningTopic(dto.publishedTopic) : undefined,
    publishedMaterial: dto.publishedMaterial
      ? mapAssignedLearningMaterial(dto.publishedMaterial)
      : undefined,
  };
}

export function mapAssignedTestContextAnswerOption(
  dto: AssignedTestContextAnswerOptionDto,
): AssignedTestContextAnswerOption {
  return {
    answerOptionId: dto.answerOptionId ?? undefined,
    body: dto.body ?? undefined,
    answerOptionRole: dto.answerOptionRole ?? undefined,
    displayOrder: dto.displayOrder ?? undefined,
  };
}

export function mapAssignedTestContextQuestion(
  dto: AssignedTestContextQuestionDto,
): AssignedTestContextQuestion {
  return {
    questionId: dto.questionId ?? undefined,
    body: dto.body ?? undefined,
    questionType: dto.questionType ?? undefined,
    displayOrder: dto.displayOrder ?? undefined,
    answerOptions: Array.isArray(dto.answerOptions)
      ? dto.answerOptions.map(mapAssignedTestContextAnswerOption)
      : [],
  };
}

export function mapAssignedTestContext(dto: AssignedTestContextDto): AssignedTestContext {
  return {
    assignmentId: dto.assignmentId ?? undefined,
    assignmentTestId: dto.assignmentTestId ?? undefined,
    testId: dto.testId ?? undefined,
    testName: dto.testName ?? undefined,
    questions: Array.isArray(dto.questions) ? dto.questions.map(mapAssignedTestContextQuestion) : [],
  };
}

export function formatAssignmentDate(value?: string): string {
  if (!value) {
    return 'Не указано';
  }

  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('DD.MM.YYYY HH:mm') : value;
}

export function getAssignmentTitle(assignment: AssignedLearningAssignment): string {
  if (assignment.courseName?.trim()) {
    return assignment.courseName.trim();
  }

  if (assignment.status === 'COMPLETED' || assignment.status === 'CLOSED') {
    return 'Завершённый курс';
  }

  if (assignment.status === 'OVERDUE') {
    return 'Курс с истекшим сроком';
  }

  return 'Назначенный курс';
}
