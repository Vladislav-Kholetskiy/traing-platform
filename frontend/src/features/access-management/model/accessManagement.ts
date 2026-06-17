export type AccessScopeType = 'GLOBAL' | 'UNIT_ONLY' | 'UNIT_SUBTREE' | string;

export type UserAccessArea = {
  id: number;
  userId: number;
  organizationalUnitId: number;
  accessScopeType: AccessScopeType;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type ManagementRelation = {
  id: number;
  userId: number;
  organizationalUnitId: number;
  managementRelationTypeId: number;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type TemporaryRoleAssignment = {
  id: number;
  userId: number;
  roleId: number;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type TemporaryAccessArea = {
  id: number;
  userId: number;
  organizationalUnitId: number;
  accessScopeType: AccessScopeType;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type TemporaryManagementDelegation = {
  id: number;
  userId: number;
  organizationalUnitId: number;
  managementRelationTypeId: number;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type ManagementRelationType = {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AccessFilter = {
  userId?: number;
  organizationalUnitId?: number;
  roleId?: number;
  managementRelationTypeId?: number;
  accessScopeType?: AccessScopeType;
  activeOnly?: boolean;
  activeAt?: string;
};

export type AssignUserAccessAreaRequest = {
  userId: number;
  organizationalUnitId: number;
  accessScopeType: AccessScopeType;
  validFrom?: string;
};

export type CloseByDateRequest = {
  validTo?: string;
};

export type AssignManagementRelationRequest = {
  userId: number;
  organizationalUnitId: number;
  managementRelationTypeId: number;
  validFrom?: string;
};

export type AssignTemporaryRoleRequest = {
  userId: number;
  roleId: number;
  validFrom?: string;
};

export type AssignTemporaryAccessAreaRequest = {
  userId: number;
  organizationalUnitId: number;
  accessScopeType: AccessScopeType;
  validFrom?: string;
};

export type AssignTemporaryManagementDelegationRequest = {
  userId: number;
  organizationalUnitId: number;
  managementRelationTypeId: number;
  validFrom?: string;
};
