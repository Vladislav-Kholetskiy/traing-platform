package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.access.repository.TemporaryManagementDelegationRepository;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.service.NotificationCommandService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentCampaignLaunchNotificationServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentCampaignLaunchNotificationServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-17T17:00:00Z");

    @Mock private NotificationCommandService notificationCommandService;
    @Mock private OrganizationQueryService organizationQueryService;
    @Mock private UserOrganizationAssignmentService userOrganizationAssignmentService;
    @Mock private ManagementRelationRepository managementRelationRepository;
    @Mock private TemporaryManagementDelegationRepository temporaryManagementDelegationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AssignmentCampaignLaunchNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentCampaignLaunchNotificationServiceImpl(
            notificationCommandService,
            new AssignmentNotificationAudienceResolver(
                organizationQueryService,
                userOrganizationAssignmentService,
                managementRelationRepository,
                temporaryManagementDelegationRepository
            ),
            objectMapper
        );
        when(notificationCommandService.createNotification(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Notification.class));
    }

    @Test
    void createsOperatorNotificationsAndAggregatedManagerNotificationsUpToProductionInclusive() throws Exception {
        when(organizationQueryService.findOrganizationalUnitById(9L)).thenReturn(unit(9L, 8L, 6L, "/department/npz/production/komt/mtbeo"));
        when(organizationQueryService.findOrganizationalUnitById(8L)).thenReturn(unit(8L, 4L, 5L, "/department/npz/production/komt"));
        when(organizationQueryService.findOrganizationalUnitById(4L)).thenReturn(unit(4L, 2L, 4L, "/department/npz/production"));
        when(organizationQueryService.findOrganizationalUnitTypeById(6L)).thenReturn(type(6L, "process_unit"));
        when(organizationQueryService.findOrganizationalUnitTypeById(5L)).thenReturn(type(5L, "production_complex"));
        when(organizationQueryService.findOrganizationalUnitTypeById(4L)).thenReturn(type(4L, "production_block"));

        when(managementRelationRepository.findManagementRelationsByOrganizationalUnitId(9L)).thenReturn(List.of(
            relation(8L, 9L),
            inactiveRelation(88L, 9L)
        ));
        when(managementRelationRepository.findManagementRelationsByOrganizationalUnitId(8L)).thenReturn(List.of(
            relation(5L, 8L)
        ));
        when(managementRelationRepository.findManagementRelationsByOrganizationalUnitId(4L)).thenReturn(List.of(
            relation(3L, 4L)
        ));
        when(temporaryManagementDelegationRepository.findTemporaryManagementDelegationsByOrganizationalUnitId(9L))
            .thenReturn(List.of(delegation(108L, 9L)));
        when(temporaryManagementDelegationRepository.findTemporaryManagementDelegationsByOrganizationalUnitId(8L))
            .thenReturn(List.of());
        when(temporaryManagementDelegationRepository.findTemporaryManagementDelegationsByOrganizationalUnitId(4L))
            .thenReturn(List.of());

        AssignmentCampaign campaign = new AssignmentCampaign(
            1001L,
            "Кампания по охране труда",
            "Плановое назначение",
            "MANUAL",
            "campaign-source-42",
            "expert-launch",
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        LaunchAssignmentCampaignCommand command = new LaunchAssignmentCampaignCommand(
            "Кампания по охране труда",
            "Плановое назначение",
            "MANUAL",
            "campaign-source-42",
            "expert-launch",
            List.of(501L, 502L),
            new LaunchAssignmentCampaignCommand.Targeting("ORG_UNIT", "/department/npz/production/komt"),
            new LaunchAssignmentCampaignCommand.DeadlinePolicy(FIXED_INSTANT.plusSeconds(86400))
        );

        service.createLaunchNotifications(
            campaign,
            command,
            List.of(
                recipientSnapshot(29L, "NPZ-OP-MTBEO-001", "Александр Тарасов"),
                recipientSnapshot(30L, "NPZ-OP-MTBEO-002", "Дмитрий Семенов")
            ),
            FIXED_INSTANT
        );

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationCommandService, times(6)).createNotification(notificationCaptor.capture());
        List<Notification> notifications = notificationCaptor.getAllValues();

        assertThat(notifications).extracting(Notification::notificationType)
            .containsExactly(
                "assignment_campaign_assigned",
                "assignment_campaign_assigned",
                "assignment_campaign_manager_notice",
                "assignment_campaign_manager_notice",
                "assignment_campaign_manager_notice",
                "assignment_campaign_manager_notice"
            );
        assertThat(notifications).extracting(Notification::recipientUserId)
            .containsExactly(29L, 30L, 8L, 108L, 5L, 3L);
        assertThat(notifications).extracting(Notification::sourceEntityType)
            .containsOnly("assignment_campaign");
        assertThat(notifications).extracting(Notification::sourceEntityId)
            .containsOnly("1001");

        JsonNode operatorPayload = objectMapper.readTree(notifications.get(0).payloadSnapshot());
        assertThat(operatorPayload.path("campaign").path("campaignId").asLong()).isEqualTo(1001L);
        assertThat(operatorPayload.path("recipient").path("employeeNumber").asText()).isEqualTo("NPZ-OP-MTBEO-001");

        JsonNode managerPayload = objectMapper.readTree(notifications.get(2).payloadSnapshot());
        assertThat(managerPayload.path("affectedRecipientCount").asInt()).isEqualTo(2);
        assertThat(managerPayload.path("affectedRecipients")).hasSize(2);
        assertThat(managerPayload.path("affectedRecipients").get(0).path("employeeNumber").asText())
            .isEqualTo("NPZ-OP-MTBEO-001");
    }

    private AssignmentCampaignRecipientSnapshot recipientSnapshot(Long userId, String employeeNumber, String fullName) {
        return new AssignmentCampaignRecipientSnapshot(
            null,
            1001L,
            userId,
            9L,
            "/department/npz/production/komt/mtbeo",
            "ORG_UNIT_TARGETING",
            employeeNumber,
            fullName,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private OrganizationalUnit unit(Long id, Long parentId, Long typeId, String path) {
        return new OrganizationalUnit(
            id,
            parentId,
            typeId,
            "unit-" + id,
            OrganizationalUnitStatus.ACTIVE,
            path,
            0,
            "ext-" + id,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private OrganizationalUnitType type(Long id, String code) {
        return new OrganizationalUnitType(
            id,
            code,
            code,
            null,
            OrganizationalNodeKind.LINEAR,
            true,
            true,
            true,
            true,
            true,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private ManagementRelation relation(Long userId, Long organizationalUnitId) {
        return new ManagementRelation(
            userId + organizationalUnitId,
            userId,
            organizationalUnitId,
            1L,
            FIXED_INSTANT.minusSeconds(3600),
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(3600)
        );
    }

    private ManagementRelation inactiveRelation(Long userId, Long organizationalUnitId) {
        return new ManagementRelation(
            userId + organizationalUnitId,
            userId,
            organizationalUnitId,
            1L,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(7200)
        );
    }

    private TemporaryManagementDelegation delegation(Long userId, Long organizationalUnitId) {
        return new TemporaryManagementDelegation(
            userId + organizationalUnitId,
            userId,
            organizationalUnitId,
            1L,
            FIXED_INSTANT.minusSeconds(1800),
            null,
            FIXED_INSTANT.minusSeconds(1800),
            FIXED_INSTANT.minusSeconds(1800)
        );
    }
}
