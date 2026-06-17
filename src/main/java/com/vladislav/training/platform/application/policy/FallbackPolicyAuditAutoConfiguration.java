package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.access.service.FallbackAdminAccessSpecificationPolicy;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.audit.service.AuditEventFactory;
import com.vladislav.training.platform.audit.service.AuditService;
import com.vladislav.training.platform.audit.service.DefaultAuditEventFactory;
import com.vladislav.training.platform.audit.service.FallbackInMemoryAuditService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Конфигурация {@code FallbackPolicyAuditAutoConfiguration}.
 */
public final class FallbackPolicyAuditAutoConfiguration {

    private FallbackPolicyAuditAutoConfiguration() {
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("foundation-bootstrap-fallback")
    @ConditionalOnProperty(name = "training-platform.foundation.bootstrap-fallback.enabled", havingValue = "true")
    @ConditionalOnMissingBean(value = {
        CapabilityAdmissionPolicy.class,
        AccessSpecificationPolicy.class,
        AuditEventFactory.class,
        AuditService.class
    })
    /**
     * Набор резервных бинов для режима первоначального запуска.
     */
    public static class BootstrapConfiguration {

        @Bean
        @ConditionalOnMissingBean
        AuthenticatedActorAdapter foundationBootstrapAuthenticatedActorAdapter() {
            return new AuthenticatedActorAdapter();
        }

        @Bean
        @ConditionalOnMissingBean
        InteractiveActorResolver foundationBootstrapInteractiveActorResolver(
            AuthenticatedActorAdapter authenticatedActorAdapter
        ) {
            return new InteractiveActorResolver(authenticatedActorAdapter);
        }

        @Bean
        @ConditionalOnMissingBean(CapabilityAdmissionPolicy.class)
        CapabilityAdmissionPolicy foundationBootstrapFallbackCapabilityAdmissionPolicy(
            InteractiveActorResolver interactiveActorResolver
        ) {
            return new FallbackAdminCapabilityAdmissionPolicy(interactiveActorResolver);
        }

        @Bean
        @ConditionalOnMissingBean(AccessSpecificationPolicy.class)
        AccessSpecificationPolicy foundationBootstrapFallbackAccessSpecificationPolicy(
            InteractiveActorResolver interactiveActorResolver
        ) {
            return new FallbackAdminAccessSpecificationPolicy(interactiveActorResolver);
        }

        @Bean
        @ConditionalOnMissingBean(AuditEventFactory.class)
        AuditEventFactory foundationBootstrapFallbackAuditEventFactory() {
            return new DefaultAuditEventFactory();
        }

        @Bean
        @ConditionalOnMissingBean(AuditService.class)
        AuditService foundationBootstrapFallbackAuditService() {
            return new FallbackInMemoryAuditService();
        }
    }
}
