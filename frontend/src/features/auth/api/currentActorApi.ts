import { apiClient } from '../../../shared/api/apiClient';
import { mapCurrentActor, type CurrentActorResponse } from '../model/currentActor';

export async function getCurrentActor() {
  const response = await apiClient.get<CurrentActorResponse>('/api/v1/me');
  return mapCurrentActor(response);
}
