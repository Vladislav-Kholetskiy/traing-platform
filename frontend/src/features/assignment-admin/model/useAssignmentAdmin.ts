import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  cancelAssignment,
  extendAssignmentDeadline,
  getAssignmentCampaigns,
  getAssignments,
  getAssignmentCampaignTargetUnits,
  launchAssignmentCampaign,
  launchIndividualAssignment,
  replaceAssignmentWithNew,
} from '../api/assignmentAdminApi';
import type {
  AssignmentCampaign,
  AssignmentCampaignFilter,
  AssignmentCampaignTargetUnit,
  AssignmentFilter,
  AssignmentRecord,
  CancelAssignmentRequest,
  ExtendAssignmentDeadlineRequest,
  LaunchAssignmentCampaignRequest,
  LaunchIndividualAssignmentRequest,
  ReplaceAssignmentWithNewRequest,
} from './assignmentAdmin';

export const assignmentAdminKeys = {
  all: ['assignment-admin'] as const,
  campaigns: (filter?: AssignmentCampaignFilter) => [...assignmentAdminKeys.all, 'campaigns', filter ?? {}] as const,
  assignments: (filter?: AssignmentFilter) => [...assignmentAdminKeys.all, 'assignments', filter ?? {}] as const,
  targetUnits: () => [...assignmentAdminKeys.all, 'target-units'] as const,
};

export function useAssignmentCampaigns(filter?: AssignmentCampaignFilter, enabled = true) {
  return useQuery({
    queryKey: assignmentAdminKeys.campaigns(filter),
    queryFn: (): Promise<AssignmentCampaign[]> => getAssignmentCampaigns(filter),
    enabled,
  });
}

export function useAssignments(filter?: AssignmentFilter, enabled = true) {
  return useQuery({
    queryKey: assignmentAdminKeys.assignments(filter),
    queryFn: (): Promise<AssignmentRecord[]> => getAssignments(filter),
    enabled,
  });
}

export function useAssignmentCampaignTargetUnits(enabled = true) {
  return useQuery({
    queryKey: assignmentAdminKeys.targetUnits(),
    queryFn: (): Promise<AssignmentCampaignTargetUnit[]> => getAssignmentCampaignTargetUnits(),
    enabled,
  });
}

export function useLaunchAssignmentCampaignMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: LaunchAssignmentCampaignRequest) => launchAssignmentCampaign(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: assignmentAdminKeys.all });
    },
  });
}

export function useLaunchIndividualAssignmentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: LaunchIndividualAssignmentRequest) => launchIndividualAssignment(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: assignmentAdminKeys.all });
    },
  });
}

export function useCancelAssignmentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { assignmentId: number; values: CancelAssignmentRequest }) =>
      cancelAssignment(payload.assignmentId, payload.values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: assignmentAdminKeys.all });
    },
  });
}

export function useExtendAssignmentDeadlineMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { assignmentId: number; values: ExtendAssignmentDeadlineRequest }) =>
      extendAssignmentDeadline(payload.assignmentId, payload.values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: assignmentAdminKeys.all });
    },
  });
}

export function useReplaceAssignmentWithNewMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { assignmentId: number; values: ReplaceAssignmentWithNewRequest }) =>
      replaceAssignmentWithNew(payload.assignmentId, payload.values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: assignmentAdminKeys.all });
    },
  });
}
