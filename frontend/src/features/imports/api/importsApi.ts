import { apiClient } from '../../../shared/api/apiClient';
import type {
  ImportItem,
  ImportJob,
  ImportJobFilter,
  ImportJobItemsFilter,
  ImportLaunchRequest,
  ImportReviewApplyRequest,
  ImportReviewRejectRequest,
  ImportReviewResponse,
  PersonnelApplyResponse,
  PersonnelDryRunResponse,
} from '../model/imports';

function buildUrl(base: string, filter?: Record<string, string | undefined>) {
  const params = new URLSearchParams();
  Object.entries(filter ?? {}).forEach(([key, value]) => {
    if (value) {
      params.set(key, value);
    }
  });
  return params.size ? `${base}?${params.toString()}` : base;
}

export function getImportJobs(filter?: ImportJobFilter) {
  return apiClient.get<ImportJob[]>(
    buildUrl('/api/v1/admin/import-jobs', {
      status: filter?.status,
      sourceType: filter?.sourceType,
    }),
  );
}

export function getImportJob(importJobId: number) {
  return apiClient.get<ImportJob>(`/api/v1/admin/import-jobs/${importJobId}`);
}

export function getImportJobItems(importJobId: number, filter?: ImportJobItemsFilter) {
  return apiClient.get<ImportItem[]>(
    buildUrl(`/api/v1/admin/import-jobs/${importJobId}/items`, {
      status: filter?.status,
    }),
  );
}

export function getImportJobItem(itemId: number) {
  return apiClient.get<ImportItem>(`/api/v1/admin/import-job-items/${itemId}`);
}

export function launchImportJob(payload: ImportLaunchRequest) {
  return apiClient.post<ImportJob>('/api/v1/admin/import-jobs', payload);
}

export function applyImportReview(itemId: number, payload: ImportReviewApplyRequest) {
  return apiClient.post<ImportReviewResponse>(`/api/v1/admin/import-job-items/${itemId}/apply-review`, payload);
}

export function rejectImportReview(itemId: number, payload: ImportReviewRejectRequest) {
  return apiClient.post<ImportReviewResponse>(`/api/v1/admin/import-job-items/${itemId}/reject-review`, payload);
}

function buildPersonnelForm(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
}

export function dryRunPersonnelExcel(file: File) {
  return apiClient.post<PersonnelDryRunResponse>(
    '/api/v1/admin/import/personnel-excel/dry-run',
    buildPersonnelForm(file),
  );
}

export function applyPersonnelExcel(file: File) {
  return apiClient.post<PersonnelApplyResponse>(
    '/api/v1/admin/import/personnel-excel/apply',
    buildPersonnelForm(file),
  );
}
