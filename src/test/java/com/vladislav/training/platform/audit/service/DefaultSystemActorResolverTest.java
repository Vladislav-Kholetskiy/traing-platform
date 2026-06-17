package com.vladislav.training.platform.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.actor.TechnicalSystemActorAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code DefaultSystemActorResolver}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSystemActorResolverTest {

    @Mock
    private TechnicalSystemActorAdapter technicalSystemActorAdapter;

    @InjectMocks
    private DefaultSystemActorResolver systemActorResolver;

    @Test
    void resolveSystemActorUserIdDelegatesToApplicationLevelActorAdapter() {
        when(technicalSystemActorAdapter.resolveSystemActorUserId()).thenReturn(900L);

        assertThat(systemActorResolver.resolveSystemActorUserId()).isEqualTo(900L);
        verify(technicalSystemActorAdapter).resolveSystemActorUserId();
    }
}
