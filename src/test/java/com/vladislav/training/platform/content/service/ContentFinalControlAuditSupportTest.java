package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code ContentFinalControlAudit}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class ContentFinalControlAuditSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TestRepository testRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;
    @Mock private TestQueryService testQueryService;

    @Test
    void assignActiveFinalWritesSynchronousAuditAfterMutation() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        com.vladislav.training.platform.content.domain.Test assigned = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.empty());
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_ASSIGN);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.assignActiveFinalTest(20L, 101L);

        InOrder inOrder = inOrder(testRepository, auditSupport);
        inOrder.verify(testRepository).saveTest(eq(assigned));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(null), eq(assigned), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_ASSIGN, null, 101L, "ASSIGN");
    }

    @Test
    void replaceActiveFinalWritesSynchronousAuditAfterMutation() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test cleared = test(100L, 20L, false);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        com.vladislav.training.platform.content.domain.Test assigned = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_REPLACE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.replaceActiveFinalTest(20L, 101L);

        InOrder inOrder = inOrder(testRepository, auditSupport);
        inOrder.verify(testRepository).saveTest(eq(cleared));
        inOrder.verify(testRepository).saveTest(eq(assigned));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(assigned), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 100L, 101L, "REPLACE");
    }

    @Test
    void clearActiveFinalWritesSynchronousAuditAfterMutation() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test cleared = test(100L, 20L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_CLEAR);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.clearActiveFinalTest(20L);

        InOrder inOrder = inOrder(testRepository, auditSupport);
        inOrder.verify(testRepository).saveTest(eq(cleared));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(cleared), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_CLEAR, 100L, null, "CLEAR");
    }

    @Test
    void assignSameActiveFinalWritesSynchronousAuditForSuccessfulNoOp() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        com.vladislav.training.platform.content.domain.Test current = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(current);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_ASSIGN);

        service.assignActiveFinalTest(20L, 101L);

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(current), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_ASSIGN, 101L, 101L, "ASSIGN");
    }

    @Test
    void replaceSameActiveFinalWritesSynchronousAuditForSuccessfulNoOp() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        com.vladislav.training.platform.content.domain.Test current = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(current);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_REPLACE);

        service.replaceActiveFinalTest(20L, 101L);

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(current), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 101L, 101L, "REPLACE");
    }

    @Test
    void clearEmptyStateWritesSynchronousAuditForSuccessfulNoOp() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.empty());
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_CLEAR);

        service.clearActiveFinalTest(20L);

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(null), eq(null), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_CLEAR, null, null, "CLEAR");
    }

    @Test
    void assignActiveFinalDoesNotCompleteWhenAuditWriteFails() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        com.vladislav.training.platform.content.domain.Test assigned = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.empty());
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_ASSIGN);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        doThrow(new IllegalStateException("audit failed"))
            .when(auditSupport)
            .recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(null), eq(assigned), any(AuditContext.class));

        assertThatThrownBy(() -> service.assignActiveFinalTest(20L, 101L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit failed");
    }

    @Test
    void replaceActiveFinalDoesNotCompleteWhenAuditWriteFails() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        com.vladislav.training.platform.content.domain.Test assigned = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_REPLACE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        doThrow(new IllegalStateException("audit failed"))
            .when(auditSupport)
            .recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(assigned), any(AuditContext.class));

        assertThatThrownBy(() -> service.replaceActiveFinalTest(20L, 101L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit failed");
    }

    @Test
    void clearActiveFinalDoesNotCompleteWhenAuditWriteFails() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = topic(20L, ContentStatus.PUBLISHED);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.empty());
        stubAuditContext(CapabilityOperationCode.CONTENT_FINAL_CLEAR);
        doThrow(new IllegalStateException("audit failed"))
            .when(auditSupport)
            .recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(null), eq(null), any(AuditContext.class));

        assertThatThrownBy(() -> service.clearActiveFinalTest(20L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit failed");
    }

    @Test
    void assignActiveFinalRejectedPathDoesNotWriteSuccessAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, ContentStatus.PUBLISHED));
        when(testRepository.lockTestById(101L)).thenReturn(new com.vladislav.training.platform.content.domain.Test(
            101L,
            20L,
            "Training",
            null,
            TestType.TRAINING,
            ContentStatus.PUBLISHED,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        doCallRealMethod().when(support).validateActiveFinalEligibility(any(com.vladislav.training.platform.content.domain.Test.class));

        assertThatThrownBy(() -> service.assignActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("CONTROL and PUBLISHED");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void replaceActiveFinalRejectedPathDoesNotWriteSuccessAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, ContentStatus.DRAFT));
        doCallRealMethod().when(support).requirePublished(ContentStatus.DRAFT, "Topic");

        assertThatThrownBy(() -> service.replaceActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic must be PUBLISHED");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void clearActiveFinalRejectedPathDoesNotWriteSuccessAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, ContentStatus.DRAFT));
        doCallRealMethod().when(support).requirePublished(ContentStatus.DRAFT, "Topic");

        assertThatThrownBy(() -> service.clearActiveFinalTest(20L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic must be PUBLISHED");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    private void stubAuditContext(CapabilityOperationCode operationCode) {
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(operationCode), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
    }

    private void assertAuditDetails(
        CapabilityOperationCode operationCode,
        Long previousActiveTestId,
        Long newActiveTestId,
        String mutationKind
    ) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditSupport).buildAuditContext(eq("content"), eq(operationCode), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue()).containsEntry("topicId", 20L);
        assertThat(detailsCaptor.getValue()).containsEntry("previousActiveTestId", previousActiveTestId);
        assertThat(detailsCaptor.getValue()).containsEntry("newActiveTestId", newActiveTestId);
        assertThat(detailsCaptor.getValue()).containsEntry("mutationKind", mutationKind);
    }

    private Topic topic(Long topicId, ContentStatus status) {
        return new Topic(topicId, 30L, "Topic", null, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private com.vladislav.training.platform.content.domain.Test test(Long testId, Long topicId, boolean activeFinal) {
        return new com.vladislav.training.platform.content.domain.Test(
            testId,
            topicId,
            "Test",
            null,
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            BigDecimal.valueOf(80),
            "DEFAULT",
            activeFinal,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
