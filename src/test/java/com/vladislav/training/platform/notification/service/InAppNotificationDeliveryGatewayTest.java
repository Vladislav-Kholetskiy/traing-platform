package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code InAppNotificationDeliveryGateway}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class InAppNotificationDeliveryGatewayTest {

    private static final Instant NOW = Instant.parse("2026-05-24T07:00:00Z");

    @Mock
    private UtcClock utcClock;

    @Test
    void inAppNotificationDeliverySucceedsWithClockTimestamp() {
        when(utcClock.now()).thenReturn(NOW);
        InAppNotificationDeliveryGateway gateway = new InAppNotificationDeliveryGateway(utcClock);

        NotificationDeliveryResult result = gateway.deliver(notification("IN_APP"));

        assertThat(result.successful()).isTrue();
        assertThat(result.sentAt()).isEqualTo(NOW);
        assertThat(result.errorCode()).isNull();
    }

    @Test
    void unsupportedChannelsFailExplicitly() {
        InAppNotificationDeliveryGateway gateway = new InAppNotificationDeliveryGateway(utcClock);

        NotificationDeliveryResult result = gateway.deliver(notification("EMAIL"));

        assertThat(result.successful()).isFalse();
        assertThat(result.errorCode()).isEqualTo("UNSUPPORTED_CHANNEL");
    }

    private Notification notification(String channelCode) {
        return new Notification(
            1L,
            10L,
            "assignment_campaign_assigned",
            new NotificationChannel(channelCode),
            NotificationStatus.PENDING,
            "dedup",
            "assignment_campaign",
            "1",
            NOW,
            null,
            null,
            0,
            null,
            null,
            "{}",
            NOW,
            NOW
        );
    }
}
