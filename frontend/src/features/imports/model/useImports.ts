import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  applyImportReview,
  applyPersonnelExcel,
  dryRunPersonnelExcel,
  getImportJob,
  getImportJobItem,
  getImportJobItems,
  getImportJobs,
  launchImportJob,
  rejectImportReview,
} from '../api/importsApi';
import type {
  ImportJobFilter,
  ImportJobItemsFilter,
  ImportLaunchRequest,
  ImportReviewApplyRequest,
  ImportReviewRejectRequest,
} from './imports';

export const importsKeys = {
  all: ['imports'] as const,
  jobs: (filter?: ImportJobFilter) => [...importsKeys.all, 'jobs', filter ?? {}] as const,
  job: (id: number) => [...importsKeys.all, 'job', id] as const,
  items: (jobId: number, filter?: ImportJobItemsFilter) => [...importsKeys.job(jobId), 'items', filter ?? {}] as const,
  item: (id: number) => [...importsKeys.all, 'item', id] as const,
};

export function useImportJobs(filter?: ImportJobFilter, enabled = true) {
  return useQuery({
    queryKey: importsKeys.jobs(filter),
    queryFn: () => getImportJobs(filter),
    enabled,
  });
}

export function useImportJob(importJobId?: number, enabled = true) {
  return useQuery({
    queryKey: importJobId ? importsKeys.job(importJobId) : [...importsKeys.all, 'job', 'missing'],
    queryFn: () => getImportJob(importJobId as number),
    enabled: enabled && Boolean(importJobId),
  });
}

export function useImportJobItems(importJobId?: number, filter?: ImportJobItemsFilter, enabled = true) {
  return useQuery({
    queryKey: importJobId
      ? importsKeys.items(importJobId, filter)
      : [...importsKeys.all, 'job', 'missing', 'items'],
    queryFn: () => getImportJobItems(importJobId as number, filter),
    enabled: enabled && Boolean(importJobId),
  });
}

export function useImportJobItem(itemId?: number, enabled = true) {
  return useQuery({
    queryKey: itemId ? importsKeys.item(itemId) : [...importsKeys.all, 'item', 'missing'],
    queryFn: () => getImportJobItem(itemId as number),
    enabled: enabled && Boolean(itemId),
  });
}

function useInvalidateImports() {
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: importsKeys.all });
  };
}

export function useLaunchImportJobMutation() {
  const invalidate = useInvalidateImports();
  return useMutation({
    mutationFn: (payload: ImportLaunchRequest) => launchImportJob(payload),
    onSuccess: invalidate,
  });
}

export function useApplyImportReviewMutation() {
  const invalidate = useInvalidateImports();
  return useMutation({
    mutationFn: (payload: { itemId: number; values: ImportReviewApplyRequest }) =>
      applyImportReview(payload.itemId, payload.values),
    onSuccess: invalidate,
  });
}

export function useRejectImportReviewMutation() {
  const invalidate = useInvalidateImports();
  return useMutation({
    mutationFn: (payload: { itemId: number; values: ImportReviewRejectRequest }) =>
      rejectImportReview(payload.itemId, payload.values),
    onSuccess: invalidate,
  });
}

export function useDryRunPersonnelExcelMutation() {
  return useMutation({
    mutationFn: (file: File) => dryRunPersonnelExcel(file),
  });
}

export function useApplyPersonnelExcelMutation() {
  const invalidate = useInvalidateImports();
  return useMutation({
    mutationFn: (file: File) => applyPersonnelExcel(file),
    onSuccess: invalidate,
  });
}
