import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  archiveOrganizationalUnit,
  createOrganizationalUnit,
  createOrganizationalUnitType,
  getOrganizationalUnit,
  getOrganizationalUnits,
  getOrganizationalUnitsTree,
  getOrganizationalUnitTypes,
  moveOrganizationalUnit,
  updateOrganizationalUnit,
  updateOrganizationalUnitType,
} from '../api/adminOrganizationApi';
import type {
  CreateOrganizationalUnitRequest,
  CreateOrganizationalUnitTypeRequest,
  MoveOrganizationalUnitRequest,
  OrganizationalUnitFilter,
  OrganizationalUnitTypeFilter,
  UpdateOrganizationalUnitRequest,
  UpdateOrganizationalUnitTypeRequest,
} from './adminOrganization';

export const adminOrganizationKeys = {
  all: ['admin-organization'] as const,
  unitTypes: (filter?: OrganizationalUnitTypeFilter) => [...adminOrganizationKeys.all, 'unit-types', filter ?? {}] as const,
  units: (filter?: OrganizationalUnitFilter) => [...adminOrganizationKeys.all, 'units', filter ?? {}] as const,
  tree: (status?: string) => [...adminOrganizationKeys.all, 'tree', status ?? 'all'] as const,
  detail: (unitId: number) => [...adminOrganizationKeys.all, 'detail', unitId] as const,
};

export function useOrganizationalUnitTypes(filter?: OrganizationalUnitTypeFilter, enabled = true) {
  return useQuery({
    queryKey: adminOrganizationKeys.unitTypes(filter),
    queryFn: () => getOrganizationalUnitTypes(filter),
    enabled,
  });
}

export function useOrganizationalUnitsTree(status?: string, enabled = true) {
  return useQuery({
    queryKey: adminOrganizationKeys.tree(status),
    queryFn: () => getOrganizationalUnitsTree(status),
    enabled,
  });
}

export function useOrganizationalUnit(unitId?: number, enabled = true) {
  return useQuery({
    queryKey: unitId ? adminOrganizationKeys.detail(unitId) : [...adminOrganizationKeys.all, 'detail', 'missing'],
    queryFn: () => getOrganizationalUnit(unitId as number),
    enabled: enabled && Boolean(unitId),
  });
}

export function useOrganizationalUnits(filter?: OrganizationalUnitFilter, enabled = true) {
  return useQuery({
    queryKey: adminOrganizationKeys.units(filter),
    queryFn: () => getOrganizationalUnits(filter),
    enabled,
  });
}

function useInvalidateAdminOrganization() {
  const queryClient = useQueryClient();

  return async (unitId?: number) => {
    await queryClient.invalidateQueries({ queryKey: adminOrganizationKeys.all });
    if (unitId) {
      await queryClient.invalidateQueries({ queryKey: adminOrganizationKeys.detail(unitId) });
    }
  };
}

export function useCreateOrganizationalUnitTypeMutation() {
  const invalidate = useInvalidateAdminOrganization();

  return useMutation({
    mutationFn: (payload: CreateOrganizationalUnitTypeRequest) => createOrganizationalUnitType(payload),
    onSuccess: async () => invalidate(),
  });
}

export function useUpdateOrganizationalUnitTypeMutation(typeId?: number) {
  const invalidate = useInvalidateAdminOrganization();

  return useMutation({
    mutationFn: (payload: UpdateOrganizationalUnitTypeRequest) =>
      updateOrganizationalUnitType(typeId as number, payload),
    onSuccess: async () => invalidate(),
  });
}

export function useCreateOrganizationalUnitMutation() {
  const invalidate = useInvalidateAdminOrganization();

  return useMutation({
    mutationFn: (payload: CreateOrganizationalUnitRequest) => createOrganizationalUnit(payload),
    onSuccess: async () => invalidate(),
  });
}

export function useUpdateOrganizationalUnitMutation(unitId?: number) {
  const invalidate = useInvalidateAdminOrganization();

  return useMutation({
    mutationFn: (payload: UpdateOrganizationalUnitRequest) =>
      updateOrganizationalUnit(unitId as number, payload),
    onSuccess: async () => invalidate(unitId),
  });
}

export function useMoveOrganizationalUnitMutation(unitId?: number) {
  const invalidate = useInvalidateAdminOrganization();

  return useMutation({
    mutationFn: (payload: MoveOrganizationalUnitRequest) =>
      moveOrganizationalUnit(unitId as number, payload),
    onSuccess: async () => invalidate(unitId),
  });
}

export function useArchiveOrganizationalUnitMutation(unitId?: number) {
  const invalidate = useInvalidateAdminOrganization();

  return useMutation({
    mutationFn: () => archiveOrganizationalUnit(unitId as number),
    onSuccess: async () => invalidate(unitId),
  });
}
