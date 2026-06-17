package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAssignmentCampaignRepositoryAdapter implements AssignmentCampaignRepository {

    private final SpringDataAssignmentCampaignJpaRepository repository;
    private final AssignmentPersistenceMapper mapper;

    public JpaAssignmentCampaignRepositoryAdapter(
        SpringDataAssignmentCampaignJpaRepository repository,
        AssignmentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AssignmentCampaign findAssignmentCampaignById(Long assignmentCampaignId) {
        return repository.findById(assignmentCampaignId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Assignment campaign not found: " + assignmentCampaignId));
    }

    @Override
    public List<AssignmentCampaign> findAllAssignmentCampaigns() {
        return mapper.toAssignmentCampaigns(repository.findAllByOrderByIdAsc());
    }

    @Override
    public List<AssignmentCampaign> findAssignmentCampaignsBySourceType(String sourceType) {
        return mapper.toAssignmentCampaigns(repository.findAllBySourceTypeOrderByIdAsc(sourceType));
    }

    @Override
    @Transactional
    public AssignmentCampaign saveAssignmentCampaign(AssignmentCampaign assignmentCampaign) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(assignmentCampaign)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist assignment_campaign", exception);
        }
    }
}
