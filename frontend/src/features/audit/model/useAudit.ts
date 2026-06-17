import { useQuery } from '@tanstack/react-query';
import { getAuditEvent, getAuditEvents } from '../api/auditApi';
import type { AuditFilter } from './audit';

export const auditKeys = {
  all: ['audit'] as const,
  list: (filter?: AuditFilter) => [...auditKeys.all, 'list', filter ?? {}] as const,
  detail: (id: number) => [...auditKeys.all, 'detail', id] as const,
};

export function useAuditEvents(filter?: AuditFilter, enabled = true) {
  return useQuery({
    queryKey: auditKeys.list(filter),
    queryFn: () => getAuditEvents(filter),
    enabled,
  });
}

export function useAuditEvent(auditEventId?: number, enabled = true) {
  return useQuery({
    queryKey: auditEventId ? auditKeys.detail(auditEventId) : [...auditKeys.all, 'detail', 'missing'],
    queryFn: () => getAuditEvent(auditEventId as number),
    enabled: enabled && Boolean(auditEventId),
  });
}
