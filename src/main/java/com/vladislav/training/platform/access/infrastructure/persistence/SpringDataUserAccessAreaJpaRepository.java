package com.vladislav.training.platform.access.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataUserAccessAreaJpaRepository}.
 */
public interface SpringDataUserAccessAreaJpaRepository
    extends JpaRepository<UserAccessAreaEntity, Long>, JpaSpecificationExecutor<UserAccessAreaEntity> {

    List<UserAccessAreaEntity> findAllByUserIdOrderByValidFromDescIdDesc(Long userId);

    List<UserAccessAreaEntity> findAllByOrganizationalUnitIdOrderByValidFromDescIdDesc(Long organizationalUnitId);

    @Query(
        """
        select area
        from UserAccessAreaEntity area
        where area.userId = :userId
          and area.validFrom <= :activeAt
          and (area.validTo is null or area.validTo > :activeAt)
        order by area.validFrom desc, area.id desc
        """
    )
    List<UserAccessAreaEntity> findActiveByUserId(@Param("userId") Long userId, @Param("activeAt") Instant activeAt);
}
