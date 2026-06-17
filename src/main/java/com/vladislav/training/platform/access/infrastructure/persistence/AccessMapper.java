package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Преобразователь {@code AccessMapper}.
 */
@Component
public class AccessMapper {

    public UserAccessArea toDomain(UserAccessAreaEntity entity) {
        return new UserAccessArea(
            entity.getId(),
            entity.getUserId(),
            entity.getOrganizationalUnitId(),
            entity.getAccessScopeType(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public UserAccessAreaEntity toEntity(UserAccessArea domain) {
        UserAccessAreaEntity entity = new UserAccessAreaEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setOrganizationalUnitId(domain.organizationalUnitId());
        entity.setAccessScopeType(domain.accessScopeType());
        entity.setValidFrom(domain.validFrom());
        entity.setValidTo(domain.validTo());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<UserAccessArea> toUserAccessAreas(List<UserAccessAreaEntity> entities) {
        return entities.stream().map(this::toDomain).toList();
    }

    public ManagementRelation toDomain(ManagementRelationEntity entity) {
        return new ManagementRelation(
            entity.getId(),
            entity.getUserId(),
            entity.getOrganizationalUnitId(),
            entity.getManagementRelationTypeId(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public ManagementRelationEntity toEntity(ManagementRelation domain) {
        ManagementRelationEntity entity = new ManagementRelationEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setOrganizationalUnitId(domain.organizationalUnitId());
        entity.setManagementRelationTypeId(domain.managementRelationTypeId());
        entity.setValidFrom(domain.validFrom());
        entity.setValidTo(domain.validTo());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<ManagementRelation> toManagementRelations(List<ManagementRelationEntity> entities) {
        return entities.stream().map(this::toDomain).toList();
    }

    public ManagementRelationType toDomain(ManagementRelationTypeEntity entity) {
        return new ManagementRelationType(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public List<ManagementRelationType> toManagementRelationTypes(List<ManagementRelationTypeEntity> entities) {
        return entities.stream().map(this::toDomain).toList();
    }

    public TemporaryRoleAssignment toDomain(TemporaryRoleAssignmentEntity entity) {
        return new TemporaryRoleAssignment(
            entity.getId(),
            entity.getUserId(),
            entity.getRoleId(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public TemporaryRoleAssignmentEntity toEntity(TemporaryRoleAssignment domain) {
        TemporaryRoleAssignmentEntity entity = new TemporaryRoleAssignmentEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setRoleId(domain.roleId());
        entity.setValidFrom(domain.validFrom());
        entity.setValidTo(domain.validTo());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<TemporaryRoleAssignment> toTemporaryRoleAssignments(List<TemporaryRoleAssignmentEntity> entities) {
        return entities.stream().map(this::toDomain).toList();
    }

    public TemporaryAccessArea toDomain(TemporaryAccessAreaEntity entity) {
        return new TemporaryAccessArea(
            entity.getId(),
            entity.getUserId(),
            entity.getOrganizationalUnitId(),
            entity.getAccessScopeType(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public TemporaryAccessAreaEntity toEntity(TemporaryAccessArea domain) {
        TemporaryAccessAreaEntity entity = new TemporaryAccessAreaEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setOrganizationalUnitId(domain.organizationalUnitId());
        entity.setAccessScopeType(domain.accessScopeType());
        entity.setValidFrom(domain.validFrom());
        entity.setValidTo(domain.validTo());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<TemporaryAccessArea> toTemporaryAccessAreas(List<TemporaryAccessAreaEntity> entities) {
        return entities.stream().map(this::toDomain).toList();
    }

    public TemporaryManagementDelegation toDomain(TemporaryManagementDelegationEntity entity) {
        return new TemporaryManagementDelegation(
            entity.getId(),
            entity.getUserId(),
            entity.getOrganizationalUnitId(),
            entity.getManagementRelationTypeId(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public TemporaryManagementDelegationEntity toEntity(TemporaryManagementDelegation domain) {
        TemporaryManagementDelegationEntity entity = new TemporaryManagementDelegationEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setOrganizationalUnitId(domain.organizationalUnitId());
        entity.setManagementRelationTypeId(domain.managementRelationTypeId());
        entity.setValidFrom(domain.validFrom());
        entity.setValidTo(domain.validTo());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<TemporaryManagementDelegation> toTemporaryManagementDelegations(
        List<TemporaryManagementDelegationEntity> entities
    ) {
        return entities.stream().map(this::toDomain).toList();
    }
}
