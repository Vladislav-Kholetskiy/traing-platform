export type ContentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | string;
export type MaterialType = 'TEXT' | 'PDF' | 'DOCX' | 'VIDEO' | string;
export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'MATCHING' | 'ORDERING' | string;
export type AnswerOptionRole = 'CHOICE_OPTION' | 'MATCH_LEFT' | 'MATCH_RIGHT' | 'ORDER_ITEM' | string;
export type TestType = 'CONTROL' | 'TRAINING' | 'ENTRANCE' | 'AUXILIARY' | 'ALL_QUESTIONS' | string;

export type Course = {
  id: number;
  name: string;
  description?: string | null;
  status?: ContentStatus | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type Topic = {
  id: number;
  courseId: number;
  name: string;
  description?: string | null;
  status?: ContentStatus | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type Material = {
  id: number;
  topicId: number;
  name: string;
  description?: string | null;
  body?: string | null;
  videoUrl?: string | null;
  materialType?: MaterialType | null;
  status?: ContentStatus | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type Question = {
  id: number;
  topicId: number;
  body: string;
  questionType?: QuestionType | null;
  status?: ContentStatus | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AnswerOption = {
  id: number;
  questionId: number;
  body?: string | null;
  answerOptionRole?: AnswerOptionRole | null;
  isCorrect?: boolean | null;
  displayOrder?: number | null;
  pairingKey?: string | null;
  canonicalOrderPosition?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type TestEntity = {
  id: number;
  topicId: number;
  name: string;
  description?: string | null;
  testType?: TestType | null;
  status?: ContentStatus | null;
  thresholdPercent?: string | number | null;
  scoringPolicyCode?: string | null;
  sortOrder?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type TestQuestionBinding = {
  id: number;
  testId: number;
  questionId: number;
  displayOrder?: number | null;
  weight?: string | number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type SaveCourseRequest = {
  name: string;
  description?: string;
  sortOrder?: number;
};

export type CreateTopicRequest = {
  courseId: number;
  name: string;
  description?: string;
  sortOrder?: number;
};

export type UpdateTopicRequest = {
  name: string;
  description?: string;
  sortOrder?: number;
};

export type CreateMaterialRequest = {
  topicId: number;
  name: string;
  description?: string;
  body?: string;
  videoUrl?: string;
  materialType: MaterialType;
  sortOrder?: number;
};

export type UpdateMaterialRequest = {
  name: string;
  description?: string;
  body?: string;
  videoUrl?: string;
  materialType: MaterialType;
  sortOrder?: number;
};

export type CreateQuestionRequest = {
  topicId: number;
  body: string;
  questionType: QuestionType;
  sortOrder?: number;
};

export type UpdateQuestionRequest = {
  body: string;
  questionType: QuestionType;
  sortOrder?: number;
};

export type SaveAnswerOptionRequest = {
  body?: string;
  answerOptionRole: AnswerOptionRole;
  isCorrect?: boolean;
  displayOrder?: number;
  pairingKey?: string;
  canonicalOrderPosition?: number;
};

export type CreateTestRequest = {
  topicId: number;
  name: string;
  description?: string;
  testType: TestType;
  thresholdPercent?: string | number;
  scoringPolicyCode?: string;
  sortOrder?: number;
};

export type UpdateTestRequest = {
  name: string;
  description?: string;
  testType: TestType;
  thresholdPercent?: string | number;
  scoringPolicyCode?: string;
  sortOrder?: number;
};

export type SaveTestQuestionRequest = {
  questionId: number;
  displayOrder?: number;
  weight?: string | number;
};
