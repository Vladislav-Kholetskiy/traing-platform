import { apiClient } from '../../../shared/api/apiClient';
import type {
  AccessFilter,
  AssignManagementRelationRequest,
  AssignTemporaryAccessAreaRequest,
  AssignTemporaryManagementDelegationRequest,
  AssignTemporaryRoleRequest,
  AssignUserAccessAreaRequest,
  CloseByDateRequest,
  ManagementRelation,
  ManagementRelationType,
  TemporaryAccessArea,
  TemporaryManagementDelegation,
  TemporaryRoleAssignment,
  UserAccessArea,
} from '../model/accessManagement';

function buildFilterUrl(path: string, filter?: AccessFilter) {
  const params = new URLSearchParams();

  Object.entries(filter ?? {}).forEach(([key, value]) => {
    if (value != null && value !== '') {
      params.set(key, String(value));
    }
  });

  return params.size ? `${path}?${params.toString()}` : path;
}

export function getAccessAreas(filter?: AccessFilter) {
  return apiClient.get<UserAccessArea[]>(buildFilterUrl('/api/v1/admin/access-areas', filter));
}

export function getManagementRelations(filter?: AccessFilter) {
  return apiClient.get<ManagementRelation[]>(
    buildFilterUrl('/api/v1/admin/management-relations', filter),
  );
}

export function getTemporaryRoleAssignments(filter?: AccessFilter) {
  return apiClient.get<TemporaryRoleAssignment[]>(
    buildFilterUrl('/api/v1/admin/temporary-role-assignments', filter),
  );
}

export function getTemporaryAccessAreas(filter?: AccessFilter) {
  return apiClient.get<TemporaryAccessArea[]>(
    buildFilterUrl('/api/v1/admin/temporary-access-areas', filter),
  );
}

export function getTemporaryManagementDelegations(filter?: AccessFilter) {
  return apiClient.get<TemporaryManagementDelegation[]>(
    buildFilterUrl('/api/v1/admin/temporary-management-delegations', filter),
  );
}

export function getManagementRelationTypes() {
  return apiClient.get<ManagementRelationType[]>('/api/v1/admin/management-relation-types');
}

export function createAccessArea(payload: AssignUserAccessAreaRequest) {
  return apiClient.post<UserAccessArea>('/api/v1/admin/access-areas', payload);
}

export function closeAccessArea(accessAreaId: number, payload?: CloseByDateRequest) {
  return apiClient.post<UserAccessArea>(`/api/v1/admin/access-areas/${accessAreaId}/close`, payload ?? null);
}

export function createManagementRelation(payload: AssignManagementRelationRequest) {
  return apiClient.post<ManagementRelation>('/api/v1/admin/management-relations', payload);
}

export function closeManagementRelation(relationId: number, payload?: CloseByDateRequest) {
  return apiClient.post<ManagementRelation>(
    `/api/v1/admin/management-relations/${relationId}/close`,
    payload ?? null,
  );
}

export function createTemporaryRoleAssignment(payload: AssignTemporaryRoleRequest) {
  return apiClient.post<TemporaryRoleAssignment>('/api/v1/admin/temporary-role-assignments', payload);
}

export function closeTemporaryRoleAssignment(assignmentId: number, payload?: CloseByDateRequest) {
  return apiClient.post<TemporaryRoleAssignment>(
    `/api/v1/admin/temporary-role-assignments/${assignmentId}/close`,
    payload ?? null,
  );
}

export function createTemporaryAccessArea(payload: AssignTemporaryAccessAreaRequest) {
  return apiClient.post<TemporaryAccessArea>('/api/v1/admin/temporary-access-areas', payload);
}

export function closeTemporaryAccessArea(areaId: number, payload?: CloseByDateRequest) {
  return apiClient.post<TemporaryAccessArea>(
    `/api/v1/admin/temporary-access-areas/${areaId}/close`,
    payload ?? null,
  );
}

export function createTemporaryManagementDelegation(
  payload: AssignTemporaryManagementDelegationRequest,
) {
  return apiClient.post<TemporaryManagementDelegation>(
    '/api/v1/admin/temporary-management-delegations',
    payload,
  );
}

export function closeTemporaryManagementDelegation(delegationId: number, payload?: CloseByDateRequest) {
  return apiClient.post<TemporaryManagementDelegation>(
    `/api/v1/admin/temporary-management-delegations/${delegationId}/close`,
    payload ?? null,
  );
}
