import { apiClient } from '../../../shared/api/apiClient';
import type { AuditEvent, AuditFilter } from '../model/audit';

function buildAuditUrl(filter?: AuditFilter) {
  const params = new URLSearchParams();
  Object.entries(filter ?? {}).forEach(([key, value]) => {
    if (value != null && value !== '') {
      params.set(key, String(value));
    }
  });
  return params.size ? `/api/v1/admin/audit-events?${params.toString()}` : '/api/v1/admin/audit-events';
}

export function getAuditEvents(filter?: AuditFilter) {
  return apiClient.get<AuditEvent[]>(buildAuditUrl(filter));
}

export function getAuditEvent(auditEventId: number) {
  return apiClient.get<AuditEvent>(`/api/v1/admin/audit-events/${auditEventId}`);
}
