import { resolveDemoActorLabel } from './demoActor';

export type EnabledSection = 'ASSIGNED_LEARNING' | 'SELF_RESULTS' | 'SELF_TESTING';
export type ExtendedEnabledSection =
  | EnabledSection
  | 'MANAGER_CURRENT_SUPERVISION'
  | 'MANAGER_HISTORICAL_ANALYTICS'
  | 'EXPERT_CONTENT'
  | 'EXPERT_QUESTION_ANALYTICS'
  | 'ADMINISTRATION';

export type CurrentActorRole =
  | 'ADMIN'
  | 'EXPERT'
  | 'LEARNER'
  | 'MANAGER'
  | 'OPERATOR'
  | 'SYSTEM_ADMIN'
  | 'SUPER_ADMIN'
  | string;

export type CurrentActorResponse = {
  actorUserId?: number | null;
  username?: string | null;
  displayName?: string | null;
  positionTitle?: string | null;
  employeeNumber?: string | null;
  primaryOrganizationalUnitName?: string | null;
  primaryOrganizationalUnitPath?: string | null;
  roles?: string[] | null;
  enabledSections?: string[] | null;
};

export type CurrentActor = {
  actorUserId?: number;
  username?: string;
  displayName?: string;
  positionTitle?: string;
  employeeNumber?: string;
  primaryOrganizationalUnitName?: string;
  primaryOrganizationalUnitPath?: string;
  roles: CurrentActorRole[];
  enabledSections: ExtendedEnabledSection[];
};

const knownSections: ExtendedEnabledSection[] = [
  'ASSIGNED_LEARNING',
  'SELF_RESULTS',
  'SELF_TESTING',
  'MANAGER_CURRENT_SUPERVISION',
  'MANAGER_HISTORICAL_ANALYTICS',
  'EXPERT_CONTENT',
  'EXPERT_QUESTION_ANALYTICS',
  'ADMINISTRATION',
];

function looksBrokenDisplayName(value?: string | null): boolean {
  if (!value) {
    return true;
  }

  const trimmed = value.trim();
  if (!trimmed) {
    return true;
  }

  return /^[?\s]+$/.test(trimmed);
}

function resolveDisplayName(response: CurrentActorResponse): string | undefined {
  if (!looksBrokenDisplayName(response.displayName)) {
    return response.displayName ?? undefined;
  }

  return resolveDemoActorLabel(response.username ?? undefined) ?? response.employeeNumber ?? response.username ?? undefined;
}

function normalizeRoles(roles?: string[] | null): CurrentActorRole[] {
  const normalized = Array.isArray(roles)
    ? roles
        .filter(Boolean)
        .map((role) => role.trim())
        .filter(Boolean)
        .map((role) => {
          const upperRole = role.toUpperCase();
          switch (upperRole) {
            case 'ROLE_USER':
              return 'ROLE_USER';
            case 'ROLE_OPERATIONS':
            case 'ROLE_OPERATOR':
              return 'OPERATOR';
            case 'ROLE_MANAGER':
              return 'MANAGER';
            case 'ROLE_EXPERT':
              return 'EXPERT';
            case 'ROLE_ADMIN':
              return 'ADMIN';
            case 'ROLE_SYSTEM_ADMIN':
              return 'SYSTEM_ADMIN';
            case 'ROLE_SUPER_ADMIN':
              return 'SUPER_ADMIN';
            default:
              return upperRole.startsWith('ROLE_') ? upperRole.slice(5) : upperRole;
          }
        })
    : [];
  const businessRoles = normalized.filter((role) => role !== 'ROLE_USER');
  return businessRoles.length > 0 ? businessRoles : normalized;
}

export function mapCurrentActor(response: CurrentActorResponse): CurrentActor {
  return {
    actorUserId: response.actorUserId ?? undefined,
    username: response.username ?? undefined,
    displayName: resolveDisplayName(response),
    positionTitle: response.positionTitle ?? undefined,
    employeeNumber: response.employeeNumber ?? response.username ?? undefined,
    primaryOrganizationalUnitName: response.primaryOrganizationalUnitName ?? undefined,
    primaryOrganizationalUnitPath: response.primaryOrganizationalUnitPath ?? undefined,
    roles: normalizeRoles(response.roles),
    enabledSections: Array.isArray(response.enabledSections)
      ? response.enabledSections.filter((section): section is ExtendedEnabledSection =>
          knownSections.includes(section as ExtendedEnabledSection),
        )
      : [],
  };
}

export function hasSection(actor: CurrentActor, section: EnabledSection): boolean {
  return actor.enabledSections.includes(section);
}

export function hasExtendedSection(
  actor: CurrentActor,
  section: ExtendedEnabledSection,
): boolean {
  return actor.enabledSections.includes(section);
}

export function hasRole(actor: CurrentActor, role: CurrentActorRole): boolean {
  return actor.roles.includes(role);
}

export function hasAnyRole(actor: CurrentActor, roles: CurrentActorRole[]): boolean {
  return roles.some((role) => actor.roles.includes(role));
}

export function canAccessManagerArea(actor: CurrentActor): boolean {
  return (
    hasRole(actor, 'MANAGER') ||
    hasExtendedSection(actor, 'MANAGER_CURRENT_SUPERVISION') ||
    hasExtendedSection(actor, 'MANAGER_HISTORICAL_ANALYTICS')
  );
}

export function canAccessExpertArea(actor: CurrentActor): boolean {
  return (
    hasAnyRole(actor, ['EXPERT', 'ADMIN', 'SYSTEM_ADMIN', 'SUPER_ADMIN']) ||
    hasExtendedSection(actor, 'EXPERT_CONTENT') ||
    hasExtendedSection(actor, 'EXPERT_QUESTION_ANALYTICS')
  );
}

export function canAccessAssignmentCampaignArea(actor: CurrentActor): boolean {
  return hasAnyRole(actor, ['EXPERT', 'ADMIN', 'SYSTEM_ADMIN', 'SUPER_ADMIN']);
}

export function canAccessAdministrationArea(actor: CurrentActor): boolean {
  return (
    hasAnyRole(actor, ['ADMIN', 'SYSTEM_ADMIN', 'SUPER_ADMIN']) ||
    hasExtendedSection(actor, 'ADMINISTRATION')
  );
}
