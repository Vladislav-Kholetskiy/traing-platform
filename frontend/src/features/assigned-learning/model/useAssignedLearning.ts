import { useQuery } from '@tanstack/react-query';
import {
  getAssignedLearningList,
  getAssignedMaterialContent,
  getAssignmentDetail,
  getAssignmentLearningContext,
  getAssignedTestContext,
} from '../api/assignedLearningApi';

export const assignedLearningQueryKeys = {
  all: ['assigned-learning'] as const,
  list: () => [...assignedLearningQueryKeys.all, 'list'] as const,
  detail: (assignmentId: string | number) =>
    [...assignedLearningQueryKeys.all, 'detail', String(assignmentId)] as const,
  learningContext: (assignmentId: string | number) =>
    [...assignedLearningQueryKeys.all, 'learning-context', String(assignmentId)] as const,
  materialContent: (assignmentId: string | number, materialId: string | number) =>
    [...assignedLearningQueryKeys.all, 'material-content', String(assignmentId), String(materialId)] as const,
  testContext: (assignmentId: string | number, assignmentTestId: string | number) =>
    [...assignedLearningQueryKeys.all, 'test-context', String(assignmentId), String(assignmentTestId)] as const,
};

export function useAssignedLearningList(enabled = true) {
  return useQuery({
    queryKey: assignedLearningQueryKeys.list(),
    queryFn: getAssignedLearningList,
    enabled,
  });
}

export function useAssignmentDetail(assignmentId?: string) {
  return useQuery({
    queryKey: assignedLearningQueryKeys.detail(assignmentId ?? 'unknown'),
    queryFn: () => getAssignmentDetail(assignmentId as string),
    enabled: Boolean(assignmentId),
  });
}

export function useAssignmentLearningContext(assignmentId?: string) {
  return useQuery({
    queryKey: assignedLearningQueryKeys.learningContext(assignmentId ?? 'unknown'),
    queryFn: () => getAssignmentLearningContext(assignmentId as string),
    enabled: Boolean(assignmentId),
  });
}

export function useAssignedTestContext(assignmentId?: string, assignmentTestId?: string) {
  return useQuery({
    queryKey: assignedLearningQueryKeys.testContext(
      assignmentId ?? 'unknown',
      assignmentTestId ?? 'unknown',
    ),
    queryFn: () => getAssignedTestContext(assignmentId as string, assignmentTestId as string),
    enabled: Boolean(assignmentId && assignmentTestId),
  });
}

export function useAssignedMaterialContent(assignmentId?: string, materialId?: string) {
  return useQuery({
    queryKey: assignedLearningQueryKeys.materialContent(
      assignmentId ?? 'unknown',
      materialId ?? 'unknown',
    ),
    queryFn: () => getAssignedMaterialContent(assignmentId as string, materialId as string),
    enabled: Boolean(assignmentId && materialId),
  });
}
