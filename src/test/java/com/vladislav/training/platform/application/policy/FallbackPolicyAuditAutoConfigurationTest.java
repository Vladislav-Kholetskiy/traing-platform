package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.access.service.FallbackAdminAccessSpecificationPolicy;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.AuditEventFactory;
import com.vladislav.training.platform.audit.service.AuditService;
import com.vladislav.training.platform.audit.service.DefaultAuditEventFactory;
import com.vladislav.training.platform.audit.service.FallbackInMemoryAuditService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет конфигурацию {@code FallbackPolicyAuditAuto}.
 * Это помогает не сломать настройки и включение компонентов.
 */
class FallbackPolicyAuditAutoConfigurationTest {

    private static final String BOOTSTRAP_PROFILE = "spring.profiles.active=foundation-bootstrap-fallback";
    private static final String BOOTSTRAP_FALLBACK_PROPERTY = "training-platform.foundation.bootstrap-fallback.enabled=true";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    private final InteractiveActorResolver interactiveActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    private final ApplicationContextRunner runtimeContextRunner = new ApplicationContextRunner();

    private final ApplicationContextRunner bootstrapContextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            FallbackPolicyAuditAutoConfiguration.BootstrapConfiguration.class
        ));

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fallbackAutoConfigurationIsNotImportedFromDefaultProductionClasspath() throws Exception {
        Path imports = Path.of(
            "src",
            "main",
            "resources",
            "META-INF",
            "spring",
            "org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        );

        assertThat(Files.notExists(imports) || Files.size(imports) == 0L).isTrue();
    }

    @Test
    void fallbackBeansDoNotLoadByDefaultInRuntimeContexts() {
        runtimeContextRunner.run(context -> {
            assertThat(context.getBeansOfType(CapabilityAdmissionPolicy.class)).isEmpty();
            assertThat(context.getBeansOfType(AccessSpecificationPolicy.class)).isEmpty();
            assertThat(context.getBeansOfType(AuditEventFactory.class)).isEmpty();
            assertThat(context.getBeansOfType(AuditService.class)).isEmpty();
        });
    }

    @Test
    void fallbackBeansDoNotLoadFromDefaultProductionContourEvenWhenProfileAndPropertyAreSet() {
        runtimeContextRunner
            .withPropertyValues(BOOTSTRAP_PROFILE, BOOTSTRAP_FALLBACK_PROPERTY)
            .run(context -> {
                assertThat(context.getBeansOfType(CapabilityAdmissionPolicy.class)).isEmpty();
                assertThat(context.getBeansOfType(AccessSpecificationPolicy.class)).isEmpty();
                assertThat(context.getBeansOfType(AuditEventFactory.class)).isEmpty();
                assertThat(context.getBeansOfType(AuditService.class)).isEmpty();
            });
    }

    @Test
    void fallbackBeansDoNotLoadWhenOnlyPropertyIsEnabledWithoutBootstrapProfile() {
        bootstrapContextRunner
            .withPropertyValues(BOOTSTRAP_FALLBACK_PROPERTY)
            .run(context -> {
                assertThat(context.getBeansOfType(CapabilityAdmissionPolicy.class)).isEmpty();
                assertThat(context.getBeansOfType(AccessSpecificationPolicy.class)).isEmpty();
                assertThat(context.getBeansOfType(AuditEventFactory.class)).isEmpty();
                assertThat(context.getBeansOfType(AuditService.class)).isEmpty();
            });
    }

    @Test
    void fallbackBeansLoadOnlyWhenExplicitlyImportedAndBootstrapProfileAndPropertyAreEnabled() {
        bootstrapContextRunner
            .withPropertyValues(BOOTSTRAP_PROFILE, BOOTSTRAP_FALLBACK_PROPERTY)
            .run(context -> {
                assertThat(context.getBeansOfType(CapabilityAdmissionPolicy.class)).hasSize(1);
                assertThat(context.getBean(CapabilityAdmissionPolicy.class))
                    .isInstanceOf(FallbackAdminCapabilityAdmissionPolicy.class);
                assertThat(context.getBeansOfType(AccessSpecificationPolicy.class)).hasSize(1);
                assertThat(context.getBean(AccessSpecificationPolicy.class))
                    .isInstanceOf(FallbackAdminAccessSpecificationPolicy.class);
                assertThat(context.getBeansOfType(AuditEventFactory.class)).hasSize(1);
                assertThat(context.getBean(AuditEventFactory.class)).isInstanceOf(DefaultAuditEventFactory.class);
                assertThat(context.getBeansOfType(AuditService.class)).hasSize(1);
                assertThat(context.getBean(AuditService.class)).isInstanceOf(FallbackInMemoryAuditService.class);
            });
    }

    @Test
    void canonicalBeansSuppressBootstrapFallbackWithoutConflictEvenWhenProfileAndPropertyAreEnabled() {
        bootstrapContextRunner
            .withUserConfiguration(PrimaryPolicyAuditConfiguration.class)
            .withPropertyValues(BOOTSTRAP_PROFILE, BOOTSTRAP_FALLBACK_PROPERTY)
            .run(context -> {
                assertThat(context.getBeansOfType(CapabilityAdmissionPolicy.class)).hasSize(1);
                assertThat(context.getBean(CapabilityAdmissionPolicy.class))
                    .isSameAs(context.getBean("DefaultCapabilityAdmissionPolicy"));
                assertThat(context.getBeansOfType(AccessSpecificationPolicy.class)).hasSize(1);
                assertThat(context.getBean(AccessSpecificationPolicy.class))
                    .isSameAs(context.getBean("canonicalAccessSpecificationPolicy"));
                assertThat(context.getBeansOfType(AuditEventFactory.class)).hasSize(1);
                assertThat(context.getBean(AuditEventFactory.class))
                    .isSameAs(context.getBean("canonicalAuditEventFactory"));
                assertThat(context.getBeansOfType(AuditService.class)).hasSize(1);
                assertThat(context.getBean(AuditService.class))
                    .isSameAs(context.getBean("canonicalAuditService"));
            });
    }

    @Test
    void temporaryCapabilityAdmissionAllowsAuthenticatedAdminActorOnly() {
        FallbackAdminCapabilityAdmissionPolicy policy = new FallbackAdminCapabilityAdmissionPolicy(interactiveActorResolver);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_ADMIN"));

        assertThatCode(() -> policy.check(request(101L)))
            .doesNotThrowAnyException();
    }

    @Test
    void temporaryCapabilityAdmissionRejectsNonAdminActor() {
        FallbackAdminCapabilityAdmissionPolicy policy = new FallbackAdminCapabilityAdmissionPolicy(interactiveActorResolver);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));

        assertThatThrownBy(() -> policy.check(request(101L)))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("ROLE_ADMIN");
    }

    @Test
    void temporaryAccessSpecificationPolicyReturnsFailClosedScopeForNonAdminActor() {
        FallbackAdminAccessSpecificationPolicy policy = new FallbackAdminAccessSpecificationPolicy(interactiveActorResolver);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_USER"));

        assertThat(policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.TREE,
            FIXED_INSTANT,
            null,
            null,
            "organizational_unit"
        )).readAllowed()).isFalse();
    }

    @Test
    void temporaryInMemoryAuditServiceRecordsImmutableEventSynchronously() {
        AuditEventFactory auditEventFactory = new DefaultAuditEventFactory();
        AuditService auditService = new FallbackInMemoryAuditService();
        AuditEvent auditEvent = auditEventFactory.createAuditEvent(
            new AuditEventType("userorg.test"),
            "organizational_unit",
            "42",
            101L,
            FIXED_INSTANT,
            null,
            null,
            new AuditContext("{\"operationCode\":\"USERORG_ORGANIZATIONAL_UNIT_UPDATE\"}"),
            "corr-1",
            null,
            FIXED_INSTANT
        );

        AuditEvent persistedEvent = auditService.recordAuditEvent(auditEvent);

        assertThat(persistedEvent.id()).isEqualTo(1L);
        assertThat(persistedEvent.entityId()).isEqualTo("42");
        assertThat(persistedEvent.actorUserId()).isEqualTo(101L);
    }

    private CapabilityAdmissionRequest request(Long actorUserId) {
        return new CapabilityAdmissionRequest(
            actorUserId,
            "USERORG_ORGANIZATIONAL_UNIT_UPDATE",
            CapabilityTargetEntityType.ORGANIZATIONAL_UNIT,
            1L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
    }

    @Configuration(proxyBeanMethods = false)
    static class PrimaryPolicyAuditConfiguration {

        @Bean
        CapabilityAdmissionPolicy DefaultCapabilityAdmissionPolicy() {
            return request -> {
            };
        }

        @Bean
        AccessSpecificationPolicy canonicalAccessSpecificationPolicy() {
            return new AccessSpecificationPolicy() {
            };
        }

        @Bean
        AuditEventFactory canonicalAuditEventFactory() {
            return (eventType, entityType, entityId, actorUserId, occurredAt, payloadBefore, payloadAfter, contextPayload,
                correlationId, requestId, createdAt) -> new AuditEvent(
                    77L,
                    eventType,
                    entityType,
                    entityId,
                    actorUserId,
                    occurredAt,
                    payloadBefore,
                    payloadAfter,
                    contextPayload,
                    correlationId,
                    requestId,
                    createdAt
            );
        }

        @Bean
        AuditService canonicalAuditService() {
            return auditEvent -> auditEvent;
        }
    }
}


