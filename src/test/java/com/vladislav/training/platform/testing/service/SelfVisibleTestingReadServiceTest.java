package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.TestType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code SelfVisibleTestingRead}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfVisibleTestingReadServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T09:00:00Z");

    @Mock
    private SelfVisibleTestingProjectionReader selfVisibleTestingProjectionReader;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    private SelfVisibleTestingReadService service;

    @BeforeEach
    void setUp() {
        service = new SelfVisibleTestingReadService(
            selfVisibleTestingProjectionReader,
            accessSpecificationPolicy,
            contextResolver
        );
    }

    @Test
    void allowedCatalogPathResolvesSelfVisiblePolicyContextBeforeCallingProjectionReader() {
        AccessPolicyQueryContext context = selfVisibleContext(AccessReadType.LIST);
        List<SelfVisibleTestCatalogEntryReadModel> projection = List.of(
            new SelfVisibleTestCatalogEntryReadModel(41L, 201L, "Course", 301L, "Topic", "Self test", "Description", TestType.TRAINING)
        );
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.LIST,
            "self_visible_testing"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(selfVisibleTestingProjectionReader.findSelfVisibleTests()).thenReturn(projection);

        List<SelfVisibleTestCatalogEntryReadModel> returned = service.findSelfVisibleTests();

        assertThat(returned).isEqualTo(projection);
        var ordered = inOrder(contextResolver, accessSpecificationPolicy, selfVisibleTestingProjectionReader);
        ordered.verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.LIST,
            "self_visible_testing"
        );
        ordered.verify(accessSpecificationPolicy).canRead(context);
        ordered.verify(selfVisibleTestingProjectionReader).findSelfVisibleTests();
    }

    @Test
    void allowedDetailPathResolvesSelfVisiblePolicyContextBeforeCallingProjectionReader() {
        AccessPolicyQueryContext context = selfVisibleContext(AccessReadType.DETAIL);
        SelfVisibleTestReadModel projection = readModel();
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.DETAIL,
            "self_visible_testing"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(selfVisibleTestingProjectionReader.findSelfVisibleTestById(41L)).thenReturn(projection);

        SelfVisibleTestReadModel returned = service.findSelfVisibleTestById(41L);

        assertThat(returned).isEqualTo(projection);
        var ordered = inOrder(contextResolver, accessSpecificationPolicy, selfVisibleTestingProjectionReader);
        ordered.verify(contextResolver).resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.DETAIL,
            "self_visible_testing"
        );
        ordered.verify(accessSpecificationPolicy).canRead(context);
        ordered.verify(selfVisibleTestingProjectionReader).findSelfVisibleTestById(41L);
    }

    @Test
    void policyDeniedCatalogPathFailsClosedBeforeProjectionReaderRuns() {
        AccessPolicyQueryContext context = selfVisibleContext(AccessReadType.LIST);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.LIST,
            "self_visible_testing"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findSelfVisibleTests())
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Actor is not authorized to read self-visible testing data");

        verify(selfVisibleTestingProjectionReader, never()).findSelfVisibleTests();
    }

    @Test
    void policyDeniedDetailPathFailsClosedBeforeProjectionReaderRuns() {
        AccessPolicyQueryContext context = selfVisibleContext(AccessReadType.DETAIL);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.DETAIL,
            "self_visible_testing"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findSelfVisibleTestById(41L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Actor is not authorized to read self-visible testing data");

        verify(selfVisibleTestingProjectionReader, never()).findSelfVisibleTestById(41L);
    }

    @Test
    void selfVisibleTestingReadServiceWillNotDescribeOrInvokeContentAuthoringQueryServices() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfVisibleTestingReadService.java"
        ));

        assertThat(source)
            .contains("SelfVisibleTestingProjectionReader")
            .contains("AccessReadArea.SELF_VISIBLE_TESTING")
            .doesNotContain("CourseQueryService")
            .doesNotContain("TopicQueryService")
            .doesNotContain("TestQueryService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("CONTENT_AUTHORING");
    }

    @Test
    void policyDeniedSelfVisibleTestingDoesNotCallProjectionCollaborator() {
        AccessPolicyQueryContext context = selfVisibleContext(AccessReadType.LIST);
        when(contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.LIST,
            "self_visible_testing"
        )).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findSelfVisibleTests())
            .isInstanceOf(PolicyViolationException.class);

        verifyNoInteractions(selfVisibleTestingProjectionReader);
    }

    private AccessPolicyQueryContext selfVisibleContext(AccessReadType readType) {
        return new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_VISIBLE_TESTING,
            readType,
            FIXED_INSTANT,
            null,
            null,
            "self_visible_testing",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private SelfVisibleTestReadModel readModel() {
        return new SelfVisibleTestReadModel(
            41L,
            301L,
            "Self test",
            "Description",
            TestType.TRAINING,
            List.of(
                new SelfVisibleTestReadModel.SelfVisibleQuestionReadModel(
                    501L,
                    "Question",
                    com.vladislav.training.platform.content.domain.QuestionType.SINGLE_CHOICE,
                    0,
                    new BigDecimal("2.00"),
                    List.of(
                        new SelfVisibleTestReadModel.SelfVisibleAnswerOptionReadModel(
                            9001L,
                            "Option A",
                            com.vladislav.training.platform.content.domain.AnswerOptionRole.CHOICE_OPTION,
                            0
                        )
                    )
                )
            )
        );
    }
}
