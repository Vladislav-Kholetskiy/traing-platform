package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataOrganizationalUnitJpaRepository}.
 */
public interface SpringDataOrganizationalUnitJpaRepository
    extends JpaRepository<OrganizationalUnitEntity, Long>, JpaSpecificationExecutor<OrganizationalUnitEntity> {

    Optional<OrganizationalUnitEntity> findByExternalId(String externalId);

    Optional<OrganizationalUnitEntity> findByPath(String path);

    @Query(
        """
        select unit
        from OrganizationalUnitEntity unit
        where unit.path = :subtreePath
           or unit.path like concat(:subtreePath, '/%')
        order by unit.id asc
        """
    )
    List<OrganizationalUnitEntity> findAllInSubtreeByPath(@Param("subtreePath") String subtreePath);

    List<OrganizationalUnitEntity> findAllByParentIdIsNullOrderByIdAsc();

    List<OrganizationalUnitEntity> findAllByParentIdOrderByIdAsc(Long parentId);

    List<OrganizationalUnitEntity> findAllByStatusOrderByIdAsc(OrganizationalUnitStatus status);

    boolean existsByPath(String path);

    boolean existsByPathAndIdNot(String path, Long id);

    boolean existsByExternalId(String externalId);

    boolean existsByExternalIdAndIdNot(String externalId, Long id);
}
