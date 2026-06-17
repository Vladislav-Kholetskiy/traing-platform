import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  closeAccessArea,
  closeManagementRelation,
  closeTemporaryAccessArea,
  closeTemporaryManagementDelegation,
  closeTemporaryRoleAssignment,
  createAccessArea,
  createManagementRelation,
  createTemporaryAccessArea,
  createTemporaryManagementDelegation,
  createTemporaryRoleAssignment,
  getAccessAreas,
  getManagementRelations,
  getManagementRelationTypes,
  getTemporaryAccessAreas,
  getTemporaryManagementDelegations,
  getTemporaryRoleAssignments,
} from '../api/accessManagementApi';
import type {
  AccessFilter,
  AssignManagementRelationRequest,
  AssignTemporaryAccessAreaRequest,
  AssignTemporaryManagementDelegationRequest,
  AssignTemporaryRoleRequest,
  AssignUserAccessAreaRequest,
  CloseByDateRequest,
} from './accessManagement';

export const accessManagementKeys = {
  all: ['access-management'] as const,
  accessAreas: (filter?: AccessFilter) => [...accessManagementKeys.all, 'access-areas', filter ?? {}] as const,
  managementRelations: (filter?: AccessFilter) => [...accessManagementKeys.all, 'management-relations', filter ?? {}] as const,
  temporaryRoles: (filter?: AccessFilter) => [...accessManagementKeys.all, 'temporary-roles', filter ?? {}] as const,
  temporaryAccessAreas: (filter?: AccessFilter) => [...accessManagementKeys.all, 'temporary-access-areas', filter ?? {}] as const,
  temporaryDelegations: (filter?: AccessFilter) => [...accessManagementKeys.all, 'temporary-delegations', filter ?? {}] as const,
  relationTypes: () => [...accessManagementKeys.all, 'relation-types'] as const,
};

export function useAccessAreas(filter?: AccessFilter, enabled = true) {
  return useQuery({
    queryKey: accessManagementKeys.accessAreas(filter),
    queryFn: () => getAccessAreas(filter),
    enabled,
  });
}

export function useManagementRelations(filter?: AccessFilter, enabled = true) {
  return useQuery({
    queryKey: accessManagementKeys.managementRelations(filter),
    queryFn: () => getManagementRelations(filter),
    enabled,
  });
}

export function useTemporaryRoleAssignments(filter?: AccessFilter, enabled = true) {
  return useQuery({
    queryKey: accessManagementKeys.temporaryRoles(filter),
    queryFn: () => getTemporaryRoleAssignments(filter),
    enabled,
  });
}

export function useTemporaryAccessAreas(filter?: AccessFilter, enabled = true) {
  return useQuery({
    queryKey: accessManagementKeys.temporaryAccessAreas(filter),
    queryFn: () => getTemporaryAccessAreas(filter),
    enabled,
  });
}

export function useTemporaryManagementDelegations(filter?: AccessFilter, enabled = true) {
  return useQuery({
    queryKey: accessManagementKeys.temporaryDelegations(filter),
    queryFn: () => getTemporaryManagementDelegations(filter),
    enabled,
  });
}

export function useManagementRelationTypes(enabled = true) {
  return useQuery({
    queryKey: accessManagementKeys.relationTypes(),
    queryFn: getManagementRelationTypes,
    enabled,
  });
}

function useInvalidateAccessManagement() {
  const queryClient = useQueryClient();

  return async () => {
    await queryClient.invalidateQueries({ queryKey: accessManagementKeys.all });
  };
}

export function useCreateAccessAreaMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: AssignUserAccessAreaRequest) => createAccessArea(payload),
    onSuccess: invalidate,
  });
}

export function useCloseAccessAreaMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: { id: number; values?: CloseByDateRequest }) =>
      closeAccessArea(payload.id, payload.values),
    onSuccess: invalidate,
  });
}

export function useCreateManagementRelationMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: AssignManagementRelationRequest) => createManagementRelation(payload),
    onSuccess: invalidate,
  });
}

export function useCloseManagementRelationMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: { id: number; values?: CloseByDateRequest }) =>
      closeManagementRelation(payload.id, payload.values),
    onSuccess: invalidate,
  });
}

export function useCreateTemporaryRoleMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: AssignTemporaryRoleRequest) => createTemporaryRoleAssignment(payload),
    onSuccess: invalidate,
  });
}

export function useCloseTemporaryRoleMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: { id: number; values?: CloseByDateRequest }) =>
      closeTemporaryRoleAssignment(payload.id, payload.values),
    onSuccess: invalidate,
  });
}

export function useCreateTemporaryAccessAreaMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: AssignTemporaryAccessAreaRequest) => createTemporaryAccessArea(payload),
    onSuccess: invalidate,
  });
}

export function useCloseTemporaryAccessAreaMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: { id: number; values?: CloseByDateRequest }) =>
      closeTemporaryAccessArea(payload.id, payload.values),
    onSuccess: invalidate,
  });
}

export function useCreateTemporaryManagementDelegationMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: AssignTemporaryManagementDelegationRequest) =>
      createTemporaryManagementDelegation(payload),
    onSuccess: invalidate,
  });
}

export function useCloseTemporaryManagementDelegationMutation() {
  const invalidate = useInvalidateAccessManagement();
  return useMutation({
    mutationFn: (payload: { id: number; values?: CloseByDateRequest }) =>
      closeTemporaryManagementDelegation(payload.id, payload.values),
    onSuccess: invalidate,
  });
}
