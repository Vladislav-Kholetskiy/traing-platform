import { useQuery } from '@tanstack/react-query';
import { getManagerialCurrentSupervision } from '../api/managerialApi';
import {
  getManagerialDepartmentTopicAnalytics,
  getManagerialUserTopicAnalytics,
} from '../api/managerialAnalyticsApi';

export const managerialQueryKeys = {
  all: ['managerial'] as const,
  currentSupervision: () => [...managerialQueryKeys.all, 'current-supervision'] as const,
  userTopicAnalytics: (periodStart: string, periodEnd: string) =>
    [...managerialQueryKeys.all, 'user-topic-analytics', periodStart, periodEnd] as const,
  departmentTopicAnalytics: (periodStart: string, periodEnd: string) =>
    [...managerialQueryKeys.all, 'department-topic-analytics', periodStart, periodEnd] as const,
};

export function useManagerialCurrentSupervision(enabled = true) {
  return useQuery({
    queryKey: managerialQueryKeys.currentSupervision(),
    queryFn: getManagerialCurrentSupervision,
    enabled,
  });
}

export function useManagerialUserTopicAnalytics(
  periodStart?: string,
  periodEnd?: string,
  enabled = true,
) {
  return useQuery({
    queryKey: managerialQueryKeys.userTopicAnalytics(periodStart ?? 'unknown', periodEnd ?? 'unknown'),
    queryFn: () =>
      getManagerialUserTopicAnalytics({
        periodStart: periodStart as string,
        periodEnd: periodEnd as string,
      }),
    enabled: enabled && Boolean(periodStart && periodEnd),
  });
}

export function useManagerialDepartmentTopicAnalytics(
  periodStart?: string,
  periodEnd?: string,
  enabled = true,
) {
  return useQuery({
    queryKey: managerialQueryKeys.departmentTopicAnalytics(
      periodStart ?? 'unknown',
      periodEnd ?? 'unknown',
    ),
    queryFn: () =>
      getManagerialDepartmentTopicAnalytics({
        periodStart: periodStart as string,
        periodEnd: periodEnd as string,
      }),
    enabled: enabled && Boolean(periodStart && periodEnd),
  });
}
