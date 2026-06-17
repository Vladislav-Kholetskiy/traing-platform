import { apiClient } from '../../../shared/api/apiClient';
import type {
  CreateOrganizationalUnitRequest,
  CreateOrganizationalUnitTypeRequest,
  MoveOrganizationalUnitRequest,
  OrganizationalUnit,
  OrganizationalUnitFilter,
  OrganizationalUnitType,
  OrganizationalUnitTypeFilter,
  UpdateOrganizationalUnitRequest,
  UpdateOrganizationalUnitTypeRequest,
} from '../model/adminOrganization';

function appendParam(params: URLSearchParams, key: string, value?: string | number | null) {
  if (value != null && value !== '') {
    params.set(key, String(value));
  }
}

function buildUrl(base: string, filter?: Record<string, string | number | undefined | null>) {
  const params = new URLSearchParams();

  Object.entries(filter ?? {}).forEach(([key, value]) => appendParam(params, key, value));

  return params.size ? `${base}?${params.toString()}` : base;
}

export function getOrganizationalUnitTypes(filter?: OrganizationalUnitTypeFilter) {
  return apiClient.get<OrganizationalUnitType[]>(
    buildUrl('/api/v1/admin/org-unit-types', {
      id: filter?.id,
      code: filter?.code,
      nodeKind: filter?.nodeKind,
    }),
  );
}

export function getOrganizationalUnitsTree(status?: string) {
  return apiClient.get<OrganizationalUnit[]>(
    buildUrl('/api/v1/admin/org-units/tree', { status }),
  );
}

export function getOrganizationalUnit(unitId: number) {
  return apiClient.get<OrganizationalUnit>(`/api/v1/admin/org-units/${unitId}`);
}

export function getOrganizationalUnits(filter?: OrganizationalUnitFilter) {
  return apiClient.get<OrganizationalUnit[]>(
    buildUrl('/api/v1/admin/org-units', {
      parentId: filter?.parentId,
      path: filter?.path,
      status: filter?.status,
    }),
  );
}

export function createOrganizationalUnitType(payload: CreateOrganizationalUnitTypeRequest) {
  return apiClient.post<OrganizationalUnitType>('/api/v1/admin/org-unit-types', payload);
}

export function updateOrganizationalUnitType(
  typeId: number,
  payload: UpdateOrganizationalUnitTypeRequest,
) {
  return apiClient.patch<OrganizationalUnitType>(`/api/v1/admin/org-unit-types/${typeId}`, payload);
}

export function createOrganizationalUnit(payload: CreateOrganizationalUnitRequest) {
  return apiClient.post<OrganizationalUnit>('/api/v1/admin/org-units', payload);
}

export function updateOrganizationalUnit(unitId: number, payload: UpdateOrganizationalUnitRequest) {
  return apiClient.patch<OrganizationalUnit>(`/api/v1/admin/org-units/${unitId}`, payload);
}

export function moveOrganizationalUnit(unitId: number, payload: MoveOrganizationalUnitRequest) {
  return apiClient.post<OrganizationalUnit>(`/api/v1/admin/org-units/${unitId}/move`, payload);
}

export function archiveOrganizationalUnit(unitId: number) {
  return apiClient.post<OrganizationalUnit>(`/api/v1/admin/org-units/${unitId}/archive`);
}
