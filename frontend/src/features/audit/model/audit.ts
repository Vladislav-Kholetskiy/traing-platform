export type AuditEvent = {
  id: number;
  eventType?: string | null;
  entityType?: string | null;
  entityId?: string | null;
  actorUserId?: number | null;
  occurredAt?: string | null;
  correlationId?: string | null;
  requestId?: string | null;
  createdAt?: string | null;
};

export type AuditFilter = {
  eventType?: string;
  entityType?: string;
  entityId?: string;
  actorUserId?: number;
  occurredFrom?: string;
  occurredTo?: string;
};
