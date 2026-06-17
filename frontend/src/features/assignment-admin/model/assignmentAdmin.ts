export type AssignmentStatus =
  | 'ASSIGNED'
  | 'COMPLETED'
  | 'OVERDUE'
  | 'CANCELLED'
  | string;

export type AssignmentCampaign = {
  id: number;
  name: string;
  description?: string | null;
  sourceType?: string | null;
  sourceRef?: string | null;
  sourceNameSnapshot?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AssignmentCampaignFilter = {
  sourceType?: string;
};

export type AssignmentCampaignTargetUnit = {
  id: number;
  name: string;
  path?: string | null;
};

export type AssignmentRecord = {
  id: number;
  campaignId?: number | null;
  userId?: number | null;
  courseId?: number | null;
  courseName?: string | null;
  status?: AssignmentStatus | null;
  assignedAt?: string | null;
  deadlineAt?: string | null;
  cancelledAt?: string | null;
  closedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AssignmentFilter = {
  campaignId?: number;
  userId?: number;
  status?: AssignmentStatus;
};

export type LaunchAssignmentCampaignRequest = {
  name: string;
  description?: string;
  sourceType: string;
  sourceRef: string;
  sourceNameSnapshot: string;
  courseIds: number[];
  targeting: {
    basisType: string;
    basisRef: string;
  };
  deadlinePolicy: {
    deadlineAt: string;
  };
};

export type LaunchIndividualAssignmentRequest = {
  name: string;
  description?: string;
  userId: number;
  courseIds: number[];
  deadlinePolicy: {
    deadlineAt: string;
  };
};

export type CancelAssignmentRequest = {
  note: string;
};

export type ExtendAssignmentDeadlineRequest = {
  newDeadlineAt: string;
  note: string;
};

export type ReplaceAssignmentWithNewRequest = {
  campaignId: number;
  newCycleDeadlineAt: string;
  note: string;
};
