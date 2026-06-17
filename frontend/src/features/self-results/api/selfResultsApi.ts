import { apiClient } from '../../../shared/api/apiClient';
import {
  mapSelfResultHistoryItem,
  mapSelfResultReview,
  type SelfResultHistoryDto,
  type SelfResultHistoryItem,
  type SelfResultReview,
  type SelfResultReviewDto,
} from '../model/selfResult';

export async function getSelfResultHistory(): Promise<SelfResultHistoryItem[]> {
  const response = await apiClient.get<SelfResultHistoryDto[]>('/api/v1/self/results/history');

  return Array.isArray(response) ? response.map(mapSelfResultHistoryItem) : [];
}

export async function getSelfResultReview(resultId: number): Promise<SelfResultReview> {
  const response = await apiClient.get<SelfResultReviewDto>(`/api/v1/self/result-review/${resultId}`);
  return mapSelfResultReview(response);
}
