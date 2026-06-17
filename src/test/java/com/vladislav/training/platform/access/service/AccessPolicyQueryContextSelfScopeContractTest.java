package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import java.io.IOException;
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
 * Проверяет договорённости вокруг {@code AccessPolicyQueryContextSelfScope}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@ExtendWith(MockitoExtension.class)
class AccessPolicyQueryContextSelfScopeContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T12:00:00Z");

    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    @Test
    void defaultQueryContextRemainsFailClosedForSelfScopeSemantics() {
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment"
        );

        assertThat(context.subjectScope()).isEqualTo(AccessReadSubjectScope.UNSPECIFIED);
        assertThat(context.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.UNSPECIFIED);
        assertThat(context.targetUserId()).isNull();
    }

    @Test
    void resolverCanExpressExplicitActorSelfScopeForAssignmentReads() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);
        AccessPolicyQueryContext context = resolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            "assignment"
        );

        assertThat(context.actorUserId()).isEqualTo(101L);
        assertThat(context.contour()).isEqualTo(AccessReadArea.ASSIGNMENT);
        assertThat(context.readType()).isEqualTo(AccessReadType.DETAIL);
        assertThat(context.targetEntityFamily()).isEqualTo("assignment");
        assertThat(context.subjectScope()).isEqualTo(AccessReadSubjectScope.ACTOR_SELF);
        assertThat(context.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.SELF);
        assertThat(context.targetUserId()).isNull();
        assertThat(context.targetOrganizationalUnitId()).isNull();

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
    }

    @Test
    void resolverCanExpressDedicatedSelfCurrentAttemptContextWithTargetCoordinates() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);
        AccessPolicyQueryContext context = resolver.resolveSelfCurrentAttemptContext(101L, 501L, 9001L, null);

        assertThat(context.actorUserId()).isEqualTo(101L);
        assertThat(context.contour()).isEqualTo(AccessReadArea.SELF_CURRENT_ATTEMPT);
        assertThat(context.readType()).isEqualTo(AccessReadType.DETAIL);
        assertThat(context.targetEntityFamily()).isEqualTo("self_current_attempt");
        assertThat(context.targetTestId()).isEqualTo(501L);
        assertThat(context.currentAttemptId()).isEqualTo(9001L);
        assertThat(context.subjectScope()).isEqualTo(AccessReadSubjectScope.ACTOR_SELF);
        assertThat(context.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.SELF);

        verify(interactiveActorResolver).resolveActorUserId();
        verify(utcClock).now();
    }

    @Test
    void dedicatedSelfCurrentAttemptContextRejectsMismatchedActorSelector() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(202L);

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);

        assertThatThrownBy(() -> resolver.resolveSelfCurrentAttemptContext(101L, 501L, null, FIXED_INSTANT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId must match");

        verify(interactiveActorResolver).resolveActorUserId();
    }

    @Test
    void historyTypedActorSelfScopeDoesNotImplicitlyBecomeBlockedSelfHistoryContour() throws IOException {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(interactiveActorResolver, utcClock);
        AccessPolicyQueryContext context = resolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNMENT,
            AccessReadType.HISTORY,
            "assignment"
        );

        assertThat(context.contour()).isEqualTo(AccessReadArea.ASSIGNMENT);
        assertThat(context.readType()).isEqualTo(AccessReadType.HISTORY);
        assertThat(context.subjectScope()).isEqualTo(AccessReadSubjectScope.ACTOR_SELF);
        assertThat(context.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.SELF);
        assertThat(context.targetEntityFamily()).isEqualTo("assignment");

        assertThat(Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContextResolver.java"
        )))
            
            .doesNotContain("SELF_RESULT_HISTORY")
            .doesNotContain("AccessReadType.HISTORY");
        assertThat(Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContext.java"
        )))
            
            .doesNotContain("SELF_RESULT_HISTORY")
            .doesNotContain("AccessReadType.HISTORY");
    }

    @Test
    void actorSelfScopeRejectsArbitrarySubjectUserSelector() {
        assertThatThrownBy(() -> new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            202L,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ACTOR_SELF");
    }

    @Test
    void queryContextRejectsNullSubjectSemantics() {
        assertThatThrownBy(() -> new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.UNSPECIFIED,
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("subjectSemantics must not be null");
    }

    @Test
    void accessQueryVocabularyStaysResolverOrPolicyBasedRatherThanExecutionBased() throws IOException {
        String packageInfo = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/package-info.java"
        ));
        Set<String> methodNames = methodNames(AccessPolicyQueryContextResolver.class);

        assertThat(methodNames)
            .contains(
                "resolve",
                "resolveActorSelfScope",
                "resolveSelfCurrentAttemptContext",
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
        assertThat(methodNames).allMatch(methodName ->
            !methodName.contains("start")
                && !methodName.contains("submit")
                && !methodName.contains("abandon")
                && !methodName.contains("answer")
                && !methodName.contains("mutate")
                && !methodName.contains("terminalize")
                && !methodName.contains("recordResult")
                && !methodName.contains("createAssignment")
                && !methodName.contains("patch")
                && !methodName.contains("repair")
                && !methodName.contains("rebuild")
                && !methodName.contains("reconcile")
                && !methodName.contains("admit")
                && !methodName.contains("capability")
                && !methodName.contains("command")
        );
        assertThat(packageInfo)
            .contains("Сервисы правил доступа")
            .contains("контекста чтения")
            .doesNotContain("start")
            .doesNotContain("submit")
            .doesNotContain("execution");
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }
}
