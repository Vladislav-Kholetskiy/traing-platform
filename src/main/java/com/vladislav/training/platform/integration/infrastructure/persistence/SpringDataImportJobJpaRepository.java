package com.vladislav.training.platform.integration.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataImportJobJpaRepository}.
 */
public interface SpringDataImportJobJpaRepository extends JpaRepository<ImportJobEntity, Long> {

    List<ImportJobEntity> findAllByStatusOrderByIdAsc(String status);

    List<ImportJobEntity> findAllBySourceTypeOrderByIdAsc(String sourceType);
}
