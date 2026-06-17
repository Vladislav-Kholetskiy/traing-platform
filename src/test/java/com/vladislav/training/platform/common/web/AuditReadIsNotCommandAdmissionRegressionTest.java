package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AuditReadIsNotCommandAdmission} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AuditReadIsNotCommandAdmissionRegressionTest {

    private static final Path CANONICAL_ADMISSION_POLICY = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/DefaultCapabilityAdmissionPolicy.java"
    );
    private static final Path ADMISSION_REQUEST_FACTORY = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/CapabilityAdmissionRequestFactory.java"
    );
    private static final Path ACCESS_SPECIFICATION_POLICY = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"
    );
    private static final Path AUDIT_ADMIN_READ_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
    );
    private static final Path AUDIT_ADMIN_READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
    );
    private static final Path AUDIT_QUERY_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditQueryServiceImpl.java"
    );

    @Test
    void commandAdmissionPathMustNotDependOnAuditReadSide() throws Exception {
        String canonicalAdmissionPolicy = read(CANONICAL_ADMISSION_POLICY);
        String admissionRequestFactory = read(ADMISSION_REQUEST_FACTORY);
        String accessSpecificationPolicy = read(ACCESS_SPECIFICATION_POLICY);

        assertThat(canonicalAdmissionPolicy)
            .doesNotContain("AuditAdminReadService")
            .doesNotContain("AuditQueryService")
            .doesNotContain("AuditAdminReadController")
            .doesNotContain("AuditEventReadResponse")
            .doesNotContain("AuditEventRepository")
            .doesNotContain("findAudit")
            .doesNotContain("readAudit")
            .doesNotContain("auditEvent");

        assertThat(admissionRequestFactory)
            .doesNotContain("AuditAdminReadService")
            .doesNotContain("AuditQueryService")
            .doesNotContain("AuditEventReadResponse")
            .doesNotContain("AuditEventRepository")
            .doesNotContain("auditEvent")
            .doesNotContain("readAudit");

        assertThat(accessSpecificationPolicy)
            .doesNotContain("AuditAdminReadService")
            .doesNotContain("AuditQueryService")
            .doesNotContain("AuditEventReadResponse")
            .doesNotContain("AuditEventRepository")
            .doesNotContain("findAudit")
            .doesNotContain("readAudit");
    }

    @Test
    void auditReadSideMustNotUseCommandAdmissionAsDecisionSource() throws Exception {
        String auditController = read(AUDIT_ADMIN_READ_CONTROLLER);
        String auditReadService = read(AUDIT_ADMIN_READ_SERVICE);
        String auditQueryService = read(AUDIT_QUERY_SERVICE);

        assertThat(auditController)
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("CapabilityAdmissionRequestFactory")
            .doesNotContain("DefaultCapabilityAdmissionPolicy");

        assertThat(auditReadService)
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("CapabilityAdmissionRequestFactory")
            .doesNotContain("DefaultCapabilityAdmissionPolicy")
            .doesNotContain("check(")
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("NotificationRuleService");

        assertThat(auditQueryService)
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("CapabilityAdmissionRequestFactory")
            .doesNotContain("DefaultCapabilityAdmissionPolicy")
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("NotificationRuleService");
    }

    @Test
    void administrativeCommandServicesMustNotReadAuditAsFutureMutationProof() throws Exception {
        for (Path commandService : List.of(
            Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"),
            Path.of("src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"),
            Path.of("src/main/java/com/vladislav/training/platform/integration/service/ImportItemReviewServiceImpl.java"),
            Path.of("src/main/java/com/vladislav/training/platform/integration/service/ImportProcessingServiceImpl.java")
        )) {
            String source = read(commandService);
            assertThat(source)
                
                .doesNotContain("AuditAdminReadService")
                .doesNotContain("AuditQueryService")
                .doesNotContain("AuditAdminReadController")
                .doesNotContain("AuditEventReadResponse")
                .doesNotContain("AuditEventRepository")
                .doesNotContain("findAudit")
                .doesNotContain("readAudit")
                .doesNotContain("audit read")
                .doesNotContain("historical audit replay");
        }
    }

    @Test
    void auditReadMayRemainEvidenceOnlyWhileAuditWriteSideCompanionStaysAllowed() throws Exception {
        String auditService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditService.java"
        ));
        String criticalCommandAuditSupport = read(Path.of(
            "src/main/java/com/vladislav/training/platform/audit/service/CriticalCommandAuditSupport.java"
        ));

        assertThat(auditService).contains("recordAuditEvent");
        assertThat(criticalCommandAuditSupport).contains("AuditService");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}

