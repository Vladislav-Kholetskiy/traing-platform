package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import java.time.Instant;
import java.util.List;

/**
 * Ответ {@code OrganizationalUnitResponse}.
 */
public record OrganizationalUnitResponse(
    Long id,
    Long parentId,
    Long organizationalUnitTypeId,
    String name,
    OrganizationalUnitStatus status,
    String path,
    int depth,
    String externalId,
    Instant createdAt,
    Instant updatedAt,
    List<OrganizationalUnitResponse> children
) {
}