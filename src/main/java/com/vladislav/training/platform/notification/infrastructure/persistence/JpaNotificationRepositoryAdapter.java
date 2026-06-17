package com.vladislav.training.platform.notification.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Адаптер репозитория {@code JpaNotificationRepositoryAdapter}.
 */

@Repository
@Transactional(readOnly = true)
public class JpaNotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationJpaRepository repository;
    private final NotificationMapper mapper;

    public JpaNotificationRepositoryAdapter(
        SpringDataNotificationJpaRepository repository,
        NotificationMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Notification findNotificationById(Long notificationId) {
        return repository.findById(notificationId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
    }

    @Override
    public Notification findNotificationByIdAndRecipientUserId(Long notificationId, Long recipientUserId) {
        return repository.findByIdAndRecipientUserId(notificationId, recipientUserId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
    }

    @Override
    public List<Notification> findPendingEligibleDispatchCandidates(Instant now, int limit) {
        return mapper.toNotifications(
            repository.findPendingEligibleDispatchCandidates(now, PageRequest.of(0, limit))
        );
    }

    @Override
    public List<Notification> findPendingEligibleDispatchNotifications(Instant now, int limit) {
        return mapper.toNotifications(repository.findPendingEligibleDispatchNotifications(now, limit));
    }

    @Override
    public List<Notification> findNotificationsByRecipientUserId(Long recipientUserId) {
        return mapper.toNotifications(repository.findAllByRecipientUserIdOrderByIdAsc(recipientUserId));
    }

    @Override
    public List<Notification> findNotificationsByStatus(NotificationStatus status) {
        return mapper.toNotifications(repository.findAllByStatusOrderByIdAsc(status.name()));
    }

    @Override
    public List<Notification> findNotificationsScheduledAtOrBefore(Instant scheduledAt) {
        return mapper.toNotifications(repository.findAllByScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(scheduledAt));
    }

    @Override
    public List<Notification> findNotificationsBySourceEntity(String sourceEntityType, String sourceEntityId) {
        return mapper.toNotifications(
            repository.findAllBySourceEntityTypeAndSourceEntityIdOrderByIdAsc(sourceEntityType, sourceEntityId)
        );
    }

    @Override
    public List<Notification> findNotificationsByDedupKey(String dedupKey) {
        return mapper.toNotifications(repository.findAllByDedupKeyOrderByIdAsc(dedupKey));
    }

    @Override
    @Transactional
    public Notification saveNotification(Notification notification) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(notification)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist notification", exception);
        }
    }
}
