import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getExpertQuestionAnalytics, rebuildAnalytics } from '../api/expertAnalyticsApi';
import type { AnalyticsRebuildRequest } from './expertAnalytics';
import { managerialQueryKeys } from '../../managerial/model/useManagerial';

export const expertAnalyticsKeys = {
  all: ['expert-analytics'] as const,
  questionAnalytics: (periodStart: string, periodEnd: string) =>
    [...expertAnalyticsKeys.all, 'question-analytics', periodStart, periodEnd] as const,
};

export function useExpertQuestionAnalytics(periodStart: string, periodEnd: string, enabled = true) {
  return useQuery({
    queryKey: expertAnalyticsKeys.questionAnalytics(periodStart, periodEnd),
    queryFn: () => getExpertQuestionAnalytics(periodStart, periodEnd),
    enabled,
  });
}

export function useAnalyticsRebuildMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: AnalyticsRebuildRequest) => rebuildAnalytics(payload),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: expertAnalyticsKeys.all }),
        queryClient.invalidateQueries({ queryKey: managerialQueryKeys.all }),
      ]);
    },
  });
}
