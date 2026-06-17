package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Преобразователь {@code UserOrgMapper}.
 */
@Component
public class UserOrgMapper {

    public OrganizationalUnit toDomain(OrganizationalUnitEntity entity) {
        return new OrganizationalUnit(
            entity.getId(),
            entity.getParentId(),
            entity.getOrganizationalUnitTypeId(),
            entity.getName(),
            entity.getStatus(),
            entity.getPath(),
            entity.getDepth(),
            entity.getExternalId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public OrganizationalUnitEntity toEntity(OrganizationalUnit domain) {
        OrganizationalUnitEntity entity = new OrganizationalUnitEntity();
        entity.setId(domain.id());
        entity.setParentId(domain.parentId());
        entity.setOrganizationalUnitTypeId(domain.organizationalUnitTypeId());
        entity.setName(domain.name());
        entity.setStatus(domain.status());
        entity.setPath(domain.path());
        entity.setDepth(domain.depth());
        entity.setExternalId(domain.externalId());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<OrganizationalUnit> toOrganizationalUnitDomains(List<OrganizationalUnitEntity> entities) {
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }

    public OrganizationalUnitType toDomain(OrganizationalUnitTypeEntity entity) {
        return new OrganizationalUnitType(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getDescription(),
            entity.getNodeKind(),
            entity.isCanBeOperatorHomeUnit(),
            entity.isCanBeCampaignTarget(),
            entity.isParticipatesInSubtreeScope(),
            entity.isCanHaveManagementRelation(),
            entity.isCanHaveAccessArea(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public OrganizationalUnitTypeEntity toEntity(OrganizationalUnitType domain) {
        OrganizationalUnitTypeEntity entity = new OrganizationalUnitTypeEntity();
        entity.setId(domain.id());
        entity.setCode(domain.code());
        entity.setName(domain.name());
        entity.setDescription(domain.description());
        entity.setNodeKind(domain.nodeKind());
        entity.setCanBeOperatorHomeUnit(domain.canBeOperatorHomeUnit());
        entity.setCanBeCampaignTarget(domain.canBeCampaignTarget());
        entity.setParticipatesInSubtreeScope(domain.participatesInSubtreeScope());
        entity.setCanHaveManagementRelation(domain.canHaveManagementRelation());
        entity.setCanHaveAccessArea(domain.canHaveAccessArea());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<OrganizationalUnitType> toOrganizationalUnitTypeDomains(List<OrganizationalUnitTypeEntity> entities) {
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }

    public AppUser toDomain(AppUserEntity entity) {
        return new AppUser(
            entity.getId(),
            entity.getEmployeeNumber(),
            entity.getExternalId(),
            entity.getLastName(),
            entity.getFirstName(),
            entity.getMiddleName(),
            entity.getPositionTitle(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public AppUserEntity toEntity(AppUser domain) {
        AppUserEntity entity = new AppUserEntity();
        entity.setId(domain.id());
        entity.setEmployeeNumber(domain.employeeNumber());
        entity.setExternalId(domain.externalId());
        entity.setLastName(domain.lastName());
        entity.setFirstName(domain.firstName());
        entity.setMiddleName(domain.middleName());
        entity.setPositionTitle(domain.positionTitle());
        entity.setStatus(domain.status());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<AppUser> toAppUsers(List<AppUserEntity> entities) {
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }

    public AppRole toDomain(AppRoleEntity entity) {
        return new AppRole(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public AppRoleEntity toEntity(AppRole domain) {
        AppRoleEntity entity = new AppRoleEntity();
        entity.setId(domain.id());
        entity.setCode(domain.code());
        entity.setName(domain.name());
        entity.setDescription(domain.description());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<AppRole> toAppRoles(List<AppRoleEntity> entities) {
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }

    public UserRoleAssignment toDomain(UserRoleAssignmentEntity entity) {
        return new UserRoleAssignment(
            entity.getId(),
            entity.getUserId(),
            entity.getRoleId(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public UserRoleAssignmentEntity toEntity(UserRoleAssignment domain) {
        UserRoleAssignmentEntity entity = new UserRoleAssignmentEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setRoleId(domain.roleId());
        entity.setValidFrom(domain.validFrom());
        entity.setValidTo(domain.validTo());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<UserRoleAssignment> toUserRoleAssignments(List<UserRoleAssignmentEntity> entities) {
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }

    public UserOrganizationAssignment toDomain(UserOrganizationAssignmentEntity entity) {
        return new UserOrganizationAssignment(
            entity.getId(),
            entity.getUserId(),
            entity.getOrganizationalUnitId(),
            entity.getAssignmentType(),
            entity.getValidFrom(),
            entity.getValidTo(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public UserOrganizationAssignmentEntity toEntity(UserOrganizationAssignment domain) {
        UserOrganizationAssignmentEntity entity = new UserOrganizationAssignmentEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setOrganizationalUnitId(domain.organizationalUnitId());
        entity.setAssignmentType(domain.assignmentType());
        entity.setValidFrom(domain.validFrom());
        entity.setValidTo(domain.validTo());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<UserOrganizationAssignment> toUserOrganizationAssignments(List<UserOrganizationAssignmentEntity> entities) {
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }

}
