package com.vladislav.training.platform.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет, как {@code CapabilityAdmissionRequest} создаёт данные и вспомогательные объекты.
 * Это помогает не сломать начальную сборку данных.
 */
@ExtendWith(MockitoExtension.class)
class CapabilityAdmissionRequestFactoryTest {

    private static final Long ACTOR_USER_ID = 101L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T18:00:00Z");
    private static final Path FACTORY_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/CapabilityAdmissionRequestFactory.java"
    );

    @Mock
    private InteractiveActorResolver interactiveActorResolver;

    private CapabilityAdmissionRequestFactory requestFactory;

    @BeforeEach
    void setUp() {
        UtcClock utcClock = () -> FIXED_INSTANT;
        requestFactory = new CapabilityAdmissionRequestFactory(interactiveActorResolver, utcClock);
        lenient().when(interactiveActorResolver.resolveActorUserId()).thenReturn(ACTOR_USER_ID);
    }

    @Test
    void createNotificationRuleCreateMustExistAsDedicatedInteractiveBuilder() throws Exception {
        assertThat(Arrays.stream(CapabilityAdmissionRequestFactory.class.getDeclaredMethods()).map(Method::getName))
            .contains("createNotificationRuleCreate");
        assertThat(Files.readString(FACTORY_SOURCE))
            .contains("createNotificationRuleCreate(")
            .contains("NOTIFICATION_RULE_CREATE")
            .contains("NOTIFICATION_RULE");

        CapabilityAdmissionRequest request = invokeRequest("createNotificationRuleCreate");

        assertThat(request.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(request.actorUserId()).isNotNull();
        assertThat(request.operationCode()).isEqualTo(CapabilityOperationCodes.NOTIFICATION_RULE_CREATE);
        assertThat(request.operationCode())
            .isNotEqualTo(CapabilityOperationCodes.ASSIGNMENT_CANCEL)
            .isNotEqualTo(CapabilityOperationCodes.CONTENT_PUBLISH)
            .isNotEqualTo(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START);
        assertThat(request.targetEntityType()).isEqualTo(CapabilityTargetEntityType.NOTIFICATION_RULE);
        assertThat(request.requestedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(request.payloadContext()).isSameAs(CapabilityAdmissionPayload.Empty.INSTANCE);
    }

    @Test
    void createNotificationRuleUpdateMustExistAsDedicatedInteractiveBuilder() throws Exception {
        assertThat(Arrays.stream(CapabilityAdmissionRequestFactory.class.getDeclaredMethods()).map(Method::getName))
            .contains("createNotificationRuleUpdate");
        assertThat(Files.readString(FACTORY_SOURCE))
            .contains("createNotificationRuleUpdate(")
            .contains("NOTIFICATION_RULE_UPDATE")
            .contains("NOTIFICATION_RULE");

        CapabilityAdmissionRequest request = invokeRequest("createNotificationRuleUpdate", 701L);

        assertThat(request.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(request.actorUserId()).isNotNull();
        assertThat(request.operationCode()).isEqualTo(CapabilityOperationCodes.NOTIFICATION_RULE_UPDATE);
        assertThat(request.operationCode())
            .isNotEqualTo(CapabilityOperationCodes.ASSIGNMENT_DEADLINE_EXTEND)
            .isNotEqualTo(CapabilityOperationCodes.CONTENT_ARCHIVE)
            .isNotEqualTo(CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_SUBMIT);
        assertThat(request.targetEntityType()).isEqualTo(CapabilityTargetEntityType.NOTIFICATION_RULE);
        assertThat(request.targetEntityId()).isEqualTo(701L);
        assertThat(request.requestedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(request.payloadContext()).isSameAs(CapabilityAdmissionPayload.Empty.INSTANCE);
    }

    @Test
    void createNotificationRuleEnableMustExistAsDedicatedInteractiveBuilder() throws Exception {
        assertThat(Arrays.stream(CapabilityAdmissionRequestFactory.class.getDeclaredMethods()).map(Method::getName))
            .contains("createNotificationRuleEnable");
        assertThat(Files.readString(FACTORY_SOURCE))
            .contains("createNotificationRuleEnable(")
            .contains("NOTIFICATION_RULE_ENABLE")
            .contains("NOTIFICATION_RULE");

        CapabilityAdmissionRequest request = invokeRequest("createNotificationRuleEnable", 702L);

        assertThat(request.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(request.actorUserId()).isNotNull();
        assertThat(request.operationCode()).isEqualTo(CapabilityOperationCodes.NOTIFICATION_RULE_ENABLE);
        assertThat(request.targetEntityType()).isEqualTo(CapabilityTargetEntityType.NOTIFICATION_RULE);
        assertThat(request.targetEntityId()).isEqualTo(702L);
        assertThat(request.requestedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(request.payloadContext()).isSameAs(CapabilityAdmissionPayload.Empty.INSTANCE);
    }

    @Test
    void createNotificationRuleDisableMustExistAsDedicatedInteractiveBuilder() throws Exception {
        assertThat(Arrays.stream(CapabilityAdmissionRequestFactory.class.getDeclaredMethods()).map(Method::getName))
            .contains("createNotificationRuleDisable");
        assertThat(Files.readString(FACTORY_SOURCE))
            .contains("createNotificationRuleDisable(")
            .contains("NOTIFICATION_RULE_DISABLE")
            .contains("NOTIFICATION_RULE");

        CapabilityAdmissionRequest request = invokeRequest("createNotificationRuleDisable", 703L);

        assertThat(request.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(request.actorUserId()).isNotNull();
        assertThat(request.operationCode()).isEqualTo(CapabilityOperationCodes.NOTIFICATION_RULE_DISABLE);
        assertThat(request.targetEntityType()).isEqualTo(CapabilityTargetEntityType.NOTIFICATION_RULE);
        assertThat(request.targetEntityId()).isEqualTo(703L);
        assertThat(request.requestedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(request.payloadContext()).isSameAs(CapabilityAdmissionPayload.Empty.INSTANCE);
    }

    @Test
    void createImportJobLaunchMustExistAsDedicatedInteractiveBuilder() throws Exception {
        assertThat(Arrays.stream(CapabilityAdmissionRequestFactory.class.getDeclaredMethods()).map(Method::getName))
            .contains("createImportJobLaunch");
        assertThat(Files.readString(FACTORY_SOURCE))
            .contains("createImportJobLaunch(")
            .contains("IMPORT_JOB_LAUNCH")
            .contains("IMPORT_JOB");

        CapabilityAdmissionRequest request = invokeRequest("createImportJobLaunch");

        assertThat(request.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(request.actorUserId()).isNotNull();
        assertThat(request.operationCode()).isEqualTo(CapabilityOperationCodes.IMPORT_JOB_LAUNCH);
        assertThat(request.operationCode())
            .isNotEqualTo(CapabilityOperationCodes.ASSIGNMENT_CAMPAIGN_LAUNCH)
            .isNotEqualTo(CapabilityOperationCodes.CONTENT_DRAFT_CREATE)
            .isNotEqualTo(CapabilityOperationCodes.TESTING_SELF_ATTEMPT_SUBMIT);
        assertThat(request.targetEntityType()).isEqualTo(CapabilityTargetEntityType.IMPORT_JOB);
        assertThat(request.targetEntityId()).isNull();
        assertThat(request.requestedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(request.payloadContext()).isSameAs(CapabilityAdmissionPayload.Empty.INSTANCE);
    }

    @Test
    void administrativeActorOverloadsMustFailFastOnNullActor() throws Exception {
        assertThatThrownBy(() -> invokeRequest("createNotificationRuleCreate", new Class<?>[] { Long.class }, new Object[] { null }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId");
        assertThatThrownBy(() -> invokeRequest(
            "createNotificationRuleUpdate",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { null, 701L }
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId");
        assertThatThrownBy(() -> invokeRequest(
            "createNotificationRuleEnable",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { null, 702L }
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId");
        assertThatThrownBy(() -> invokeRequest(
            "createNotificationRuleDisable",
            new Class<?>[] { Long.class, Long.class },
            new Object[] { null, 703L }
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId");
        assertThatThrownBy(() -> invokeRequest("createImportJobLaunch", new Class<?>[] { Long.class }, new Object[] { null }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId");
    }

    private CapabilityAdmissionRequest invokeRequest(String methodName, Object... args) throws Exception {
        Class<?>[] parameterTypes = Arrays.stream(args)
            .map(this::toParameterType)
            .toArray(Class<?>[]::new);
        return invokeRequest(methodName, parameterTypes, args);
    }

    private CapabilityAdmissionRequest invokeRequest(String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        Method method = CapabilityAdmissionRequestFactory.class.getDeclaredMethod(methodName, parameterTypes);
        try {
            return (CapabilityAdmissionRequest) method.invoke(requestFactory, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private Class<?> toParameterType(Object arg) {
        if (arg instanceof Long) {
            return Long.class;
        }
        throw new IllegalArgumentException("Неподдерживаемый тип аргумента в тесте фабрики административного контура: " + arg);
    }
}
