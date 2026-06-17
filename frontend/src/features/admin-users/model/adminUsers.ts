export type UserStatus = 'ACTIVE' | 'INACTIVE' | string;
export type OrganizationAssignmentType = 'PRIMARY' | 'SECONDARY' | 'TEMPORARY' | string;

export type AdminUser = {
  id: number;
  employeeNumber: string;
  externalId?: string | null;
  lastName: string;
  firstName: string;
  middleName?: string | null;
  status: UserStatus;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AdminUserRole = {
  id: number;
  userId: number;
  roleId: number;
  roleCode?: string | null;
  roleName?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AdminUserOrganizationAssignment = {
  id: number;
  userId: number;
  organizationalUnitId: number;
  organizationalUnitName?: string | null;
  organizationalUnitPath?: string | null;
  assignmentType: OrganizationAssignmentType;
  validFrom?: string | null;
  validTo?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type AdminUserCard = AdminUser & {
  activeRoleAssignments: AdminUserRole[];
  activeOrganizationAssignments: AdminUserOrganizationAssignment[];
};

export type RoleOption = {
  id: number;
  code?: string | null;
  name?: string | null;
  description?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type CreateUserRequest = {
  employeeNumber: string;
  externalId?: string;
  lastName: string;
  firstName: string;
  middleName?: string;
  status: UserStatus;
};

export type UpdateUserRequest = {
  lastName: string;
  firstName: string;
  middleName?: string;
};

export type AssignRoleRequest = {
  roleId: number;
  validFrom?: string;
};

export type CloseRoleRequest = {
  validTo?: string;
};

export type AssignOrganizationUnitRequest = {
  organizationalUnitId: number;
  assignmentType: OrganizationAssignmentType;
  validFrom?: string;
};

export type CloseOrganizationAssignmentRequest = {
  validTo?: string;
};

export type ReplacePrimaryHomeUnitRequest = {
  organizationalUnitId: number;
  effectiveAt?: string;
};

export type UserListFilter = {
  status?: UserStatus;
};
