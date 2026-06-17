package com.vladislav.training.platform.access.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataManagementRelationJpaRepository}.
 */
public interface SpringDataManagementRelationJpaRepository
    extends JpaRepository<ManagementRelationEntity, Long>, JpaSpecificationExecutor<ManagementRelationEntity> {

    List<ManagementRelationEntity> findAllByUserIdOrderByValidFromDescIdDesc(Long userId);

    List<ManagementRelationEntity> findAllByOrganizationalUnitIdOrderByValidFromDescIdDesc(Long organizationalUnitId);

    @Query(
        """
        select relation
        from ManagementRelationEntity relation
        where relation.userId = :userId
          and relation.validFrom <= :activeAt
          and (relation.validTo is null or relation.validTo > :activeAt)
        order by relation.validFrom desc, relation.id desc
        """
    )
    List<ManagementRelationEntity> findActiveByUserId(@Param("userId") Long userId, @Param("activeAt") Instant activeAt);
}
