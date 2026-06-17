package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContext;
import com.vladislav.training.platform.content.service.PublishedCourseLearningContextReader;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.Topic;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@ConditionalOnBean(AssignmentSelfScopedReadRepository.class)
class AssignmentSelfScopedQueryServiceImpl implements AssignmentSelfScopedQueryService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String ASSIGNED_LEARNING_TARGET_FAMILY = "assigned_learning_context";
    private static final String SELF_SCOPED_ASSIGNMENT_DATA_MESSAGE =
        "Actor is not authorized to read self-scoped assignment data";
    private static final String ASSIGNED_LEARNING_CONTEXT_MESSAGE =
        "Actor is not authorized to read assigned learning context";
    private static final String ASSIGNED_TEST_CONTEXT_MESSAGE =
        "Actor is not authorized to read assigned test context";
    private static final String ASSIGNED_MATERIAL_CONTENT_MESSAGE =
        "Actor is not authorized to read assigned material content";

    private final AssignmentSelfScopedReadRepository assignmentSelfScopedReadRepository;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;
    private final PublishedCourseLearningContextReader publishedCourseLearningContextReader;
    private final AssignedTestContextProjectionReader assignedTestContextProjectionReader;

    AssignmentSelfScopedQueryServiceImpl(
        AssignmentSelfScopedReadRepository assignmentSelfScopedReadRepository,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver,
        PublishedCourseLearningContextReader publishedCourseLearningContextReader,
        AssignedTestContextProjectionReader assignedTestContextProjectionReader
    ) {
        this.assignmentSelfScopedReadRepository = assignmentSelfScopedReadRepository;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.contextResolver = contextResolver;
        this.publishedCourseLearningContextReader = publishedCourseLearningContextReader;
        this.assignedTestContextProjectionReader = assignedTestContextProjectionReader;
    }

    @Override
    public List<Assignment> findSelfAssignments(Long actorUserId) {
        AccessPolicyQueryContext context = resolveSelfScopedContext(
            actorUserId,
            AccessReadType.LIST,
            ASSIGNED_LEARNING_TARGET_FAMILY,
            SELF_SCOPED_ASSIGNMENT_DATA_MESSAGE
        );
        ensureSelfScopedReadAllowed(context, SELF_SCOPED_ASSIGNMENT_DATA_MESSAGE);
        return assignmentSelfScopedReadRepository.findSelfScopedAssignments(actorUserId);
    }

    @Override
    public Assignment findSelfAssignmentById(Long actorUserId, Long assignmentId) {
        AccessPolicyQueryContext context = resolveSelfScopedContext(
            actorUserId,
            AccessReadType.DETAIL,
            ASSIGNED_LEARNING_TARGET_FAMILY,
            SELF_SCOPED_ASSIGNMENT_DATA_MESSAGE
        );
        ensureSelfScopedReadAllowed(context, SELF_SCOPED_ASSIGNMENT_DATA_MESSAGE);
        return assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(actorUserId, assignmentId);
    }

    @Override
    public AssignedLearningContext findAssignedLearningContext(Long actorUserId, Long assignmentId) {
        AccessPolicyQueryContext context = resolveSelfScopedContext(
            actorUserId,
            AccessReadType.DETAIL,
            ASSIGNED_LEARNING_TARGET_FAMILY,
            ASSIGNED_LEARNING_CONTEXT_MESSAGE
        );
        ensureSelfScopedReadAllowed(context, ASSIGNED_LEARNING_CONTEXT_MESSAGE);
        Assignment assignment = assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(actorUserId, assignmentId);
        List<AssignmentTest> assignmentTests = assignmentSelfScopedReadRepository
            .findSelfScopedAssignmentTestsByAssignmentId(actorUserId, assignmentId);
        PublishedCourseLearningContext publishedLearningContext = publishedCourseLearningContextReader
            .readPublishedCourseLearningContext(assignment.courseId());
        return new AssignedLearningContext(assignment, assignmentTests, publishedLearningContext);
    }

    @Override
    public AssignedTestContext findAssignedTestContext(Long actorUserId, Long assignmentId, Long assignmentTestId) {
        AccessPolicyQueryContext context = resolveSelfScopedContext(
            actorUserId,
            AccessReadType.DETAIL,
            ASSIGNED_LEARNING_TARGET_FAMILY,
            ASSIGNED_TEST_CONTEXT_MESSAGE
        );
        ensureSelfScopedReadAllowed(context, ASSIGNED_TEST_CONTEXT_MESSAGE);
        assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(actorUserId, assignmentId);
        AssignmentTest assignmentTest = assignmentSelfScopedReadRepository
            .findSelfScopedAssignmentTestsByAssignmentId(actorUserId, assignmentId).stream()
            .filter(candidate -> candidate.id().equals(assignmentTestId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(
                "Assignment test not found in self-scoped assignment context: assignmentId="
                    + assignmentId
                    + ", assignmentTestId="
                    + assignmentTestId
            ));
        return assignedTestContextProjectionReader.readAssignedTestContext(assignmentTest);
    }

    @Override
    public AssignedMaterialContent findAssignedMaterialContent(Long actorUserId, Long assignmentId, Long materialId) {
        AccessPolicyQueryContext context = resolveSelfScopedContext(
            actorUserId,
            AccessReadType.DETAIL,
            ASSIGNED_LEARNING_TARGET_FAMILY,
            ASSIGNED_MATERIAL_CONTENT_MESSAGE
        );
        ensureSelfScopedReadAllowed(context, ASSIGNED_MATERIAL_CONTENT_MESSAGE);
        Assignment assignment = assignmentSelfScopedReadRepository.findSelfScopedAssignmentById(actorUserId, assignmentId);
        PublishedCourseLearningContext publishedLearningContext = publishedCourseLearningContextReader
            .readPublishedCourseLearningContext(assignment.courseId());

        Material material = publishedLearningContext.materials().stream()
            .filter(candidate -> candidate.id().equals(materialId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(
                "Material not found in assigned learning context: assignmentId="
                    + assignmentId
                    + ", materialId="
                    + materialId
            ));

        Topic topic = publishedLearningContext.topics().stream()
            .filter(candidate -> candidate.id().equals(material.topicId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(
                "Material topic not found in assigned learning context: assignmentId="
                    + assignmentId
                    + ", materialId="
                    + materialId
                    + ", topicId="
                    + material.topicId()
            ));

        return new AssignedMaterialContent(
            assignmentId,
            publishedLearningContext.course(),
            topic,
            material
        );
    }

    private AccessPolicyQueryContext resolveSelfScopedContext(
        Long actorUserId,
        AccessReadType readType,
        String targetEntityFamily,
        String denialMessage
    ) {
        AccessPolicyQueryContext context = contextResolver.resolveActorSelfScope(
            AccessReadArea.ASSIGNMENT,
            readType,
            targetEntityFamily
        );
        if (!context.actorUserId().equals(actorUserId)) {
            throw new PolicyViolationException(
                NOT_AUTHORIZED,
                denialMessage
            );
        }
        return context;
    }

    private void ensureSelfScopedReadAllowed(AccessPolicyQueryContext context, String denialMessage) {
        if (!accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException(
                NOT_AUTHORIZED,
                denialMessage
            );
        }
    }
}
