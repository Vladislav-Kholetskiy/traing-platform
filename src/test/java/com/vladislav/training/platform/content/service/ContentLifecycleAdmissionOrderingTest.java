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
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
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
 * Проверяет поведение {@code ContentLifecycleAdmissionOrdering}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ContentLifecycleAdmissionOrderingTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private com.vladislav.training.platform.content.repository.CourseRepository courseRepository;
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
    void publishCourseRejectsAdmissionBeforeMutationAndAudit() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(courseRepository, support, auditSupport, topicRepository, utcClock);
        when(courseRepository.findCourseById(10L)).thenReturn(course(10L, ContentStatus.DRAFT));
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.COURSE, 10L);

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.COURSE, 10L);
        verify(courseRepository, never()).saveCourse(any(Course.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void publishCourseOrdersAdmissionBeforeSaveAndAudit() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(courseRepository, support, auditSupport, topicRepository, utcClock);
        Course existing = course(10L, ContentStatus.DRAFT);
        Course saved = course(10L, ContentStatus.PUBLISHED);
        when(courseRepository.findCourseById(10L)).thenReturn(existing);
        when(courseRepository.saveCourse(any(Course.class))).thenReturn(saved);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_PUBLISH), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(courseRepository, support, auditSupport);
        inOrder.verify(courseRepository).findCourseById(10L);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.COURSE, 10L);
        inOrder.verify(courseRepository).saveCourse(any(Course.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("course"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveCourseOrdersAdmissionBeforeSaveAndAudit() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(courseRepository, support, auditSupport, topicRepository, utcClock);
        Course existing = course(10L, ContentStatus.PUBLISHED);
        Course saved = course(10L, ContentStatus.ARCHIVED);
        when(courseRepository.findCourseById(10L)).thenReturn(existing);
        when(courseRepository.saveCourse(any(Course.class))).thenReturn(saved);
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(support, courseRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.COURSE, 10L);
        inOrder.verify(courseRepository).saveCourse(any(Course.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("course"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveCourseRejectsAdmissionBeforeMutationAndAudit() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(courseRepository, support, auditSupport, topicRepository, utcClock);
        when(courseRepository.findCourseById(10L)).thenReturn(course(10L, ContentStatus.PUBLISHED));
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.COURSE, 10L);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.COURSE, 10L);
        verify(courseRepository, never()).saveCourse(any(Course.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void publishTopicRejectsAdmissionBeforeMutationAndAudit() {
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
        when(topicRepository.findTopicById(10L)).thenReturn(topic(10L, 20L, ContentStatus.DRAFT));
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TOPIC, 10L);

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TOPIC, 10L);
        verify(topicRepository, never()).saveTopic(any(Topic.class));
        verifyNoInteractions(courseRepository, auditSupport);
    }

    @Test
    void publishTopicOrdersAdmissionBeforeSaveAndAudit() {
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
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(support, topicRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TOPIC, 10L);
        inOrder.verify(topicRepository).saveTopic(any(Topic.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveTopicRejectsAdmissionBeforeMutationAndAudit() {
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
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.TOPIC, 10L);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.TOPIC, 10L);
        verify(topicRepository, never()).saveTopic(any(Topic.class));
        verifyNoInteractions(materialRepository, questionRepository, testRepository, auditSupport);
    }

    @Test
    void archiveTopicOrdersAdmissionBeforeSaveAndAudit() {
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
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(support, topicRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.TOPIC, 10L);
        inOrder.verify(topicRepository).saveTopic(any(Topic.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void publishMaterialRejectsAdmissionBeforeMutationAndAudit() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(materialRepository, support, auditSupport, topicRepository, utcClock);
        when(materialRepository.findMaterialById(10L)).thenReturn(material(10L, 20L, ContentStatus.DRAFT));
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.MATERIAL, 10L);

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.MATERIAL, 10L);
        verify(materialRepository, never()).saveMaterial(any(Material.class));
        verifyNoInteractions(topicRepository, auditSupport);
    }

    @Test
    void publishMaterialOrdersAdmissionBeforeSaveAndAudit() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(materialRepository, support, auditSupport, topicRepository, utcClock);
        Material existing = material(10L, 20L, ContentStatus.DRAFT);
        Material saved = material(10L, 20L, ContentStatus.PUBLISHED);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.PUBLISHED));
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(saved);
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(support, materialRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.MATERIAL, 10L);
        inOrder.verify(materialRepository).saveMaterial(any(Material.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("material"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveMaterialRejectsAdmissionBeforeMutationAndAudit() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(materialRepository, support, auditSupport, topicRepository, utcClock);
        when(materialRepository.findMaterialById(10L)).thenReturn(material(10L, 20L, ContentStatus.PUBLISHED));
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.MATERIAL, 10L);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.MATERIAL, 10L);
        verify(materialRepository, never()).saveMaterial(any(Material.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void archiveMaterialOrdersAdmissionBeforeSaveAndAudit() {
        MaterialLifecycleServiceImpl service = new MaterialLifecycleServiceImpl(materialRepository, support, auditSupport, topicRepository, utcClock);
        Material existing = material(10L, 20L, ContentStatus.PUBLISHED);
        Material saved = material(10L, 20L, ContentStatus.ARCHIVED);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(saved);
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(support, materialRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.MATERIAL, 10L);
        inOrder.verify(materialRepository).saveMaterial(any(Material.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("material"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void publishQuestionRejectsAdmissionBeforeMutationAndAudit() {
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
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.QUESTION, 10L);

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.QUESTION, 10L);
        verify(questionRepository, never()).saveQuestion(any(Question.class));
        verifyNoInteractions(topicRepository, publicationValidationService, auditSupport);
    }

    @Test
    void publishQuestionOrdersAdmissionBeforeSaveAndAudit() {
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
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(support, topicRepository, publicationValidationService, questionRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.QUESTION, 10L);
        inOrder.verify(topicRepository).findTopicById(20L);
        inOrder.verify(publicationValidationService).validateQuestionPublishable(10L);
        inOrder.verify(questionRepository).saveQuestion(any(Question.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveQuestionRejectsAdmissionBeforeMutationAndAudit() {
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
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.QUESTION, 10L);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.QUESTION, 10L);
        verify(questionRepository, never()).saveQuestion(any(Question.class));
        verifyNoInteractions(testQuestionRepository, auditSupport);
    }

    @Test
    void archiveQuestionOrdersAdmissionBeforeSaveAndAudit() {
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
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(support, questionRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.QUESTION, 10L);
        inOrder.verify(questionRepository).saveQuestion(any(Question.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void publishTestRejectsAdmissionBeforeMutationAndAudit() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        when(testRepository.findTestById(10L)).thenReturn(test(10L, 20L, ContentStatus.DRAFT));
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TEST, 10L);

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TEST, 10L);
        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(topicRepository, publicationValidationService, auditSupport);
    }

    @Test
    void publishTestOrdersAdmissionBeforeSaveAndAudit() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(10L, 20L, ContentStatus.DRAFT);
        com.vladislav.training.platform.content.domain.Test saved = test(10L, 20L, ContentStatus.PUBLISHED);
        when(testRepository.findTestById(10L)).thenReturn(existing);
        when(topicRepository.findTopicById(20L)).thenReturn(topic(20L, 30L, ContentStatus.PUBLISHED));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(saved);
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_PUBLISH);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.publish(10L);

        InOrder inOrder = inOrder(support, topicRepository, publicationValidationService, testRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TEST, 10L);
        inOrder.verify(topicRepository).findTopicById(20L);
        inOrder.verify(publicationValidationService).validateTestPublishable(10L);
        inOrder.verify(testRepository).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void archiveTestRejectsAdmissionBeforeMutationAndAudit() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        when(testRepository.findTestById(10L)).thenReturn(test(10L, 20L, ContentStatus.PUBLISHED));
        doThrow(admissionDenied()).when(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.TEST, 10L);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.TEST, 10L);
        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void archiveTestOrdersAdmissionBeforeSaveAndAudit() {
        TestLifecycleServiceImpl service = new TestLifecycleServiceImpl(
            testRepository,
            support,
            auditSupport,
            topicRepository,
            publicationValidationService,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(10L, 20L, ContentStatus.PUBLISHED);
        com.vladislav.training.platform.content.domain.Test saved = test(10L, 20L, ContentStatus.ARCHIVED);
        when(testRepository.findTestById(10L)).thenReturn(existing);
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(saved);
        stubLifecycleAuditContext(CapabilityOperationCode.CONTENT_ARCHIVE);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.archive(10L);

        InOrder inOrder = inOrder(support, testRepository, auditSupport);
        inOrder.verify(support).checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.TEST, 10L);
        inOrder.verify(testRepository).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    private void stubLifecycleAuditContext(CapabilityOperationCode operationCode) {
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(operationCode), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
    }

    private ConflictException admissionDenied() {
        return new ConflictException("admission denied");
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
        return new Question(questionId, topicId, "Question", QuestionType.SINGLE_CHOICE, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private com.vladislav.training.platform.content.domain.Test test(Long testId, Long topicId, ContentStatus status) {
        return new com.vladislav.training.platform.content.domain.Test(
            testId,
            topicId,
            "Test",
            null,
            TestType.CONTROL,
            status,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
