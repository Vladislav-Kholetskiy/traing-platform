package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import java.time.Instant;

/**
 * Ответ {@code OrganizationalUnitTypeResponse}.
 */
public record OrganizationalUnitTypeResponse(
    Long id,
    String code,
    String name,
    String description,
    OrganizationalNodeKind nodeKind,
    boolean canBeOperatorHomeUnit,
    boolean canBeCampaignTarget,
    boolean participatesInSubtreeScope,
    boolean canHaveManagementRelation,
    boolean canHaveAccessArea,
    Instant createdAt,
    Instant updatedAt
) {
}