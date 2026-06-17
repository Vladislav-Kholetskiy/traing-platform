package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code ContentDraftAuthoringAdmissionOrdering}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ContentDraftAuthoringAdmissionOrderingTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private CourseRepository courseRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerOptionRepository answerOptionRepository;
    @Mock private TestRepository testRepository;
    @Mock private TestQuestionRepository testQuestionRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;

    @Test
    void createCourseRejectsAdmissionBeforeMutationAndAudit() {
        CourseCommandServiceImpl service = new CourseCommandServiceImpl(courseRepository, support, auditSupport, utcClock);
        doThrow(admissionDenied()).when(support).checkCreate(CapabilityOperationCode.CONTENT_DRAFT_CREATE, CapabilityTargetEntityType.COURSE);

        assertThatThrownBy(() -> service.createCourse(new CreateCourseCommand("Course", null, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkCreate(CapabilityOperationCode.CONTENT_DRAFT_CREATE, CapabilityTargetEntityType.COURSE);
        verify(courseRepository, never()).saveCourse(any(Course.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void createCourseOrdersAdmissionBeforeSaveAndAudit() {
        CourseCommandServiceImpl service = new CourseCommandServiceImpl(courseRepository, support, auditSupport, utcClock);
        Course saved = new Course(10L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(courseRepository.saveCourse(any(Course.class))).thenReturn(saved);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_CREATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.createCourse(new CreateCourseCommand("Course", null, 0));

        InOrder inOrder = inOrder(support, courseRepository, auditSupport);
        inOrder.verify(support).checkCreate(CapabilityOperationCode.CONTENT_DRAFT_CREATE, CapabilityTargetEntityType.COURSE);
        inOrder.verify(courseRepository).saveCourse(any(Course.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("course"), eq(10L), eq(null), eq(saved), any(AuditContext.class));
    }

    @Test
    void updateCourseRejectsAdmissionBeforeMutationAndAudit() {
        CourseCommandServiceImpl service = new CourseCommandServiceImpl(courseRepository, support, auditSupport, utcClock);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(CapabilityOperationCode.CONTENT_DRAFT_UPDATE, CapabilityTargetEntityType.COURSE, 10L);

        assertThatThrownBy(() -> service.updateCourse(10L, new UpdateCourseCommand("Course", null, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(CapabilityOperationCode.CONTENT_DRAFT_UPDATE, CapabilityTargetEntityType.COURSE, 10L);
        verifyNoInteractions(courseRepository, auditSupport);
    }

    @Test
    void updateCourseOrdersAdmissionBeforeSaveAndAudit() {
        CourseCommandServiceImpl service = new CourseCommandServiceImpl(courseRepository, support, auditSupport, utcClock);
        Course existing = new Course(10L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Course saved = new Course(10L, "Updated", null, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(courseRepository.findCourseById(10L)).thenReturn(existing);
        when(courseRepository.saveCourse(any(Course.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateCourse(10L, new UpdateCourseCommand("Updated", null, 1));

        InOrder inOrder = inOrder(support, courseRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(CapabilityOperationCode.CONTENT_DRAFT_UPDATE, CapabilityTargetEntityType.COURSE, 10L);
        inOrder.verify(courseRepository).saveCourse(any(Course.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("course"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void createTopicRejectsAdmissionBeforeMutationAndAudit() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        doThrow(admissionDenied()).when(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TOPIC,
            20L,
            CapabilityTargetEntityType.COURSE
        );

        assertThatThrownBy(() -> service.createTopic(new CreateTopicCommand(20L, "Topic", null, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TOPIC,
            20L,
            CapabilityTargetEntityType.COURSE
        );
        verifyNoInteractions(courseRepository, auditSupport);
        verify(topicRepository, never()).saveTopic(any(Topic.class));
    }

    @Test
    void createTopicOrdersAdmissionBeforeSaveAndAudit() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Course parent = new Course(20L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Topic saved = topic(10L, 20L, ContentStatus.DRAFT);
        when(courseRepository.findCourseById(20L)).thenReturn(parent);
        when(topicRepository.findTopicsByCourseId(20L)).thenReturn(List.of());
        when(topicRepository.saveTopic(any(Topic.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_CREATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.createTopic(new CreateTopicCommand(20L, "Topic", null, 0));

        InOrder inOrder = inOrder(support, topicRepository, auditSupport);
        inOrder.verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TOPIC,
            20L,
            CapabilityTargetEntityType.COURSE
        );
        inOrder.verify(topicRepository).saveTopic(any(Topic.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(10L), eq(null), eq(saved), any(AuditContext.class));
    }

    @Test
    void updateTopicRejectsAdmissionBeforeMutationAndAudit() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Topic existing = topic(10L, 20L, ContentStatus.DRAFT);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TOPIC,
            10L,
            20L,
            CapabilityTargetEntityType.COURSE
        );

        assertThatThrownBy(() -> service.updateTopic(10L, new UpdateTopicCommand("Topic", null, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TOPIC,
            10L,
            20L,
            CapabilityTargetEntityType.COURSE
        );
        verify(topicRepository, never()).saveTopic(any(Topic.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void updateTopicOrdersAdmissionBeforeSaveAndAudit() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Topic existing = topic(10L, 20L, ContentStatus.DRAFT);
        Topic saved = new Topic(10L, 20L, "Updated", null, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        when(topicRepository.findTopicsByCourseId(20L)).thenReturn(List.of(existing));
        when(topicRepository.saveTopic(any(Topic.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateTopic(10L, new UpdateTopicCommand("Updated", null, 1));

        InOrder inOrder = inOrder(support, topicRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TOPIC,
            10L,
            20L,
            CapabilityTargetEntityType.COURSE
        );
        inOrder.verify(topicRepository).saveTopic(any(Topic.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("topic"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void createMaterialRejectsAdmissionBeforeMutationAndAudit() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        doThrow(admissionDenied()).when(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.MATERIAL,
            20L,
            CapabilityTargetEntityType.TOPIC
        );

        assertThatThrownBy(() -> service.createMaterial(new CreateMaterialCommand(20L, "Material", null, null, null, MaterialType.TEXT, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.MATERIAL,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        verifyNoInteractions(topicRepository, auditSupport);
        verify(materialRepository, never()).saveMaterial(any(Material.class));
    }

    @Test
    void createMaterialOrdersAdmissionBeforeSaveAndAudit() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Topic parent = topic(20L, 30L, ContentStatus.DRAFT);
        Material saved = material(10L, 20L, ContentStatus.DRAFT);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        when(materialRepository.findMaterialsByTopicId(20L)).thenReturn(List.of());
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_CREATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.createMaterial(new CreateMaterialCommand(20L, "Material", null, null, null, MaterialType.TEXT, 0));

        InOrder inOrder = inOrder(support, materialRepository, auditSupport);
        inOrder.verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.MATERIAL,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        inOrder.verify(materialRepository).saveMaterial(any(Material.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("material"), eq(10L), eq(null), eq(saved), any(AuditContext.class));
    }

    @Test
    void updateMaterialRejectsAdmissionBeforeMutationAndAudit() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Material existing = material(10L, 20L, ContentStatus.DRAFT);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.MATERIAL,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );

        assertThatThrownBy(() -> service.updateMaterial(10L, new UpdateMaterialCommand("Material", null, null, null, MaterialType.TEXT, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.MATERIAL,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        verify(materialRepository, never()).saveMaterial(any(Material.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void updateMaterialOrdersAdmissionBeforeSaveAndAudit() {
        MaterialCommandServiceImpl service = new MaterialCommandServiceImpl(materialRepository, topicRepository, support, auditSupport, utcClock);
        Material existing = material(10L, 20L, ContentStatus.DRAFT);
        Material saved = new Material(10L, 20L, "Updated", null, null, null, MaterialType.TEXT, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(materialRepository.findMaterialById(10L)).thenReturn(existing);
        when(materialRepository.findMaterialsByTopicId(20L)).thenReturn(List.of(existing));
        when(materialRepository.saveMaterial(any(Material.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateMaterial(10L, new UpdateMaterialCommand("Updated", null, null, null, MaterialType.TEXT, 1));

        InOrder inOrder = inOrder(support, materialRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.MATERIAL,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        inOrder.verify(materialRepository).saveMaterial(any(Material.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("material"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void createQuestionRejectsAdmissionBeforeMutationAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        doThrow(admissionDenied()).when(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.QUESTION,
            20L,
            CapabilityTargetEntityType.TOPIC
        );

        assertThatThrownBy(() -> service.createQuestion(new CreateQuestionCommand(20L, "Question", QuestionType.SINGLE_CHOICE, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.QUESTION,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        verifyNoInteractions(topicRepository, auditSupport);
        verify(questionRepository, never()).saveQuestion(any(Question.class));
    }

    @Test
    void createQuestionOrdersAdmissionBeforeSaveAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Topic parent = topic(20L, 30L, ContentStatus.DRAFT);
        Question saved = question(10L, 20L, ContentStatus.DRAFT);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        when(questionRepository.saveQuestion(any(Question.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_CREATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.createQuestion(new CreateQuestionCommand(20L, "Question", QuestionType.SINGLE_CHOICE, 0));

        InOrder inOrder = inOrder(support, questionRepository, auditSupport);
        inOrder.verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.QUESTION,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        inOrder.verify(questionRepository).saveQuestion(any(Question.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(null), eq(saved), any(AuditContext.class));
    }

    @Test
    void updateQuestionRejectsAdmissionBeforeMutationAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Question existing = question(10L, 20L, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(10L)).thenReturn(existing);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );

        assertThatThrownBy(() -> service.updateQuestion(10L, new UpdateQuestionCommand("Question", QuestionType.SINGLE_CHOICE, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        verify(questionRepository, never()).saveQuestion(any(Question.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void updateQuestionOrdersAdmissionBeforeSaveAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Question existing = question(10L, 20L, ContentStatus.DRAFT);
        Question saved = new Question(10L, 20L, "Updated", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(questionRepository.findQuestionById(10L)).thenReturn(existing);
        when(questionRepository.saveQuestion(any(Question.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateQuestion(10L, new UpdateQuestionCommand("Updated", QuestionType.SINGLE_CHOICE, 1));

        InOrder inOrder = inOrder(support, questionRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        inOrder.verify(questionRepository).saveQuestion(any(Question.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void createAnswerOptionRejectsAdmissionBeforeMutationAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );

        assertThatThrownBy(() -> service.createAnswerOption(10L, new CreateAnswerOptionCommand(
            "Option",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );
        verifyNoInteractions(questionRepository, answerOptionRepository, auditSupport);
    }

    @Test
    void createAnswerOptionOrdersAdmissionBeforeSaveAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Question parent = question(10L, 20L, ContentStatus.DRAFT);
        AnswerOption saved = answerOption(11L, 10L);
        when(questionRepository.findQuestionById(10L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(10L)).thenReturn(List.of());
        when(answerOptionRepository.saveAnswerOption(any(AnswerOption.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.createAnswerOption(10L, new CreateAnswerOptionCommand(
            "Option",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null
        ));

        InOrder inOrder = inOrder(support, answerOptionRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );
        inOrder.verify(answerOptionRepository).saveAnswerOption(any(AnswerOption.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(null), eq(saved), any(AuditContext.class));
    }

    @Test
    void updateAnswerOptionRejectsAdmissionBeforeMutationAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = answerOption(11L, 10L);
        Question parent = question(10L, 20L, ContentStatus.DRAFT);
        when(answerOptionRepository.findAnswerOptionById(11L)).thenReturn(existing);
        when(questionRepository.findQuestionById(10L)).thenReturn(parent);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );

        assertThatThrownBy(() -> service.updateAnswerOption(10L, 11L, new UpdateAnswerOptionCommand(
            "Option",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );
        verify(answerOptionRepository, never()).saveAnswerOption(any(AnswerOption.class));
        verify(answerOptionRepository, never()).deleteAnswerOption(anyLong());
        verify(answerOptionRepository, never()).findAnswerOptionsByQuestionId(anyLong());
        verifyNoInteractions(auditSupport);
    }

    @Test
    void updateAnswerOptionOrdersAdmissionBeforeSaveAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = answerOption(11L, 10L);
        AnswerOption saved = new AnswerOption(
            11L,
            10L,
            "Updated",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            1,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        Question parent = question(10L, 20L, ContentStatus.DRAFT);
        when(answerOptionRepository.findAnswerOptionById(11L)).thenReturn(existing);
        when(questionRepository.findQuestionById(10L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(10L)).thenReturn(List.of(existing));
        when(answerOptionRepository.saveAnswerOption(any(AnswerOption.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateAnswerOption(10L, 11L, new UpdateAnswerOptionCommand(
            "Updated",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            1,
            null,
            null
        ));

        InOrder inOrder = inOrder(support, answerOptionRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );
        inOrder.verify(answerOptionRepository).saveAnswerOption(any(AnswerOption.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void deleteAnswerOptionRejectsAdmissionBeforeMutationAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = answerOption(11L, 10L);
        Question parent = question(10L, 20L, ContentStatus.DRAFT);
        when(answerOptionRepository.findAnswerOptionById(11L)).thenReturn(existing);
        when(questionRepository.findQuestionById(10L)).thenReturn(parent);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );

        assertThatThrownBy(() -> service.deleteAnswerOption(10L, 11L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );
        verify(answerOptionRepository, never()).deleteAnswerOption(11L);
        verify(answerOptionRepository, never()).findAnswerOptionsByQuestionId(anyLong());
        verifyNoInteractions(auditSupport);
    }

    @Test
    void deleteAnswerOptionOrdersAdmissionBeforeDeleteAndAudit() {
        QuestionCommandServiceImpl service = new QuestionCommandServiceImpl(
            questionRepository,
            answerOptionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        AnswerOption existing = answerOption(11L, 10L);
        Question parent = question(10L, 20L, ContentStatus.DRAFT);
        when(answerOptionRepository.findAnswerOptionById(11L)).thenReturn(existing);
        when(questionRepository.findQuestionById(10L)).thenReturn(parent);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(10L)).thenReturn(List.of(existing));
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));

        service.deleteAnswerOption(10L, 11L);

        InOrder inOrder = inOrder(answerOptionRepository, questionRepository, support, auditSupport);
        inOrder.verify(answerOptionRepository).findAnswerOptionById(11L);
        inOrder.verify(questionRepository).findQuestionById(10L);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.QUESTION,
            10L,
            10L,
            CapabilityTargetEntityType.QUESTION
        );
        inOrder.verify(answerOptionRepository).findAnswerOptionsByQuestionId(10L);
        inOrder.verify(answerOptionRepository).deleteAnswerOption(11L);
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("question"), eq(10L), eq(existing), eq(null), any(AuditContext.class));
    }

    @Test
    void createTestRejectsAdmissionBeforeMutationAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        doThrow(admissionDenied()).when(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TEST,
            20L,
            CapabilityTargetEntityType.TOPIC
        );

        assertThatThrownBy(() -> service.createTest(new CreateTestCommand(
            20L,
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            0
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TEST,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        verifyNoInteractions(topicRepository, auditSupport);
        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
    }

    @Test
    void createTestOrdersAdmissionBeforeSaveAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        Topic parent = topic(20L, 30L, ContentStatus.DRAFT);
        com.vladislav.training.platform.content.domain.Test saved = test(10L, 20L, ContentStatus.DRAFT);
        when(topicRepository.findTopicById(20L)).thenReturn(parent);
        when(testRepository.findTestsByTopicId(20L)).thenReturn(List.of());
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_CREATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.createTest(new CreateTestCommand(
            20L,
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            0
        ));

        InOrder inOrder = inOrder(support, testRepository, auditSupport);
        inOrder.verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TEST,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        inOrder.verify(testRepository).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(null), eq(saved), any(AuditContext.class));
    }

    @Test
    void updateTestRejectsAdmissionBeforeMutationAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(10L, 20L, ContentStatus.DRAFT);
        when(testRepository.findTestById(10L)).thenReturn(existing);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );

        assertThatThrownBy(() -> service.updateTest(10L, new UpdateTestCommand(
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            0
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        verify(testRepository, never()).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void updateTestOrdersAdmissionBeforeSaveAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(10L, 20L, ContentStatus.DRAFT);
        com.vladislav.training.platform.content.domain.Test saved = new com.vladislav.training.platform.content.domain.Test(
            10L,
            20L,
            "Updated",
            null,
            TestType.CONTROL,
            ContentStatus.DRAFT,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(testRepository.findTestById(10L)).thenReturn(existing);
        when(testRepository.findTestsByTopicId(20L)).thenReturn(List.of(existing));
        when(testRepository.saveTest(any(com.vladislav.training.platform.content.domain.Test.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateTest(10L, new UpdateTestCommand(
            "Updated",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            1
        ));

        InOrder inOrder = inOrder(support, testRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            20L,
            CapabilityTargetEntityType.TOPIC
        );
        inOrder.verify(testRepository).saveTest(any(com.vladislav.training.platform.content.domain.Test.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void createTestQuestionRejectsAdmissionBeforeMutationAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parent = test(10L, 20L, ContentStatus.DRAFT);
        when(testRepository.findTestById(10L)).thenReturn(parent);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );

        assertThatThrownBy(() -> service.createTestQuestion(10L, new CreateTestQuestionCommand(30L, 0, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );
        verify(questionRepository, never()).findQuestionById(anyLong());
        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void createTestQuestionOrdersAdmissionBeforeSaveAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parent = test(10L, 20L, ContentStatus.DRAFT);
        Question question = question(30L, 20L, ContentStatus.DRAFT);
        TestQuestion saved = testQuestion(11L, 10L, 30L);
        when(testRepository.findTestById(10L)).thenReturn(parent);
        when(questionRepository.findQuestionById(30L)).thenReturn(question);
        when(testQuestionRepository.findTestQuestionsByTestId(10L)).thenReturn(List.of());
        when(testQuestionRepository.saveTestQuestion(any(TestQuestion.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.createTestQuestion(10L, new CreateTestQuestionCommand(30L, 0, BigDecimal.ONE));

        InOrder inOrder = inOrder(support, testQuestionRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );
        inOrder.verify(testQuestionRepository).saveTestQuestion(any(TestQuestion.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(null), eq(saved), any(AuditContext.class));
    }

    @Test
    void updateTestQuestionRejectsAdmissionBeforeMutationAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = testQuestion(11L, 10L, 30L);
        com.vladislav.training.platform.content.domain.Test parent = test(10L, 20L, ContentStatus.DRAFT);
        when(testQuestionRepository.findTestQuestionById(11L)).thenReturn(existing);
        when(testRepository.findTestById(10L)).thenReturn(parent);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );

        assertThatThrownBy(() -> service.updateTestQuestion(10L, 11L, new UpdateTestQuestionCommand(30L, 0, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );
        verify(questionRepository, never()).findQuestionById(anyLong());
        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
        verifyNoInteractions(auditSupport);
    }

    @Test
    void updateTestQuestionOrdersAdmissionBeforeSaveAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = testQuestion(11L, 10L, 30L);
        TestQuestion saved = new TestQuestion(11L, 10L, 30L, 1, BigDecimal.TWO, FIXED_INSTANT, FIXED_INSTANT);
        com.vladislav.training.platform.content.domain.Test parent = test(10L, 20L, ContentStatus.DRAFT);
        Question question = question(30L, 20L, ContentStatus.DRAFT);
        when(testQuestionRepository.findTestQuestionById(11L)).thenReturn(existing);
        when(testRepository.findTestById(10L)).thenReturn(parent);
        when(questionRepository.findQuestionById(30L)).thenReturn(question);
        when(testQuestionRepository.findTestQuestionsByTestId(10L)).thenReturn(List.of(existing));
        when(testQuestionRepository.saveTestQuestion(any(TestQuestion.class))).thenReturn(saved);
        stubDraftAuditContext(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code());
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateTestQuestion(10L, 11L, new UpdateTestQuestionCommand(30L, 1, BigDecimal.TWO));

        InOrder inOrder = inOrder(support, testQuestionRepository, auditSupport);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );
        inOrder.verify(testQuestionRepository).saveTestQuestion(any(TestQuestion.class));
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(saved), any(AuditContext.class));
    }

    @Test
    void deleteTestQuestionRejectsAdmissionBeforeMutationAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = testQuestion(11L, 10L, 30L);
        com.vladislav.training.platform.content.domain.Test parent = test(10L, 20L, ContentStatus.DRAFT);
        when(testQuestionRepository.findTestQuestionById(11L)).thenReturn(existing);
        when(testRepository.findTestById(10L)).thenReturn(parent);
        doThrow(admissionDenied()).when(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );

        assertThatThrownBy(() -> service.deleteTestQuestion(10L, 11L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );
        verify(testQuestionRepository, never()).deleteTestQuestion(11L);
        verifyNoInteractions(auditSupport);
    }

    @Test
    void deleteTestQuestionOrdersAdmissionBeforeDeleteAndAudit() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = testQuestion(11L, 10L, 30L);
        com.vladislav.training.platform.content.domain.Test parent = test(10L, 20L, ContentStatus.DRAFT);
        when(testQuestionRepository.findTestQuestionById(11L)).thenReturn(existing);
        when(testRepository.findTestById(10L)).thenReturn(parent);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));

        service.deleteTestQuestion(10L, 11L);

        InOrder inOrder = inOrder(testQuestionRepository, testRepository, support, auditSupport);
        inOrder.verify(testQuestionRepository).findTestQuestionById(11L);
        inOrder.verify(testRepository).findTestById(10L);
        inOrder.verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            10L,
            10L,
            CapabilityTargetEntityType.TEST
        );
        inOrder.verify(testQuestionRepository).deleteTestQuestion(11L);
        inOrder.verify(auditSupport).recordAudit(eq(777L), any(), eq("test"), eq(10L), eq(existing), eq(null), any(AuditContext.class));
    }

    private ConflictException admissionDenied() {
        return new ConflictException("admission denied");
    }

    private void stubDraftAuditContext(String operationCode) {
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(operationCode), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
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

    private AnswerOption answerOption(Long answerOptionId, Long questionId) {
        return new AnswerOption(
            answerOptionId,
            questionId,
            "Option",
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.TRUE,
            0,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
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

    private TestQuestion testQuestion(Long testQuestionId, Long testId, Long questionId) {
        return new TestQuestion(testQuestionId, testId, questionId, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
    }
}
