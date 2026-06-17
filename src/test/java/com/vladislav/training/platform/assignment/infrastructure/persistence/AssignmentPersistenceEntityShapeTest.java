package com.vladislav.training.platform.assignment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет форму и состав {@code AssignmentPersistenceEntity}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class AssignmentPersistenceEntityShapeTest {

    @Test
    void campaignEntityShapeMatchesCanonicalTraceabilityRoot() throws NoSuchFieldException {
        assertEntityFieldSet(
            AssignmentCampaignEntity.class,
            Set.of("id", "name", "description", "sourceType", "sourceRef", "sourceNameSnapshot", "createdAt", "updatedAt")
        );
        assertColumn(AssignmentCampaignEntity.class, "name", "name", false);
        assertColumn(AssignmentCampaignEntity.class, "sourceType", "source_type", false);
        assertColumn(AssignmentCampaignEntity.class, "sourceRef", "source_ref", true);
        assertColumn(AssignmentCampaignEntity.class, "sourceNameSnapshot", "source_name_snapshot", true);
        assertColumn(AssignmentCampaignEntity.class, "createdAt", "created_at", false);
        assertColumn(AssignmentCampaignEntity.class, "updatedAt", "updated_at", false);
    }

    @Test
    void campaignCourseEntityShapeMatchesHistoricalCompositionLink() throws NoSuchFieldException {
        assertEntityFieldSet(
            AssignmentCampaignCourseEntity.class,
            Set.of("id", "campaignId", "courseId", "createdAt", "updatedAt")
        );
        assertColumn(AssignmentCampaignCourseEntity.class, "campaignId", "campaign_id", false);
        assertColumn(AssignmentCampaignCourseEntity.class, "courseId", "course_id", false);
        assertColumn(AssignmentCampaignCourseEntity.class, "createdAt", "created_at", false);
        assertColumn(AssignmentCampaignCourseEntity.class, "updatedAt", "updated_at", false);
    }

    @Test
    void recipientSnapshotEntityShapeRemainsImmutableAndSnapshotSpecific() throws NoSuchFieldException {
        assertEntityFieldSet(
            AssignmentCampaignRecipientSnapshotEntity.class,
            Set.of(
                "id",
                "campaignId",
                "userId",
                "organizationalUnitIdSnapshot",
                "organizationalPathSnapshot",
                "capturedAt",
                "inclusionBasisCode",
                "employeeNumberSnapshot",
                "fullNameSnapshot",
                "createdAt"
            )
        );
        assertThat(fieldNames(AssignmentCampaignRecipientSnapshotEntity.class)).doesNotContain("updatedAt");
        assertColumn(AssignmentCampaignRecipientSnapshotEntity.class, "organizationalUnitIdSnapshot", "organizational_unit_id_snapshot", false);
        assertColumn(AssignmentCampaignRecipientSnapshotEntity.class, "organizationalPathSnapshot", "organizational_path_snapshot", false);
        assertColumn(AssignmentCampaignRecipientSnapshotEntity.class, "capturedAt", "captured_at", false);
        assertColumn(AssignmentCampaignRecipientSnapshotEntity.class, "inclusionBasisCode", "inclusion_basis_code", false);
        assertColumn(AssignmentCampaignRecipientSnapshotEntity.class, "employeeNumberSnapshot", "employee_number_snapshot", true);
        assertColumn(AssignmentCampaignRecipientSnapshotEntity.class, "fullNameSnapshot", "full_name_snapshot", true);
        assertColumn(AssignmentCampaignRecipientSnapshotEntity.class, "createdAt", "created_at", false);
    }

    @Test
    void assignmentEntityShapeKeepsMaterializedStatusAndOwnerFactsSeparate() throws NoSuchFieldException {
        assertEntityFieldSet(
            AssignmentEntity.class,
            Set.of(
                "id",
                "campaignId",
                "userId",
                "courseId",
                "status",
                "assignedAt",
                "deadlineAt",
                "cancelledAt",
                "closedAt",
                "createdAt",
                "updatedAt"
            )
        );
        assertColumn(AssignmentEntity.class, "campaignId", "campaign_id", false);
        assertColumn(AssignmentEntity.class, "userId", "user_id", false);
        assertColumn(AssignmentEntity.class, "courseId", "course_id", false);
        assertColumn(AssignmentEntity.class, "assignedAt", "assigned_at", false);
        assertColumn(AssignmentEntity.class, "deadlineAt", "deadline_at", false);
        assertColumn(AssignmentEntity.class, "cancelledAt", "cancelled_at", true);
        assertColumn(AssignmentEntity.class, "closedAt", "closed_at", true);
        assertColumn(AssignmentEntity.class, "createdAt", "created_at", false);
        assertColumn(AssignmentEntity.class, "updatedAt", "updated_at", false);

        Field statusField = AssignmentEntity.class.getDeclaredField("status");
        assertThat(statusField.isAnnotationPresent(Enumerated.class)).isTrue();
        assertThat(statusField.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(statusField.getType()).isEqualTo(AssignmentStatus.class);
        assertColumn(AssignmentEntity.class, "status", "status", false);
    }

    @Test
    void assignmentTestEntityShapeKeepsSnapshotRoleAndCountedResultAnchorWithoutOwnerLogic() throws NoSuchFieldException {
        assertEntityFieldSet(
            AssignmentTestEntity.class,
            Set.of(
                "id",
                "assignmentId",
                "testId",
                "assignmentTestRole",
                "countedResultId",
                "closedAt",
                "closed",
                "createdAt",
                "updatedAt"
            )
        );
        assertColumn(AssignmentTestEntity.class, "assignmentId", "assignment_id", false);
        assertColumn(AssignmentTestEntity.class, "testId", "test_id", false);
        assertColumn(AssignmentTestEntity.class, "countedResultId", "counted_result_id", true);
        assertColumn(AssignmentTestEntity.class, "closedAt", "closed_at", true);
        assertColumn(AssignmentTestEntity.class, "closed", "is_closed", false);
        assertColumn(AssignmentTestEntity.class, "createdAt", "created_at", false);
        assertColumn(AssignmentTestEntity.class, "updatedAt", "updated_at", false);

        Field roleField = AssignmentTestEntity.class.getDeclaredField("assignmentTestRole");
        assertThat(roleField.isAnnotationPresent(Enumerated.class)).isTrue();
        assertThat(roleField.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(roleField.getType()).isEqualTo(AssignmentTestRole.class);
        assertColumn(AssignmentTestEntity.class, "assignmentTestRole", "assignment_test_role", false);
    }

    @Test
    void administrativeActionEntityShapeStaysTypedHistoryAndNotAuditSubstitute() throws NoSuchFieldException {
        assertEntityFieldSet(
            AssignmentAdministrativeActionEntity.class,
            Set.of("id", "assignmentId", "actionType", "occurredAt", "note", "createdAt")
        );
        assertThat(fieldNames(AssignmentAdministrativeActionEntity.class))
            .doesNotContain("actorUserId", "metadataJson", "updatedAt", "auditEventId");
        assertColumn(AssignmentAdministrativeActionEntity.class, "assignmentId", "assignment_id", false);
        assertColumn(AssignmentAdministrativeActionEntity.class, "occurredAt", "occurred_at", false);
        assertColumn(AssignmentAdministrativeActionEntity.class, "note", "note", true);
        assertColumn(AssignmentAdministrativeActionEntity.class, "createdAt", "created_at", false);

        Field actionTypeField = AssignmentAdministrativeActionEntity.class.getDeclaredField("actionType");
        assertThat(actionTypeField.isAnnotationPresent(Enumerated.class)).isTrue();
        assertThat(actionTypeField.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(actionTypeField.getType()).isEqualTo(AssignmentAdministrativeActionType.class);
        assertColumn(AssignmentAdministrativeActionEntity.class, "actionType", "action_type", false);
    }

    @Test
    void entitySkeletonsRemainPlainPersistenceCarriersWithoutServiceBehavior() {
        assertPlainPersistenceCarrier(AssignmentCampaignEntity.class);
        assertPlainPersistenceCarrier(AssignmentCampaignCourseEntity.class);
        assertPlainPersistenceCarrier(AssignmentCampaignRecipientSnapshotEntity.class);
        assertPlainPersistenceCarrier(AssignmentEntity.class);
        assertPlainPersistenceCarrier(AssignmentTestEntity.class);
        assertPlainPersistenceCarrier(AssignmentAdministrativeActionEntity.class);
    }

    private void assertEntityFieldSet(Class<?> entityType, Set<String> expectedFieldNames) {
        assertThat(fieldNames(entityType)).isEqualTo(expectedFieldNames);
    }

    private void assertPlainPersistenceCarrier(Class<?> entityType) {
        boolean persistenceAnnotationsOnly = Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .allMatch(field -> field.isAnnotationPresent(Column.class)
                || field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(GeneratedValue.class)
                || field.isAnnotationPresent(Enumerated.class));
        boolean scalarFieldTypesOnly = Stream.of(entityType.getDeclaredFields())
            .map(Field::getType)
            .allMatch(this::isPlainPersistenceFieldType);

        assertThat(entityType.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(persistenceAnnotationsOnly).isTrue();
        assertThat(scalarFieldTypesOnly).isTrue();
    }

    private boolean isPlainPersistenceFieldType(Class<?> fieldType) {
        return fieldType.equals(Long.class)
            || fieldType.equals(String.class)
            || fieldType.equals(Instant.class)
            || fieldType.equals(boolean.class)
            || fieldType.isEnum();
    }

    private Set<String> fieldNames(Class<?> entityType) {
        return Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private void assertColumn(Class<?> entityType, String fieldName, String expectedColumnName, boolean expectedNullable)
        throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(Column.class)).isTrue();
        Column column = field.getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(expectedColumnName);
        assertThat(column.nullable()).isEqualTo(expectedNullable);
    }
}
