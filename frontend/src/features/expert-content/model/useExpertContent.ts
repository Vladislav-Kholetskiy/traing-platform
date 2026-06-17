import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  archiveCourse,
  archiveMaterial,
  archiveQuestion,
  archiveTest,
  archiveTopic,
  assignActiveFinalTest,
  clearActiveFinalTest,
  createAnswerOption,
  createCourse,
  createMaterial,
  createQuestion,
  createTest,
  createTestQuestion,
  createTopic,
  deleteAnswerOption,
  deleteTestQuestion,
  getActiveFinalTest,
  getCourses,
  getCourse,
  getEligibleFinalControlTests,
  getLifecycleCourse,
  getLifecycleMaterial,
  getLifecycleQuestion,
  getLifecycleTest,
  getLifecycleTopic,
  getMaterial,
  getMaterialsByTopic,
  getQuestion,
  getQuestionAnswerOptions,
  getQuestionsByTopic,
  getTest,
  getTestsByTopic,
  getTestQuestions,
  getTopic,
  getTopicsByCourse,
  publishCourse,
  publishMaterial,
  publishQuestion,
  publishTest,
  publishTopic,
  replaceActiveFinalTest,
  updateAnswerOption,
  updateCourse,
  updateMaterial,
  updateQuestion,
  updateTest,
  updateTestQuestion,
  updateTopic,
} from '../api/expertContentApi';
import type {
  CreateMaterialRequest,
  CreateQuestionRequest,
  CreateTestRequest,
  CreateTopicRequest,
  SaveAnswerOptionRequest,
  SaveCourseRequest,
  SaveTestQuestionRequest,
  UpdateMaterialRequest,
  UpdateQuestionRequest,
  UpdateTestRequest,
  UpdateTopicRequest,
} from './expertContent';

export const expertContentKeys = {
  all: ['expert-content'] as const,
  courses: () => [...expertContentKeys.all, 'courses'] as const,
  course: (id: number) => [...expertContentKeys.courses(), id] as const,
  lifecycleCourse: (id: number) => [...expertContentKeys.course(id), 'lifecycle'] as const,
  topics: (courseId: number) => [...expertContentKeys.all, 'topics', courseId] as const,
  topic: (id: number) => [...expertContentKeys.all, 'topic', id] as const,
  lifecycleTopic: (id: number) => [...expertContentKeys.topic(id), 'lifecycle'] as const,
  materials: (topicId: number) => [...expertContentKeys.all, 'materials', topicId] as const,
  material: (id: number) => [...expertContentKeys.all, 'material', id] as const,
  lifecycleMaterial: (id: number) => [...expertContentKeys.material(id), 'lifecycle'] as const,
  questions: (topicId: number) => [...expertContentKeys.all, 'questions', topicId] as const,
  question: (id: number) => [...expertContentKeys.all, 'question', id] as const,
  lifecycleQuestion: (id: number) => [...expertContentKeys.question(id), 'lifecycle'] as const,
  answerOptions: (questionId: number) => [...expertContentKeys.question(questionId), 'answer-options'] as const,
  tests: (topicId: number) => [...expertContentKeys.all, 'tests', topicId] as const,
  test: (id: number) => [...expertContentKeys.all, 'test', id] as const,
  lifecycleTest: (id: number) => [...expertContentKeys.test(id), 'lifecycle'] as const,
  testQuestions: (testId: number) => [...expertContentKeys.test(testId), 'questions'] as const,
  activeFinalTest: (topicId: number) => [...expertContentKeys.topic(topicId), 'active-final-test'] as const,
  eligibleFinalTests: (topicId: number) => [...expertContentKeys.topic(topicId), 'eligible-final-tests'] as const,
};

export function useCourses(enabled = true) {
  return useQuery({ queryKey: expertContentKeys.courses(), queryFn: getCourses, enabled });
}

export function useCourse(courseId?: number, enabled = true) {
  return useQuery({
    queryKey: courseId ? expertContentKeys.course(courseId) : [...expertContentKeys.all, 'course', 'missing'],
    queryFn: () => getCourse(courseId as number),
    enabled: enabled && Boolean(courseId),
  });
}

export function useLifecycleCourse(courseId?: number, enabled = true) {
  return useQuery({
    queryKey: courseId
      ? expertContentKeys.lifecycleCourse(courseId)
      : [...expertContentKeys.all, 'course-lifecycle', 'missing'],
    queryFn: () => getLifecycleCourse(courseId as number),
    enabled: enabled && Boolean(courseId),
  });
}

export function useTopicsByCourse(courseId?: number, enabled = true) {
  return useQuery({
    queryKey: courseId ? expertContentKeys.topics(courseId) : [...expertContentKeys.all, 'topics', 'missing'],
    queryFn: () => getTopicsByCourse(courseId as number),
    enabled: enabled && Boolean(courseId),
  });
}

