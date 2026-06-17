import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  assignAdminUserOrganizationUnit,
  assignAdminUserRole,
  closeAdminUserOrganizationAssignment,
  closeAdminUserRole,
  createAdminUser,
  deactivateAdminUser,
  getAdminRoles,
  getAdminUser,
  getAdminUserOrganizationAssignments,
  getAdminUserRoles,
  getAdminUsers,
  replaceAdminUserPrimaryHomeUnit,
  updateAdminUser,
} from '../api/adminUsersApi';
import type {
  AssignOrganizationUnitRequest,
  AssignRoleRequest,
  CloseOrganizationAssignmentRequest,
  CloseRoleRequest,
  CreateUserRequest,
  ReplacePrimaryHomeUnitRequest,
  UpdateUserRequest,
  UserListFilter,
} from './adminUsers';

export const adminUsersKeys = {
  all: ['admin-users'] as const,
  lists: () => [...adminUsersKeys.all, 'list'] as const,
  list: (filter?: UserListFilter) => [...adminUsersKeys.lists(), filter ?? {}] as const,
  details: () => [...adminUsersKeys.all, 'detail'] as const,
  detail: (userId: number) => [...adminUsersKeys.details(), userId] as const,
  roles: (userId: number) => [...adminUsersKeys.detail(userId), 'roles'] as const,
  assignments: (userId: number) => [...adminUsersKeys.detail(userId), 'organization-assignments'] as const,
  roleOptions: () => [...adminUsersKeys.all, 'role-options'] as const,
};

export function useAdminUsers(filter?: UserListFilter, enabled = true) {
  return useQuery({
    queryKey: adminUsersKeys.list(filter),
    queryFn: () => getAdminUsers(filter),
    enabled,
  });
}

export function useAdminUser(userId?: number, enabled = true) {
  return useQuery({
    queryKey: userId ? adminUsersKeys.detail(userId) : [...adminUsersKeys.details(), 'missing'],
    queryFn: () => getAdminUser(userId as number),
    enabled: enabled && Boolean(userId),
  });
}

export function useAdminUserRoles(userId?: number, enabled = true) {
  return useQuery({
    queryKey: userId ? adminUsersKeys.roles(userId) : [...adminUsersKeys.all, 'roles', 'missing'],
    queryFn: () => getAdminUserRoles(userId as number),
    enabled: enabled && Boolean(userId),
  });
}

export function useAdminUserOrganizationAssignments(userId?: number, enabled = true) {
  return useQuery({
    queryKey: userId
      ? adminUsersKeys.assignments(userId)
      : [...adminUsersKeys.all, 'assignments', 'missing'],
    queryFn: () => getAdminUserOrganizationAssignments(userId as number),
    enabled: enabled && Boolean(userId),
  });
}

export function useAdminRoles(enabled = true) {
  return useQuery({
    queryKey: adminUsersKeys.roleOptions(),
    queryFn: getAdminRoles,
    enabled,
  });
}

function useInvalidateAdminUser() {
  const queryClient = useQueryClient();

  return async (userId?: number) => {
    await queryClient.invalidateQueries({ queryKey: adminUsersKeys.all });
    if (userId) {
      await queryClient.invalidateQueries({ queryKey: adminUsersKeys.detail(userId) });
    }
  };
}

export function useCreateAdminUserMutation() {
  const invalidate = useInvalidateAdminUser();

  return useMutation({
    mutationFn: (payload: CreateUserRequest) => createAdminUser(payload),
    onSuccess: async (user) => invalidate(user.id),
  });
}

export function useUpdateAdminUserMutation(userId?: number) {
  const invalidate = useInvalidateAdminUser();

  return useMutation({
    mutationFn: (payload: UpdateUserRequest) => updateAdminUser(userId as number, payload),
    onSuccess: async () => invalidate(userId),
  });
}

export function useDeactivateAdminUserMutation(userId?: number) {
  const invalidate = useInvalidateAdminUser();

  return useMutation({
    mutationFn: () => deactivateAdminUser(userId as number),
    onSuccess: async () => invalidate(userId),
  });
}

export function useAssignAdminUserRoleMutation(userId?: number) {
  const invalidate = useInvalidateAdminUser();

  return useMutation({
    mutationFn: (payload: AssignRoleRequest) => assignAdminUserRole(userId as number, payload),
    onSuccess: async () => invalidate(userId),
  });
}

export function useCloseAdminUserRoleMutation(userId?: number) {
  const invalidate = useInvalidateAdminUser();

  return useMutation({
    mutationFn: (payload: { assignmentId: number; values?: CloseRoleRequest }) =>
      closeAdminUserRole(userId as number, payload.assignmentId, payload.values),
    onSuccess: async () => invalidate(userId),
  });
}

export function useAssignAdminUserOrganizationMutation(userId?: number) {
  const invalidate = useInvalidateAdminUser();

  return useMutation({
    mutationFn: (payload: AssignOrganizationUnitRequest) =>
      assignAdminUserOrganizationUnit(userId as number, payload),
    onSuccess: async () => invalidate(userId),
  });
}

export function useCloseAdminUserOrganizationMutation(userId?: number) {
  const invalidate = useInvalidateAdminUser();

  return useMutation({
    mutationFn: (payload: { assignmentId: number; values?: CloseOrganizationAssignmentRequest }) =>
      closeAdminUserOrganizationAssignment(userId as number, payload.assignmentId, payload.values),
    onSuccess: async () => invalidate(userId),
  });
}

export function useReplaceAdminUserPrimaryHomeUnitMutation(userId?: number) {
  const invalidate = useInvalidateAdminUser();

  return useMutation({
    mutationFn: (payload: ReplacePrimaryHomeUnitRequest) =>
      replaceAdminUserPrimaryHomeUnit(userId as number, payload),
    onSuccess: async () => invalidate(userId),
  });
}
