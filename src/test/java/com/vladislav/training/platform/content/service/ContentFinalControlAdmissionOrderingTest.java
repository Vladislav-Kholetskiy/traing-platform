package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code ContentFinalControlAdmissionOrdering}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ContentFinalControlAdmissionOrderingTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TestRepository testRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;
    @Mock private TestQueryService testQueryService;

    @Test
    void assignActiveFinalRejectsAdmissionBeforeMutationAndAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        doThrow(admissionDenied()).when(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_ASSIGN, 20L, 101L);

        assertThatThrownBy(() -> service.assignActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_ASSIGN, 20L, 101L);
        verifyNoInteractions(topicRepository, testRepository, auditSupport);
    }

    @Test
    void replaceActiveFinalRejectsAdmissionBeforeMutationAndAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        doThrow(admissionDenied()).when(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 20L, 101L);

        assertThatThrownBy(() -> service.replaceActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 20L, 101L);
        verifyNoInteractions(topicRepository, testRepository, auditSupport);
    }

    @Test
    void clearActiveFinalRejectsAdmissionBeforeMutationAndAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        doThrow(admissionDenied()).when(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_CLEAR, 20L);

        assertThatThrownBy(() -> service.clearActiveFinalTest(20L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_CLEAR, 20L);
        verifyNoInteractions(topicRepository, testRepository, auditSupport);
    }

    @Test
    void assignActiveFinalOrdersAdmissionBeforeMutationAndAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        com.vladislav.training.platform.content.domain.Test assigned = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.empty());
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(assigned);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_ASSIGN), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.assignActiveFinalTest(20L, 101L);

        InOrder inOrder = inOrder(support, topicRepository, testRepository, auditSupport);
        inOrder.verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_ASSIGN, 20L, 101L);
        inOrder.verify(topicRepository).findTopicById(20L);
        inOrder.verify(testRepository).lockTestById(101L);
        inOrder.verify(testRepository).findActiveFinalTestByTopicIdForUpdate(20L);
        inOrder.verify(testRepository).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(null), eq(assigned), any(AuditContext.class));
    }

    @Test
    void assignActiveFinalSameCandidateOrdersAdmissionBeforeAuditWithoutMutation() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(current);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        stubFinalAuditContext(CapabilityOperationCode.CONTENT_FINAL_ASSIGN);

        service.assignActiveFinalTest(20L, 101L);

        InOrder inOrder = inOrder(support, topicRepository, testRepository, auditSupport);
        inOrder.verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_ASSIGN, 20L, 101L);
        inOrder.verify(topicRepository).findTopicById(20L);
        inOrder.verify(testRepository).lockTestById(101L);
        inOrder.verify(testRepository).findActiveFinalTestByTopicIdForUpdate(20L);
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(current), any(AuditContext.class));
        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    @Test
    void replaceActiveFinalOrdersAdmissionBeforeMutationAndAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        com.vladislav.training.platform.content.domain.Test cleared = test(100L, 20L, false);
        com.vladislav.training.platform.content.domain.Test assigned = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubFinalAuditContext(CapabilityOperationCode.CONTENT_FINAL_REPLACE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.replaceActiveFinalTest(20L, 101L);

        InOrder inOrder = inOrder(support, testRepository, auditSupport);
        inOrder.verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 20L, 101L);
        inOrder.verify(testRepository).saveTest(eq(cleared));
        inOrder.verify(testRepository).saveTest(eq(assigned));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(assigned), any(AuditContext.class));
    }

    @Test
    void replaceActiveFinalSameCandidateOrdersAdmissionBeforeAuditWithoutMutation() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(current);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        stubFinalAuditContext(CapabilityOperationCode.CONTENT_FINAL_REPLACE);

        service.replaceActiveFinalTest(20L, 101L);

        InOrder inOrder = inOrder(support, topicRepository, testRepository, auditSupport);
        inOrder.verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 20L, 101L);
        inOrder.verify(topicRepository).findTopicById(20L);
        inOrder.verify(testRepository).lockTestById(101L);
        inOrder.verify(testRepository).findActiveFinalTestByTopicIdForUpdate(20L);
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(current), any(AuditContext.class));
        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    @Test
    void clearActiveFinalOrdersAdmissionBeforeMutationAndAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test cleared = test(100L, 20L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubFinalAuditContext(CapabilityOperationCode.CONTENT_FINAL_CLEAR);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.clearActiveFinalTest(20L);

        InOrder inOrder = inOrder(support, testRepository, auditSupport);
        inOrder.verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_CLEAR, 20L);
        inOrder.verify(testRepository).saveTest(eq(cleared));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(cleared), any(AuditContext.class));
    }

    @Test
    void clearActiveFinalEmptyStateOrdersAdmissionBeforeAuditWithoutMutation() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(
            testRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock,
            testQueryService
        );
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.empty());
        stubFinalAuditContext(CapabilityOperationCode.CONTENT_FINAL_CLEAR);

        service.clearActiveFinalTest(20L);

        InOrder inOrder = inOrder(support, topicRepository, testRepository, auditSupport);
        inOrder.verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_CLEAR, 20L);
        inOrder.verify(topicRepository).findTopicById(20L);
        inOrder.verify(testRepository).findActiveFinalTestByTopicIdForUpdate(20L);
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(null), eq(null), any(AuditContext.class));
        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    private void stubFinalAuditContext(CapabilityOperationCode operationCode) {
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(operationCode), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
    }

    private ConflictException admissionDenied() {
        return new ConflictException("admission denied");
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
