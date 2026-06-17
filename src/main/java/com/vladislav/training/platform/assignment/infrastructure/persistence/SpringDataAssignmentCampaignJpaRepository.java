package com.vladislav.training.platform.assignment.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAssignmentCampaignJpaRepository extends JpaRepository<AssignmentCampaignEntity, Long> {

    List<AssignmentCampaignEntity> findAllByOrderByIdAsc();

    List<AssignmentCampaignEntity> findAllBySourceTypeOrderByIdAsc(String sourceType);
}
