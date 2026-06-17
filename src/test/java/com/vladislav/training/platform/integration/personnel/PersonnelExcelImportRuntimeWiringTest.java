package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.vladislav.training.platform.access.service.AccessAdministrationQueryService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.personnel.controller.PersonnelExcelImportController;
import com.vladislav.training.platform.integration.personnel.service.OwnerReadPersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelOwnerMutationExecutor;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookDryRunFacade;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import com.vladislav.training.platform.userorg.service.UserRoleAssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Проверяет, что {@code PersonnelExcelImport} правильно связан с зависимостями и конфигурацией при запуске.
 * Такой тест быстро показывает ошибки в сборке связей.
 */
class PersonnelExcelImportRuntimeWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PersonnelRuntimeWiringConfiguration.class);

    @Test
    void runtimeSliceCanConstructControllerDryRunFacadeApplyServiceAndReaderImplementation() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PersonnelCurrentStateReader.class);
            assertThat(context.getBean(PersonnelCurrentStateReader.class))
                .isInstanceOf(OwnerReadPersonnelCurrentStateReader.class);
            assertThat(context).hasSingleBean(PersonnelWorkbookDryRunFacade.class);
            assertThat(context).hasSingleBean(PersonnelApplyService.class);
            assertThat(context).hasSingleBean(PersonnelExcelImportController.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class PersonnelRuntimeWiringConfiguration {

        @Bean
        UtcClock utcClock() {
            return () -> java.time.Instant.parse("2026-05-11T12:00:00Z");
        }

        @Bean
        UserQueryService userQueryService() {
            return mock(UserQueryService.class);
        }

        @Bean
        UserOrganizationAssignmentService userOrganizationAssignmentService() {
            return mock(UserOrganizationAssignmentService.class);
        }

        @Bean
        UserRoleAssignmentService userRoleAssignmentService() {
            return mock(UserRoleAssignmentService.class);
        }

        @Bean
        OrganizationQueryService organizationQueryService() {
            return mock(OrganizationQueryService.class);
        }

        @Bean
        AccessAdministrationQueryService accessAdministrationQueryService() {
            return mock(AccessAdministrationQueryService.class);
        }

        @Bean
        PersonnelCurrentStateReader personnelCurrentStateReader(
            UserQueryService userQueryService,
            UserOrganizationAssignmentService userOrganizationAssignmentService,
            UserRoleAssignmentService userRoleAssignmentService,
            OrganizationQueryService organizationQueryService,
            AccessAdministrationQueryService accessAdministrationQueryService,
            UtcClock utcClock
        ) {
            return new OwnerReadPersonnelCurrentStateReader(
                userQueryService,
                userOrganizationAssignmentService,
                userRoleAssignmentService,
                organizationQueryService,
                accessAdministrationQueryService,
                utcClock
            );
        }

        @Bean
        PersonnelWorkbookDryRunFacade personnelWorkbookDryRunFacade(ObjectProvider<PersonnelCurrentStateReader> readerProvider) {
            return new PersonnelWorkbookDryRunFacade(readerProvider);
        }

        @Bean
        PersonnelImportAdmissionService personnelImportAdmissionService() {
            return mock(PersonnelImportAdmissionService.class);
        }

        @Bean
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory() {
            return mock(CapabilityAdmissionRequestFactory.class);
        }

        @Bean
        PersonnelOwnerMutationExecutor personnelOwnerMutationExecutor() {
            return mock(PersonnelOwnerMutationExecutor.class);
        }

        @Bean
        PersonnelApplyService personnelApplyService(
            ObjectProvider<PersonnelCurrentStateReader> readerProvider,
            PersonnelImportAdmissionService personnelImportAdmissionService,
            CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
            PersonnelOwnerMutationExecutor personnelOwnerMutationExecutor
        ) {
            return new PersonnelApplyService(
                readerProvider,
                personnelImportAdmissionService,
                capabilityAdmissionRequestFactory,
                personnelOwnerMutationExecutor
            );
        }

        @Bean
        PersonnelExcelImportController personnelExcelImportController(
            PersonnelImportAdmissionService personnelImportAdmissionService,
            PersonnelWorkbookDryRunFacade personnelWorkbookDryRunFacade,
            PersonnelApplyService personnelApplyService,
            CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory
        ) {
            return new PersonnelExcelImportController(
                personnelImportAdmissionService,
                personnelWorkbookDryRunFacade,
                personnelApplyService,
                capabilityAdmissionRequestFactory
            );
        }
    }
}
