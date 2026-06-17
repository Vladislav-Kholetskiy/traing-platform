package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.notification.domain.Notification;
import java.util.Objects;
import org.springframework.stereotype.Component;
/**
 * Шлюз {@code InAppNotificationDeliveryGateway}.
 */

@Component
class InAppNotificationDeliveryGateway implements NotificationDeliveryGateway {

    private static final String IN_APP_CHANNEL = "IN_APP";

    private final UtcClock utcClock;

    InAppNotificationDeliveryGateway(UtcClock utcClock) {
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @Override
    public NotificationDeliveryResult deliver(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        if (!IN_APP_CHANNEL.equals(notification.channelCode().value())) {
            return NotificationDeliveryResult.failure(
                "UNSUPPORTED_CHANNEL",
                "Only IN_APP notification delivery is configured in this prototype"
            );
        }
        return NotificationDeliveryResult.success(utcClock.now());
    }
}
