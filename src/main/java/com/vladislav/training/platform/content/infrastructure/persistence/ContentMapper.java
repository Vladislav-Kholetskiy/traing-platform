package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.*;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Преобразователь {@code ContentMapper}.
 */
@Component
public class ContentMapper {
    public Course toDomain(CourseEntity e){ return new Course(e.getId(), e.getName(), e.getDescription(), e.getStatus(), e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt()); }
    public CourseEntity toEntity(Course d){ CourseEntity e=new CourseEntity(); e.setId(d.id()); e.setName(d.name()); e.setDescription(d.description()); e.setStatus(d.status()); e.setSortOrder(d.sortOrder()); e.setCreatedAt(d.createdAt()); e.setUpdatedAt(d.updatedAt()); return e; }
    public Topic toDomain(TopicEntity e){ return new Topic(e.getId(), e.getCourseId(), e.getName(), e.getDescription(), e.getStatus(), e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt()); }
    public TopicEntity toEntity(Topic d){ TopicEntity e=new TopicEntity(); e.setId(d.id()); e.setCourseId(d.courseId()); e.setName(d.name()); e.setDescription(d.description()); e.setStatus(d.status()); e.setSortOrder(d.sortOrder()); e.setCreatedAt(d.createdAt()); e.setUpdatedAt(d.updatedAt()); return e; }
    public Material toDomain(MaterialEntity e){ return new Material(e.getId(), e.getTopicId(), e.getName(), e.getDescription(), e.getBody(), e.getVideoUrl(), e.getMaterialType(), e.getStatus(), e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt()); }
    public MaterialEntity toEntity(Material d){ MaterialEntity e=new MaterialEntity(); e.setId(d.id()); e.setTopicId(d.topicId()); e.setName(d.name()); e.setDescription(d.description()); e.setBody(d.body()); e.setVideoUrl(d.videoUrl()); e.setMaterialType(d.materialType()); e.setStatus(d.status()); e.setSortOrder(d.sortOrder()); e.setCreatedAt(d.createdAt()); e.setUpdatedAt(d.updatedAt()); return e; }
    public Question toDomain(QuestionEntity e){ return new Question(e.getId(), e.getTopicId(), e.getBody(), e.getQuestionType(), e.getStatus(), e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt()); }
    public QuestionEntity toEntity(Question d){ QuestionEntity e=new QuestionEntity(); e.setId(d.id()); e.setTopicId(d.topicId()); e.setBody(d.body()); e.setQuestionType(d.questionType()); e.setStatus(d.status()); e.setSortOrder(d.sortOrder()); e.setCreatedAt(d.createdAt()); e.setUpdatedAt(d.updatedAt()); return e; }
    public AnswerOption toDomain(AnswerOptionEntity e){ return new AnswerOption(e.getId(), e.getQuestionId(), e.getBody(), e.getAnswerOptionRole(), e.getIsCorrect(), e.getDisplayOrder(), e.getPairingKey(), e.getCanonicalOrderPosition(), e.getCreatedAt(), e.getUpdatedAt()); }
    public AnswerOptionEntity toEntity(AnswerOption d){ AnswerOptionEntity e=new AnswerOptionEntity(); e.setId(d.id()); e.setQuestionId(d.questionId()); e.setBody(d.body()); e.setAnswerOptionRole(d.answerOptionRole()); e.setIsCorrect(d.isCorrect()); e.setDisplayOrder(d.displayOrder()); e.setPairingKey(d.pairingKey()); e.setCanonicalOrderPosition(d.canonicalOrderPosition()); e.setCreatedAt(d.createdAt()); e.setUpdatedAt(d.updatedAt()); return e; }
    public Test toDomain(TestEntity e){ return new Test(e.getId(), e.getTopicId(), e.getName(), e.getDescription(), e.getTestType(), e.getStatus(), e.getThresholdPercent(), e.getScoringPolicyCode(), e.isActiveFinalForTopic(), e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt()); }
    public TestEntity toEntity(Test d){ TestEntity e=new TestEntity(); e.setId(d.id()); e.setTopicId(d.topicId()); e.setName(d.name()); e.setDescription(d.description()); e.setTestType(d.testType()); e.setStatus(d.status()); e.setThresholdPercent(d.thresholdPercent()); e.setScoringPolicyCode(d.scoringPolicyCode()); e.setActiveFinalForTopic(d.isActiveFinalForTopic()); e.setSortOrder(d.sortOrder()); e.setCreatedAt(d.createdAt()); e.setUpdatedAt(d.updatedAt()); return e; }
    public TestQuestion toDomain(TestQuestionEntity e){ return new TestQuestion(e.getId(), e.getTestId(), e.getQuestionId(), e.getDisplayOrder(), e.getWeight(), e.getCreatedAt(), e.getUpdatedAt()); }
    public TestQuestionEntity toEntity(TestQuestion d){ TestQuestionEntity e=new TestQuestionEntity(); e.setId(d.id()); e.setTestId(d.testId()); e.setQuestionId(d.questionId()); e.setDisplayOrder(d.displayOrder()); e.setWeight(d.weight()); e.setCreatedAt(d.createdAt()); e.setUpdatedAt(d.updatedAt()); return e; }
    public List<Course> toCourses(List<CourseEntity> entities){ return entities.stream().map(this::toDomain).toList(); }
    public List<Topic> toTopics(List<TopicEntity> entities){ return entities.stream().map(this::toDomain).toList(); }
    public List<Material> toMaterials(List<MaterialEntity> entities){ return entities.stream().map(this::toDomain).toList(); }
    public List<Question> toQuestions(List<QuestionEntity> entities){ return entities.stream().map(this::toDomain).toList(); }
    public List<AnswerOption> toAnswerOptions(List<AnswerOptionEntity> entities){ return entities.stream().map(this::toDomain).toList(); }
    public List<Test> toTests(List<TestEntity> entities){ return entities.stream().map(this::toDomain).toList(); }
    public List<TestQuestion> toTestQuestions(List<TestQuestionEntity> entities){ return entities.stream().map(this::toDomain).toList(); }
}
