package com.vladislav.training.platform.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataNotificationJpaRepository}.
 */
public interface SpringDataNotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    java.util.Optional<NotificationEntity> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    @Query("""
        select n
        from NotificationEntity n
        where n.status = 'PENDING'
          and (n.scheduledAt is null or n.scheduledAt <= :now)
        order by
          case when n.scheduledAt is null then 0 else 1 end,
          n.scheduledAt asc,
          n.id asc
        """)
    List<NotificationEntity> findPendingEligibleDispatchCandidates(@Param("now") Instant now, Pageable pageable);

    default List<NotificationEntity> findPendingEligibleDispatchCandidates(Instant now, int limit) {
        return findPendingEligibleDispatchCandidates(now, PageRequest.of(0, limit));
    }

    default List<NotificationEntity> findPendingEligibleDispatchNotifications(Instant now, int limit) {
        return findPendingEligibleDispatchCandidates(now, limit);
    }

    List<NotificationEntity> findAllByRecipientUserIdOrderByIdAsc(Long recipientUserId);

    List<NotificationEntity> findAllByStatusOrderByIdAsc(String status);

    List<NotificationEntity> findAllByScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(Instant scheduledAt);

    List<NotificationEntity> findAllBySourceEntityTypeAndSourceEntityIdOrderByIdAsc(
        String sourceEntityType,
        String sourceEntityId
    );

    List<NotificationEntity> findAllByDedupKeyOrderByIdAsc(String dedupKey);
}
