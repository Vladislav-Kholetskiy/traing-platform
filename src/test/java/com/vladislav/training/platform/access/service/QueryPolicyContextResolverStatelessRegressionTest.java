package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет, что {@code QueryPolicyContextResolverStateless} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
@ExtendWith(MockitoExtension.class)
class QueryPolicyContextResolverStatelessRegressionTest {

    @Mock
    private InteractiveActorResolver interactiveActorResolver;

    @Mock
    private UtcClock utcClock;

    @Test
    void administrativeNotificationContextsRemainStatelessAcrossSequentialActorsInSameResolverInstance() {
        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(
            interactiveActorResolver,
            utcClock
        );
        Instant t1 = Instant.parse("2026-05-08T10:00:00Z");
        Instant t2 = Instant.parse("2026-05-08T10:05:00Z");

        when(interactiveActorResolver.resolveActorUserId()).thenReturn(101L, 202L);
        when(utcClock.now()).thenReturn(t1, t2);

        AccessPolicyQueryContext first = resolver.resolveNotificationAdministrationContext(101L);
        AccessPolicyQueryContext second = resolver.resolveNotificationAdministrationContext(202L);

        assertThat(first.actorUserId()).isEqualTo(101L);
        assertThat(first.effectiveAt()).isEqualTo(t1);
        assertThat(second.actorUserId()).isEqualTo(202L);
        assertThat(second.effectiveAt()).isEqualTo(t2);
    }

    @Test
    void actorMismatchStaysFailClosedForAdministrativeNotificationContexts() {
        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(
            interactiveActorResolver,
            utcClock
        );
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(202L);

        assertThatThrownBy(() -> resolver.resolveNotificationAdministrationContext(101L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("current authenticated actor");
        assertThatThrownBy(() -> resolver.resolveNotificationRecipientSelfContext(101L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("current authenticated actor");
    }

    @Test
    void effectiveAtIsNotCachedAcrossAdministrativeAdminAndSelfContexts() {
        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(
            interactiveActorResolver,
            utcClock
        );
        Instant t1 = Instant.parse("2026-05-08T11:00:00Z");
        Instant t2 = Instant.parse("2026-05-08T11:03:00Z");
        Instant t3 = Instant.parse("2026-05-08T11:06:00Z");
        Instant t4 = Instant.parse("2026-05-08T11:09:00Z");

        when(interactiveActorResolver.resolveActorUserId()).thenReturn(303L, 303L, 303L, 303L);
        when(utcClock.now()).thenReturn(t1, t2, t3, t4);

        AccessPolicyQueryContext adminList = resolver.resolveNotificationAdministrationContext(303L);
        AccessPolicyQueryContext adminDetail = resolver.resolveNotificationAdministrationDetailContext(303L, 11L);
        AccessPolicyQueryContext selfList = resolver.resolveNotificationRecipientSelfContext(303L);
        AccessPolicyQueryContext selfDetail = resolver.resolveNotificationRecipientSelfDetailContext(303L, 22L);

        assertThat(adminList.effectiveAt()).isEqualTo(t1);
        assertThat(adminDetail.effectiveAt()).isEqualTo(t2);
        assertThat(selfList.effectiveAt()).isEqualTo(t3);
        assertThat(selfDetail.effectiveAt()).isEqualTo(t4);
    }
}
