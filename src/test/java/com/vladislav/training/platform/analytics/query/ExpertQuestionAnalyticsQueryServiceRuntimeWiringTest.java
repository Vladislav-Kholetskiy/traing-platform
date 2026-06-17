package com.vladislav.training.platform.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(
    ExpertQuestionAnalyticsQueryServiceRuntimeWiringTest.ExpertQuestionAnalyticsRuntimeTestConfiguration.class
)
/**
 * Проверяет, что {@code ExpertQuestionAnalyticsQueryService} правильно связан с зависимостями и конфигурацией при запуске.
 * Такой тест быстро показывает ошибки в сборке связей.
 */
class ExpertQuestionAnalyticsQueryServiceRuntimeWiringTest {

    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-28T14:00:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");
    private static final Instant CALCULATED_AT = Instant.parse("2026-05-01T09:00:00Z");
    private static final Instant REFRESHED_AT = Instant.parse("2026-05-01T09:15:00Z");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ExpertQuestionAnalyticsQueryService queryService;

    @Autowired
    private ExpertQuestionAnalyticsReadRepository expertQuestionAnalyticsReadRepository;

    @Autowired
    private AccessSpecificationPolicy accessSpecificationPolicy;

    @Test
    void contextPublishesExpertQuestionAnalyticsQueryServiceBean() {
        assertThat(applicationContext.getBean(ExpertQuestionAnalyticsQueryService.class))
            .isInstanceOf(ExpertQuestionAnalyticsQueryServiceImpl.class);
    }

    @Test
    void missingRepositoryOrPolicyDependencyFailsContextFastInsteadOfSilentlySkippingServicePublication() {
        Throwable thrown = catchThrowable(() -> {
            try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(MissingExpertQuestionAnalyticsDependenciesConfiguration.class)) {
                context.getBean(ExpertQuestionAnalyticsQueryService.class);
            }
        });

        assertThat(thrown).isInstanceOf(BeansException.class);
        assertThat(hasCauseOfType(thrown, UnsatisfiedDependencyException.class)).isTrue();
    }

    @Test
    void allowPathWiresServiceThroughPolicyAndDedicatedReadRepository() {
        ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery query =
            new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
                101L,
                EFFECTIVE_AT,
                PERIOD_START,
                PERIOD_END
            );
        AccessReadScope accessReadScope = AccessReadScope.fullAccess();
        ExpertQuestionAnalyticsReadCriteria criteria = new ExpertQuestionAnalyticsReadCriteria(
            accessReadScope,
            PERIOD_START,
            PERIOD_END
        );
        when(accessSpecificationPolicy.resolveReadScope(org.mockito.ArgumentMatchers.any()))
            .thenReturn(accessReadScope);
        when(expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(criteria))
            .thenReturn(List.of(
                new ExpertQuestionAnalyticsReadRow(
                    701L,
                    PERIOD_START,
                    PERIOD_END,
                    15,
                    8,
                    7,
                    new BigDecimal("4.7500"),
                    CALCULATED_AT,
                    REFRESHED_AT
                )
            ));

        List<ExpertQuestionAnalyticsDto> result = queryService.findQuestionAnalytics(query);

        assertThat(result).containsExactly(
            new ExpertQuestionAnalyticsDto(
                701L,
                PERIOD_START,
                PERIOD_END,
                15,
                8,
                7,
                new BigDecimal("4.7500"),
                CALCULATED_AT,
                REFRESHED_AT
            )
        );
        var ordered = inOrder(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
        ordered.verify(accessSpecificationPolicy).resolveReadScope(org.mockito.ArgumentMatchers.any());
        ordered.verify(expertQuestionAnalyticsReadRepository).findQuestionAnalyticsRows(criteria);
        verifyNoMoreInteractions(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
        basePackageClasses = ExpertQuestionAnalyticsQueryServiceImpl.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = ExpertQuestionAnalyticsQueryServiceImpl.class
        )
    )
    static class ExpertQuestionAnalyticsRuntimeTestConfiguration {

        @Bean
        ExpertQuestionAnalyticsReadRepository expertQuestionAnalyticsReadRepository() {
            return org.mockito.Mockito.mock(ExpertQuestionAnalyticsReadRepository.class);
        }

        @Bean
        AccessSpecificationPolicy accessSpecificationPolicy() {
            return org.mockito.Mockito.mock(AccessSpecificationPolicy.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
        basePackageClasses = ExpertQuestionAnalyticsQueryServiceImpl.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = ExpertQuestionAnalyticsQueryServiceImpl.class
        )
    )
    static class MissingExpertQuestionAnalyticsDependenciesConfiguration {
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
