package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code QueryPolicyContextResolver}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class QueryPolicyContextResolverTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T19:00:00Z");
    private static final Long ACTOR_USER_ID = 101L;
    private static final Path RESOLVER_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContextResolver.java"
    );

    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    @Test
    void administrativeQueryBuildersMustExistAsDedicatedNotificationAndImportAndAuditResolvers() throws Exception {
        assertThat(methodNames(AccessPolicyQueryContextResolver.class))
            .contains(
                "resolveNotificationAdministrationContext",
                "resolveNotificationAdministrationDetailContext",
                "resolveNotificationRuleAdministrationContext",
                "resolveNotificationRuleAdministrationDetailContext",
                "resolveImportJobAdministrationContext",
                "resolveImportJobAdministrationDetailContext",
                "resolveImportJobItemAdministrationContext",
                "resolveImportJobItemAdministrationDetailContext",
                "resolveAuditEventAdministrationContext",
                "resolveAuditEventAdministrationDetailContext"
            );

        String source = Files.readString(RESOLVER_SOURCE);
        assertThat(source)
            .contains("resolveNotificationAdministrationContext(")
            .contains("resolveNotificationAdministrationDetailContext(")
            .contains("resolveNotificationRuleAdministrationContext(")
            .contains("resolveNotificationRuleAdministrationDetailContext(")
            .contains("resolveImportJobAdministrationContext(")
            .contains("resolveImportJobAdministrationDetailContext(")
            .contains("resolveImportJobItemAdministrationContext(")
            .contains("resolveImportJobItemAdministrationDetailContext(")
            .contains("resolveAuditEventAdministrationContext(")
            .contains("resolveAuditEventAdministrationDetailContext(");
    }

    @Test
    void notificationAdministrationResolversMustBuildDedicatedAdministrativeContexts() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);
        AccessPolicyQueryContext listContext = invokeContext(
            resolver,
            "resolveNotificationAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { ACTOR_USER_ID }
        );
        AccessPolicyQueryContext detailContext = invokeContext(
            resolver,
            "resolveNotificationAdministrationDetailContext",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { ACTOR_USER_ID, 701L }
        );

        assertAdministrativeContext(listContext, AccessReadArea.NOTIFICATION_ADMINISTRATION, AccessReadType.LIST, "notification");
        assertAdministrativeContext(detailContext, AccessReadArea.NOTIFICATION_ADMINISTRATION, AccessReadType.DETAIL, "notification");
        assertForbiddenContourIsolation(listContext);
        assertForbiddenContourIsolation(detailContext);
        assertNoGenericFamily(detailContext);

        verify(interactiveActorResolver, times(2)).resolveActorUserId();
        verify(utcClock, times(2)).now();
    }

    @Test
    void notificationRuleAdministrationResolversMustBuildDedicatedAdministrativeContexts() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);
        AccessPolicyQueryContext listContext = invokeContext(
            resolver,
            "resolveNotificationRuleAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { ACTOR_USER_ID }
        );
        AccessPolicyQueryContext detailContext = invokeContext(
            resolver,
            "resolveNotificationRuleAdministrationDetailContext",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { ACTOR_USER_ID, 702L }
        );

        assertAdministrativeContext(listContext, AccessReadArea.NOTIFICATION_RULE_ADMINISTRATION, AccessReadType.LIST, "notification_rule");
        assertAdministrativeContext(detailContext, AccessReadArea.NOTIFICATION_RULE_ADMINISTRATION, AccessReadType.DETAIL, "notification_rule");
        assertForbiddenContourIsolation(listContext);
        assertForbiddenContourIsolation(detailContext);
    }

    @Test
    void importAdministrationResolversMustBuildDedicatedAdministrativeContexts() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);
        AccessPolicyQueryContext jobListContext = invokeContext(
            resolver,
            "resolveImportJobAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { ACTOR_USER_ID }
        );
        AccessPolicyQueryContext jobDetailContext = invokeContext(
            resolver,
            "resolveImportJobAdministrationDetailContext",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { ACTOR_USER_ID, 703L }
        );
        AccessPolicyQueryContext itemListContext = invokeContext(
            resolver,
            "resolveImportJobItemAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { ACTOR_USER_ID }
        );
        AccessPolicyQueryContext itemDetailContext = invokeContext(
            resolver,
            "resolveImportJobItemAdministrationDetailContext",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { ACTOR_USER_ID, 704L }
        );

        assertAdministrativeContext(jobListContext, AccessReadArea.IMPORT_JOB_ADMINISTRATION, AccessReadType.LIST, "import_job");
        assertAdministrativeContext(jobDetailContext, AccessReadArea.IMPORT_JOB_ADMINISTRATION, AccessReadType.DETAIL, "import_job");
        assertAdministrativeContext(itemListContext, AccessReadArea.IMPORT_JOB_ADMINISTRATION, AccessReadType.LIST, "import_job_item");
        assertAdministrativeContext(itemDetailContext, AccessReadArea.IMPORT_JOB_ADMINISTRATION, AccessReadType.DETAIL, "import_job_item");
        assertNoGenericFamily(jobListContext);
        assertNoGenericFamily(itemListContext);
    }

    @Test
    void auditAdministrationResolversMustBuildDedicatedAdministrativeContexts() throws Exception {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);
        AccessPolicyQueryContext listContext = invokeContext(
            resolver,
            "resolveAuditEventAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { ACTOR_USER_ID }
        );
        AccessPolicyQueryContext detailContext = invokeContext(
            resolver,
            "resolveAuditEventAdministrationDetailContext",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { ACTOR_USER_ID, 705L }
        );

        assertAdministrativeContext(listContext, AccessReadArea.AUDIT_EVENT_ADMINISTRATION, AccessReadType.LIST, "audit_event");
        assertAdministrativeContext(detailContext, AccessReadArea.AUDIT_EVENT_ADMINISTRATION, AccessReadType.DETAIL, "audit_event");
        assertNoGenericFamily(detailContext);
    }

    @Test
    void administrativeQueryBuildersMustFailFastOnNullActorAndStayQueryOnly() throws Exception {
        String source = Files.readString(RESOLVER_SOURCE);

        assertThat(source)
            .doesNotContain("CapabilityAdmissionRequestFactory")
            .doesNotContain("CapabilityOperationCode")
            .doesNotContain("CapabilityTargetEntityType")
            .doesNotContain("owner_table")
            .doesNotContain("database_table")
            .doesNotContain("generic_admin")
            .doesNotContain("audit_event_mutation");

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);

        assertThatThrownBy(() -> invokeContext(
            resolver,
            "resolveNotificationAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { null }
        ))
            .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
        assertThatThrownBy(() -> invokeContext(
            resolver,
            "resolveNotificationAdministrationDetailContext",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { null, 701L }
        ))
            .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
        assertThatThrownBy(() -> invokeContext(
            resolver,
            "resolveNotificationRuleAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { null }
        ))
            .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
        assertThatThrownBy(() -> invokeContext(
            resolver,
            "resolveImportJobAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { null }
        ))
            .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
        assertThatThrownBy(() -> invokeContext(
            resolver,
            "resolveAuditEventAdministrationContext",
            new Class<?>[] { Long.class },
            new Object[] { null }
        ))
            .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private AccessPolicyQueryContext invokeContext(
        AccessPolicyQueryContextResolver resolver,
        String methodName,
        Class<?>[] parameterTypes,
        Object[] args
    ) throws Exception {
        Method method = AccessPolicyQueryContextResolver.class.getDeclaredMethod(methodName, parameterTypes);
        try {
            return (AccessPolicyQueryContext) method.invoke(resolver, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private void assertAdministrativeContext(
        AccessPolicyQueryContext context,
        AccessReadArea contour,
        AccessReadType readType,
        String targetEntityFamily
    ) {
        assertThat(context).isNotNull();
        assertThat(context.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(context.contour()).isEqualTo(contour);
        assertThat(context.readType()).isEqualTo(readType);
        assertThat(context.effectiveAt()).isEqualTo(FIXED_INSTANT);
        assertThat(context.targetEntityFamily()).isEqualTo(targetEntityFamily);
    }

    private void assertForbiddenContourIsolation(AccessPolicyQueryContext context) {
        assertThat(context.contour())
            .isNotEqualTo(AccessReadArea.SELF_RESULT_HISTORY)
            .isNotEqualTo(AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION)
            .isNotEqualTo(AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS)
            .isNotEqualTo(AccessReadArea.EXPERT_QUESTION_ANALYTICS);
    }

    private void assertNoGenericFamily(AccessPolicyQueryContext context) {
        assertThat(context.targetEntityFamily())
            .isNotEqualTo("owner_table")
            .isNotEqualTo("database_table")
            .isNotEqualTo("generic_admin")
            .isNotEqualTo("audit_event_mutation");
    }
}
