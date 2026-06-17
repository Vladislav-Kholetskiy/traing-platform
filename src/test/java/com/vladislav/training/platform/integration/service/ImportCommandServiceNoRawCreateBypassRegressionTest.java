package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImportCommandServiceNoRawCreateBypass} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportCommandServiceNoRawCreateBypassRegressionTest {

    private static final Path IMPORT_COMMAND_SERVICE_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"
    );

    @Test
    void importCommandServiceContractMustNotExposeRawCreateBypassMethods() {
        Set<String> interfaceMethodNames = Arrays.stream(ImportCommandService.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());

        assertThat(interfaceMethodNames)
            .contains("launchImportJob", "launchSystemImportJob")
            .doesNotContain("createImportJob", "createImportJobItem");
    }

    @Test
    void importCommandServiceImplMustNotExposePublicRawCreateBypassMethods() throws Exception {
        Set<String> publicMethodNames = Arrays.stream(ImportCommandServiceImpl.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());

        assertThat(publicMethodNames)
            .contains("launchImportJob", "launchSystemImportJob")
            .doesNotContain("createImportJob", "createImportJobItem");
    }

    @Test
    void importCommandServiceImplKeepsRepositorySavesInsidePrivateAdmittedMaterializationPathOnly() throws Exception {
        String source = Files.readString(IMPORT_COMMAND_SERVICE_IMPL);

        assertThat(source)
            .contains("private ImportJob materializeRawImportJob(")
            .contains("capabilityAdmissionPolicy.check(request);")
            .contains("return materializeRawImportJob(importJob, importJobItems, request.actorUserId(), true);")
            .contains("return materializeRawImportJob(importJob, importJobItems, systemActorUserId, false);")
            .doesNotContain("public ImportJob createImportJob(")
            .doesNotContain("public ImportJobItem createImportJobItem(");
    }
}
