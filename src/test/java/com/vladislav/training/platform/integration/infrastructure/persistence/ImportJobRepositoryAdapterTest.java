package com.vladislav.training.platform.integration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
/**
 * Проверяет поведение {@code ImportJobRepositoryAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ImportJobRepositoryAdapterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T08:00:00Z");
    private static final Path IMPORT_PERSISTENCE_PACKAGE =
        Path.of("src/main/java/com/vladislav/training/platform/integration/infrastructure/persistence");
    private static final Path IMPORT_CONTROLLER_PACKAGE =
        Path.of("src/main/java/com/vladislav/training/platform/integration/controller");

    @Mock
    private SpringDataImportJobJpaRepository importJobJpaRepository;

    private final ImportMapper mapper = new ImportMapper();

    @Test
    void importJobAdapterMapsPrimaryKeyLookupStatusSourceTypeAndSave() {
        JpaImportJobRepositoryAdapter adapter = new JpaImportJobRepositoryAdapter(importJobJpaRepository, mapper);
        ImportJobEntity entity = importJobEntity(11L, "HR_CSV");

        when(importJobJpaRepository.findById(11L)).thenReturn(Optional.of(entity));
        when(importJobJpaRepository.findAllByStatusOrderByIdAsc(ImportJobStatus.PENDING.name())).thenReturn(List.of(entity));
        when(importJobJpaRepository.findAllBySourceTypeOrderByIdAsc("HR_CSV")).thenReturn(List.of(entity));
        when(importJobJpaRepository.save(any(ImportJobEntity.class))).thenReturn(entity);

        ImportJob found = adapter.findImportJobById(11L);
        ImportJob saved = adapter.saveImportJob(mapper.toDomain(entity));

        assertThat(found.id()).isEqualTo(11L);
        assertThat(found.sourceType()).isEqualTo("HR_CSV");
        assertThat(found.sourceRef()).isEqualTo("batch-11.csv");
        assertThat(found.initiatedByUserId()).isEqualTo(901L);
        assertThat(found.status()).isEqualTo(ImportJobStatus.PENDING);
        assertThat(found.payload()).isEqualTo("{\"rows\":2}");
        assertThat(found.totalItemCount()).isEqualTo(2);

        assertThat(adapter.findImportJobsByStatus(ImportJobStatus.PENDING))
            .singleElement()
            .satisfies(job -> assertThat(job.status()).isEqualTo(ImportJobStatus.PENDING));
        assertThat(adapter.findImportJobsBySourceType("HR_CSV"))
            .singleElement()
            .satisfies(job -> assertThat(job.sourceType()).isEqualTo("HR_CSV"));

        assertThat(saved.id()).isEqualTo(11L);
        assertThat(saved.processedItemCount()).isZero();
        verify(importJobJpaRepository).save(any(ImportJobEntity.class));
    }

    @Test
    void importJobAdapterThrowsForMissingPrimaryKeyAndWrapsConstraintViolations() {
        JpaImportJobRepositoryAdapter adapter = new JpaImportJobRepositoryAdapter(importJobJpaRepository, mapper);
        ImportJob domain = mapper.toDomain(importJobEntity(12L, "HR_CSV"));

        when(importJobJpaRepository.findById(999L)).thenReturn(Optional.empty());
        when(importJobJpaRepository.save(any(ImportJobEntity.class)))
            .thenThrow(new DataIntegrityViolationException("invalid import job"));

        assertThatThrownBy(() -> adapter.findImportJobById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");
        assertThatThrownBy(() -> adapter.saveImportJob(domain))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("import job")
            .hasRootCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void importJobMapperPreservesScalarFieldsAndPersistenceBoundaryStaysControllerFree() throws Exception {
        ImportJobEntity entity = importJobEntity(31L, "LDAP_FEED");

        ImportJob domain = mapper.toDomain(entity);
        ImportJobEntity roundTrip = mapper.toEntity(domain);

        assertThat(domain.id()).isEqualTo(31L);
        assertThat(domain.sourceType()).isEqualTo("LDAP_FEED");
        assertThat(domain.sourceRef()).isEqualTo("batch-31.csv");
        assertThat(domain.initiatedByUserId()).isEqualTo(901L);
        assertThat(domain.status()).isEqualTo(ImportJobStatus.PENDING);
        assertThat(domain.payload()).isEqualTo("{\"rows\":2}");
        assertThat(domain.startedAt()).isEqualTo(FIXED_INSTANT.plusSeconds(60));
        assertThat(domain.completedAt()).isNull();
        assertThat(domain.totalItemCount()).isEqualTo(2);
        assertThat(domain.processedItemCount()).isZero();
        assertThat(domain.appliedItemCount()).isZero();
        assertThat(domain.failedItemCount()).isZero();
        assertThat(domain.requiresReviewItemCount()).isZero();
        assertThat(domain.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(domain.updatedAt()).isEqualTo(FIXED_INSTANT);

        assertThat(roundTrip.getId()).isEqualTo(31L);
        assertThat(roundTrip.getSourceType()).isEqualTo("LDAP_FEED");
        assertThat(roundTrip.getSourceRef()).isEqualTo("batch-31.csv");
        assertThat(roundTrip.getInitiatedByUserId()).isEqualTo(901L);
        assertThat(roundTrip.getStatus()).isEqualTo("PENDING");
        assertThat(roundTrip.getPayload()).isEqualTo("{\"rows\":2}");
        assertThat(roundTrip.getStartedAt()).isEqualTo(FIXED_INSTANT.plusSeconds(60));
        assertThat(roundTrip.getTotalItemCount()).isEqualTo(2);

        String persistenceSource = Files.walk(IMPORT_PERSISTENCE_PACKAGE)
            .filter(path -> path.toString().endsWith(".java"))
            .map(path -> {
                try {
                    return Files.readString(path);
                } catch (Exception exception) {
                    throw new IllegalStateException("Cannot read source: " + path, exception);
                }
            })
            .reduce("", (left, right) -> left + "\n" + right);

        assertThat(persistenceSource)
            .doesNotContain("AssignmentEntity")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("ResultEntity")
            .doesNotContain("CourseEntity")
            .doesNotContain("UserEntity");
        assertThat(Files.exists(IMPORT_CONTROLLER_PACKAGE)).isTrue();
    }

    private ImportJobEntity importJobEntity(Long id, String sourceType) {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(id);
        entity.setSourceType(sourceType);
        entity.setSourceRef("batch-" + id + ".csv");
        entity.setInitiatedByUserId(901L);
        entity.setStatus(ImportJobStatus.PENDING.name());
        entity.setPayload("{\"rows\":2}");
        entity.setStartedAt(FIXED_INSTANT.plusSeconds(60));
        entity.setCompletedAt(null);
        entity.setTotalItemCount(2);
        entity.setProcessedItemCount(0);
        entity.setAppliedItemCount(0);
        entity.setFailedItemCount(0);
        entity.setRequiresReviewItemCount(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }
}
