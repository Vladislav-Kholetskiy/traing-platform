import { useQuery } from '@tanstack/react-query';
import { getSelfResultHistory, getSelfResultReview } from '../api/selfResultsApi';

export const selfResultsQueryKeys = {
  all: ['self-results'] as const,
  history: () => [...selfResultsQueryKeys.all, 'history'] as const,
  detail: (resultId?: number) => [...selfResultsQueryKeys.all, 'detail', resultId ?? 'missing'] as const,
};

export function useSelfResultHistory(enabled = true) {
  return useQuery({
    queryKey: selfResultsQueryKeys.history(),
    queryFn: getSelfResultHistory,
    enabled,
  });
}

export function useSelfResultReview(resultId?: number, enabled = true) {
  return useQuery({
    queryKey: selfResultsQueryKeys.detail(resultId),
    queryFn: () => getSelfResultReview(resultId as number),
    enabled: enabled && resultId != null,
  });
}
