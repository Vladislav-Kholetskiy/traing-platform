package com.vladislav.training.platform.application.actor;

import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.service.RoleCodeNormalizer;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppRoleRepository;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code CurrentActorReadService}.
 */
@Service
@Transactional(readOnly = true)
public class CurrentActorReadService {

    private static final Set<String> ADMIN_ROLE_CODES = Set.of("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> EXPERT_ROLE_CODES = Set.of("EXPERT", "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> MANAGER_ROLE_CODES = Set.of("MANAGER", "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> LEARNER_ROLE_CODES = Set.of("OPERATOR", "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");

    private final InteractiveActorResolver interactiveActorResolver;
    private final AppUserRepository appUserRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final AppRoleRepository appRoleRepository;
    private final TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService;
    private final UtcClock utcClock;

    public CurrentActorReadService(
        InteractiveActorResolver interactiveActorResolver,
        AppUserRepository appUserRepository,
        UserRoleAssignmentRepository userRoleAssignmentRepository,
        UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
        OrganizationalUnitRepository organizationalUnitRepository,
        AppRoleRepository appRoleRepository,
        TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService,
        UtcClock utcClock
    ) {
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
        this.appUserRepository = Objects.requireNonNull(appUserRepository, "appUserRepository must not be null");
        this.userRoleAssignmentRepository = Objects.requireNonNull(
            userRoleAssignmentRepository,
            "userRoleAssignmentRepository must not be null"
        );
        this.userOrganizationAssignmentRepository = Objects.requireNonNull(
            userOrganizationAssignmentRepository,
            "userOrganizationAssignmentRepository must not be null"
        );
        this.organizationalUnitRepository = Objects.requireNonNull(
            organizationalUnitRepository,
            "organizationalUnitRepository must not be null"
        );
        this.appRoleRepository = Objects.requireNonNull(appRoleRepository, "appRoleRepository must not be null");
        this.temporaryRoleAssignmentReadService = Objects.requireNonNull(
            temporaryRoleAssignmentReadService,
            "temporaryRoleAssignmentReadService must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    public CurrentActorResponse readCurrentActor() {
        ResolvedAuthenticatedActor resolvedActor = interactiveActorResolver.resolveActor();
        AppUser user = appUserRepository.findUserById(resolvedActor.actorUserId());
        Instant effectiveAt = utcClock.now();

        List<String> roleCodes = loadRoleCodes(user.id(), effectiveAt);
        OrganizationalUnit primaryUnit = resolvePrimaryUnit(user.id(), effectiveAt);
        return new CurrentActorResponse(
            user.id(),
            safeUsername(user, resolvedActor),
            safeDisplayName(user, resolvedActor),
            user.positionTitle(),
            user.employeeNumber(),
            primaryUnit != null ? primaryUnit.name() : null,
            primaryUnit != null ? primaryUnit.path() : null,
            roleCodes,
            determineEnabledSections(user.status(), roleCodes)
        );
    }

    private List<String> loadRoleCodes(Long actorUserId, Instant effectiveAt) {
        LinkedHashSet<String> roleCodes = new LinkedHashSet<>();
        userRoleAssignmentRepository.findActiveRoleAssignmentsByUserId(actorUserId, effectiveAt)
            .forEach(assignment -> roleCodes.add(
                RoleCodeNormalizer.normalize(appRoleRepository.findRoleById(assignment.roleId()).code())
            ));
        temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(actorUserId, effectiveAt)
            .forEach(roleId -> roleCodes.add(
                RoleCodeNormalizer.normalize(appRoleRepository.findRoleById(roleId).code())
            ));
        return roleCodes.stream().toList();
    }

    private String safeUsername(AppUser user, ResolvedAuthenticatedActor resolvedActor) {
        if (user.employeeNumber() != null && !user.employeeNumber().isBlank()) {
            return user.employeeNumber();
        }
        String principalName = resolvedActor.principalName();
        if (principalName != null && !principalName.isBlank()) {
            return principalName;
        }
        return "actor-" + user.id();
    }

    private String safeDisplayName(AppUser user, ResolvedAuthenticatedActor resolvedActor) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, user.lastName());
        appendPart(builder, user.firstName());
        appendPart(builder, user.middleName());
        if (builder.length() > 0) {
            return builder.toString();
        }
        return safeUsername(user, resolvedActor);
    }

    private OrganizationalUnit resolvePrimaryUnit(Long actorUserId, Instant effectiveAt) {
        return userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(actorUserId, effectiveAt).stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .findFirst()
            .map(assignment -> organizationalUnitRepository.findOrganizationalUnitById(assignment.organizationalUnitId()))
            .orElse(null);
    }

    private void appendPart(StringBuilder builder, String part) {
        if (part == null) {
            return;
        }
        String trimmed = part.trim();
        if (trimmed.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(trimmed);
    }

    private List<String> determineEnabledSections(UserStatus status, List<String> roleCodes) {
        if (status != UserStatus.ACTIVE) {
            return List.of();
        }

        LinkedHashSet<String> sections = new LinkedHashSet<>();
        if (matchesAny(roleCodes, LEARNER_ROLE_CODES)) {
            sections.add("ASSIGNED_LEARNING");
            sections.add("SELF_TESTING");
            sections.add("SELF_RESULTS");
        }
        if (matchesAny(roleCodes, MANAGER_ROLE_CODES)) {
            sections.add("MANAGER_CURRENT_SUPERVISION");
            sections.add("MANAGER_HISTORICAL_ANALYTICS");
        }
        if (matchesAny(roleCodes, EXPERT_ROLE_CODES)) {
            sections.add("EXPERT_CONTENT");
            sections.add("EXPERT_QUESTION_ANALYTICS");
        }
        if (matchesAny(roleCodes, ADMIN_ROLE_CODES)) {
            sections.add("ADMINISTRATION");
        }
        return sections.stream().toList();
    }

    private boolean matchesAny(List<String> roleCodes, Set<String> allowedRoleCodes) {
        return roleCodes.stream()
            .filter(Objects::nonNull)
            .map(roleCode -> roleCode.toUpperCase(Locale.ROOT))
            .anyMatch(allowedRoleCodes::contains);
    }
}

