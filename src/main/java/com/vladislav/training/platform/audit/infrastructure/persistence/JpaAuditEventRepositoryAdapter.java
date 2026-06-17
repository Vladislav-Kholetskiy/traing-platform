package com.vladislav.training.platform.audit.infrastructure.persistence;

import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.repository.AuditEventRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaAuditEventRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaAuditEventRepositoryAdapter implements AuditEventRepository {

    private final SpringDataAuditEventJpaRepository repository;
    private final AuditMapper mapper;

    public JpaAuditEventRepositoryAdapter(
        SpringDataAuditEventJpaRepository repository,
        AuditMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AuditEvent findAuditEventById(Long auditEventId) {
        return repository.findById(auditEventId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Audit event not found by id: " + auditEventId));
    }

    @Override
    public List<AuditEvent> findAllAuditEvents() {
        return mapper.toDomainEvents(repository.findAllByOrderByOccurredAtAscIdAsc());
    }

    @Override
    public List<AuditEvent> findAuditEventsByEventType(AuditEventType eventType) {
        return mapper.toDomainEvents(repository.findAllByEventTypeOrderByOccurredAtAscIdAsc(eventType.value()));
    }

    @Override
    public List<AuditEvent> findAuditEventsByEntity(String entityType, String entityId) {
        return mapper.toDomainEvents(repository.findAllByEntityTypeAndEntityIdOrderByOccurredAtAscIdAsc(entityType, entityId));
    }

    @Override
    public List<AuditEvent> findAuditEventsByActorUserId(Long actorUserId) {
        return mapper.toDomainEvents(repository.findAllByActorUserIdOrderByOccurredAtAscIdAsc(actorUserId));
    }

    @Override
    @Transactional
    public AuditEvent saveAuditEvent(AuditEvent auditEvent) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(auditEvent)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist audit_event", exception);
        }
    }
}
