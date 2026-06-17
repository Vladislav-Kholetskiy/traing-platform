package com.vladislav.training.platform.assignment.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAssignmentCampaignCourseJpaRepository extends JpaRepository<AssignmentCampaignCourseEntity, Long> {

    List<AssignmentCampaignCourseEntity> findAllByCampaignIdOrderByIdAsc(Long campaignId);
}
