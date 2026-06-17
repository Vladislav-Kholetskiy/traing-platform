package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataOrganizationalUnitTypeJpaRepository}.
 */
public interface SpringDataOrganizationalUnitTypeJpaRepository extends JpaRepository<OrganizationalUnitTypeEntity, Long> {

    Optional<OrganizationalUnitTypeEntity> findByCode(String code);

    List<OrganizationalUnitTypeEntity> findAllByNodeKindOrderByIdAsc(OrganizationalNodeKind nodeKind);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
