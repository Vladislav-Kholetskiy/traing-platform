package com.vladislav.training.platform.integration.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataImportJobItemJpaRepository}.
 */
public interface SpringDataImportJobItemJpaRepository extends JpaRepository<ImportJobItemEntity, Long> {

    List<ImportJobItemEntity> findAllByImportJobIdOrderByItemNoAsc(Long importJobId);

    List<ImportJobItemEntity> findAllByStatusOrderByIdAsc(String status);
}
