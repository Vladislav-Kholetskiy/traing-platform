package com.vladislav.training.platform.userorg.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataAppRoleJpaRepository}.
 */
public interface SpringDataAppRoleJpaRepository extends JpaRepository<AppRoleEntity, Long> {

    Optional<AppRoleEntity> findByCode(String code);

    List<AppRoleEntity> findAllByOrderByIdAsc();
}
