package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignCourseRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAssignmentCampaignCourseRepositoryAdapter implements AssignmentCampaignCourseRepository {

    private final SpringDataAssignmentCampaignCourseJpaRepository repository;
    private final AssignmentPersistenceMapper mapper;

    public JpaAssignmentCampaignCourseRepositoryAdapter(
        SpringDataAssignmentCampaignCourseJpaRepository repository,
        AssignmentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AssignmentCampaignCourse findAssignmentCampaignCourseById(Long assignmentCampaignCourseId) {
        return repository.findById(assignmentCampaignCourseId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Assignment campaign course not found: " + assignmentCampaignCourseId
            ));
    }

    @Override
    public List<AssignmentCampaignCourse> findAssignmentCampaignCoursesByCampaignId(Long assignmentCampaignId) {
        return mapper.toAssignmentCampaignCourses(repository.findAllByCampaignIdOrderByIdAsc(assignmentCampaignId));
    }

    @Override
    @Transactional
    public AssignmentCampaignCourse saveAssignmentCampaignCourse(AssignmentCampaignCourse assignmentCampaignCourse) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(assignmentCampaignCourse)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist assignment_campaign_course", exception);
        }
    }
}
