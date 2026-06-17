import { apiClient } from '../../../shared/api/apiClient';
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
} from '../model/assignmentAdmin';

function buildQueryString(params: Record<string, string | number | undefined>) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      searchParams.set(key, String(value));
    }
  });
  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

export function launchAssignmentCampaign(payload: LaunchAssignmentCampaignRequest) {
  return apiClient.post<AssignmentCampaign>('/api/v1/assignment-campaigns/launch', payload);
}

export function launchIndividualAssignment(payload: LaunchIndividualAssignmentRequest) {
  return apiClient.post<AssignmentCampaign>('/api/v1/assignment-campaigns/launch-individual', payload);
}

export function getAssignmentCampaignTargetUnits() {
  return apiClient.get<AssignmentCampaignTargetUnit[]>('/api/v1/assignment-campaigns/target-units');
}

export function getAssignmentCampaigns(filter: AssignmentCampaignFilter = {}) {
  return apiClient.get<AssignmentCampaign[]>(
    `/api/v1/admin/assignment-campaigns${buildQueryString({ sourceType: filter.sourceType })}`,
  );
}

export function getAssignments(filter: AssignmentFilter = {}) {
  return apiClient.get<AssignmentRecord[]>(
    `/api/v1/admin/assignments${buildQueryString({
      campaignId: filter.campaignId,
      userId: filter.userId,
      status: filter.status,
    })}`,
  );
}

export function cancelAssignment(assignmentId: number, payload: CancelAssignmentRequest) {
  return apiClient.post<AssignmentRecord>(
    `/api/v1/assignment-administrative-actions/cancel/${assignmentId}`,
    payload,
  );
}

export function extendAssignmentDeadline(
  assignmentId: number,
  payload: ExtendAssignmentDeadlineRequest,
) {
  return apiClient.post<AssignmentRecord>(
    `/api/v1/assignment-administrative-actions/deadline-extend/${assignmentId}`,
    payload,
  );
}

export function replaceAssignmentWithNew(
  assignmentId: number,
  payload: ReplaceAssignmentWithNewRequest,
) {
  return apiClient.post<AssignmentRecord>(
    `/api/v1/assignment-administrative-actions/replace-with-new/${assignmentId}`,
    payload,
  );
}
