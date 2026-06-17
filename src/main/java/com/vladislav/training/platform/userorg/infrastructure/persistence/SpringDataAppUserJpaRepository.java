package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Контракт репозитория {@code SpringDataAppUserJpaRepository}.
 */
public interface SpringDataAppUserJpaRepository
    extends JpaRepository<AppUserEntity, Long>, JpaSpecificationExecutor<AppUserEntity> {

    Optional<AppUserEntity> findByEmployeeNumber(String employeeNumber);

    List<AppUserEntity> findAllByOrderByIdAsc();

    List<AppUserEntity> findAllByStatusOrderByIdAsc(UserStatus status);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByExternalId(String externalId);
}
