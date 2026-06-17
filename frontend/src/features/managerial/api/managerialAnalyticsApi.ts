import { apiClient } from '../../../shared/api/apiClient';
import {
  mapManagerialDepartmentTopicAnalyticsItem,
  mapManagerialUserTopicAnalyticsItem,
  type ManagerialDepartmentTopicAnalyticsDto,
  type ManagerialDepartmentTopicAnalyticsItem,
  type ManagerialUserTopicAnalyticsDto,
  type ManagerialUserTopicAnalyticsItem,
} from '../model/managerialAnalytics';

type AnalyticsPeriodRequest = {
  periodEnd: string;
  periodStart: string;
};

function buildAnalyticsPath(
  basePath: string,
  period: AnalyticsPeriodRequest,
): string {
  const params = new URLSearchParams({
    periodStart: period.periodStart,
    periodEnd: period.periodEnd,
  });

  return `${basePath}?${params.toString()}`;
}

export async function getManagerialUserTopicAnalytics(
  period: AnalyticsPeriodRequest,
): Promise<ManagerialUserTopicAnalyticsItem[]> {
  const response = await apiClient.get<ManagerialUserTopicAnalyticsDto[]>(
    buildAnalyticsPath('/api/v1/managerial/historical-analytics/user-topic', period),
  );

  return Array.isArray(response) ? response.map(mapManagerialUserTopicAnalyticsItem) : [];
}

export async function getManagerialDepartmentTopicAnalytics(
  period: AnalyticsPeriodRequest,
): Promise<ManagerialDepartmentTopicAnalyticsItem[]> {
  const response = await apiClient.get<ManagerialDepartmentTopicAnalyticsDto[]>(
    buildAnalyticsPath('/api/v1/managerial/historical-analytics/department-topic', period),
  );

  return Array.isArray(response) ? response.map(mapManagerialDepartmentTopicAnalyticsItem) : [];
}
