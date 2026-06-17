import { apiClient } from '../../../shared/api/apiClient';
import type {
  AnalyticsRebuildRequest,
  AnalyticsRebuildResponse,
  ExpertQuestionAnalyticsItem,
} from '../model/expertAnalytics';

function buildPeriodUrl(path: string, periodStart: string, periodEnd: string) {
  const params = new URLSearchParams({ periodStart, periodEnd });
  return `${path}?${params.toString()}`;
}

export function getExpertQuestionAnalytics(periodStart: string, periodEnd: string) {
  return apiClient.get<ExpertQuestionAnalyticsItem[]>(
    buildPeriodUrl('/api/v1/expert/question-analytics', periodStart, periodEnd),
  );
}

export function rebuildAnalytics(payload: AnalyticsRebuildRequest) {
  return apiClient.post<AnalyticsRebuildResponse>('/api/v1/admin/analytics/result-rebuild', payload);
}