export function useTopic(topicId?: number, enabled = true) {
  return useQuery({
    queryKey: topicId ? expertContentKeys.topic(topicId) : [...expertContentKeys.all, 'topic', 'missing'],
    queryFn: () => getTopic(topicId as number),
    enabled: enabled && Boolean(topicId),
  });
}

export function useLifecycleTopic(topicId?: number, enabled = true) {
  return useQuery({
    queryKey: topicId
      ? expertContentKeys.lifecycleTopic(topicId)
      : [...expertContentKeys.all, 'topic-lifecycle', 'missing'],
    queryFn: () => getLifecycleTopic(topicId as number),
    enabled: enabled && Boolean(topicId),
  });
}

export function useMaterialsByTopic(topicId?: number, enabled = true) {
  return useQuery({
    queryKey: topicId ? expertContentKeys.materials(topicId) : [...expertContentKeys.all, 'materials', 'missing'],
    queryFn: () => getMaterialsByTopic(topicId as number),
    enabled: enabled && Boolean(topicId),
  });
}

export function useMaterial(materialId?: number, enabled = true) {
  return useQuery({
    queryKey: materialId ? expertContentKeys.material(materialId) : [...expertContentKeys.all, 'material', 'missing'],
    queryFn: () => getMaterial(materialId as number),
    enabled: enabled && Boolean(materialId),
  });
}

export function useLifecycleMaterial(materialId?: number, enabled = true) {
  return useQuery({
    queryKey: materialId
      ? expertContentKeys.lifecycleMaterial(materialId)
      : [...expertContentKeys.all, 'material-lifecycle', 'missing'],
    queryFn: () => getLifecycleMaterial(materialId as number),
    enabled: enabled && Boolean(materialId),
  });
}

export function useQuestionsByTopic(topicId?: number, enabled = true) {
  return useQuery({
    queryKey: topicId ? expertContentKeys.questions(topicId) : [...expertContentKeys.all, 'questions', 'missing'],
    queryFn: () => getQuestionsByTopic(topicId as number),
    enabled: enabled && Boolean(topicId),
  });
}

export function useQuestion(questionId?: number, enabled = true) {
  return useQuery({
    queryKey: questionId ? expertContentKeys.question(questionId) : [...expertContentKeys.all, 'question', 'missing'],
    queryFn: () => getQuestion(questionId as number),
    enabled: enabled && Boolean(questionId),
  });
}

export function useLifecycleQuestion(questionId?: number, enabled = true) {
  return useQuery({
    queryKey: questionId
      ? expertContentKeys.lifecycleQuestion(questionId)
      : [...expertContentKeys.all, 'question-lifecycle', 'missing'],
    queryFn: () => getLifecycleQuestion(questionId as number),
    enabled: enabled && Boolean(questionId),
  });
}

export function useQuestionAnswerOptions(questionId?: number, enabled = true) {
  return useQuery({
    queryKey: questionId
      ? expertContentKeys.answerOptions(questionId)
      : [...expertContentKeys.all, 'answer-options', 'missing'],
    queryFn: () => getQuestionAnswerOptions(questionId as number),
    enabled: enabled && Boolean(questionId),
  });
}

export function useTestsByTopic(topicId?: number, enabled = true) {
  return useQuery({
    queryKey: topicId ? expertContentKeys.tests(topicId) : [...expertContentKeys.all, 'tests', 'missing'],
    queryFn: () => getTestsByTopic(topicId as number),
    enabled: enabled && Boolean(topicId),
  });
}

export function useTest(testId?: number, enabled = true) {
  return useQuery({
    queryKey: testId ? expertContentKeys.test(testId) : [...expertContentKeys.all, 'test', 'missing'],
    queryFn: () => getTest(testId as number),
    enabled: enabled && Boolean(testId),
  });
}

export function useLifecycleTest(testId?: number, enabled = true) {
  return useQuery({
    queryKey: testId
      ? expertContentKeys.lifecycleTest(testId)
      : [...expertContentKeys.all, 'test-lifecycle', 'missing'],
    queryFn: () => getLifecycleTest(testId as number),
    enabled: enabled && Boolean(testId),
  });
}

export function useTestQuestions(testId?: number, enabled = true) {
  return useQuery({
    queryKey: testId ? expertContentKeys.testQuestions(testId) : [...expertContentKeys.all, 'test-questions', 'missing'],
    queryFn: () => getTestQuestions(testId as number),
    enabled: enabled && Boolean(testId),
  });
}

export function useActiveFinalTest(topicId?: number, enabled = true) {
  return useQuery({
    queryKey: topicId
      ? expertContentKeys.activeFinalTest(topicId)
      : [...expertContentKeys.all, 'active-final-test', 'missing'],
    queryFn: () => getActiveFinalTest(topicId as number),
    enabled: enabled && Boolean(topicId),
    retry: false,
  });
}

