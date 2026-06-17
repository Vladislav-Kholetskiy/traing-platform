package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContext;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContextReader;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentSelfScopedQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentSelfScopedQueryServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-14T09:00:00Z");

    @Mock
    private AssignmentSelfScopedReadRepository assignmentSelfScopedReadRepository;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;
    @Mock
    private PublishedCourseLearningContextReader publishedCourseLearningContextReader;
    @Mock
    private AssignedTestContextProjectionReader assignedTestContextProjectionReader;

    @Test
    void selfScopedListChecksPolicyBeforeUsingSelfScopedRepository() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        AccessPolicyQueryContext context = selfScopedListContext(101L);
        Assignment assignment = assignment(77L, 101L);
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assigned_learning_context"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignments(101L)).thenReturn(List.of(assignment));

        assertThat(service.findSelfAssignments(101L)).containsExactly(assignment);

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assigned_learning_context");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignments(101L);
    }

    @Test
    void selfScopedDetailChecksPolicyBeforeUsingSelfScopedRepository() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        AccessPolicyQueryContext context = selfScopedDetailContext(101L);
        Assignment assignment = assignment(77L, 101L);
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(101L, 77L)).thenReturn(assignment);

        assertThat(service.findSelfAssignmentById(101L, 77L)).isEqualTo(assignment);

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentById(101L, 77L);
    }

    @Test
    void deniedSelfScopedReadStopsBeforeRepositoryAccess() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        AccessPolicyQueryContext context = selfScopedDetailContext(101L);
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findSelfAssignmentById(101L, 77L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("self-scoped assignment data");

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context");
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(assignmentSelfScopedReadRepository);
    }

    @Test
    void actorMismatchStopsBeforePolicyAndRepositoryAccess() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assigned_learning_context"))
            .thenReturn(selfScopedListContext(999L));

        assertThatThrownBy(() -> service.findSelfAssignments(101L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("self-scoped assignment data");

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assigned_learning_context");
        verifyNoInteractions(accessSpecificationPolicy);
        verifyNoInteractions(assignmentSelfScopedReadRepository);
    }

    @Test
    void selfScopedDetailPropagatesOwnerSafeNotFoundFromSelfScopedRepositoryPath() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        AccessPolicyQueryContext context = selfScopedDetailContext(101L);
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(101L, 77L))
            .thenThrow(new NotFoundException("Assignment not found in self scope: actorUserId=101, assignmentId=77"));

        assertThatThrownBy(() -> service.findSelfAssignmentById(101L, 77L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("self scope");

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentById(101L, 77L);
    }

    @Test
    void assignedLearningContextReusesAssignmentAnchoredDetailPolicyPathAndAddsSubordinateTests() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        AccessPolicyQueryContext context = selfScopedDetailContext(101L);
        Assignment assignment = assignment(77L, 101L);
        AssignmentTest finalControl = assignmentTest(701L, 77L, 501L);
        AssignmentTest secondControl = assignmentTest(702L, 77L, 502L);
        PublishedCourseLearningContext publishedLearningContext = publishedLearningContext(assignment.courseId());
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(101L, 77L)).thenReturn(assignment);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentTestsByAssignmentId(101L, 77L))
            .thenReturn(List.of(finalControl, secondControl));
        when(publishedCourseLearningContextReader.readPublishedCourseLearningContext(assignment.courseId()))
            .thenReturn(publishedLearningContext);

        AssignedLearningContext learningContext = service.findAssignedLearningContext(101L, 77L);

        assertThat(learningContext.assignment()).isEqualTo(assignment);
        assertThat(learningContext.assignmentTests()).containsExactly(finalControl, secondControl);
        assertThat(learningContext.publishedLearningContext()).isEqualTo(publishedLearningContext);

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentById(101L, 77L);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentTestsByAssignmentId(101L, 77L);
        verify(publishedCourseLearningContextReader).readPublishedCourseLearningContext(assignment.courseId());
    }

    @Test
    void deniedAssignedLearningContextStopsBeforeRepositoryAccess() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        AccessPolicyQueryContext context = selfScopedDetailContext(101L);
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findAssignedLearningContext(101L, 77L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("assigned learning context");

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context");
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(assignmentSelfScopedReadRepository);
        verifyNoInteractions(publishedCourseLearningContextReader);
    }

    @Test
    void assignedLearningContextActorMismatchStopsBeforePolicyAndRepositoryAccess() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context"))
            .thenReturn(selfScopedDetailContext(999L));

        assertThatThrownBy(() -> service.findAssignedLearningContext(101L, 77L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("assigned learning context");

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context");
        verifyNoInteractions(accessSpecificationPolicy);
        verifyNoInteractions(assignmentSelfScopedReadRepository);
        verifyNoInteractions(publishedCourseLearningContextReader);
    }

    @Test
    void assignedTestContextUsesOwnedAssignmentAndAssignmentTestAnchorBeforeProjectionReader() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        AccessPolicyQueryContext context = selfScopedDetailContext(101L);
        Assignment assignment = assignment(77L, 101L);
        AssignmentTest assignmentTest = assignmentTest(701L, 77L, 501L);
        AssignedTestContext projection = new AssignedTestContext(
            77L,
            701L,
            501L,
            "Assigned final test",
            List.of()
        );
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(101L, 77L)).thenReturn(assignment);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentTestsByAssignmentId(101L, 77L))
            .thenReturn(List.of(assignmentTest));
        when(assignedTestContextProjectionReader.readAssignedTestContext(assignmentTest)).thenReturn(projection);

        AssignedTestContext returned = service.findAssignedTestContext(101L, 77L, 701L);

        assertThat(returned).isEqualTo(projection);
        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentById(101L, 77L);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentTestsByAssignmentId(101L, 77L);
        verify(assignedTestContextProjectionReader).readAssignedTestContext(assignmentTest);
    }

    @Test
    void assignedTestContextRejectsAssignmentTestThatDoesNotBelongToOwnedAssignmentBeforeProjectionReaderRuns() {
        AssignmentSelfScopedQueryServiceImpl service = service();
        AccessPolicyQueryContext context = selfScopedDetailContext(101L);
        Assignment assignment = assignment(77L, 101L);
        when(contextResolver.resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(101L, 77L)).thenReturn(assignment);
        when(assignmentSelfScopedReadRepository.findSelfScopedAssignmentTestsByAssignmentId(101L, 77L))
            .thenReturn(List.of(assignmentTest(999L, 77L, 501L)));

        assertThatThrownBy(() -> service.findAssignedTestContext(101L, 77L, 701L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Assignment test not found in self-scoped assignment context");

        verify(contextResolver).resolveActorSelfScope(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assigned_learning_context");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentById(101L, 77L);
        verify(assignmentSelfScopedReadRepository).findSelfScopedAssignmentTestsByAssignmentId(101L, 77L);
        verifyNoInteractions(assignedTestContextProjectionReader);
    }

    @Test
    void selfScopedQueryServiceStaysReadOnlyAndPolicyAware() {
        assertThat(fieldTypes(AssignmentSelfScopedQueryServiceImpl.class))
            .contains(
                AssignmentSelfScopedReadRepository.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class,
                PublishedCourseLearningContextReader.class,
                AssignedTestContextProjectionReader.class
            )
            .doesNotContain(
                com.vladislav.training.platform.testing.service.AssignedAttemptEntryService.class,
                com.vladislav.training.platform.testing.service.SelfAttemptEntryService.class,
                com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService.class,
                com.vladislav.training.platform.testing.service.SelfVisibleTestingReadService.class,
                com.vladislav.training.platform.result.service.ResultRecordingService.class
            );
        assertThat(fieldTypes(AssignedLearningContext.class))
            .containsExactlyInAnyOrder(Assignment.class, List.class, PublishedCourseLearningContext.class);
    }

    @Test
    void assignedLearningContextRecordDefensivelyCopiesSubordinateTests() {
        Assignment assignment = assignment(77L, 101L);
        List<AssignmentTest> mutableList = new java.util.ArrayList<>(List.of(assignmentTest(701L, 77L, 501L)));

        AssignedLearningContext context = new AssignedLearningContext(
            assignment,
            mutableList,
            publishedLearningContext(assignment.courseId())
        );
        mutableList.clear();

        assertThat(context.assignmentTests()).hasSize(1);
        assertThat(context.assignmentTests()).isUnmodifiable();
    }

    @Test
    void assignedTestContextPathDoesNotReuseSelfVisibleTestingShortcutVocabulary() throws Exception {
        String serviceSource = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentSelfScopedQueryServiceImpl.java"
        ));
        String projectionSource = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/RepositoryBackedAssignedTestContextProjectionReader.java"
        ));

        assertThat(serviceSource)
            .doesNotContain("SelfVisibleTestingReadService")
            .doesNotContain("SelfVisibleTestingProjectionReader")
            .doesNotContain("findSelfVisibleTestById");
        assertThat(projectionSource)
            .contains("assignmentTest.testId()")
            .doesNotContain("findActiveFinalTestByTopicId")
            .doesNotContain("SelfVisibleTesting");
    }

    private AssignmentSelfScopedQueryServiceImpl service() {
        return new AssignmentSelfScopedQueryServiceImpl(
            assignmentSelfScopedReadRepository,
            accessSpecificationPolicy,
            contextResolver,
            publishedCourseLearningContextReader,
            assignedTestContextProjectionReader
        );
    }

    private AccessPolicyQueryContext selfScopedListContext(Long actorUserId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assigned_learning_context",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private AccessPolicyQueryContext selfScopedDetailContext(Long actorUserId) {
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assigned_learning_context",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private Assignment assignment(Long assignmentId, Long userId) {
        return new Assignment(
            assignmentId,
            900L,
            userId,
            301L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(86400),
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentTest assignmentTest(Long assignmentTestId, Long assignmentId, Long testId) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            testId,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            null,
            false,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }

    private PublishedCourseLearningContext publishedLearningContext(Long courseId) {
        Topic publishedTopic = new Topic(
            401L,
            courseId,
            "Topic",
            "Published topic",
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        Material publishedMaterial = new Material(
            501L,
            publishedTopic.id(),
            "Material",
            "Published material",
            null,
            null,
            MaterialType.TEXT,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        return new PublishedCourseLearningContext(
            new Course(courseId, "Course", "Published course", ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT),
            List.of(publishedTopic),
            List.of(publishedMaterial)
        );
    }
}
