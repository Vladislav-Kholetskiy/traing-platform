package com.vladislav.training.platform.audit.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataAuditEventJpaRepository}.
 */
public interface SpringDataAuditEventJpaRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findAllByOrderByOccurredAtAscIdAsc();

    List<AuditEventEntity> findAllByEventTypeOrderByOccurredAtAscIdAsc(String eventType);

    List<AuditEventEntity> findAllByEntityTypeAndEntityIdOrderByOccurredAtAscIdAsc(String entityType, String entityId);

    List<AuditEventEntity> findAllByActorUserIdOrderByOccurredAtAscIdAsc(Long actorUserId);
}
