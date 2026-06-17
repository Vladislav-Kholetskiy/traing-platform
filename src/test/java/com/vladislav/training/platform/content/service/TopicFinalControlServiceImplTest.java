package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
/**
 * Проверяет поведение {@code TopicFinalControlServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TopicFinalControlServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TestRepository testRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;
    @Mock private TestQueryService testQueryService;

    @Test
    void assignActiveFinalAllowsEligibleControlPublishedCandidate() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.empty());
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_ASSIGN), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatCode(() -> service.assignActiveFinalTest(20L, 101L)).doesNotThrowAnyException();

        ArgumentCaptor<com.vladislav.training.platform.content.domain.Test> captor =
            ArgumentCaptor.forClass(com.vladislav.training.platform.content.domain.Test.class);
        verify(testRepository).saveTest(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(101L);
        assertThat(captor.getValue().isActiveFinalForTopic()).isTrue();
        assertThat(captor.getValue().status()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(captor.getValue().testType()).isEqualTo(TestType.CONTROL);
    }

    @Test
    void assignActiveFinalSameCandidateIsIdempotentNoOpAndWritesTopicAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(current);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_ASSIGN), any(Map.class)))
            .thenReturn(new AuditContext("{}"));

        assertThatCode(() -> service.assignActiveFinalTest(20L, 101L)).doesNotThrowAnyException();

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(current), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_ASSIGN, 101L, 101L, "ASSIGN");
    }

    @Test
    void assignActiveFinalRejectsNonControlTest() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test candidate = new com.vladislav.training.platform.content.domain.Test(101L, 20L, "Training", null, TestType.TRAINING, ContentStatus.PUBLISHED, BigDecimal.valueOf(80), "DEFAULT", false, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        doCallRealMethod().when(support).validateActiveFinalEligibility(candidate);

        assertThatThrownBy(() -> service.assignActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("CONTROL and PUBLISHED");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
        verifyNoInteractions(auditSupport);
    }

    @Test
    void assignActiveFinalRejectsNonPublishedTest() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test candidate = new com.vladislav.training.platform.content.domain.Test(101L, 20L, "Control", null, TestType.CONTROL, ContentStatus.DRAFT, BigDecimal.valueOf(80), "DEFAULT", false, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        doCallRealMethod().when(support).validateActiveFinalEligibility(candidate);

        assertThatThrownBy(() -> service.assignActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("CONTROL and PUBLISHED");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    @Test
    void assignActiveFinalRejectsTestFromAnotherTopic() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 21L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);

        assertThatThrownBy(() -> service.assignActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("same topic");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void assignActiveFinalRejectsWhenCurrentActiveFinalAlreadyExists() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        doCallRealMethod().when(support).validateActiveFinalEligibility(candidate);

        assertThatThrownBy(() -> service.assignActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already has active final test");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void replaceUsesLockingReadPath() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_REPLACE), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatCode(() -> service.replaceActiveFinalTest(20L, 101L)).doesNotThrowAnyException();

        verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 20L, 101L);
        verify(support).requirePublished(ContentStatus.PUBLISHED, "Topic");
        verify(testRepository).lockTestById(101L);
        verify(testRepository).findActiveFinalTestByTopicIdForUpdate(20L);
    }

    @Test
    void replaceActiveFinalSameCandidateIsIdempotentNoOpAndWritesTopicAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(101L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(current);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_REPLACE), any(Map.class)))
            .thenReturn(new AuditContext("{}"));

        assertThatCode(() -> service.replaceActiveFinalTest(20L, 101L)).doesNotThrowAnyException();

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(current), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 101L, 101L, "REPLACE");
    }

    @Test
    void replaceActiveFinalRejectsNonControlCandidate() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = new com.vladislav.training.platform.content.domain.Test(
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
        );
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        doCallRealMethod().when(support).validateActiveFinalEligibility(candidate);

        assertThatThrownBy(() -> service.replaceActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("CONTROL and PUBLISHED");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void replaceActiveFinalRejectsNonPublishedCandidate() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = new com.vladislav.training.platform.content.domain.Test(
            101L,
            20L,
            "Control",
            null,
            TestType.CONTROL,
            ContentStatus.DRAFT,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        doCallRealMethod().when(support).validateActiveFinalEligibility(candidate);

        assertThatThrownBy(() -> service.replaceActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("CONTROL and PUBLISHED");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void replaceActiveFinalRejectsCandidateFromAnotherTopic() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 21L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);

        assertThatThrownBy(() -> service.replaceActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("same topic");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void replaceActiveFinalReplacesAtomicallyWithinTopic() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_REPLACE), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        doCallRealMethod().when(support).validateActiveFinalEligibility(candidate);

        assertThatCode(() -> service.replaceActiveFinalTest(20L, 101L)).doesNotThrowAnyException();

        ArgumentCaptor<com.vladislav.training.platform.content.domain.Test> captor = ArgumentCaptor.forClass(com.vladislav.training.platform.content.domain.Test.class);
        verify(testRepository, org.mockito.Mockito.times(2)).saveTest(captor.capture());
        List<com.vladislav.training.platform.content.domain.Test> saved = captor.getAllValues();
        org.assertj.core.api.Assertions.assertThat(saved.get(0).id()).isEqualTo(100L);
        org.assertj.core.api.Assertions.assertThat(saved.get(0).isActiveFinalForTopic()).isFalse();
        org.assertj.core.api.Assertions.assertThat(saved.get(1).id()).isEqualTo(101L);
        org.assertj.core.api.Assertions.assertThat(saved.get(1).isActiveFinalForTopic()).isTrue();
        verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), eq(saved.get(1)), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_REPLACE, 100L, 101L, "REPLACE");
    }

    @Test
    void replaceActiveFinalKeepsLifecycleStateUnchangedExceptFinalFlag() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = new com.vladislav.training.platform.content.domain.Test(
            100L,
            20L,
            "Current",
            "Current description",
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            BigDecimal.valueOf(75),
            "WEIGHTED",
            true,
            4,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        com.vladislav.training.platform.content.domain.Test candidate = new com.vladislav.training.platform.content.domain.Test(
            101L,
            20L,
            "Candidate",
            "Candidate description",
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            BigDecimal.valueOf(85),
            "DEFAULT",
            false,
            7,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_REPLACE), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatCode(() -> service.replaceActiveFinalTest(20L, 101L)).doesNotThrowAnyException();

        ArgumentCaptor<com.vladislav.training.platform.content.domain.Test> captor =
            ArgumentCaptor.forClass(com.vladislav.training.platform.content.domain.Test.class);
        verify(testRepository, org.mockito.Mockito.times(2)).saveTest(captor.capture());
        List<com.vladislav.training.platform.content.domain.Test> saved = captor.getAllValues();

        assertThat(saved.get(0).id()).isEqualTo(current.id());
        assertThat(saved.get(0).status()).isEqualTo(current.status());
        assertThat(saved.get(0).testType()).isEqualTo(current.testType());
        assertThat(saved.get(0).thresholdPercent()).isEqualByComparingTo(current.thresholdPercent());
        assertThat(saved.get(0).scoringPolicyCode()).isEqualTo(current.scoringPolicyCode());
        assertThat(saved.get(0).sortOrder()).isEqualTo(current.sortOrder());
        assertThat(saved.get(0).name()).isEqualTo(current.name());
        assertThat(saved.get(0).description()).isEqualTo(current.description());
        assertThat(saved.get(0).isActiveFinalForTopic()).isFalse();

        assertThat(saved.get(1).id()).isEqualTo(candidate.id());
        assertThat(saved.get(1).status()).isEqualTo(candidate.status());
        assertThat(saved.get(1).testType()).isEqualTo(candidate.testType());
        assertThat(saved.get(1).thresholdPercent()).isEqualByComparingTo(candidate.thresholdPercent());
        assertThat(saved.get(1).scoringPolicyCode()).isEqualTo(candidate.scoringPolicyCode());
        assertThat(saved.get(1).sortOrder()).isEqualTo(candidate.sortOrder());
        assertThat(saved.get(1).name()).isEqualTo(candidate.name());
        assertThat(saved.get(1).description()).isEqualTo(candidate.description());
        assertThat(saved.get(1).isActiveFinalForTopic()).isTrue();
    }

    @Test
    void clearUsesLockedActiveFinalReadPath() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_CLEAR), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatCode(() -> service.clearActiveFinalTest(20L)).doesNotThrowAnyException();

        verify(support).checkFinal(CapabilityOperationCode.CONTENT_FINAL_CLEAR, 20L);
        verify(support).requirePublished(ContentStatus.PUBLISHED, "Topic");
        verify(testRepository).findActiveFinalTestByTopicIdForUpdate(20L);
    }

    @Test
    void clearActiveFinalClearsCurrentTopicFinal() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_CLEAR), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatCode(() -> service.clearActiveFinalTest(20L)).doesNotThrowAnyException();

        ArgumentCaptor<com.vladislav.training.platform.content.domain.Test> captor = ArgumentCaptor.forClass(com.vladislav.training.platform.content.domain.Test.class);
        verify(testRepository).saveTest(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().id()).isEqualTo(100L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().isActiveFinalForTopic()).isFalse();
        verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(current), any(com.vladislav.training.platform.content.domain.Test.class), any(AuditContext.class));
    }

    @Test
    void clearActiveFinalEmptyStateIsIdempotentNoOpAndWritesTopicAudit() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.empty());
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_FINAL_CLEAR), any(Map.class)))
            .thenReturn(new AuditContext("{}"));

        assertThatCode(() -> service.clearActiveFinalTest(20L)).doesNotThrowAnyException();

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(20L), eq(null), eq(null), any(AuditContext.class));
        assertAuditDetails(CapabilityOperationCode.CONTENT_FINAL_CLEAR, null, null, "CLEAR");
    }

    @Test
    void replaceRejectsWhenTopicIsNotPublished() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        doThrow(new ConflictException("Topic must be PUBLISHED")).when(support).requirePublished(ContentStatus.DRAFT, "Topic");

        assertThatThrownBy(() -> service.replaceActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic must be PUBLISHED");

        verifyNoInteractions(auditSupport);
    }

    @Test
    void replaceNormalizesConcurrentUniqueFailureToDomainConflict() {
        TopicFinalControlServiceImpl service = new TopicFinalControlServiceImpl(testRepository, topicRepository, support, auditSupport, utcClock, testQueryService);
        Topic topic = new Topic(20L, 30L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test current = test(100L, 20L, true);
        com.vladislav.training.platform.content.domain.Test candidate = test(101L, 20L, false);
        when(topicRepository.findTopicById(20L)).thenReturn(topic);
        when(testRepository.lockTestById(101L)).thenReturn(candidate);
        when(testRepository.findActiveFinalTestByTopicIdForUpdate(20L)).thenReturn(Optional.of(current));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class)))
            .thenThrow(new PersistenceConstraintViolationException("unique conflict"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatThrownBy(() -> service.replaceActiveFinalTest(20L, 101L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Concurrent active final test change detected");

        verifyNoInteractions(auditSupport);
    }

    private com.vladislav.training.platform.content.domain.Test test(Long testId, Long topicId, boolean activeFinal) {
        return new com.vladislav.training.platform.content.domain.Test(testId, topicId, "Test", null, TestType.CONTROL, ContentStatus.PUBLISHED, BigDecimal.valueOf(80), "DEFAULT", activeFinal, 0, FIXED_INSTANT, FIXED_INSTANT);
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
}
