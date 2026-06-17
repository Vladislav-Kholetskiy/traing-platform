import { useQuery } from '@tanstack/react-query';
import { getCurrentActor } from '../api/currentActorApi';

export function useCurrentActor() {
  return useQuery({
    queryKey: ['current-actor'],
    queryFn: getCurrentActor,
  });
}
