export type OrganizationalNodeKind = 'LINEAR' | 'FUNCTIONAL' | string;
export type OrganizationalUnitStatus = 'ACTIVE' | 'ARCHIVED' | string;

export type OrganizationalUnitType = {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  nodeKind: OrganizationalNodeKind;
  canBeOperatorHomeUnit: boolean;
  canBeCampaignTarget: boolean;
  participatesInSubtreeScope: boolean;
  canHaveManagementRelation: boolean;
  canHaveAccessArea: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type OrganizationalUnit = {
  id: number;
  parentId?: number | null;
  organizationalUnitTypeId: number;
  name: string;
  status: OrganizationalUnitStatus;
  path?: string | null;
  depth?: number | null;
  externalId?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  children?: OrganizationalUnit[];
};

export type OrganizationalUnitTypeFilter = {
  id?: number;
  code?: string;
  nodeKind?: OrganizationalNodeKind;
};

export type OrganizationalUnitFilter = {
  parentId?: number;
  path?: string;
  status?: OrganizationalUnitStatus;
};

export type CreateOrganizationalUnitTypeRequest = {
  code: string;
  name: string;
  description?: string;
  nodeKind: OrganizationalNodeKind;
  canBeOperatorHomeUnit: boolean;
  canBeCampaignTarget: boolean;
  participatesInSubtreeScope: boolean;
  canHaveManagementRelation: boolean;
  canHaveAccessArea: boolean;
};

export type UpdateOrganizationalUnitTypeRequest = Omit<CreateOrganizationalUnitTypeRequest, 'code'>;

export type CreateOrganizationalUnitRequest = {
  parentId?: number | null;
  organizationalUnitTypeId: number;
  name: string;
  externalId?: string;
};

export type UpdateOrganizationalUnitRequest = {
  name: string;
  externalId?: string;
  organizationalUnitTypeId: number;
};

export type MoveOrganizationalUnitRequest = {
  newParentOrganizationalUnitId: number;
};
