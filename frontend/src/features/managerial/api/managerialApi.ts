import { apiClient } from '../../../shared/api/apiClient';
import {
  mapManagerialCurrentSupervisionItem,
  type ManagerialCurrentSupervisionDto,
  type ManagerialCurrentSupervisionItem,
} from '../model/managerialSupervision';

export async function getManagerialCurrentSupervision(): Promise<ManagerialCurrentSupervisionItem[]> {
  const response = await apiClient.get<ManagerialCurrentSupervisionDto[]>(
    '/api/v1/managerial/current-supervision',
  );

  return Array.isArray(response) ? response.map(mapManagerialCurrentSupervisionItem) : [];
}
