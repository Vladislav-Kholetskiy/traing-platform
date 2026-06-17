import { apiClient } from '../../../shared/api/apiClient';
import type {
  AnswerOption,
  Course,
  CreateMaterialRequest,
  CreateQuestionRequest,
  CreateTestRequest,
  CreateTopicRequest,
  Material,
  Question,
  SaveAnswerOptionRequest,
  SaveCourseRequest,
  SaveTestQuestionRequest,
  TestEntity,
  TestQuestionBinding,
  Topic,
  UpdateMaterialRequest,
  UpdateQuestionRequest,
  UpdateTestRequest,
  UpdateTopicRequest,
} from '../model/expertContent';

export function getCourses() {
  return apiClient.get<Course[]>('/api/v1/expert/content/courses');
}

export function getCourse(courseId: number) {
  return apiClient.get<Course>(`/api/v1/expert/content/courses/${courseId}`);
}

export function createCourse(payload: SaveCourseRequest) {
  return apiClient.post<Course>('/api/v1/expert/content/courses', payload);
}

export function updateCourse(courseId: number, payload: SaveCourseRequest) {
  return apiClient.patch<Course>(`/api/v1/expert/content/courses/${courseId}`, payload);
}

export function getTopicsByCourse(courseId: number) {
  return apiClient.get<Topic[]>(`/api/v1/expert/content/topics?courseId=${courseId}`);
}

export function getTopic(topicId: number) {
  return apiClient.get<Topic>(`/api/v1/expert/content/topics/${topicId}`);
}

export function createTopic(payload: CreateTopicRequest) {
  return apiClient.post<Topic>('/api/v1/expert/content/topics', payload);
}

export function updateTopic(topicId: number, payload: UpdateTopicRequest) {
  return apiClient.patch<Topic>(`/api/v1/expert/content/topics/${topicId}`, payload);
}

export function getMaterialsByTopic(topicId: number) {
  return apiClient.get<Material[]>(`/api/v1/expert/content/materials?topicId=${topicId}`);
}

export function getMaterial(materialId: number) {
  return apiClient.get<Material>(`/api/v1/expert/content/materials/${materialId}`);
}

export function createMaterial(payload: CreateMaterialRequest) {
  return apiClient.post<Material>('/api/v1/expert/content/materials', payload);
}

export function updateMaterial(materialId: number, payload: UpdateMaterialRequest) {
  return apiClient.patch<Material>(`/api/v1/expert/content/materials/${materialId}`, payload);
}

export function getQuestionsByTopic(topicId: number) {
  return apiClient.get<Question[]>(`/api/v1/expert/content/questions?topicId=${topicId}`);
}

export function getQuestion(questionId: number) {
  return apiClient.get<Question>(`/api/v1/expert/content/questions/${questionId}`);
}

export function getQuestionAnswerOptions(questionId: number) {
  return apiClient.get<AnswerOption[]>(`/api/v1/expert/content/questions/${questionId}/answer-options`);
}

export function createQuestion(payload: CreateQuestionRequest) {
  return apiClient.post<Question>('/api/v1/expert/content/questions', payload);
}

export function updateQuestion(questionId: number, payload: UpdateQuestionRequest) {
  return apiClient.patch<Question>(`/api/v1/expert/content/questions/${questionId}`, payload);
}

export function createAnswerOption(questionId: number, payload: SaveAnswerOptionRequest) {
  return apiClient.post<AnswerOption>(
    `/api/v1/expert/content/questions/${questionId}/answer-options`,
    payload,
  );
}

export function updateAnswerOption(questionId: number, answerOptionId: number, payload: SaveAnswerOptionRequest) {
  return apiClient.patch<AnswerOption>(
    `/api/v1/expert/content/questions/${questionId}/answer-options/${answerOptionId}`,
    payload,
  );
}

export function deleteAnswerOption(questionId: number, answerOptionId: number) {
  return apiClient.delete<void>(`/api/v1/expert/content/questions/${questionId}/answer-options/${answerOptionId}`);
}

export function getTestsByTopic(topicId: number) {
  return apiClient.get<TestEntity[]>(`/api/v1/expert/content/tests?topicId=${topicId}`);
}

export function getTest(testId: number) {
  return apiClient.get<TestEntity>(`/api/v1/expert/content/tests/${testId}`);
}

export function getTestQuestions(testId: number) {
  return apiClient.get<TestQuestionBinding[]>(`/api/v1/expert/content/tests/${testId}/questions`);
}

