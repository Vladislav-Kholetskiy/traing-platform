package com.vladislav.training.platform.access.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataManagementRelationTypeJpaRepository}.
 */
public interface SpringDataManagementRelationTypeJpaRepository extends JpaRepository<ManagementRelationTypeEntity, Long> {

    Optional<ManagementRelationTypeEntity> findByCode(String code);

    List<ManagementRelationTypeEntity> findAllByOrderByCodeAsc();
}
