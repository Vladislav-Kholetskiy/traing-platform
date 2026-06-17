package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.DefaultCapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.repository.ImportJobItemRepository;
import com.vladislav.training.platform.integration.repository.ImportJobRepository;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет поведение {@code ImportCommandServiceSystemActor}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportCommandServiceSystemActorTest {

    private static final String IMPORT_SERVICE_IMPL =
        "com.vladislav.training.platform.integration.service.ImportCommandServiceImpl";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T12:00:00Z");

    @Test
    void importCommandServiceMustExposeExplicitSystemLaunchPath() throws Exception {
        assertThatCode(() -> ImportCommandService.class.getDeclaredMethod(
            "launchSystemImportJob",
            ImportJob.class,
            List.class
        )).doesNotThrowAnyException();
    }

    @Test
    void systemLaunchUsesExplicitSystemActorAndNotInteractiveShortcut() throws Throwable {
        ImportJobRepository importJobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository importJobItemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        CapabilityAdmissionPolicy admissionPolicy = org.mockito.Mockito.mock(CapabilityAdmissionPolicy.class);
        CapabilityAdmissionRequestFactory requestFactory = org.mockito.Mockito.mock(CapabilityAdmissionRequestFactory.class);
        SystemActorResolver systemActorResolver = org.mockito.Mockito.mock(SystemActorResolver.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        Object service = newService(
            importJobRepository,
            importJobItemRepository,
            admissionPolicy,
            requestFactory,
            systemActorResolver,
            utcClock
        );
        Method launchMethod = service.getClass().getMethod("launchSystemImportJob", ImportJob.class, List.class);

        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(9900L);
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCode.IMPORT_JOB_LAUNCH.code(),
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(requestFactory.createImportJobLaunch(9900L)).thenReturn(request);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(importJobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> {
            ImportJob saved = invocation.getArgument(0, ImportJob.class);
            return new ImportJob(
                2001L,
                saved.sourceType(),
                saved.sourceRef(),
                saved.initiatedByUserId(),
                saved.status(),
                saved.payload(),
                saved.startedAt(),
                saved.completedAt(),
                saved.totalItemCount(),
                saved.processedItemCount(),
                saved.appliedItemCount(),
                saved.failedItemCount(),
                saved.requiresReviewItemCount(),
                saved.createdAt(),
                saved.updatedAt()
            );
        });
        when(importJobItemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));

        ImportJob launched = (ImportJob) invoke(launchMethod, service, rawSystemJob(), rawItems());

        assertThat(launched.id()).isEqualTo(2001L);
        assertThat(launched.status()).isEqualTo(ImportJobStatus.PENDING);
        assertThat(launched.initiatedByUserId()).isNull();

        verify(systemActorResolver).resolveSystemActorUserId();
        verify(requestFactory).createImportJobLaunch(9900L);
        verify(requestFactory, never()).createImportJobLaunch();
        ArgumentCaptor<CapabilityAdmissionRequest> requestCaptor = ArgumentCaptor.forClass(CapabilityAdmissionRequest.class);
        verify(admissionPolicy).check(requestCaptor.capture());
        assertThat(requestCaptor.getValue().actorUserId()).isEqualTo(9900L);
        assertThat(requestCaptor.getValue().actorUserId()).isNotNull();
        assertThat(requestCaptor.getValue().operationCode()).isEqualTo(CapabilityOperationCode.IMPORT_JOB_LAUNCH.code());
        assertThat(requestCaptor.getValue().targetEntityType()).isEqualTo(CapabilityTargetEntityType.IMPORT_JOB);

        ArgumentCaptor<ImportJob> savedJobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository).saveImportJob(savedJobCaptor.capture());
        assertThat(savedJobCaptor.getValue().initiatedByUserId())
            
            .isNull();
        assertThat(savedJobCaptor.getValue().status()).isEqualTo(ImportJobStatus.PENDING);
    }

    @Test
    void deniedSystemLaunchDoesNotPersistRowsAndDoesNotUseNullActorShortcut() throws Throwable {
        ImportJobRepository importJobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository importJobItemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        CapabilityAdmissionPolicy admissionPolicy = org.mockito.Mockito.mock(CapabilityAdmissionPolicy.class);
        CapabilityAdmissionRequestFactory requestFactory = org.mockito.Mockito.mock(CapabilityAdmissionRequestFactory.class);
        SystemActorResolver systemActorResolver = org.mockito.Mockito.mock(SystemActorResolver.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);

        Object service = newService(
            importJobRepository,
            importJobItemRepository,
            admissionPolicy,
            requestFactory,
            systemActorResolver,
            utcClock
        );
        Method launchMethod = service.getClass().getMethod("launchSystemImportJob", ImportJob.class, List.class);

        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(9900L);
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCode.IMPORT_JOB_LAUNCH.code(),
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(requestFactory.createImportJobLaunch(9900L)).thenReturn(request);
        doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "system import denied"))
            .when(admissionPolicy).check(request);

        assertThatThrownBy(() -> invoke(launchMethod, service, rawSystemJob(), rawItems()))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("system import denied");

        verify(systemActorResolver).resolveSystemActorUserId();
        verify(requestFactory).createImportJobLaunch(9900L);
        ArgumentCaptor<CapabilityAdmissionRequest> requestCaptor = ArgumentCaptor.forClass(CapabilityAdmissionRequest.class);
        verify(admissionPolicy).check(requestCaptor.capture());
        assertThat(requestCaptor.getValue().actorUserId()).isEqualTo(9900L);
        assertThat(requestCaptor.getValue().actorUserId()).isNotNull();
        verifyNoInteractions(importJobRepository, importJobItemRepository);
    }

    @Test
    void canonicalPolicyAllowsSystemLaunchWithoutInteractivePrincipalWhenRequestActorMatchesResolvedSystemActor()
        throws Throwable {
        SecurityContextHolder.clearContext();
        ImportJobRepository importJobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository importJobItemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        CapabilityAdmissionRequestFactory requestFactory = org.mockito.Mockito.mock(CapabilityAdmissionRequestFactory.class);
        SystemActorResolver systemActorResolver = org.mockito.Mockito.mock(SystemActorResolver.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);
        UserOrgFoundationStateReadService userOrgFoundationStateReadService = org.mockito.Mockito.mock(
            UserOrgFoundationStateReadService.class
        );
        AccessFoundationStateReadService accessFoundationStateReadService = org.mockito.Mockito.mock(
            AccessFoundationStateReadService.class
        );

        DefaultCapabilityAdmissionPolicy canonicalPolicy = new DefaultCapabilityAdmissionPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            new InteractiveActorResolver(new AuthenticatedActorAdapter())
        );
        canonicalPolicy.setSystemActorResolver(systemActorResolver);

        Object service = newService(
            importJobRepository,
            importJobItemRepository,
            canonicalPolicy,
            requestFactory,
            systemActorResolver,
            utcClock
        );
        Method launchMethod = service.getClass().getMethod("launchSystemImportJob", ImportJob.class, List.class);

        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(9900L);
        when(userOrgFoundationStateReadService.findActorCommandFoundationState(9900L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(9900L, true, Set.of())
        );
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCode.IMPORT_JOB_LAUNCH.code(),
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(requestFactory.createImportJobLaunch(9900L)).thenReturn(request);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(importJobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> {
            ImportJob saved = invocation.getArgument(0, ImportJob.class);
            return new ImportJob(
                3001L,
                saved.sourceType(),
                saved.sourceRef(),
                saved.initiatedByUserId(),
                saved.status(),
                saved.payload(),
                saved.startedAt(),
                saved.completedAt(),
                saved.totalItemCount(),
                saved.processedItemCount(),
                saved.appliedItemCount(),
                saved.failedItemCount(),
                saved.requiresReviewItemCount(),
                saved.createdAt(),
                saved.updatedAt()
            );
        });
        when(importJobItemRepository.saveImportJobItem(any(ImportJobItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, ImportJobItem.class));

        ImportJob launched = (ImportJob) invoke(launchMethod, service, rawSystemJob(), rawItems());

        assertThat(launched.id()).isEqualTo(3001L);
        verify(importJobRepository).saveImportJob(any(ImportJob.class));
        verify(importJobItemRepository).saveImportJobItem(any(ImportJobItem.class));
    }

    @Test
    void canonicalPolicyRejectsSystemLaunchWithoutInteractivePrincipalWhenRequestActorDoesNotMatchResolvedSystemActor()
        throws Throwable {
        SecurityContextHolder.clearContext();
        ImportJobRepository importJobRepository = org.mockito.Mockito.mock(ImportJobRepository.class);
        ImportJobItemRepository importJobItemRepository = org.mockito.Mockito.mock(ImportJobItemRepository.class);
        CapabilityAdmissionRequestFactory requestFactory = org.mockito.Mockito.mock(CapabilityAdmissionRequestFactory.class);
        SystemActorResolver systemActorResolver = org.mockito.Mockito.mock(SystemActorResolver.class);
        UtcClock utcClock = org.mockito.Mockito.mock(UtcClock.class);
        UserOrgFoundationStateReadService userOrgFoundationStateReadService = org.mockito.Mockito.mock(
            UserOrgFoundationStateReadService.class
        );
        AccessFoundationStateReadService accessFoundationStateReadService = org.mockito.Mockito.mock(
            AccessFoundationStateReadService.class
        );

        DefaultCapabilityAdmissionPolicy canonicalPolicy = new DefaultCapabilityAdmissionPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            new InteractiveActorResolver(new AuthenticatedActorAdapter())
        );
        canonicalPolicy.setSystemActorResolver(systemActorResolver);

        Object service = newService(
            importJobRepository,
            importJobItemRepository,
            canonicalPolicy,
            requestFactory,
            systemActorResolver,
            utcClock
        );
        Method launchMethod = service.getClass().getMethod("launchSystemImportJob", ImportJob.class, List.class);

        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(9900L);
        when(userOrgFoundationStateReadService.findActorCommandFoundationState(8800L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(8800L, true, Set.of())
        );
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            8800L,
            CapabilityOperationCode.IMPORT_JOB_LAUNCH.code(),
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(requestFactory.createImportJobLaunch(9900L)).thenReturn(request);

        assertThatThrownBy(() -> invoke(launchMethod, service, rawSystemJob(), rawItems()))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Authenticated principal is required");

        verifyNoInteractions(importJobRepository, importJobItemRepository);
    }

    private Object newService(
        ImportJobRepository importJobRepository,
        ImportJobItemRepository importJobItemRepository,
        CapabilityAdmissionPolicy admissionPolicy,
        CapabilityAdmissionRequestFactory requestFactory,
        SystemActorResolver systemActorResolver,
        UtcClock utcClock
    ) throws Exception {
        Class<?> serviceType = Class.forName(IMPORT_SERVICE_IMPL);
        return serviceType.getConstructor(
            ImportJobRepository.class,
            ImportJobItemRepository.class,
            CapabilityAdmissionPolicy.class,
            CapabilityAdmissionRequestFactory.class,
            SystemActorResolver.class,
            UtcClock.class
        ).newInstance(
            importJobRepository,
            importJobItemRepository,
            admissionPolicy,
            requestFactory,
            systemActorResolver,
            utcClock
        );
    }

    private Object invoke(Method method, Object target, Object... args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getTargetException();
        }
    }

    private ImportJob rawSystemJob() {
        return new ImportJob(
            null,
            "LDAP_FEED",
            "system-sync-2026-05-08",
            null,
            ImportJobStatus.IN_PROGRESS,
            "{\"rows\":2}",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
    }

    private List<ImportJobItem> rawItems() {
        return List.of(
            new ImportJobItem(
                null,
                0L,
                50,
                "APP_USER",
                "EXT-A",
                "EMP-A",
                ImportItemStatus.PROCESSING,
                null,
                "{\"row\":1}",
                null,
                null,
                null,
                FIXED_INSTANT.minusSeconds(60),
                FIXED_INSTANT.minusSeconds(60)
            )
        );
    }
}

