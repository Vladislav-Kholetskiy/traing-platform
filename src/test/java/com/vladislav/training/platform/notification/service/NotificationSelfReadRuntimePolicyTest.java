package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentDeadlineNotificationSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code NotificationSelfReadRuntimePolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class NotificationSelfReadRuntimePolicyTest {

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private NotificationQueryService notificationQueryService;
    @Mock
    private AssignmentDeadlineNotificationSyncService assignmentDeadlineNotificationSyncService;
    @Mock
    private OrganizationQueryService organizationQueryService;
    @Mock
    private UserOrganizationAssignmentService userOrganizationAssignmentService;

    @Test
    void denyHappensBeforeMaterializationForSelfDetail() {
        NotificationSelfReadServiceImpl service = new NotificationSelfReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            notificationQueryService,
            assignmentDeadlineNotificationSyncService,
            organizationQueryService,
            userOrganizationAssignmentService,
            new ObjectMapper()
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            202L,
            AccessReadArea.NOTIFICATION_RECIPIENT_SELF,
            AccessReadType.DETAIL,
            Instant.parse("2026-05-08T12:20:00Z"),
            null,
            null,
            "self_notification",
            22L,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
        when(queryContextResolver.resolveNotificationRecipientSelfDetailContext(202L, 22L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findSelfNotificationById(202L, 22L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("forbidden");

        verify(queryContextResolver).resolveNotificationRecipientSelfDetailContext(202L, 22L);
        verify(accessSpecificationPolicy).canRead(context);
        verify(notificationQueryService, never()).findNotificationByIdAndRecipientUserId(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong()
        );
    }

    @Test
    void selfDetailUsesScopedLookupByNotificationIdAndActorUserId() {
        NotificationSelfReadServiceImpl service = new NotificationSelfReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            notificationQueryService,
            assignmentDeadlineNotificationSyncService,
            organizationQueryService,
            userOrganizationAssignmentService,
            new ObjectMapper()
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            202L,
            AccessReadArea.NOTIFICATION_RECIPIENT_SELF,
            AccessReadType.DETAIL,
            Instant.parse("2026-05-08T12:25:00Z"),
            null,
            null,
            "self_notification",
            22L,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
        Notification notification = notification(22L, 202L);

        when(queryContextResolver.resolveNotificationRecipientSelfDetailContext(202L, 22L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(notificationQueryService.findNotificationByIdAndRecipientUserId(22L, 202L)).thenReturn(notification);

        NotificationSelfReadService.NotificationSelfReadModel result = service.findSelfNotificationById(202L, 22L);

        assertThat(result.id()).isEqualTo(22L);
        assertThat(result.title()).isNotBlank();
        assertThat(result.message()).isNotBlank();
        assertThat(result.channelCode().value()).isEqualTo("EMAIL");
        assertThat(result.createdAt()).isEqualTo(notification.createdAt());
        verify(notificationQueryService).findNotificationByIdAndRecipientUserId(22L, 202L);
        verify(notificationQueryService, never()).findNotificationById(22L);
    }

    @Test
    void selfListReturnsOnlyRecipientScopedRowsFromQueryService() {
        NotificationSelfReadServiceImpl service = new NotificationSelfReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            notificationQueryService,
            assignmentDeadlineNotificationSyncService,
            organizationQueryService,
            userOrganizationAssignmentService,
            new ObjectMapper()
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            202L,
            AccessReadArea.NOTIFICATION_RECIPIENT_SELF,
            AccessReadType.LIST,
            Instant.parse("2026-05-08T12:30:00Z"),
            null,
            null,
            "self_notification",
            null,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
        Notification ownNotification = notification(33L, 202L);

        when(queryContextResolver.resolveNotificationRecipientSelfContext(202L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(notificationQueryService.findNotificationsByRecipientUserId(202L)).thenReturn(List.of(ownNotification));

        List<NotificationSelfReadService.NotificationSelfReadModel> result = service.listSelfNotifications(202L);

        assertThat(result).singleElement().satisfies(model -> assertThat(model.id()).isEqualTo(33L));
        verify(notificationQueryService).findNotificationsByRecipientUserId(202L);
    }

    private Notification notification(Long id, Long recipientUserId) {
        Instant now = Instant.parse("2026-05-08T12:35:00Z");
        return new Notification(
            id,
            recipientUserId,
            "DEADLINE_REMINDER_7D",
            new NotificationChannel("EMAIL"),
            NotificationStatus.PENDING,
            "DEDUP-" + id,
            "ASSIGNMENT",
            "asg-" + id,
            now,
            null,
            null,
            0,
            null,
            null,
            "{\"payload\":\"hidden\"}",
            now,
            now
        );
    }
}
