package com.vladislav.training.platform.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentDeadlineNotificationSyncService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationSelfReadServiceImpl implements NotificationSelfReadService {

    private static final AccessReadArea SUPPORTED_CONTOUR = AccessReadArea.NOTIFICATION_RECIPIENT_SELF;
    private static final String DENIAL_MESSAGE = "Self notification read is forbidden by AccessSpecificationPolicy";

    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;
    private final NotificationQueryService notificationQueryService;
    private final AssignmentDeadlineNotificationSyncService assignmentDeadlineNotificationSyncService;
    private final OrganizationQueryService organizationQueryService;
    private final UserOrganizationAssignmentService userOrganizationAssignmentService;
    private final ObjectMapper objectMapper;

    public NotificationSelfReadServiceImpl(
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver queryContextResolver,
        NotificationQueryService notificationQueryService,
        AssignmentDeadlineNotificationSyncService assignmentDeadlineNotificationSyncService,
        OrganizationQueryService organizationQueryService,
        UserOrganizationAssignmentService userOrganizationAssignmentService,
        ObjectMapper objectMapper
    ) {
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.queryContextResolver = Objects.requireNonNull(
            queryContextResolver,
            "queryContextResolver must not be null"
        );
        this.notificationQueryService = Objects.requireNonNull(
            notificationQueryService,
            "notificationQueryService must not be null"
        );
        this.assignmentDeadlineNotificationSyncService = Objects.requireNonNull(
            assignmentDeadlineNotificationSyncService,
            "assignmentDeadlineNotificationSyncService must not be null"
        );
        this.organizationQueryService = Objects.requireNonNull(
            organizationQueryService,
            "organizationQueryService must not be null"
        );
        this.userOrganizationAssignmentService = Objects.requireNonNull(
            userOrganizationAssignmentService,
            "userOrganizationAssignmentService must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public List<NotificationSelfReadModel> listSelfNotifications(Long actorUserId) {
        AccessPolicyQueryContext context = queryContextResolver.resolveNotificationRecipientSelfContext(actorUserId);
        ensureReadAllowed(context);
        assignmentDeadlineNotificationSyncService.materializeCurrentStateNotifications(Instant.now());
        NotificationResolutionContext resolutionContext = new NotificationResolutionContext();
        return notificationQueryService.findNotificationsByRecipientUserId(actorUserId).stream()
            .sorted(Comparator
                .comparing(Notification::createdAt, Comparator.reverseOrder())
                .thenComparing(Notification::id, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(notification -> toReadModel(notification, resolutionContext))
            .toList();
    }

    @Override
    public NotificationSelfReadModel findSelfNotificationById(Long actorUserId, Long notificationId) {
        AccessPolicyQueryContext context = queryContextResolver.resolveNotificationRecipientSelfDetailContext(
            actorUserId,
            notificationId
        );
        ensureReadAllowed(context);
        assignmentDeadlineNotificationSyncService.materializeCurrentStateNotifications(Instant.now());
        return toReadModel(
            notificationQueryService.findNotificationByIdAndRecipientUserId(notificationId, actorUserId),
            new NotificationResolutionContext()
        );
    }

    private void ensureReadAllowed(AccessPolicyQueryContext context) {
        if (context.contour() != SUPPORTED_CONTOUR || !accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException("NOTIFICATION_RECIPIENT_SELF_DENIED", DENIAL_MESSAGE);
        }
    }

    private NotificationSelfReadModel toReadModel(
        Notification notification,
        NotificationResolutionContext resolutionContext
    ) {
        UserFacingNotificationProjection projection = toUserFacingProjection(notification);
        NotificationSupplement supplement = buildSupplement(notification, resolutionContext);
        return new NotificationSelfReadModel(
            notification.id(),
            projection.title(),
            projection.message(),
            notification.channelCode(),
            notification.createdAt(),
            notification.readAt(),
            notification.readAt() != null,
            notification.notificationType(),
            supplement.companyName(),
            supplement.assignmentRecipients()
        );
    }

    private NotificationSupplement buildSupplement(
        Notification notification,
        NotificationResolutionContext resolutionContext
    ) {
        return switch (notification.notificationType()) {
            case "assignment_assigned_manager_notice" ->
                buildManagerAssignedSupplement(notification, resolutionContext);
            case "assignment_campaign_manager_notice" ->
                buildCampaignManagerSupplement(notification, resolutionContext);
            case "assignment_cancelled_manager_notice",
                "assignment_deadline_extended_manager_notice",
                "assignment_replaced_manager_notice" ->
                buildManagerAssignedSupplement(notification, resolutionContext);
            default -> NotificationSupplement.empty();
        };
    }

    private UserFacingNotificationProjection toUserFacingProjection(Notification notification) {
        return switch (notification.notificationType()) {
            case "assignment_campaign_assigned" -> assignmentLaunchProjection(notification);
            case "assignment_campaign_manager_notice" -> managerAssignmentLaunchProjection(notification);
            case "assignment_assigned_manager_notice" -> managerAssignmentStateProjection(
                notification,
                "Назначение сотруднику",
                "Сотруднику вашего подразделения назначено обучение.",
                (fullName, courseName) -> "Сотруднику " + fullName + " назначено обучение \"" + courseName + "\"."
            );
            case "DEADLINE_REMINDER_7D" -> deadlineReminderProjection(notification);
            case "assignment_deadline_reminder_7d_manager_notice" -> managerDeadlineReminderProjection(notification);
            case "assignment_overdue" -> overdueProjection(notification);
            case "assignment_overdue_manager_notice" -> managerOverdueProjection(notification);
            case "assignment_cancelled" -> assignmentStateProjection(
                notification,
                "Назначение отменено",
                "Ваше назначение на обучение отменено.",
                courseName -> "Назначение на обучение \"" + courseName + "\" отменено."
            );
            case "assignment_cancelled_manager_notice" -> managerAssignmentStateProjection(
                notification,
                "Назначение сотрудника отменено",
                "Назначение сотрудника вашего подразделения отменено.",
                (fullName, courseName) -> "Назначение сотрудника " + fullName + " на обучение \"" + courseName + "\" отменено."
            );
            case "assignment_deadline_extended" -> assignmentStateProjection(
                notification,
                "Срок обучения продлён",
                "Срок по вашему назначению на обучение продлён.",
                courseName -> "Срок по обучению \"" + courseName + "\" продлён."
            );
            case "assignment_deadline_extended_manager_notice" -> managerAssignmentStateProjection(
                notification,
                "Срок обучения сотрудника продлён",
                "Срок назначения сотрудника вашего подразделения продлён.",
                (fullName, courseName) -> "Срок обучения \"" + courseName + "\" для сотрудника " + fullName + " продлён."
            );
            case "assignment_replaced" -> assignmentStateProjection(
                notification,
                "Назначение заменено",
                "Ваше назначение заменено новым циклом обучения.",
                courseName -> "Назначение на обучение \"" + courseName + "\" заменено новым циклом."
            );
            case "assignment_replaced_manager_notice" -> managerAssignmentStateProjection(
                notification,
                "Назначение сотрудника заменено",
                "Назначение сотрудника вашего подразделения заменено новым циклом обучения.",
                (fullName, courseName) -> "Назначение сотрудника " + fullName + " на обучение \"" + courseName + "\" заменено новым циклом."
            );
            default -> new UserFacingNotificationProjection(
                "Рабочее уведомление",
                "В вашей рабочей ленте появилось новое уведомление."
            );
        };
    }

    private UserFacingNotificationProjection assignmentLaunchProjection(Notification notification) {
        String defaultTitle = "Новое назначение на обучение";
        String defaultMessage = "Вам назначено новое обучение.";
        JsonNode root = parsePayload(notification.payloadSnapshot());
        if (root == null) {
            return new UserFacingNotificationProjection(defaultTitle, defaultMessage);
        }

        String subjectTitle = firstNonBlank(
            textValue(root.path("campaign"), "sourceNameSnapshot"),
            textValue(root.path("campaign"), "campaignName")
        );
        if (subjectTitle == null) {
            return new UserFacingNotificationProjection(defaultTitle, defaultMessage);
        }
        return new UserFacingNotificationProjection(
            defaultTitle,
            "Вам назначено обучение \"" + subjectTitle + "\"."
        );
    }

    private UserFacingNotificationProjection managerAssignmentLaunchProjection(Notification notification) {
        String defaultTitle = "Назначение сотруднику";
        String defaultMessage = "В вашем подразделении появилось новое назначение на обучение.";
        JsonNode root = parsePayload(notification.payloadSnapshot());
        if (root == null) {
            return new UserFacingNotificationProjection(defaultTitle, defaultMessage);
        }

        String subjectTitle = firstNonBlank(
            textValue(root.path("campaign"), "sourceNameSnapshot"),
            textValue(root.path("campaign"), "campaignName")
        );
        JsonNode affectedRecipients = root.path("affectedRecipients");
        if (affectedRecipients.isArray() && affectedRecipients.size() == 1) {
            String fullName = textValue(affectedRecipients.get(0), "fullName");
            if (fullName != null && subjectTitle != null) {
                return new UserFacingNotificationProjection(
                    defaultTitle,
                    "Сотруднику " + fullName + " назначено обучение \"" + subjectTitle + "\"."
                );
            }
        }

        Integer recipientCount = integerValue(root, "affectedRecipientCount");
        if (recipientCount != null && subjectTitle != null) {
            return new UserFacingNotificationProjection(
                defaultTitle,
                "Назначено обучение \"" + subjectTitle + "\" для " + recipientCount + " сотрудников."
            );
        }
        return new UserFacingNotificationProjection(defaultTitle, defaultMessage);
    }

    private NotificationSupplement buildManagerAssignedSupplement(
        Notification notification,
        NotificationResolutionContext resolutionContext
    ) {
        JsonNode root = parsePayload(notification.payloadSnapshot());
        if (root == null) {
            return NotificationSupplement.empty();
        }

        JsonNode subjectUser = root.path("subjectUser");
        Long subjectUserId = longValue(subjectUser, "userId");
        String fullName = textValue(subjectUser, "fullName");
        String courseName = textValue(root.path("assignment"), "courseName");
        String companyName = resolveCompanyNameForUser(
            subjectUserId,
            notification.createdAt(),
            resolutionContext
        );
        String organizationalUnitName = resolvePrimaryUnitNameForUser(
            subjectUserId,
            notification.createdAt(),
            resolutionContext
        );
        if (fullName == null && courseName == null && companyName == null && organizationalUnitName == null) {
            return NotificationSupplement.empty();
        }
        return new NotificationSupplement(
            companyName,
            List.of(
                new NotificationAssignmentRecipientReadModel(
                    subjectUserId,
                    fullName,
                    courseName,
                    companyName,
                    organizationalUnitName
                )
            )
        );
    }

    private NotificationSupplement buildCampaignManagerSupplement(
        Notification notification,
        NotificationResolutionContext resolutionContext
    ) {
        JsonNode root = parsePayload(notification.payloadSnapshot());
        if (root == null) {
            return NotificationSupplement.empty();
        }

        String courseName = firstNonBlank(
            textValue(root.path("campaign"), "sourceNameSnapshot"),
            textValue(root.path("campaign"), "campaignName")
        );
        JsonNode affectedRecipients = root.path("affectedRecipients");
        if (!affectedRecipients.isArray() || affectedRecipients.isEmpty()) {
            return NotificationSupplement.empty();
        }

        List<NotificationAssignmentRecipientReadModel> assignmentRecipients = new ArrayList<>();
        String companyName = null;
        for (JsonNode recipientNode : affectedRecipients) {
            Long userId = longValue(recipientNode, "userId");
            Long organizationalUnitId = longValue(recipientNode, "organizationalUnitId");
            String recipientCompanyName = resolveCompanyNameForUnit(organizationalUnitId, resolutionContext);
            String organizationalUnitName = resolveUnitName(organizationalUnitId, resolutionContext);
            if (companyName == null && recipientCompanyName != null) {
                companyName = recipientCompanyName;
            }
            assignmentRecipients.add(
                new NotificationAssignmentRecipientReadModel(
                    userId,
                    textValue(recipientNode, "fullName"),
                    courseName,
                    recipientCompanyName,
                    organizationalUnitName
                )
            );
        }
        return new NotificationSupplement(companyName, List.copyOf(assignmentRecipients));
    }

    private UserFacingNotificationProjection deadlineReminderProjection(Notification notification) {
        return assignmentStateProjection(
            notification,
            "Срок подходит",
            "Через неделю истекает срок по вашему обучению.",
            courseName -> "Через неделю истекает срок по обучению \"" + courseName + "\"."
        );
    }

    private UserFacingNotificationProjection managerDeadlineReminderProjection(Notification notification) {
        return managerAssignmentStateProjection(
            notification,
            "Скоро дедлайн у сотрудника",
            "У сотрудника вашего подразделения через неделю истекает срок по обучению.",
            (fullName, courseName) ->
                "У сотрудника " + fullName + " через неделю истекает срок по обучению \"" + courseName + "\"."
        );
    }

    private UserFacingNotificationProjection overdueProjection(Notification notification) {
        return assignmentStateProjection(
            notification,
            "Обучение просрочено",
            "Срок по вашему обучению истёк.",
            courseName -> "Срок по обучению \"" + courseName + "\" истёк."
        );
    }

    private UserFacingNotificationProjection managerOverdueProjection(Notification notification) {
        return managerAssignmentStateProjection(
            notification,
            "Просрочка у сотрудника",
            "Сотрудник вашего подразделения просрочил обучение.",
            (fullName, courseName) -> "Сотрудник " + fullName + " просрочил обучение \"" + courseName + "\"."
        );
    }

    private UserFacingNotificationProjection assignmentStateProjection(
        Notification notification,
        String title,
        String fallbackMessage,
        CourseMessageBuilder messageBuilder
    ) {
        JsonNode root = parsePayload(notification.payloadSnapshot());
        String courseName = textValue(root == null ? null : root.path("assignment"), "courseName");
        if (courseName == null) {
            return new UserFacingNotificationProjection(title, fallbackMessage);
        }
        return new UserFacingNotificationProjection(title, messageBuilder.build(courseName));
    }

    private UserFacingNotificationProjection managerAssignmentStateProjection(
        Notification notification,
        String title,
        String fallbackMessage,
        ManagerMessageBuilder messageBuilder
    ) {
        JsonNode root = parsePayload(notification.payloadSnapshot());
        String courseName = textValue(root == null ? null : root.path("assignment"), "courseName");
        String fullName = textValue(root == null ? null : root.path("subjectUser"), "fullName");
        if (courseName == null || fullName == null) {
            return new UserFacingNotificationProjection(title, fallbackMessage);
        }
        return new UserFacingNotificationProjection(title, messageBuilder.build(fullName, courseName));
    }

    private JsonNode parsePayload(String payloadSnapshot) {
        if (payloadSnapshot == null || payloadSnapshot.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payloadSnapshot);
        } catch (Exception exception) {
            return null;
        }
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Integer integerValue(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull() || !child.canConvertToInt()) {
            return null;
        }
        return child.intValue();
    }

    private Long longValue(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull() || !child.canConvertToLong()) {
            return null;
        }
        return child.longValue();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String resolveCompanyNameForUser(
        Long userId,
        Instant activeAt,
        NotificationResolutionContext resolutionContext
    ) {
        if (userId == null) {
            return null;
        }
        if (resolutionContext.companyByUserId.containsKey(userId)) {
            return resolutionContext.companyByUserId.get(userId);
        }

        Instant effectiveAt = activeAt == null ? Instant.now() : activeAt;
        String companyName = userOrganizationAssignmentService.findActiveOrganizationAssignmentsByUserId(userId, effectiveAt).stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .map(UserOrganizationAssignment::organizationalUnitId)
            .findFirst()
            .map(unitId -> resolveCompanyNameForUnit(unitId, resolutionContext))
            .orElse(null);
        resolutionContext.companyByUserId.put(userId, companyName);
        return companyName;
    }

    private String resolvePrimaryUnitNameForUser(
        Long userId,
        Instant activeAt,
        NotificationResolutionContext resolutionContext
    ) {
        if (userId == null) {
            return null;
        }
        if (resolutionContext.unitNameByUserId.containsKey(userId)) {
            return resolutionContext.unitNameByUserId.get(userId);
        }

        Instant effectiveAt = activeAt == null ? Instant.now() : activeAt;
        String unitName = userOrganizationAssignmentService.findActiveOrganizationAssignmentsByUserId(userId, effectiveAt).stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .map(UserOrganizationAssignment::organizationalUnitId)
            .findFirst()
            .map(unitId -> resolveUnitName(unitId, resolutionContext))
            .orElse(null);
        resolutionContext.unitNameByUserId.put(userId, unitName);
        return unitName;
    }

    private String resolveCompanyNameForUnit(
        Long organizationalUnitId,
        NotificationResolutionContext resolutionContext
    ) {
        if (organizationalUnitId == null) {
            return null;
        }
        if (resolutionContext.companyByUnitId.containsKey(organizationalUnitId)) {
            return resolutionContext.companyByUnitId.get(organizationalUnitId);
        }

        OrganizationalUnit cursor = resolutionContext.unitById.computeIfAbsent(
            organizationalUnitId,
            organizationQueryService::findOrganizationalUnitById
        );
        while (cursor.parentId() != null) {
            Long parentId = cursor.parentId();
            cursor = resolutionContext.unitById.computeIfAbsent(parentId, organizationQueryService::findOrganizationalUnitById);
        }

        String companyName = cursor.name();
        resolutionContext.companyByUnitId.put(organizationalUnitId, companyName);
        return companyName;
    }

    private String resolveUnitName(
        Long organizationalUnitId,
        NotificationResolutionContext resolutionContext
    ) {
        if (organizationalUnitId == null) {
            return null;
        }
        if (resolutionContext.unitNameByUnitId.containsKey(organizationalUnitId)) {
            return resolutionContext.unitNameByUnitId.get(organizationalUnitId);
        }

        OrganizationalUnit unit = resolutionContext.unitById.computeIfAbsent(
            organizationalUnitId,
            organizationQueryService::findOrganizationalUnitById
        );
        String unitName = unit.name();
        resolutionContext.unitNameByUnitId.put(organizationalUnitId, unitName);
        return unitName;
    }

    private record UserFacingNotificationProjection(String title, String message) {
    }

    private record NotificationSupplement(
        String companyName,
        List<NotificationAssignmentRecipientReadModel> assignmentRecipients
    ) {

        private static NotificationSupplement empty() {
            return new NotificationSupplement(null, List.of());
        }
    }

    private static final class NotificationResolutionContext {

        private final Map<Long, String> companyByUserId = new LinkedHashMap<>();
        private final Map<Long, String> unitNameByUserId = new LinkedHashMap<>();
        private final Map<Long, String> companyByUnitId = new LinkedHashMap<>();
        private final Map<Long, String> unitNameByUnitId = new LinkedHashMap<>();
        private final Map<Long, OrganizationalUnit> unitById = new LinkedHashMap<>();
    }

    @FunctionalInterface
    private interface CourseMessageBuilder {

        String build(String courseName);
    }

    @FunctionalInterface
    private interface ManagerMessageBuilder {

        String build(String fullName, String courseName);
    }
}
