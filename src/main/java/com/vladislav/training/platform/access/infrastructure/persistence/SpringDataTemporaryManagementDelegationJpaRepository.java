package com.vladislav.training.platform.access.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataTemporaryManagementDelegationJpaRepository}.
 */
public interface SpringDataTemporaryManagementDelegationJpaRepository
    extends JpaRepository<TemporaryManagementDelegationEntity, Long>, JpaSpecificationExecutor<TemporaryManagementDelegationEntity> {

    List<TemporaryManagementDelegationEntity> findAllByUserIdOrderByValidFromDescIdDesc(Long userId);

    @Query(
        """
        select delegation
        from TemporaryManagementDelegationEntity delegation
        where delegation.userId = :userId
          and delegation.validFrom <= :activeAt
          and (delegation.validTo is null or delegation.validTo > :activeAt)
        order by delegation.validFrom desc, delegation.id desc
        """
    )
    List<TemporaryManagementDelegationEntity> findActiveByUserId(
        @Param("userId") Long userId,
        @Param("activeAt") Instant activeAt
    );
}
