package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.repository.AssignmentCampaignCourseRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AssignmentCampaignContractHardening}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentCampaignContractHardeningTest {

    @Test
    void campaignCommandServiceNoLongerExposesMutableCrudLikeMethods() {
        Set<String> methodNames = methodNames(AssignmentCampaignCommandService.class);

        assertThat(methodNames)
            .containsExactly("launchAssignmentCampaign")
            .doesNotContain(
                "createAssignmentCampaign",
                "recordRecipientSnapshot",
                "updateAssignmentCampaign",
                "addCourseToCampaign",
                "removeCourseFromCampaign"
            );
    }

    @Test
    void campaignRepositoriesDoNotExposeExplicitHistoricalRewriteMethods() {
        Set<String> campaignRepositoryMethods = methodNames(AssignmentCampaignRepository.class);
        Set<String> campaignCourseRepositoryMethods = methodNames(AssignmentCampaignCourseRepository.class);

        assertThat(campaignRepositoryMethods)
            .containsExactlyInAnyOrder(
                "findAssignmentCampaignById",
                "findAllAssignmentCampaigns",
                "findAssignmentCampaignsBySourceType",
                "saveAssignmentCampaign"
            )
            .doesNotContain("updateAssignmentCampaign");

        assertThat(campaignCourseRepositoryMethods)
            .containsExactlyInAnyOrder(
                "findAssignmentCampaignCourseById",
                "findAssignmentCampaignCoursesByCampaignId",
                "saveAssignmentCampaignCourse"
            )
            .doesNotContain("deleteAssignmentCampaignCourse", "removeCourseFromCampaign");
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
    }
}
