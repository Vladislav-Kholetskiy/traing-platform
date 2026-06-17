package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
/**
 * Проверяет поведение {@code ImportCommandServiceAdmission}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ImportCommandServiceAdmissionTest {

    private static final String IMPORT_SERVICE_IMPL =
        "com.vladislav.training.platform.integration.service.ImportCommandServiceImpl";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T11:00:00Z");

    @Test
    void importCommandServiceMustExposeInteractiveAndSystemLaunchOperations() throws Exception {
        assertThatCode(() -> ImportCommandService.class.getDeclaredMethod(
            "launchImportJob",
            ImportJob.class,
            List.class
        )).doesNotThrowAnyException();
        assertThatCode(() -> ImportCommandService.class.getDeclaredMethod(
            "launchSystemImportJob",
            ImportJob.class,
            List.class
        )).doesNotThrowAnyException();
        assertThatCode(() -> Class.forName(IMPORT_SERVICE_IMPL)).doesNotThrowAnyException();
    }

    @Test
    void interactiveLaunchChecksAdmissionBeforeAnyPersistenceAndDeniedPathCreatesNoRows() throws Exception {
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
        Method launchMethod = service.getClass().getMethod("launchImportJob", ImportJob.class, List.class);

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            701L,
            CapabilityOperationCode.IMPORT_JOB_LAUNCH.code(),
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(requestFactory.createImportJobLaunch()).thenReturn(request);
        doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "import denied"))
            .when(admissionPolicy).check(request);

        assertThatThrownBy(() -> invoke(launchMethod, service, rawJob(), rawItems(2)))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("import denied");

        verify(requestFactory).createImportJobLaunch();
        verify(admissionPolicy).check(request);
        verifyNoInteractions(importJobRepository, importJobItemRepository);
        verify(systemActorResolver, never()).resolveSystemActorUserId();
    }

    @Test
    void allowedInteractiveLaunchCreatesPendingJobAndSequentialPendingItems() throws Throwable {
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
        Method launchMethod = service.getClass().getMethod("launchImportJob", ImportJob.class, List.class);

        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            701L,
            CapabilityOperationCode.IMPORT_JOB_LAUNCH.code(),
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(requestFactory.createImportJobLaunch()).thenReturn(request);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(importJobRepository.saveImportJob(any(ImportJob.class))).thenAnswer(invocation -> {
            ImportJob saved = invocation.getArgument(0, ImportJob.class);
            return new ImportJob(
                1001L,
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

        ImportJob launched = (ImportJob) invoke(launchMethod, service, rawJob(), rawItems(3));

        assertThat(launched.id()).isEqualTo(1001L);
        assertThat(launched.sourceType()).isEqualTo("HR_CSV");
        assertThat(launched.sourceRef()).isEqualTo("hr-feed-2026-05.csv");
        assertThat(launched.payload()).isEqualTo("{\"rows\":3}");
        assertThat(launched.initiatedByUserId()).isEqualTo(701L);
        assertThat(launched.status()).isEqualTo(ImportJobStatus.PENDING);
        assertThat(launched.totalItemCount()).isEqualTo(3);
        assertThat(launched.processedItemCount()).isZero();
        assertThat(launched.appliedItemCount()).isZero();
        assertThat(launched.failedItemCount()).isZero();
        assertThat(launched.requiresReviewItemCount()).isZero();

        ArgumentCaptor<ImportJob> savedJobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository).saveImportJob(savedJobCaptor.capture());
        ImportJob savedJob = savedJobCaptor.getValue();
        assertThat(savedJob.id()).isNull();
        assertThat(savedJob.status()).isEqualTo(ImportJobStatus.PENDING);
        assertThat(savedJob.initiatedByUserId()).isEqualTo(701L);
        assertThat(savedJob.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(savedJob.updatedAt()).isEqualTo(FIXED_INSTANT);

        ArgumentCaptor<ImportJobItem> savedItemCaptor = ArgumentCaptor.forClass(ImportJobItem.class);
        verify(importJobItemRepository, org.mockito.Mockito.times(3)).saveImportJobItem(savedItemCaptor.capture());
        assertThat(savedItemCaptor.getAllValues())
            .extracting(ImportJobItem::importJobId)
            .containsExactly(1001L, 1001L, 1001L);
        assertThat(savedItemCaptor.getAllValues())
            .extracting(ImportJobItem::itemNo)
            .containsExactly(0, 1, 2);
        assertThat(savedItemCaptor.getAllValues())
            .extracting(ImportJobItem::status)
            .containsOnly(ImportItemStatus.PENDING);

        InOrder inOrder = inOrder(admissionPolicy, importJobRepository, importJobItemRepository);
        inOrder.verify(admissionPolicy).check(request);
        inOrder.verify(importJobRepository).saveImportJob(any(ImportJob.class));
        inOrder.verify(importJobItemRepository, org.mockito.Mockito.times(3)).saveImportJobItem(any(ImportJobItem.class));
        verify(systemActorResolver, never()).resolveSystemActorUserId();
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

    private ImportJob rawJob() {
        return new ImportJob(
            null,
            "HR_CSV",
            "hr-feed-2026-05.csv",
            null,
            ImportJobStatus.IN_PROGRESS,
            "{\"rows\":3}",
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

    private List<ImportJobItem> rawItems(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(index -> new ImportJobItem(
                null,
                0L,
                99 + index,
                "APP_USER",
                "EXT-" + index,
                "EMP-" + index,
                ImportItemStatus.PROCESSING,
                null,
                "{\"row\":" + (index + 1) + "}",
                null,
                null,
                null,
                FIXED_INSTANT.minusSeconds(60),
                FIXED_INSTANT.minusSeconds(60)
            ))
            .toList();
    }
}
