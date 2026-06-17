package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScopeProjectionService;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.ManagerialCurrentSupervisionReadRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(
    ManagerialCurrentSupervisionQueryServiceRuntimeWiringTest.ManagerialCurrentSupervisionRuntimeTestConfiguration.class
)
/**
 * Проверяет, что {@code ManagerialCurrentSupervisionQueryService} правильно связан с зависимостями и конфигурацией при запуске.
 * Такой тест быстро показывает ошибки в сборке связей.
 */
class ManagerialCurrentSupervisionQueryServiceRuntimeWiringTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-27T21:30:00Z");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ManagerialCurrentSupervisionQueryService queryService;

    @Autowired
    private ManagerialCurrentSupervisionReadRepository readRepository;

    @Autowired
    private ManagerialReadScopeProjectionService managerialReadScopeProjectionService;

    @Test
    void contextPublishesManagerialCurrentSupervisionQueryServiceBean() {
        assertThat(applicationContext.getBean(ManagerialCurrentSupervisionQueryService.class))
            .isInstanceOf(ManagerialCurrentSupervisionQueryServiceImpl.class);
    }

    @Test
    void missingRepositoryOrProjectionDependencyFailsContextFastInsteadOfSilentlySkippingServicePublication() {
        Throwable thrown = catchThrowable(() -> {
            try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(MissingManagerialCurrentSupervisionDependenciesConfiguration.class)) {
                context.getBean(ManagerialCurrentSupervisionQueryService.class);
            }
        });

        assertThat(thrown).isInstanceOf(BeansException.class);
        assertThat(hasCauseOfType(thrown, UnsatisfiedDependencyException.class)).isTrue();
    }

    @Test
    void allowPathWiresServiceThroughProjectionAndDedicatedReadRepository() {
        ManagerialReadScope scope = new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadScope.scoped(java.util.Set.of(30L), java.util.Set.of("/company/division-30"))
        );
        when(managerialReadScopeProjectionService.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        )).thenReturn(scope);
        when(readRepository.findCurrentSupervisionRows(
            new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(scope)
        )).thenReturn(List.of(
            new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow(
                77L,
                201L,
                "Ivanov Ivan Ivanovich",
                501L,
                "Labor Safety",
                701L,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT.plusSeconds(7200),
                AssignmentStatus.ASSIGNED
            )
        ));

        List<ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow> rows =
            queryService.findCurrentSupervision(
                new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT)
            );

        assertThat(rows).containsExactly(
            new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow(
                77L,
                201L,
                "Ivanov Ivan Ivanovich",
                501L,
                "Labor Safety",
                701L,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT.plusSeconds(7200),
                AssignmentStatus.ASSIGNED
            )
        );
        var ordered = inOrder(managerialReadScopeProjectionService, readRepository);
        ordered.verify(managerialReadScopeProjectionService).project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );
        ordered.verify(readRepository).findCurrentSupervisionRows(
            new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(scope)
        );
        verifyNoMoreInteractions(managerialReadScopeProjectionService, readRepository);
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
        basePackageClasses = ManagerialCurrentSupervisionQueryServiceImpl.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = ManagerialCurrentSupervisionQueryServiceImpl.class
        )
    )
    static class ManagerialCurrentSupervisionRuntimeTestConfiguration {

        @Bean
        ManagerialCurrentSupervisionReadRepository managerialCurrentSupervisionReadRepository() {
            return org.mockito.Mockito.mock(ManagerialCurrentSupervisionReadRepository.class);
        }

        @Bean
        ManagerialReadScopeProjectionService managerialReadScopeProjectionService() {
            return org.mockito.Mockito.mock(ManagerialReadScopeProjectionService.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
        basePackageClasses = ManagerialCurrentSupervisionQueryServiceImpl.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = ManagerialCurrentSupervisionQueryServiceImpl.class
        )
    )
    static class MissingManagerialCurrentSupervisionDependenciesConfiguration {
    }

    private boolean hasCauseOfType(Throwable thrown, Class<? extends Throwable> expectedType) {
        Throwable current = thrown;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
