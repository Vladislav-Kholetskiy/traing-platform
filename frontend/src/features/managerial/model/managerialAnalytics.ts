export type ManagerialUserTopicAnalyticsDto = {
  userId?: number | null;
  userEmployeeNumber?: string | null;
  userDisplayName?: string | null;
  topicId?: number | null;
  topicName?: string | null;
  periodStart?: string | null;
  periodEnd?: string | null;
  averageScorePercent?: string | number | null;
  passRatePercent?: string | number | null;
  attemptCount?: number | null;
  errorCount?: number | null;
  calculatedAt?: string | null;
  refreshedAt?: string | null;
};

export type ManagerialDepartmentTopicAnalyticsDto = {
  organizationalUnitIdSnapshot?: number | null;
  organizationalUnitName?: string | null;
  organizationalPathSnapshot?: string | null;
  topicId?: number | null;
  topicName?: string | null;
  periodStart?: string | null;
  periodEnd?: string | null;
  averageScorePercent?: string | number | null;
  passRatePercent?: string | number | null;
  attemptCount?: number | null;
  errorCount?: number | null;
  calculatedAt?: string | null;
  refreshedAt?: string | null;
};

export type ManagerialUserTopicAnalyticsItem = {
  userId?: number;
  userEmployeeNumber?: string;
  userDisplayName?: string;
  topicId?: number;
  topicName?: string;
  periodStart?: string;
  periodEnd?: string;
  averageScorePercent?: string;
  passRatePercent?: string;
  attemptCount?: number;
  errorCount?: number;
  calculatedAt?: string;
  refreshedAt?: string;
};

export type ManagerialDepartmentTopicAnalyticsItem = {
  organizationalUnitIdSnapshot?: number;
  organizationalUnitName?: string;
  organizationalPathSnapshot?: string;
  topicId?: number;
  topicName?: string;
  periodStart?: string;
  periodEnd?: string;
  averageScorePercent?: string;
  passRatePercent?: string;
  attemptCount?: number;
  errorCount?: number;
  calculatedAt?: string;
  refreshedAt?: string;
};

export function mapManagerialUserTopicAnalyticsItem(
  dto: ManagerialUserTopicAnalyticsDto,
): ManagerialUserTopicAnalyticsItem {
  return {
    userId: dto.userId ?? undefined,
    userEmployeeNumber: dto.userEmployeeNumber ?? undefined,
    userDisplayName: dto.userDisplayName ?? undefined,
    topicId: dto.topicId ?? undefined,
    topicName: dto.topicName ?? undefined,
    periodStart: dto.periodStart ?? undefined,
    periodEnd: dto.periodEnd ?? undefined,
    averageScorePercent:
      dto.averageScorePercent != null ? String(dto.averageScorePercent) : undefined,
    passRatePercent: dto.passRatePercent != null ? String(dto.passRatePercent) : undefined,
    attemptCount: dto.attemptCount ?? undefined,
    errorCount: dto.errorCount ?? undefined,
    calculatedAt: dto.calculatedAt ?? undefined,
    refreshedAt: dto.refreshedAt ?? undefined,
  };
}

export function mapManagerialDepartmentTopicAnalyticsItem(
  dto: ManagerialDepartmentTopicAnalyticsDto,
): ManagerialDepartmentTopicAnalyticsItem {
  return {
    organizationalUnitIdSnapshot: dto.organizationalUnitIdSnapshot ?? undefined,
    organizationalUnitName: dto.organizationalUnitName ?? undefined,
    organizationalPathSnapshot: dto.organizationalPathSnapshot ?? undefined,
    topicId: dto.topicId ?? undefined,
    topicName: dto.topicName ?? undefined,
    periodStart: dto.periodStart ?? undefined,
    periodEnd: dto.periodEnd ?? undefined,
    averageScorePercent:
      dto.averageScorePercent != null ? String(dto.averageScorePercent) : undefined,
    passRatePercent: dto.passRatePercent != null ? String(dto.passRatePercent) : undefined,
    attemptCount: dto.attemptCount ?? undefined,
    errorCount: dto.errorCount ?? undefined,
    calculatedAt: dto.calculatedAt ?? undefined,
    refreshedAt: dto.refreshedAt ?? undefined,
  };
}
