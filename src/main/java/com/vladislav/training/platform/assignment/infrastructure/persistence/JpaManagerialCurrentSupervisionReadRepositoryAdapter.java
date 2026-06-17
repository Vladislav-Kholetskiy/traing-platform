package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.repository.ManagerialCurrentSupervisionReadRepository;
import com.vladislav.training.platform.assignment.repository.ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria;
import com.vladislav.training.platform.assignment.repository.ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.ManagerialVisibleUsersRestrictionBuilder;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaManagerialCurrentSupervisionReadRepositoryAdapter implements ManagerialCurrentSupervisionReadRepository {

    private final EntityManager entityManager;

    public JpaManagerialCurrentSupervisionReadRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
    }

    @Override
    public List<ManagerialCurrentSupervisionReadRow> findCurrentSupervisionRows(
        ManagerialCurrentSupervisionReadCriteria criteria
    ) {
        Objects.requireNonNull(criteria, "criteria must not be null");

        var restriction = ManagerialVisibleUsersRestrictionBuilder.build(criteria.managerialReadScope());
        if (restriction.failClosed()) {
            return List.of();
        }

        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var query = criteriaBuilder.createQuery(ManagerialCurrentSupervisionReadRow.class);
        var assignment = query.from(AssignmentEntity.class);
        var assignmentTest = query.from(AssignmentTestEntity.class);
        var user = query.from(AppUserEntity.class);
        var course = query.from(CourseEntity.class);
        Specification<AssignmentEntity> visibleUsers = restriction.toSpecification("userId");

        query.select(criteriaBuilder.construct(
            ManagerialCurrentSupervisionReadRow.class,
            assignment.get("id"),
            assignment.get("userId"),
            criteriaBuilder.concat(
                criteriaBuilder.concat(user.get("lastName"), " "),
                user.get("firstName")
            ),
            assignment.get("courseId"),
            course.get("name"),
            criteriaBuilder.count(assignmentTest.get("id")),
            assignment.get("assignedAt"),
            assignment.get("deadlineAt"),
            assignment.get("status")
        )).where(
            criteriaBuilder.equal(assignmentTest.get("assignmentId"), assignment.get("id")),
            criteriaBuilder.equal(user.get("id"), assignment.get("userId")),
            criteriaBuilder.equal(course.get("id"), assignment.get("courseId")),
            visibleUsers.toPredicate(assignment, query, criteriaBuilder)
        ).groupBy(
            assignment.get("id"),
            assignment.get("userId"),
            user.get("lastName"),
            user.get("firstName"),
            assignment.get("courseId"),
            course.get("name"),
            assignment.get("assignedAt"),
            assignment.get("deadlineAt"),
            assignment.get("status")
        ).orderBy(
            criteriaBuilder.asc(assignment.get("id")),
            criteriaBuilder.asc(assignment.get("courseId"))
        );

        return entityManager.createQuery(query).getResultList();
    }
}