export function useEligibleFinalControlTests(topicId?: number, enabled = true) {
  return useQuery({
    queryKey: topicId
      ? expertContentKeys.eligibleFinalTests(topicId)
      : [...expertContentKeys.all, 'eligible-final-tests', 'missing'],
    queryFn: () => getEligibleFinalControlTests(topicId as number),
    enabled: enabled && Boolean(topicId),
  });
}

function useInvalidateExpertContent() {
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: expertContentKeys.all });
  };
}

export function useCreateCourseMutation() {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: (payload: SaveCourseRequest) => createCourse(payload), onSuccess: invalidate });
}

export function useUpdateCourseMutation(courseId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: SaveCourseRequest) => updateCourse(courseId as number, payload),
    onSuccess: invalidate,
  });
}

export function useCreateTopicMutation() {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: (payload: CreateTopicRequest) => createTopic(payload), onSuccess: invalidate });
}

export function useUpdateTopicMutation(topicId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: UpdateTopicRequest) => updateTopic(topicId as number, payload),
    onSuccess: invalidate,
  });
}

export function useCreateMaterialMutation() {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: CreateMaterialRequest) => createMaterial(payload),
    onSuccess: invalidate,
  });
}

export function useUpdateMaterialMutation(materialId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: UpdateMaterialRequest) => updateMaterial(materialId as number, payload),
    onSuccess: invalidate,
  });
}

export function useCreateQuestionMutation() {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: CreateQuestionRequest) => createQuestion(payload),
    onSuccess: invalidate,
  });
}

export function useUpdateQuestionMutation(questionId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: UpdateQuestionRequest) => updateQuestion(questionId as number, payload),
    onSuccess: invalidate,
  });
}

export function useCreateAnswerOptionMutation(questionId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: SaveAnswerOptionRequest) => createAnswerOption(questionId as number, payload),
    onSuccess: invalidate,
  });
}

export function useUpdateAnswerOptionMutation(questionId?: number, answerOptionId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: SaveAnswerOptionRequest) =>
      updateAnswerOption(questionId as number, answerOptionId as number, payload),
    onSuccess: invalidate,
  });
}

export function useDeleteAnswerOptionMutation(questionId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (answerOptionId: number) => deleteAnswerOption(questionId as number, answerOptionId),
    onSuccess: invalidate,
  });
}

export function useCreateTestMutation() {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: (payload: CreateTestRequest) => createTest(payload), onSuccess: invalidate });
}

export function useUpdateTestMutation(testId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: UpdateTestRequest) => updateTest(testId as number, payload),
    onSuccess: invalidate,
  });
}

export function useCreateTestQuestionMutation(testId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: SaveTestQuestionRequest) => createTestQuestion(testId as number, payload),
    onSuccess: invalidate,
  });
}

export function useUpdateTestQuestionMutation(testId?: number, testQuestionId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (payload: SaveTestQuestionRequest) =>
      updateTestQuestion(testId as number, testQuestionId as number, payload),
    onSuccess: invalidate,
  });
}

export function useDeleteTestQuestionMutation(testId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (testQuestionId: number) => deleteTestQuestion(testId as number, testQuestionId),
    onSuccess: invalidate,
  });
}

export function usePublishCourseMutation(courseId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => publishCourse(courseId as number), onSuccess: invalidate });
}

export function useArchiveCourseMutation(courseId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => archiveCourse(courseId as number), onSuccess: invalidate });
}

export function usePublishTopicMutation(topicId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => publishTopic(topicId as number), onSuccess: invalidate });
}

export function useArchiveTopicMutation(topicId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => archiveTopic(topicId as number), onSuccess: invalidate });
}

export function usePublishMaterialMutation(materialId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => publishMaterial(materialId as number), onSuccess: invalidate });
}

export function useArchiveMaterialMutation(materialId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => archiveMaterial(materialId as number), onSuccess: invalidate });
}

export function usePublishQuestionMutation(questionId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => publishQuestion(questionId as number), onSuccess: invalidate });
}

export function useArchiveQuestionMutation(questionId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => archiveQuestion(questionId as number), onSuccess: invalidate });
}

export function usePublishTestMutation(testId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => publishTest(testId as number), onSuccess: invalidate });
}

export function useArchiveTestMutation(testId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({ mutationFn: () => archiveTest(testId as number), onSuccess: invalidate });
}

export function useAssignActiveFinalTestMutation(topicId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (testId: number) => assignActiveFinalTest(topicId as number, testId),
    onSuccess: invalidate,
  });
}

export function useReplaceActiveFinalTestMutation(topicId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: (testId: number) => replaceActiveFinalTest(topicId as number, testId),
    onSuccess: invalidate,
  });
}

export function useClearActiveFinalTestMutation(topicId?: number) {
  const invalidate = useInvalidateExpertContent();
  return useMutation({
    mutationFn: () => clearActiveFinalTest(topicId as number),
    onSuccess: invalidate,
  });
}

export const lifecycleReaders = {
  getLifecycleCourse,
  getLifecycleTopic,
  getLifecycleMaterial,
  getLifecycleQuestion,
  getLifecycleTest,
};