export function createTest(payload: CreateTestRequest) {
  return apiClient.post<TestEntity>('/api/v1/expert/content/tests', payload);
}

export function updateTest(testId: number, payload: UpdateTestRequest) {
  return apiClient.patch<TestEntity>(`/api/v1/expert/content/tests/${testId}`, payload);
}

export function createTestQuestion(testId: number, payload: SaveTestQuestionRequest) {
  return apiClient.post<TestQuestionBinding>(`/api/v1/expert/content/tests/${testId}/questions`, payload);
}

export function updateTestQuestion(testId: number, testQuestionId: number, payload: SaveTestQuestionRequest) {
  return apiClient.patch<TestQuestionBinding>(
    `/api/v1/expert/content/tests/${testId}/questions/${testQuestionId}`,
    payload,
  );
}

export function deleteTestQuestion(testId: number, testQuestionId: number) {
  return apiClient.delete<void>(`/api/v1/expert/content/tests/${testId}/questions/${testQuestionId}`);
}

export function getLifecycleCourse(courseId: number) {
  return apiClient.get<Course>(`/api/v1/expert/content/lifecycle/courses/${courseId}`);
}

export function getLifecycleTopic(topicId: number) {
  return apiClient.get<Topic>(`/api/v1/expert/content/lifecycle/topics/${topicId}`);
}

export function getLifecycleMaterial(materialId: number) {
  return apiClient.get<Material>(`/api/v1/expert/content/lifecycle/materials/${materialId}`);
}

export function getLifecycleQuestion(questionId: number) {
  return apiClient.get<Question>(`/api/v1/expert/content/lifecycle/questions/${questionId}`);
}

export function getLifecycleTest(testId: number) {
  return apiClient.get<TestEntity>(`/api/v1/expert/content/lifecycle/tests/${testId}`);
}

export function publishCourse(courseId: number) {
  return apiClient.post<Course>(`/api/v1/expert/content/lifecycle/courses/${courseId}/publish`);
}

export function archiveCourse(courseId: number) {
  return apiClient.post<Course>(`/api/v1/expert/content/lifecycle/courses/${courseId}/archive`);
}

export function publishTopic(topicId: number) {
  return apiClient.post<Topic>(`/api/v1/expert/content/lifecycle/topics/${topicId}/publish`);
}

export function archiveTopic(topicId: number) {
  return apiClient.post<Topic>(`/api/v1/expert/content/lifecycle/topics/${topicId}/archive`);
}

export function publishMaterial(materialId: number) {
  return apiClient.post<Material>(`/api/v1/expert/content/lifecycle/materials/${materialId}/publish`);
}

export function archiveMaterial(materialId: number) {
  return apiClient.post<Material>(`/api/v1/expert/content/lifecycle/materials/${materialId}/archive`);
}

export function publishQuestion(questionId: number) {
  return apiClient.post<Question>(`/api/v1/expert/content/lifecycle/questions/${questionId}/publish`);
}

export function archiveQuestion(questionId: number) {
  return apiClient.post<Question>(`/api/v1/expert/content/lifecycle/questions/${questionId}/archive`);
}

export function publishTest(testId: number) {
  return apiClient.post<TestEntity>(`/api/v1/expert/content/lifecycle/tests/${testId}/publish`);
}

export function archiveTest(testId: number) {
  return apiClient.post<TestEntity>(`/api/v1/expert/content/lifecycle/tests/${testId}/archive`);
}

export function getActiveFinalTest(topicId: number) {
  return apiClient.get<TestEntity>(`/api/v1/expert/content/topics/${topicId}/active-final-test`);
}

export function getEligibleFinalControlTests(topicId: number) {
  return apiClient.get<TestEntity[]>(
    `/api/v1/expert/content/topics/${topicId}/tests?eligibleForFinalControl=true`,
  );
}

export function assignActiveFinalTest(topicId: number, testId: number) {
  return apiClient.post<void>(`/api/v1/expert/content/topics/${topicId}/active-final-tests/${testId}/assign`);
}

export function replaceActiveFinalTest(topicId: number, testId: number) {
  return apiClient.post<void>(`/api/v1/expert/content/topics/${topicId}/active-final-tests/${testId}/replace`);
}

export function clearActiveFinalTest(topicId: number) {
  return apiClient.delete<void>(`/api/v1/expert/content/topics/${topicId}/active-final-test`);
}
