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
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code NotificationAdminReadRuntimePolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class NotificationAdminReadRuntimePolicyTest {

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private NotificationQueryService notificationQueryService;

    @Test
    void denyHappensBeforeMaterializationForAdminList() {
        NotificationAdminReadServiceImpl service = new NotificationAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            notificationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.NOTIFICATION_ADMINISTRATION,
            AccessReadType.LIST,
            Instant.parse("2026-05-08T12:00:00Z"),
            null,
            null,
            "notification",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        when(queryContextResolver.resolveNotificationAdministrationContext(101L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.listAdminNotifications(101L, null))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("forbidden");

        verify(queryContextResolver).resolveNotificationAdministrationContext(101L);
        verify(accessSpecificationPolicy).canRead(context);
        verify(notificationQueryService, never()).findNotificationsByStatus(org.mockito.ArgumentMatchers.any());
        verify(notificationQueryService, never()).findNotificationsByDedupKey(org.mockito.ArgumentMatchers.any());
        verify(notificationQueryService, never()).findNotificationsBySourceEntity(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void allowPathMaterializesDtoWithoutPayloadSnapshotLeak() {
        NotificationAdminReadServiceImpl service = new NotificationAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            notificationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.NOTIFICATION_ADMINISTRATION,
            AccessReadType.DETAIL,
            Instant.parse("2026-05-08T12:05:00Z"),
            null,
            null,
            "notification",
            11L,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        Notification notification = notification(11L, 501L);

        when(queryContextResolver.resolveNotificationAdministrationDetailContext(101L, 11L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(notificationQueryService.findNotificationById(11L)).thenReturn(notification);

        NotificationAdminReadService.NotificationAdminReadModel result = service.findAdminNotificationById(101L, 11L);

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.recipientUserId()).isEqualTo(501L);
        assertThat(result.notificationType()).isEqualTo("DEADLINE_REMINDER_7D");
        assertThat(result.channelCode().value()).isEqualTo("EMAIL");
        assertThat(result.status()).isEqualTo(NotificationStatus.PENDING);
        verify(queryContextResolver).resolveNotificationAdministrationDetailContext(101L, 11L);
        verify(accessSpecificationPolicy).canRead(context);
        verify(notificationQueryService).findNotificationById(11L);
    }

    private Notification notification(Long id, Long recipientUserId) {
        Instant now = Instant.parse("2026-05-08T12:10:00Z");
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
