import { apiClient } from '../../../shared/api/apiClient';
import type {
  AdminUser,
  AdminUserCard,
  AdminUserOrganizationAssignment,
  AdminUserRole,
  AssignOrganizationUnitRequest,
  AssignRoleRequest,
  CloseOrganizationAssignmentRequest,
  CloseRoleRequest,
  CreateUserRequest,
  ReplacePrimaryHomeUnitRequest,
  RoleOption,
  UpdateUserRequest,
  UserListFilter,
} from '../model/adminUsers';

function buildUsersUrl(filter?: UserListFilter) {
  const params = new URLSearchParams();

  if (filter?.status) {
    params.set('status', filter.status);
  }

  const suffix = params.size ? `?${params.toString()}` : '';
  return `/api/v1/admin/users${suffix}`;
}

export function getAdminUsers(filter?: UserListFilter) {
  return apiClient.get<AdminUser[]>(buildUsersUrl(filter));
}

export function getAdminUser(userId: number) {
  return apiClient.get<AdminUserCard>(`/api/v1/admin/users/${userId}`);
}

export function getAdminUserRoles(userId: number) {
  return apiClient.get<AdminUserRole[]>(`/api/v1/admin/users/${userId}/roles`);
}

export function getAdminUserOrganizationAssignments(userId: number) {
  return apiClient.get<AdminUserOrganizationAssignment[]>(
    `/api/v1/admin/users/${userId}/organization-assignments`,
  );
}

export function getAdminRoles() {
  return apiClient.get<RoleOption[]>('/api/v1/admin/roles');
}

export function createAdminUser(payload: CreateUserRequest) {
  return apiClient.post<AdminUser>('/api/v1/admin/users', payload);
}

export function updateAdminUser(userId: number, payload: UpdateUserRequest) {
  return apiClient.patch<AdminUser>(`/api/v1/admin/users/${userId}`, payload);
}

export function deactivateAdminUser(userId: number) {
  return apiClient.post<AdminUser>(`/api/v1/admin/users/${userId}/deactivate`);
}

export function assignAdminUserRole(userId: number, payload: AssignRoleRequest) {
  return apiClient.post<AdminUserRole>(`/api/v1/admin/users/${userId}/roles`, payload);
}

export function closeAdminUserRole(
  userId: number,
  assignmentId: number,
  payload?: CloseRoleRequest,
) {
  return apiClient.post<AdminUserRole>(
    `/api/v1/admin/users/${userId}/roles/${assignmentId}/close`,
    payload ?? null,
  );
}

export function assignAdminUserOrganizationUnit(
  userId: number,
  payload: AssignOrganizationUnitRequest,
) {
  return apiClient.post<AdminUserOrganizationAssignment>(
    `/api/v1/admin/users/${userId}/organization-assignments`,
    payload,
  );
}

export function closeAdminUserOrganizationAssignment(
  userId: number,
  assignmentId: number,
  payload?: CloseOrganizationAssignmentRequest,
) {
  return apiClient.post<AdminUserOrganizationAssignment>(
    `/api/v1/admin/users/${userId}/organization-assignments/${assignmentId}/close`,
    payload ?? null,
  );
}

export function replaceAdminUserPrimaryHomeUnit(
  userId: number,
  payload: ReplacePrimaryHomeUnitRequest,
) {
  return apiClient.post<AdminUserOrganizationAssignment>(
    `/api/v1/admin/users/${userId}/primary-home-unit/replace`,
    payload,
  );
}
