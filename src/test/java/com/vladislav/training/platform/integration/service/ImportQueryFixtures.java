package com.vladislav.training.platform.integration.service;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.time.Instant;
/**
 * Проверяет поведение {@code ImportQueryFixtures}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
final class ImportQueryFixtures {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-09T09:00:00Z");

    private ImportQueryFixtures() {
    }

    static ImportJob importJob(Long id, String sourceType, ImportJobStatus status, int totalItemCount) {
        return new ImportJob(
            id,
            sourceType,
            sourceType.toLowerCase() + "-ref",
            701L,
            status,
            "{\"rows\":" + totalItemCount + "}",
            null,
            null,
            totalItemCount,
            0,
            0,
            0,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    static ImportJobItem importJobItem(Long id, Long importJobId, int itemNo) {
        return new ImportJobItem(
            id,
            importJobId,
            itemNo,
            "APP_USER",
            "EXT-" + itemNo,
            "EMP-" + itemNo,
            ImportItemStatus.PENDING,
            null,
            "{\"employeeNumber\":\"EMP-" + itemNo + "\"}",
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
