package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code NotificationRuleServiceFailClosedConstruction}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class NotificationRuleServiceFailClosedConstructionTest {

    @Test
    void constructorRejectsNullCapabilityAdmissionPolicy() {
        assertThatThrownBy(() -> new NotificationRuleServiceImpl(
            mock(NotificationRuleRepository.class),
            null,
            mock(CapabilityAdmissionRequestFactory.class),
            mock(CriticalCommandAuditSupport.class)
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("capabilityAdmissionPolicy");
    }

    @Test
    void constructorRejectsNullCapabilityAdmissionRequestFactory() {
        assertThatThrownBy(() -> new NotificationRuleServiceImpl(
            mock(NotificationRuleRepository.class),
            mock(CapabilityAdmissionPolicy.class),
            null,
            mock(CriticalCommandAuditSupport.class)
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("capabilityAdmissionRequestFactory");
    }

    @Test
    void constructorRejectsNullCriticalAuditSupportForCriticalRuleCommands() {
        assertThatThrownBy(() -> new NotificationRuleServiceImpl(
            mock(NotificationRuleRepository.class),
            mock(CapabilityAdmissionPolicy.class),
            mock(CapabilityAdmissionRequestFactory.class),
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("criticalCommandAuditSupport");
    }

    @Test
    void implementationDoesNotKeepConditionalNullBypassForAdmission() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));

        assertThat(source)
            .doesNotContain("if (capabilityAdmissionPolicy != null && capabilityAdmissionRequestFactory != null)");
    }
}
