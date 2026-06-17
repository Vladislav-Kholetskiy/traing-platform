package com.vladislav.training.platform.userorg.service;

import java.time.Instant;
import java.util.Set;

/**
 * Контракт сервиса чтения {@code OrganizationalTargetingQueryService}.
 */
public interface OrganizationalTargetingQueryService {

    Set<Long> resolveCurrentCandidateUserIdsForUnitSubtree(String organizationalUnitPath, Instant effectiveAt);
}
