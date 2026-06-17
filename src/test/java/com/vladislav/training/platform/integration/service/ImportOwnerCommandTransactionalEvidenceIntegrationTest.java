package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.infrastructure.persistence.ImportJobEntity;
import com.vladislav.training.platform.integration.infrastructure.persistence.ImportJobItemEntity;
import com.vladislav.training.platform.integration.infrastructure.persistence.ImportMapper;
import com.vladislav.training.platform.integration.infrastructure.persistence.JpaImportJobItemRepositoryAdapter;
import com.vladislav.training.platform.integration.infrastructure.persistence.JpaImportJobRepositoryAdapter;
import com.vladislav.training.platform.integration.infrastructure.persistence.SpringDataImportJobItemJpaRepository;
import com.vladislav.training.platform.integration.infrastructure.persistence.SpringDataImportJobJpaRepository;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.JpaAppUserRepositoryAdapter;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import com.vladislav.training.platform.userorg.infrastructure.persistence.UserOrgMapper;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.service.UserCommandService;
import com.vladislav.training.platform.userorg.service.UserCommandServiceImpl;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = ImportOwnerCommandTransactionalEvidenceIntegrationTest.ImportOwnerCommandTransactionalEvidenceTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code ImportOwnerCommandTransactionalEvidence} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class ImportOwnerCommandTransactionalEvidenceIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private SpringDataAppUserJpaRepository appUserJpaRepository;
    @Autowired
    private SpringDataImportJobJpaRepository importJobJpaRepository;
    @Autowired
    private SpringDataImportJobItemJpaRepository importJobItemJpaRepository;
    @Autowired
    private ImportProcessingService importProcessingService;
    @Autowired
    private ImportItemReviewService importItemReviewService;
    @Autowired
    private FailingAfterPersistAppUserRepository failingAfterPersistAppUserRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        failingAfterPersistAppUserRepository.reset();
        importJobItemJpaRepository.deleteAllInBatch();
        importJobJpaRepository.deleteAllInBatch();
        appUserJpaRepository.deleteAllInBatch();
    }

    @Test
    void processingOwnerFailureDoesNotPersistAppliedAndDoesNotLeaveOwnerMutated() {
        AppUserEntity user = appUserJpaRepository.saveAndFlush(appUser("EMP-PROC-1", "EXT-PROC-1", "Before", "Alice"));
        ImportJobEntity job = importJobEntity("processing-transaction.csv", "PENDING", user.getId());
        job = importJobJpaRepository.saveAndFlush(job);
        ImportJobItemEntity item = importJobItemJpaRepository.saveAndFlush(importItemEntity(
            job.getId(),
            0,
            "PENDING",
            "EXT-PROC-1",
            "EMP-PROC-1",
            "{\"employeeNumber\":\"EMP-PROC-1\",\"externalId\":\"EXT-PROC-1\",\"lastName\":\"After\",\"firstName\":\"Alice\",\"status\":\"ACTIVE\"}"
        ));

        failingAfterPersistAppUserRepository.failNextSave();
        var processedJob = importProcessingService.processImportJob(job.getId());

        AppUserEntity reloadedUser = appUserJpaRepository.findById(user.getId()).orElseThrow();
        ImportJobItemEntity reloadedItem = importJobItemJpaRepository.findById(item.getId()).orElseThrow();
        ImportJobEntity reloadedJob = importJobJpaRepository.findById(job.getId()).orElseThrow();

        assertThat(processedJob.status()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(reloadedUser.getLastName()).isEqualTo("Before");
        assertThat(reloadedUser.getUpdatedAt()).isEqualTo(FIXED_INSTANT.minusSeconds(300));
        assertThat(reloadedItem.getStatus()).isEqualTo(ImportItemStatus.FAILED.name());
        assertThat(reloadedItem.getErrorCode()).isEqualTo("OWNER_COMMAND_FAILED");
        assertThat(reloadedItem.getProcessedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(reloadedJob.getStatus()).isEqualTo(ImportJobStatus.FAILED.name());
        assertThat(reloadedJob.getAppliedItemCount()).isZero();
        assertThat(reloadedJob.getFailedItemCount()).isEqualTo(1);
        assertThat(reloadedJob.getProcessedItemCount()).isEqualTo(1);
    }

    @Test
    void reviewOwnerFailureDoesNotPersistAppliedAndDoesNotLeaveOwnerMutated() {
        setAuthentication(701L);
        AppUserEntity user = appUserJpaRepository.saveAndFlush(appUser("EMP-REV-1", "EXT-REV-1", "Before", "Ivan"));
        ImportJobEntity job = importJobEntity("review-transaction.csv", "COMPLETED_WITH_ERRORS", user.getId());
        job.setStartedAt(FIXED_INSTANT.minusSeconds(600));
        job.setCompletedAt(FIXED_INSTANT.minusSeconds(120));
        job.setTotalItemCount(1);
        job.setProcessedItemCount(1);
        job.setAppliedItemCount(0);
        job.setFailedItemCount(0);
        job.setRequiresReviewItemCount(1);
        job = importJobJpaRepository.saveAndFlush(job);

        ImportJobItemEntity reviewSeed = importItemEntity(
            job.getId(),
            0,
            "REQUIRES_REVIEW",
            "EXT-REV-1",
            "EMP-REV-1",
            "{\"employeeNumber\":\"EMP-REV-1\",\"externalId\":\"EXT-REV-1\",\"lastName\":\"After\",\"firstName\":\"Ivan\",\"status\":\"ACTIVE\"}"
        );
        reviewSeed.setProcessedAt(FIXED_INSTANT.minusSeconds(120));
        ImportJobItemEntity item = importJobItemJpaRepository.saveAndFlush(reviewSeed);

        failingAfterPersistAppUserRepository.failNextSave();
        ImportItemReviewService.ImportReviewResult result = importItemReviewService.applyReview(
            701L,
            item.getId(),
            new ImportItemReviewService.ImportReviewApplyCommand(user.getId())
        );

        AppUserEntity reloadedUser = appUserJpaRepository.findById(user.getId()).orElseThrow();
        ImportJobItemEntity reloadedItem = importJobItemJpaRepository.findById(item.getId()).orElseThrow();
        ImportJobEntity reloadedJob = importJobJpaRepository.findById(job.getId()).orElseThrow();

        assertThat(result.status()).isEqualTo(ImportItemStatus.FAILED);
        assertThat(reloadedUser.getLastName()).isEqualTo("Before");
        assertThat(reloadedUser.getUpdatedAt()).isEqualTo(FIXED_INSTANT.minusSeconds(300));
        assertThat(reloadedItem.getStatus()).isEqualTo(ImportItemStatus.FAILED.name());
        assertThat(reloadedItem.getErrorCode()).isEqualTo("OWNER_COMMAND_FAILED");
        assertThat(reloadedItem.getMatchedEntityId()).isEqualTo(String.valueOf(user.getId()));
        assertThat(reloadedJob.getStatus()).isEqualTo(ImportJobStatus.FAILED.name());
        assertThat(reloadedJob.getAppliedItemCount()).isZero();
        assertThat(reloadedJob.getFailedItemCount()).isEqualTo(1);
        assertThat(reloadedJob.getRequiresReviewItemCount()).isZero();
    }

    private void setAuthentication(Long actorUserId) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(actorUserId, null, "ROLE_ADMIN");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private AppUserEntity appUser(String employeeNumber, String externalId, String lastName, String firstName) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(externalId);
        entity.setLastName(lastName);
        entity.setFirstName(firstName);
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(900));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(300));
        return entity;
    }

    private ImportJobEntity importJobEntity(String sourceRef, String status, Long initiatedByUserId) {
        ImportJobEntity entity = instantiate(ImportJobEntity.class);
        entity.setSourceType("HR_CSV");
        entity.setSourceRef(sourceRef);
        entity.setInitiatedByUserId(initiatedByUserId);
        entity.setStatus(status);
        entity.setPayload("{\"rows\":1}");
        entity.setStartedAt(null);
        entity.setCompletedAt(null);
        entity.setTotalItemCount(1);
        entity.setProcessedItemCount(0);
        entity.setAppliedItemCount(0);
        entity.setFailedItemCount(0);
        entity.setRequiresReviewItemCount(0);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(900));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(300));
        return entity;
    }

    private ImportJobItemEntity importItemEntity(
        Long importJobId,
        int itemNo,
        String status,
        String externalId,
        String employeeNumber,
        String payload
    ) {
        ImportJobItemEntity entity = instantiate(ImportJobItemEntity.class);
        entity.setImportJobId(importJobId);
        entity.setItemNo(itemNo);
        entity.setTargetEntityType("APP_USER");
        entity.setExternalId(externalId);
        entity.setEmployeeNumber(employeeNumber);
        entity.setStatus(status);
        entity.setMatchedEntityId(null);
        entity.setPayload(payload);
        entity.setErrorCode(null);
        entity.setErrorMessage(null);
        entity.setProcessedAt(null);
        entity.setCreatedAt(FIXED_INSTANT.minusSeconds(900));
        entity.setUpdatedAt(FIXED_INSTANT.minusSeconds(300));
        return entity;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity: " + type.getName(), exception);
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AppUserEntity.class,
        ImportJobEntity.class,
        ImportJobItemEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
        SpringDataAppUserJpaRepository.class,
        SpringDataImportJobJpaRepository.class,
        SpringDataImportJobItemJpaRepository.class
    })
    @Import({
        UserOrgMapper.class,
        ImportMapper.class,
        JpaAppUserRepositoryAdapter.class,
        JpaImportJobRepositoryAdapter.class,
        JpaImportJobItemRepositoryAdapter.class,
        UserCommandServiceImpl.class,
        TransactionalImportTypedOwnerCommandExecutor.class,
        ImportProcessingServiceImpl.class,
        ImportItemReviewServiceImpl.class
    })
    static class ImportOwnerCommandTransactionalEvidenceTestApplication {

        @Bean
        UtcClock utcClock() {
            return () -> FIXED_INSTANT;
        }

        @Bean
        InteractiveActorResolver interactiveActorResolver() {
            return new InteractiveActorResolver(new AuthenticatedActorAdapter());
        }

        @Bean
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory(
            InteractiveActorResolver interactiveActorResolver,
            UtcClock utcClock
        ) {
            return new CapabilityAdmissionRequestFactory(interactiveActorResolver, utcClock);
        }

        @Bean
        CapabilityAdmissionPolicy capabilityAdmissionPolicy() {
            return request -> {
            };
        }

        @Bean
        @Primary
        FailingAfterPersistAppUserRepository appUserRepository(JpaAppUserRepositoryAdapter delegate) {
            return new FailingAfterPersistAppUserRepository(delegate);
        }
    }

    static class FailingAfterPersistAppUserRepository implements AppUserRepository {

        private final JpaAppUserRepositoryAdapter delegate;
        private final AtomicBoolean failAfterPersist = new AtomicBoolean(false);

        FailingAfterPersistAppUserRepository(JpaAppUserRepositoryAdapter delegate) {
            this.delegate = delegate;
        }

        void failNextSave() {
            failAfterPersist.set(true);
        }

        void reset() {
            failAfterPersist.set(false);
        }

        @Override
        public AppUser findUserById(Long userId) {
            return delegate.findUserById(userId);
        }

        @Override
        public AppUser findUserByEmployeeNumber(String employeeNumber) {
            return delegate.findUserByEmployeeNumber(employeeNumber);
        }

        @Override
        public java.util.Optional<AppUser> findOptionalUserByEmployeeNumber(String employeeNumber) {
            return delegate.findOptionalUserByEmployeeNumber(employeeNumber);
        }

        @Override
        public java.util.List<AppUser> findAllUsers() {
            return delegate.findAllUsers();
        }

        @Override
        public java.util.List<AppUser> findUsersByStatus(UserStatus status) {
            return delegate.findUsersByStatus(status);
        }

        @Override
        public boolean existsUserByEmployeeNumber(String employeeNumber) {
            return delegate.existsUserByEmployeeNumber(employeeNumber);
        }

        @Override
        public boolean existsUserByExternalId(String externalId) {
            return delegate.existsUserByExternalId(externalId);
        }

        @Override
        @Transactional
        public AppUser saveUser(AppUser user) {
            AppUser saved = delegate.saveUser(user);
            if (failAfterPersist.compareAndSet(true, false)) {
                throw new IllegalStateException("owner save failed after persistence");
            }
            return saved;
        }
    }
}
