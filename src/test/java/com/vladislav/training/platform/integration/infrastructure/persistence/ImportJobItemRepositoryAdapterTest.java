package com.vladislav.training.platform.integration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
/**
 * Проверяет поведение {@code ImportJobItemRepositoryAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ImportJobItemRepositoryAdapterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T08:30:00Z");

    @Mock
    private SpringDataImportJobItemJpaRepository importJobItemJpaRepository;

    private final ImportMapper mapper = new ImportMapper();

    @Test
    void importJobItemAdapterMapsPrimaryKeyLookupJobSliceStatusSliceAndSave() {
        JpaImportJobItemRepositoryAdapter adapter =
            new JpaImportJobItemRepositoryAdapter(importJobItemJpaRepository, mapper);
        ImportJobItemEntity entity = importJobItemEntity(21L, 11L, 0);

        when(importJobItemJpaRepository.findById(21L)).thenReturn(Optional.of(entity));
        when(importJobItemJpaRepository.findAllByImportJobIdOrderByItemNoAsc(11L)).thenReturn(List.of(entity));
        when(importJobItemJpaRepository.findAllByStatusOrderByIdAsc(ImportItemStatus.PENDING.name()))
            .thenReturn(List.of(entity));
        when(importJobItemJpaRepository.save(any(ImportJobItemEntity.class))).thenReturn(entity);

        ImportJobItem found = adapter.findImportJobItemById(21L);
        ImportJobItem saved = adapter.saveImportJobItem(mapper.toDomain(entity));

        assertThat(found.id()).isEqualTo(21L);
        assertThat(found.importJobId()).isEqualTo(11L);
        assertThat(found.itemNo()).isZero();
        assertThat(found.targetEntityType()).isEqualTo("APP_USER");
        assertThat(found.externalId()).isEqualTo("EXT-21");
        assertThat(found.employeeNumber()).isEqualTo("EMP-021");
        assertThat(found.status()).isEqualTo(ImportItemStatus.PENDING);
        assertThat(found.payload()).isEqualTo("{\"row\":1}");

        assertThat(adapter.findImportJobItemsByJobId(11L))
            .singleElement()
            .satisfies(item -> assertThat(item.importJobId()).isEqualTo(11L));
        assertThat(adapter.findImportJobItemsByStatus(ImportItemStatus.PENDING))
            .singleElement()
            .satisfies(item -> assertThat(item.status()).isEqualTo(ImportItemStatus.PENDING));

        assertThat(saved.id()).isEqualTo(21L);
        assertThat(saved.errorCode()).isNull();
        verify(importJobItemJpaRepository).save(any(ImportJobItemEntity.class));
    }

    @Test
    void importJobItemAdapterThrowsForMissingPrimaryKeyAndWrapsConstraintViolations() {
        JpaImportJobItemRepositoryAdapter adapter =
            new JpaImportJobItemRepositoryAdapter(importJobItemJpaRepository, mapper);
        ImportJobItem domain = mapper.toDomain(importJobItemEntity(22L, 11L, 1));

        when(importJobItemJpaRepository.findById(999L)).thenReturn(Optional.empty());
        when(importJobItemJpaRepository.save(any(ImportJobItemEntity.class)))
            .thenThrow(new DataIntegrityViolationException("invalid import job item"));

        assertThatThrownBy(() -> adapter.findImportJobItemById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");
        assertThatThrownBy(() -> adapter.saveImportJobItem(domain))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("import job item")
            .hasRootCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void importJobItemMapperPreservesScalarFieldsAcrossDomainAndEntityBoundaries() {
        ImportJobItemEntity entity = importJobItemEntity(31L, 17L, 3);
        entity.setStatus(ImportItemStatus.REQUIRES_REVIEW.name());
        entity.setMatchedEntityId("USR-31");
        entity.setErrorCode("AMBIGUOUS_MATCH");
        entity.setErrorMessage("Multiple user candidates");
        entity.setProcessedAt(FIXED_INSTANT.plusSeconds(120));

        ImportJobItem domain = mapper.toDomain(entity);
        ImportJobItemEntity roundTrip = mapper.toEntity(domain);

        assertThat(domain.id()).isEqualTo(31L);
        assertThat(domain.importJobId()).isEqualTo(17L);
        assertThat(domain.itemNo()).isEqualTo(3);
        assertThat(domain.targetEntityType()).isEqualTo("APP_USER");
        assertThat(domain.externalId()).isEqualTo("EXT-31");
        assertThat(domain.employeeNumber()).isEqualTo("EMP-031");
        assertThat(domain.status()).isEqualTo(ImportItemStatus.REQUIRES_REVIEW);
        assertThat(domain.matchedEntityId()).isEqualTo("USR-31");
        assertThat(domain.payload()).isEqualTo("{\"row\":4}");
        assertThat(domain.errorCode()).isEqualTo("AMBIGUOUS_MATCH");
        assertThat(domain.errorMessage()).isEqualTo("Multiple user candidates");
        assertThat(domain.processedAt()).isEqualTo(FIXED_INSTANT.plusSeconds(120));
        assertThat(domain.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(domain.updatedAt()).isEqualTo(FIXED_INSTANT);

        assertThat(roundTrip.getId()).isEqualTo(31L);
        assertThat(roundTrip.getImportJobId()).isEqualTo(17L);
        assertThat(roundTrip.getItemNo()).isEqualTo(3);
        assertThat(roundTrip.getTargetEntityType()).isEqualTo("APP_USER");
        assertThat(roundTrip.getExternalId()).isEqualTo("EXT-31");
        assertThat(roundTrip.getEmployeeNumber()).isEqualTo("EMP-031");
        assertThat(roundTrip.getStatus()).isEqualTo("REQUIRES_REVIEW");
        assertThat(roundTrip.getMatchedEntityId()).isEqualTo("USR-31");
        assertThat(roundTrip.getPayload()).isEqualTo("{\"row\":4}");
        assertThat(roundTrip.getErrorCode()).isEqualTo("AMBIGUOUS_MATCH");
        assertThat(roundTrip.getErrorMessage()).isEqualTo("Multiple user candidates");
        assertThat(roundTrip.getProcessedAt()).isEqualTo(FIXED_INSTANT.plusSeconds(120));
    }

    private ImportJobItemEntity importJobItemEntity(Long id, Long importJobId, int itemNo) {
        ImportJobItemEntity entity = new ImportJobItemEntity();
        entity.setId(id);
        entity.setImportJobId(importJobId);
        entity.setItemNo(itemNo);
        entity.setTargetEntityType("APP_USER");
        entity.setExternalId("EXT-" + id);
        entity.setEmployeeNumber("EMP-0" + id);
        entity.setStatus(ImportItemStatus.PENDING.name());
        entity.setMatchedEntityId(null);
        entity.setPayload("{\"row\":" + (itemNo + 1) + "}");
        entity.setErrorCode(null);
        entity.setErrorMessage(null);
        entity.setProcessedAt(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }
}
