package com.vladislav.training.platform.application.actor;

import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.repository.AppRoleRepository;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
/**
 * Проверяет поведение {@code CurrentActorControllerWebMvcTestConfig}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@TestConfiguration(proxyBeanMethods = false)
class CurrentActorControllerWebMvcTestConfig {

    @Bean
    AppUserRepository appUserRepository() {
        return Mockito.mock(AppUserRepository.class);
    }

    @Bean
    UserRoleAssignmentRepository userRoleAssignmentRepository() {
        return Mockito.mock(UserRoleAssignmentRepository.class);
    }

    @Bean
    UserOrganizationAssignmentRepository userOrganizationAssignmentRepository() {
        return Mockito.mock(UserOrganizationAssignmentRepository.class);
    }

    @Bean
    OrganizationalUnitRepository organizationalUnitRepository() {
        return Mockito.mock(OrganizationalUnitRepository.class);
    }

    @Bean
    AppRoleRepository appRoleRepository() {
        return Mockito.mock(AppRoleRepository.class);
    }

    @Bean
    TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService() {
        return Mockito.mock(TemporaryRoleAssignmentReadService.class);
    }

    @Bean
    UtcClock utcClock() {
        return Mockito.mock(UtcClock.class);
    }
}
