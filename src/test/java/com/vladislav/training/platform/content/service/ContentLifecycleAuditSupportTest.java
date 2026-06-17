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
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет вспомогательную логику {@code ContentLifecycleAudit}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
@ExtendWith(MockitoExtension.class)
class ContentLifecycleAuditSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private CourseRepository courseRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TestRepository testRepository;
    @Mock private TestQuestionRepository testQuestionRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private ContentPublicationValidationService publicationValidationService;
    @Mock private UtcClock utcClock;

    @Test
    void publishCourseWritesSynchronousAuditAfterSave() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(courseRepository, support, auditSupport, topicRepository, utcClock);
        Course existing = course(10L, ContentStatus.DRAFT);
        Course saved = course(10L, ContentStatus.PUBLISHED);
        when(courseRepository.findCourseById(10L)).thenReturn(existing);
        when(courseRepository.saveCourse(any(Course.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(courseRepository, auditSupport);
        inOrder.verify(courseRepository).saveCourse(any(Course.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("course"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveCourseWritesSynchronousAuditAfterSave() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(courseRepository, support, auditSupport, topicRepository, utcClock);
        Course existing = course(10L, ContentStatus.PUBLISHED);
        Course saved = course(10L, ContentStatus.ARCHIVED);
        when(courseRepository.findCourseById(10L)).thenReturn(existing);
        when(courseRepository.saveCourse(any(Course.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(courseRepository, auditSupport);
        inOrder.verify(courseRepository).saveCourse(any(Course.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("course"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveCourseRejectedPathDoesNotWriteSuccessAudit() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(courseRepository, support, auditSupport, topicRepository, utcClock);
        when(courseRepository.findCourseById(10L)).thenReturn(course(10L, ContentStatus.PUBLISHED));
        when(topicRepository.existsNonArchivedByCourseId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-archived topics");

        verify(courseRepository, never()).saveCourse(any(Course.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void publishCourseDoesNotCompleteWhenAuditWriteFails() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(courseRepository, support, auditSupport, topicRepository, utcClock);
        Course existing = course(10L, ContentStatus.DRAFT);
        Course saved = course(10L, ContentStatus.PUBLISHED);
        when(courseRepository.findCourseById(10L)).thenReturn(existing);
        when(courseRepository.saveCourse(any(Course.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        doThrow(new IllegalStateException("audit failed"))
            .when(auditSupport)
            .recordAudit(eq(777L), any(), eq("course"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit failed");
    }

    @Test
    void publishTopicWritesSynchronousAuditAfterSave() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(
            topicRepository,
            support,
            auditSupport,
            courseRepository,
            materialRepository,
            questionRepository,
            testRepository,
            utcClock
        );
        Topic existing = topic(10L, 20L, ContentStatus.DRAFT);
        Topic saved = topic(10L, 20L, ContentStatus.PUBLISHED);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        when(courseRepository.findCourseById(20L)).thenReturn(course(20L, ContentStatus.PUBLISHED));
        when(topicRepository.saveTopic(any(Topic.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(topicRepository, auditSupport);
        inOrder.verify(topicRepository).saveTopic(any(Topic.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveTopicWritesSynchronousAuditAfterSave() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(
            topicRepository,
            support,
            auditSupport,
            courseRepository,
            materialRepository,
            questionRepository,
            testRepository,
            utcClock
        );
        Topic existing = topic(10L, 20L, ContentStatus.PUBLISHED);
        Topic saved = topic(10L, 20L, ContentStatus.ARCHIVED);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        when(topicRepository.saveTopic(any(Topic.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(topicRepository, auditSupport);
        inOrder.verify(topicRepository).saveTopic(any(Topic.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveTopicRejectedPathDoesNotWriteSuccessAudit() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(
            topicRepository,
            support,
            auditSupport,
            courseRepository,
            materialRepository,
            questionRepository,
            testRepository,
            utcClock
        );
        when(topicRepository.findTopicById(10L)).thenReturn(topic(10L, 20L, ContentStatus.PUBLISHED));
        when(materialRepository.existsNonArchivedByTopicId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("child content exists");

        verify(topicRepository, never()).saveTopic(any(Topic.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void archiveTopicDoesNotCompleteWhenAuditWriteFails() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(
            topicRepository,
            support,
            auditSupport,
            courseRepository,
            materialRepository,
            questionRepository,
            testRepository,
            utcClock
        );
        Topic existing = topic(10L, 20L, ContentStatus.PUBLISHED);
        Topic saved = topic(10L, 20L, ContentStatus.ARCHIVED);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        when(topicRepository.saveTopic(any(Topic.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        doThrow(new IllegalStateException("audit failed"))
            .when(auditSupport)
            .recordAudit(eq(777L), any(), eq("topic"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit failed");
    }

    @Test
    void publishMaterialWritesSynchronousAuditAfterSave() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(materialRepository, support, auditSupport, topicRepository, utcClock);
        Material existing = material(10L, 20L, ContentStatus.DRAFT);
        Material saved = material(10L, 20L, ContentStatus.PUBLISHED);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.PUBLISHED));
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(materialRepository, auditSupport);
        inOrder.verify(materialRepository).saveMaterial(any(Material.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("material"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveMaterialWritesSynchronousAuditAfterSave() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(materialRepository, support, auditSupport, topicRepository, utcClock);
        Material existing = material(10L, 20L, ContentStatus.PUBLISHED);
        Material saved = material(10L, 20L, ContentStatus.ARCHIVED);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(materialRepository, auditSupport);
        inOrder.verify(materialRepository).saveMaterial(any(Material.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("material"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void publishMaterialRejectedPathDoesNotWriteSuccessAudit() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(materialRepository, support, auditSupport, topicRepository, utcClock);
        when(materialRepository.findMaterialById(10L)).thenReturn(material(10L, 20L, ContentStatus.DRAFT));
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.DRAFT));

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Parent topic must be PUBLISHED");

        verify(materialRepository, never()).saveMaterial(any(Material.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void publishMaterialDoesNotCompleteWhenAuditWriteFails() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(materialRepository, support, auditSupport, topicRepository, utcClock);
        Material existing = material(10L, 20L, ContentStatus.DRAFT);
        Material saved = material(10L, 20L, ContentStatus.PUBLISHED);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.PUBLISHED));
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        doThrow(new IllegalStateException("audit failed"))
            .when(auditSupport)
            .recordAudit(eq(777L), any(), eq("material"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit failed");
    }

    @Test
    void publishQuestionWritesSynchronousAuditAfterSave() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            questionRepository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        Question existing = question(10L, 20L, ContentStatus.DRAFT);
        Question saved = question(10L, 20L, ContentStatus.PUBLISHED);
        when(questionRepository.findQuestionById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.PUBLISHED));
        when(questionRepository.saveQuestion(any(Question.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(questionRepository, auditSupport);
        inOrder.verify(questionRepository).saveQuestion(any(Question.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveQuestionWritesSynchronousAuditAfterSave() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            questionRepository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        Question existing = question(10L, 20L, ContentStatus.PUBLISHED);
        Question saved = question(10L, 20L, ContentStatus.ARCHIVED);
        when(questionRepository.findQuestionById(10L)).thenReturn(existing);
        when(questionRepository.saveQuestion(any(Question.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(questionRepository, auditSupport);
        inOrder.verify(questionRepository).saveQuestion(any(Question.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void publishQuestionRejectedPathDoesNotWriteSuccessAudit() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            questionRepository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        when(questionRepository.findQuestionById(10L)).thenReturn(question(10L, 20L, ContentStatus.DRAFT));
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.DRAFT));

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Parent topic must be PUBLISHED");

        verify(questionRepository, never()).saveQuestion(any(Question.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void archiveQuestionRejectedPathDoesNotWriteSuccessAudit() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            questionRepository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        when(questionRepository.findQuestionById(10L)).thenReturn(question(10L, 20L, ContentStatus.PUBLISHED));
        when(testQuestionRepository.existsPublishedTestUsingQuestion(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("used by a PUBLISHED test");

        verify(questionRepository, never()).saveQuestion(any(Question.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void archiveQuestionDoesNotCompleteWhenAuditWriteFails() {
        QuestionLifecycleServiceImpl service = new QuestionLifecycleServiceImpl(
            questionRepository,
            support,
            auditSupport,
            topicRepository,
            testQuestionRepository,
            publicationValidationService,
            utcClock
        );
        Question existing = question(10L, 20L, ContentStatus.PUBLISHED);
        Question saved = question(10L, 20L, ContentStatus.ARCHIVED);
        when(questionRepository.findQuestionById(10L)).thenReturn(existing);
        when(testQuestionRepository.existsPublishedTestUsingQuestion(10L)).thenReturn(false);
        when(questionRepository.saveQuestion(any(Question.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        doThrow(new IllegalStateException("audit failed"))
            .when(auditSupport)
            .recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit failed");
    }

    @Test
    void publishTestWritesSynchronousAuditAfterSave() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(10L, 20L, ContentStatus.DRAFT, false);
        com.vladislav.training.platform.content.domain.Test saved = test(10L, 20L, ContentStatus.PUBLISHED, false);
        when(testRepository.findTestById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.PUBLISHED));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(testRepository, auditSupport);
        inOrder.verify(testRepository).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveTestWritesSynchronousAuditAfterSave() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(10L, 20L, ContentStatus.PUBLISHED, false);
        com.vladislav.training.platform.content.domain.Test saved = test(10L, 20L, ContentStatus.ARCHIVED, false);
        when(testRepository.findTestById(10L)).thenReturn(existing);
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(testRepository, auditSupport);
        inOrder.verify(testRepository).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void publishTestRejectedPathDoesNotWriteSuccessAudit() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        when(testRepository.findTestById(10L)).thenReturn(test(10L, 20L, ContentStatus.DRAFT, false));
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.DRAFT));

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Parent topic must be PUBLISHED");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void archiveTestRejectedPathDoesNotWriteSuccessAudit() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        when(testRepository.findTestById(10L)).thenReturn(test(10L, 20L, ContentStatus.PUBLISHED, true));

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Current active final test cannot be archived");

        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void publishTestDoesNotCompleteWhenAuditWriteFails() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(10L, 20L, ContentStatus.DRAFT, false);
        com.vladislav.training.platform.content.domain.Test saved = test(10L, 20L, ContentStatus.PUBLISHED, false);
        when(testRepository.findTestById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.PUBLISHED));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(saved);
        stubAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        doThrow(new IllegalStateException("audit failed"))
            .when(auditSupport)
            .recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit failed");
    }

    private void stubAuditContext(CapabilityOperationCode operationCode) {
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(operationCode), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
    }

    private Course course(Long courseId, ContentStatus status) {
        return new Course(courseId, "Course", null, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private Topic topic(Long topicId, Long courseId, ContentStatus status) {
        return new Topic(topicId, courseId, "Topic", null, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private Material material(Long materialId, Long topicId, ContentStatus status) {
        return new Material(materialId, topicId, "Material", null, null, null, MaterialType.TEXT, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private Question question(Long questionId, Long topicId, ContentStatus status) {
        return new Question(questionId, topicId, "Body", QuestionType.SINGLE_CHOICE, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private com.vladislav.training.platform.content.domain.Test test(
        Long testId,
        Long topicId,
        ContentStatus status,
        boolean activeFinal
    ) {
        return new com.vladislav.training.platform.content.domain.Test(
            testId,
            topicId,
            "Test",
            null,
            TestType.CONTROL,
            status,
            BigDecimal.valueOf(80),
            "DEFAULT",
            activeFinal,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
