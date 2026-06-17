export type ImportJobStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'COMPLETED_WITH_ERRORS'
  | 'FAILED'
  | string;

export type ImportItemStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'APPLIED'
  | 'NO_CHANGE'
  | 'FAILED'
  | 'REQUIRES_REVIEW'
  | string;

export type ImportJob = {
  id: number;
  sourceType?: string | null;
  sourceRef?: string | null;
  status?: ImportJobStatus | null;
  totalItemCount?: number | null;
  processedItemCount?: number | null;
  appliedItemCount?: number | null;
  failedItemCount?: number | null;
  requiresReviewItemCount?: number | null;
  startedAt?: string | null;
  completedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type ImportItem = {
  id: number;
  importJobId: number;
  itemNo?: number | null;
  targetEntityType?: string | null;
  externalId?: string | null;
  employeeNumber?: string | null;
  status?: ImportItemStatus | null;
  matchedEntityId?: number | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  processedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type ImportJobFilter = {
  status?: ImportJobStatus;
  sourceType?: string;
};

export type ImportJobItemsFilter = {
  status?: ImportItemStatus;
};

export type ImportLaunchItemRequest = {
  targetEntityType: string;
  externalId?: string;
  employeeNumber?: string;
  payload?: string;
};

export type ImportLaunchRequest = {
  sourceType: string;
  sourceRef?: string;
  payload?: string;
  items?: ImportLaunchItemRequest[];
};

export type ImportReviewApplyRequest = {
  matchedUserId: number;
};

export type ImportReviewRejectRequest = {
  reason: string;
};

export type ImportReviewResponse = {
  itemId?: number | null;
  status?: string | null;
  matchedEntityId?: number | null;
  errorCode?: string | null;
  errorMessage?: string | null;
};

export type PersonnelDryRunMutation = {
  mutationType?: string | null;
  targetRef?: string | null;
  detail?: string | null;
};

export type PersonnelDryRunRow = {
  rowNumber?: number | null;
  employeeNumber?: string | null;
  outcomeCode?: string | null;
  decision?: string | null;
  targetUserStatus?: string | null;
  issues?: string[] | null;
  plannedMutations?: PersonnelDryRunMutation[] | null;
};

export type PersonnelDryRunResponse = {
  rows?: PersonnelDryRunRow[] | null;
};

export type PersonnelApplyRow = {
  rowNumber?: number | null;
  employeeNumber?: string | null;
  outcomeCode?: string | null;
  decision?: string | null;
  targetUserStatus?: string | null;
  issues?: string[] | null;
  appliedMutationTypes?: string[] | null;
  createdUserId?: number | null;
};

export type PersonnelApplyResponse = {
  rows?: PersonnelApplyRow[] | null;
};
