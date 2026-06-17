export type ExpertQuestionAnalyticsItem = {
  questionId: number;
  periodStart?: string | null;
  periodEnd?: string | null;
  attemptCount?: number | null;
  correctCount?: number | null;
  incorrectCount?: number | null;
  averageEarnedScore?: string | number | null;
  calculatedAt?: string | null;
  refreshedAt?: string | null;
};

export type AnalyticsRebuildRequest = {
  periodStart: string;
  periodEnd: string;
};

export type AnalyticsRebuildResponse = {
  periodStart?: string | null;
  periodEnd?: string | null;
  sourceRowCount?: number | null;
  supportedTopicRowCount?: number | null;
  unsupportedTopicRowCount?: number | null;
  userTopicAggregateRowCount?: number | null;
  departmentTopicAggregateRowCount?: number | null;
  questionAggregateRowCount?: number | null;
};
