package com.vladislav.training.platform.notification.service;

import com.vladislav.training.platform.notification.domain.Notification;
/**
 * Шлюз {@code NotificationDeliveryGateway}.
 */
public interface NotificationDeliveryGateway {

    NotificationDeliveryResult deliver(Notification notification);
}
