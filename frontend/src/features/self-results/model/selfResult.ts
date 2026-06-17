export type SelfResultHistoryDto = {
  resultId?: number | null;
  recordedAt?: string | null;
  testAttemptId?: number | null;
  testId?: number | null;
  testName?: string | null;
  scorePercent?: string | number | null;
  score?: string | number | null;
  passed?: boolean | null;
  attemptMode?: string | null;
  assignmentId?: number | null;
};

export type SelfResultHistoryItem = {
  resultId?: number;
  recordedAt?: string;
  testAttemptId?: number;
  testId?: number;
  testName?: string;
  scorePercent?: string;
  score?: string;
  passed?: boolean;
  attemptMode?: string;
  assignmentId?: number;
};

export type SelfResultReviewOptionDto = {
  resultAnswerOptionSnapshotId?: number | null;
  answerOptionOriginalId?: number | null;
  body?: string | null;
  displayOrder?: number | null;
  correctAtSnapshot?: boolean | null;
  selectedByUser?: boolean | null;
};

export type SelfResultReviewQuestionDto = {
  resultQuestionSnapshotId?: number | null;
  questionOriginalId?: number | null;
  body?: string | null;
  questionType?: string | null;
  displayOrder?: number | null;
  earnedScore?: string | number | null;
  maxScore?: string | number | null;
  correct?: boolean | null;
  evaluationNote?: string | null;
  correctAnswerSnapshot?: string | null;
  userAnswerSnapshot?: string | null;
  answerOptions?: SelfResultReviewOptionDto[] | null;
};

export type SelfResultReviewDto = SelfResultHistoryDto & {
  questions?: SelfResultReviewQuestionDto[] | null;
};

export type SelfResultReviewOption = {
  resultAnswerOptionSnapshotId?: number;
  answerOptionOriginalId?: number;
  body?: string;
  displayOrder?: number;
  correctAtSnapshot?: boolean;
  selectedByUser?: boolean;
};

export type SelfResultReviewQuestion = {
  resultQuestionSnapshotId?: number;
  questionOriginalId?: number;
  body?: string;
  questionType?: string;
  displayOrder?: number;
  earnedScore?: string;
  maxScore?: string;
  correct?: boolean;
  evaluationNote?: string;
  correctAnswerSnapshot?: string;
  userAnswerSnapshot?: string;
  answerOptions: SelfResultReviewOption[];
};

export type SelfResultReview = SelfResultHistoryItem & {
  questions: SelfResultReviewQuestion[];
};

export function mapSelfResultHistoryItem(dto: SelfResultHistoryDto): SelfResultHistoryItem {
  return {
    resultId: dto.resultId ?? undefined,
    recordedAt: dto.recordedAt ?? undefined,
    testAttemptId: dto.testAttemptId ?? undefined,
    testId: dto.testId ?? undefined,
    testName: dto.testName ?? undefined,
    scorePercent: dto.scorePercent != null ? String(dto.scorePercent) : undefined,
    score: dto.score != null ? String(dto.score) : undefined,
    passed: dto.passed ?? undefined,
    attemptMode: dto.attemptMode ?? undefined,
    assignmentId: dto.assignmentId ?? undefined,
  };
}

export function mapSelfResultReviewOption(dto: SelfResultReviewOptionDto): SelfResultReviewOption {
  return {
    resultAnswerOptionSnapshotId: dto.resultAnswerOptionSnapshotId ?? undefined,
    answerOptionOriginalId: dto.answerOptionOriginalId ?? undefined,
    body: dto.body ?? undefined,
    displayOrder: dto.displayOrder ?? undefined,
    correctAtSnapshot: dto.correctAtSnapshot ?? undefined,
    selectedByUser: dto.selectedByUser ?? undefined,
  };
}

export function mapSelfResultReviewQuestion(dto: SelfResultReviewQuestionDto): SelfResultReviewQuestion {
  return {
    resultQuestionSnapshotId: dto.resultQuestionSnapshotId ?? undefined,
    questionOriginalId: dto.questionOriginalId ?? undefined,
    body: dto.body ?? undefined,
    questionType: dto.questionType ?? undefined,
    displayOrder: dto.displayOrder ?? undefined,
    earnedScore: dto.earnedScore != null ? String(dto.earnedScore) : undefined,
    maxScore: dto.maxScore != null ? String(dto.maxScore) : undefined,
    correct: dto.correct ?? undefined,
    evaluationNote: dto.evaluationNote ?? undefined,
    correctAnswerSnapshot: dto.correctAnswerSnapshot ?? undefined,
    userAnswerSnapshot: dto.userAnswerSnapshot ?? undefined,
    answerOptions: Array.isArray(dto.answerOptions) ? dto.answerOptions.map(mapSelfResultReviewOption) : [],
  };
}

export function mapSelfResultReview(dto: SelfResultReviewDto): SelfResultReview {
  return {
    ...mapSelfResultHistoryItem(dto),
    questions: Array.isArray(dto.questions) ? dto.questions.map(mapSelfResultReviewQuestion) : [],
  };
}
